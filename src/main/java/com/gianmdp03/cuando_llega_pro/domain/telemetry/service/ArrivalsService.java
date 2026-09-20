package com.gianmdp03.cuando_llega_pro.domain.telemetry.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gianmdp03.cuando_llega_pro.client.MgpProxyClient;
import com.gianmdp03.cuando_llega_pro.config.CaffeineCacheConfig;
import com.gianmdp03.cuando_llega_pro.domain.telemetry.dto.ArrivalResponseDTO;
import com.gianmdp03.cuando_llega_pro.domain.telemetry.dto.BusArrivalItemDTO;
import com.gianmdp03.cuando_llega_pro.domain.telemetry.model.RawMgpArrivalResponse;
import com.gianmdp03.cuando_llega_pro.domain.telemetry.model.TelemetryStatus;
import com.gianmdp03.cuando_llega_pro.exception.UpstreamServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service orchestrating transit telemetry ingestion, Caffeine L1 caching,
 * and high-availability predictive fallback extrapolation via {@link ExtrapolationEngine}.
 */
@Service
public class ArrivalsService {

    private static final Logger log = LoggerFactory.getLogger(ArrivalsService.class);
    private static final String UPSTREAM_ACTION = "RecuperarProximosArribosW";

    private final MgpProxyClient mgpProxyClient;
    private final ObjectMapper objectMapper;
    private final ExtrapolationEngine extrapolationEngine;
    private final CacheManager cacheManager;
    private final TelemetryMapper telemetryMapper;

    /**
     * Fallback in-memory store retaining the last confirmed telemetry snapshot per stop and line.
     * When upstream fails, times out, or returns malformed payloads, snapshots are supplied
     * to the {@link ExtrapolationEngine} for accurate ETA degradation.
     */
    private final ConcurrentHashMap<String, ArrivalResponseDTO> lastKnownTelemetryStore = new ConcurrentHashMap<>();

    public ArrivalsService(
            MgpProxyClient mgpProxyClient,
            ObjectMapper objectMapper,
            ExtrapolationEngine extrapolationEngine,
            CacheManager cacheManager
    ) {
        this(mgpProxyClient, objectMapper, extrapolationEngine, cacheManager, new TelemetryMapper(objectMapper));
    }

    @Autowired
    public ArrivalsService(
            MgpProxyClient mgpProxyClient,
            ObjectMapper objectMapper,
            ExtrapolationEngine extrapolationEngine,
            CacheManager cacheManager,
            TelemetryMapper telemetryMapper
    ) {
        this.mgpProxyClient = mgpProxyClient;
        this.objectMapper = objectMapper;
        this.extrapolationEngine = extrapolationEngine;
        this.cacheManager = cacheManager;
        this.telemetryMapper = telemetryMapper != null ? telemetryMapper : new TelemetryMapper(objectMapper);
    }

    /**
     * Retrieves arrival predictions for a specific bus stop and line code, filtered by preset branch.
     *
     * @param lineCode      bus line identifier (e.g. "511", "522")
     * @param stopId        bus stop identifier
     * @param targetBandera route branch / variant (bandera) criteria (null or blank includes all)
     * @return normalized arrival response DTO filtered by branch
     */
    public ArrivalResponseDTO getArrivals(String lineCode, String stopId, String targetBandera) {
        ArrivalResponseDTO response = getArrivals(lineCode, stopId);
        if (targetBandera == null || targetBandera.isBlank()) {
            return response;
        }
        List<BusArrivalItemDTO> filtered = telemetryMapper.filterBusArrivalsByBranch(response.arrivals(), targetBandera);
        return new ArrivalResponseDTO(
                response.lineCode(),
                response.stopId(),
                targetBandera.trim(),
                response.status(),
                response.timestamp(),
                response.deltaMinutes(),
                filtered,
                response.stopLatitude(),
                response.stopLongitude()
        );
    }

    /**
     * Retrieves arrival predictions for a specific bus stop and line code.
     * <ol>
     *   <li>Inspects Caffeine L1 cache ("arrivals", 15s TTL) for key {@code stopId + ":" + lineCode}.</li>
     *   <li>On cache miss, executes a live query via {@link MgpProxyClient}.</li>
     *   <li>Parses upstream payload, normalizes to {@link ArrivalResponseDTO} with status LIVE, and updates caches.</li>
     *   <li>On upstream failure (timeout, 502, network error, bad JSON), falls back to {@link ExtrapolationEngine}
     *       using the last known telemetry from {@link #lastKnownTelemetryStore}.</li>
     *   <li>If no previous telemetry is present, throws {@link UpstreamServiceException}.</li>
     * </ol>
     *
     * @param lineCode bus line identifier (e.g. "511", "522")
     * @param stopId   bus stop identifier
     * @return normalized arrival response DTO
     * @throws UpstreamServiceException if upstream fails and no fallback telemetry is stored
     * @throws IllegalArgumentException if lineCode or stopId is null or blank
     */
    public ArrivalResponseDTO getArrivals(String lineCode, String stopId) {
        if (lineCode == null || lineCode.isBlank()) {
            throw new IllegalArgumentException("lineCode must not be null or blank");
        }
        if (stopId == null || stopId.isBlank()) {
            throw new IllegalArgumentException("stopId must not be null or blank");
        }

        String cacheKey = stopId + ":" + lineCode;

        // 1. Check Caffeine L1 cache
        Cache arrivalsCache = cacheManager.getCache(CaffeineCacheConfig.ARRIVALS_CACHE);
        if (arrivalsCache != null) {
            Cache.ValueWrapper wrapper = arrivalsCache.get(cacheKey);
            if (wrapper != null && wrapper.get() instanceof ArrivalResponseDTO cachedLive) {
                log.debug("Caffeine cache hit for key: {}", cacheKey);
                return cachedLive;
            }
        }

        // 2. Query upstream Go proxy
        try {
            String requestId = UUID.randomUUID().toString();
            log.debug("Fetching live telemetry from upstream proxy for stopId={} lineCode={} (requestId={})",
                    stopId, lineCode, requestId);

            String rawJson = mgpProxyClient.getArrivals(requestId, UPSTREAM_ACTION, stopId, lineCode);

            if (rawJson == null || rawJson.isBlank()) {
                throw new UpstreamServiceException("Empty response payload received from upstream proxy for stop: "
                        + stopId + ", line: " + lineCode);
            }

            List<com.gianmdp03.cuando_llega_pro.domain.telemetry.dto.MgpArriboRaw> rawList =
                    telemetryMapper.parseRawArrivals(rawJson);

            List<BusArrivalItemDTO> items = rawList.stream()
                    .map(rawItem -> telemetryMapper.toBusArrivalItemDTO(rawItem, TelemetryStatus.LIVE, lineCode))
                    .toList();

            String branch = items.isEmpty() ? null : items.getFirst().branch();

            Double stopLatitude = null;
            Double stopLongitude = null;
            if (!rawList.isEmpty()) {
                com.gianmdp03.cuando_llega_pro.domain.telemetry.dto.MgpArriboRaw firstRaw = rawList.getFirst();
                stopLatitude = telemetryMapper.parseCoordinate(firstRaw.latitudParada());
                stopLongitude = telemetryMapper.parseCoordinate(firstRaw.longitudParada());
            }

            ArrivalResponseDTO liveResponse = new ArrivalResponseDTO(
                    lineCode,
                    stopId,
                    branch,
                    TelemetryStatus.LIVE,
                    Instant.now(),
                    0L,
                    items,
                    stopLatitude,
                    stopLongitude
            );

            // Populate Caffeine L1 cache
            if (arrivalsCache != null) {
                arrivalsCache.put(cacheKey, liveResponse);
            }

            // Persist to fallback store
            lastKnownTelemetryStore.put(cacheKey, liveResponse);

            return liveResponse;

        } catch (Exception ex) {
            log.warn("Upstream proxy error for stopId={} lineCode={}: {}. Checking fallback telemetry store.",
                    stopId, lineCode, ex.getMessage());

            ArrivalResponseDTO lastKnown = lastKnownTelemetryStore.get(cacheKey);
            if (lastKnown != null) {
                log.info("Degrading to extrapolated fallback telemetry for key: {}", cacheKey);
                return extrapolationEngine.extrapolate(lastKnown, Instant.now());
            }

            throw new UpstreamServiceException(
                    "Upstream proxy failed and no cached telemetry exists for stop: " + stopId + ", line: " + lineCode,
                    ex
            );
        }
    }

    /**
     * Retrieves the internal fallback telemetry map.
     *
     * @return map of last known telemetry records
     */
    public ConcurrentHashMap<String, ArrivalResponseDTO> getLastKnownTelemetryStore() {
        return lastKnownTelemetryStore;
    }
}
