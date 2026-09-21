package com.gianmdp03.cuando_llega_pro.domain.telemetry.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gianmdp03.cuando_llega_pro.client.MgpProxyClient;
import com.gianmdp03.cuando_llega_pro.config.CaffeineCacheConfig;
import com.gianmdp03.cuando_llega_pro.domain.telemetry.dto.ArrivalResponseDTO;
import com.gianmdp03.cuando_llega_pro.domain.telemetry.dto.BusArrivalItemDTO;
import com.gianmdp03.cuando_llega_pro.domain.telemetry.dto.MgpArriboRaw;
import com.gianmdp03.cuando_llega_pro.domain.telemetry.model.TelemetryStatus;
import com.gianmdp03.cuando_llega_pro.domain.transit.StopLocationEntity;
import com.gianmdp03.cuando_llega_pro.domain.transit.StopLocationRepository;
import com.gianmdp03.cuando_llega_pro.domain.transit.TransitLineResolver;
import com.gianmdp03.cuando_llega_pro.exception.UpstreamServiceException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service orchestrating transit telemetry ingestion, Caffeine L1 caching, cache stampede prevention,
 * vehicle-unit tracking buffer by IdentificadorCoche, dead reckoning extrapolation, passive stop location harvesting,
 * and Resilience4j Circuit Breaker protection.
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
    private final TransitLineResolver transitLineResolver;
    private final StopLocationRepository stopLocationRepository;
    private final CircuitBreaker circuitBreaker;

    /**
     * Fallback in-memory store retaining the last confirmed telemetry snapshot per stop and line.
     */
    private final ConcurrentHashMap<String, ArrivalResponseDTO> lastKnownTelemetryStore = new ConcurrentHashMap<>();

    /**
     * In-memory buffer tracking active vehicle units deterministically by IdentificadorCoche per stop and line.
     * Maps: "stopId:lineCode" -> (identificadorCoche -> BusArrivalItemDTO)
     */
    private final ConcurrentHashMap<String, ConcurrentHashMap<String, BusArrivalItemDTO>> vehicleTrackingBuffer = new ConcurrentHashMap<>();

    /**
     * In-flight requests registry coalescing concurrent cache-miss requests for the same cache key.
     */
    private final ConcurrentHashMap<String, CompletableFuture<ArrivalResponseDTO>> inFlightRequests = new ConcurrentHashMap<>();

    public ArrivalsService(
            MgpProxyClient mgpProxyClient,
            ObjectMapper objectMapper,
            ExtrapolationEngine extrapolationEngine,
            CacheManager cacheManager
    ) {
        this(mgpProxyClient, objectMapper, extrapolationEngine, cacheManager, new TelemetryMapper(objectMapper), new TransitLineResolver(), null, null);
    }

    public ArrivalsService(
            MgpProxyClient mgpProxyClient,
            ObjectMapper objectMapper,
            ExtrapolationEngine extrapolationEngine,
            CacheManager cacheManager,
            TelemetryMapper telemetryMapper,
            TransitLineResolver transitLineResolver
    ) {
        this(mgpProxyClient, objectMapper, extrapolationEngine, cacheManager, telemetryMapper, transitLineResolver, null, null);
    }

    public ArrivalsService(
            MgpProxyClient mgpProxyClient,
            ObjectMapper objectMapper,
            ExtrapolationEngine extrapolationEngine,
            CacheManager cacheManager,
            TelemetryMapper telemetryMapper,
            TransitLineResolver transitLineResolver,
            StopLocationRepository stopLocationRepository
    ) {
        this(mgpProxyClient, objectMapper, extrapolationEngine, cacheManager, telemetryMapper, transitLineResolver, stopLocationRepository, null);
    }

    @Autowired
    public ArrivalsService(
            MgpProxyClient mgpProxyClient,
            ObjectMapper objectMapper,
            ExtrapolationEngine extrapolationEngine,
            CacheManager cacheManager,
            TelemetryMapper telemetryMapper,
            TransitLineResolver transitLineResolver,
            @Autowired(required = false) StopLocationRepository stopLocationRepository,
            @Autowired(required = false) CircuitBreaker circuitBreaker
    ) {
        this.mgpProxyClient = mgpProxyClient;
        this.objectMapper = objectMapper;
        this.extrapolationEngine = extrapolationEngine;
        this.cacheManager = cacheManager;
        this.telemetryMapper = telemetryMapper != null ? telemetryMapper : new TelemetryMapper(objectMapper);
        this.transitLineResolver = transitLineResolver != null ? transitLineResolver : new TransitLineResolver();
        this.stopLocationRepository = stopLocationRepository;
        this.circuitBreaker = circuitBreaker != null ? circuitBreaker : CircuitBreaker.ofDefaults("mgpUpstream");
    }

    /**
     * Retrieves arrival predictions for a specific bus stop and line code, filtered by preset branch.
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
     * Integrates dead reckoning tracking buffer, cache stampede coalescence, and passive stop location learning.
     */
    public ArrivalResponseDTO getArrivals(String lineCode, String stopId) {
        if (lineCode == null || lineCode.isBlank()) {
            throw new IllegalArgumentException("lineCode must not be null or blank");
        }
        if (stopId == null || stopId.isBlank()) {
            throw new IllegalArgumentException("stopId must not be null or blank");
        }

        // 1. Resolver código comercial a código interno upstream ("511" -> "98")
        String internalLineCode = transitLineResolver.toInternalCode(lineCode);
        String cacheKey = stopId.trim() + ":" + internalLineCode;

        // 2. Check Caffeine L1 cache (Live primero, Fallback reciente después)
        Cache arrivalsCache = cacheManager.getCache(CaffeineCacheConfig.ARRIVALS_CACHE);
        if (arrivalsCache != null) {
            Cache.ValueWrapper wrapper = arrivalsCache.get(cacheKey);
            if (wrapper != null && wrapper.get() instanceof ArrivalResponseDTO cachedLive) {
                log.debug("Caffeine live cache hit for key: {}", cacheKey);
                return cachedLive;
            }
        }

        Cache fallbackCache = cacheManager.getCache(CaffeineCacheConfig.FALLBACK_ARRIVALS_CACHE);
        if (fallbackCache != null) {
            Cache.ValueWrapper wrapper = fallbackCache.get(cacheKey);
            if (wrapper != null && wrapper.get() instanceof ArrivalResponseDTO cachedFallback) {
                log.debug("Caffeine fallback cache hit for key: {}", cacheKey);
                return extrapolationEngine.extrapolate(cachedFallback, Instant.now());
            }
        }

        // 3. Singleflight Coalescing contra Cache Stampede
        CompletableFuture<ArrivalResponseDTO> myFuture = new CompletableFuture<>();
        CompletableFuture<ArrivalResponseDTO> existing = inFlightRequests.putIfAbsent(cacheKey, myFuture);
        if (existing != null) {
            try {
                log.debug("Coalescing concurrent request for cacheKey: {}", cacheKey);
                return existing.join();
            } catch (CompletionException ce) {
                if (ce.getCause() instanceof RuntimeException re) {
                    throw re;
                }
                throw new UpstreamServiceException("Coalesced in-flight request failed for " + cacheKey, ce.getCause());
            }
        }

        try {
            ArrivalResponseDTO response = fetchArrivalsFromUpstream(cacheKey, lineCode, stopId, internalLineCode, arrivalsCache, fallbackCache);
            myFuture.complete(response);
            return response;
        } catch (Throwable t) {
            myFuture.completeExceptionally(t);
            throw t;
        } finally {
            inFlightRequests.remove(cacheKey, myFuture);
        }
    }

    private ArrivalResponseDTO fetchArrivalsFromUpstream(
            String cacheKey,
            String lineCode,
            String stopId,
            String internalLineCode,
            Cache arrivalsCache,
            Cache fallbackCache
    ) {
        try {
            String requestId = UUID.randomUUID().toString();
            log.debug("Fetching live telemetry: stopId={}, lineCode={}, internalCode={}, reqId={}",
                    stopId, lineCode, internalLineCode, requestId);

            // Llamada protegida por el Circuit Breaker
            String rawJson = circuitBreaker.executeSupplier(() ->
                    mgpProxyClient.getArrivals(requestId, UPSTREAM_ACTION, stopId.trim(), internalLineCode)
            );

            if (rawJson == null || rawJson.isBlank()) {
                throw new UpstreamServiceException("Empty response payload received from upstream proxy for stop: "
                        + stopId + ", line: " + lineCode);
            }

            List<MgpArriboRaw> rawList = telemetryMapper.parseRawArrivals(rawJson);

            List<BusArrivalItemDTO> liveItems = rawList.stream()
                    .map(rawItem -> telemetryMapper.toBusArrivalItemDTO(rawItem, TelemetryStatus.LIVE, lineCode))
                    .toList();

            ConcurrentHashMap<String, BusArrivalItemDTO> trackedUnits =
                    vehicleTrackingBuffer.computeIfAbsent(cacheKey, k -> new ConcurrentHashMap<>());

            Set<String> activeUnitsInPoll = new HashSet<>();
            List<BusArrivalItemDTO> consolidatedItems = new ArrayList<>();

            for (BusArrivalItemDTO liveItem : liveItems) {
                String unitId = liveItem.vehicleUnit();
                BusArrivalItemDTO processedItem = liveItem;

                if (unitId != null && !unitId.isBlank()) {
                    String cleanUnitId = unitId.trim();
                    activeUnitsInPoll.add(cleanUnitId);

                    BusArrivalItemDTO previous = trackedUnits.get(cleanUnitId);
                    Double bearing = null;
                    Double speedKmH = null;

                    if (previous != null && previous.latitude() != null && previous.longitude() != null && previous.timestamp() != null
                            && liveItem.latitude() != null && liveItem.longitude() != null && liveItem.timestamp() != null) {
                        double deltaD = calculateHaversineMeters(
                                previous.latitude(), previous.longitude(),
                                liveItem.latitude(), liveItem.longitude()
                        );
                        long deltaT = Duration.between(previous.timestamp(), liveItem.timestamp()).toSeconds();

                        if (deltaD >= 15.0 && deltaT > 0) {
                            speedKmH = (deltaD / (double) deltaT) * 3.6;
                            double lat1 = Math.toRadians(previous.latitude());
                            double lat2 = Math.toRadians(liveItem.latitude());
                            double dLon = Math.toRadians(liveItem.longitude() - previous.longitude());
                            double y = Math.sin(dLon) * Math.cos(lat2);
                            double x = Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1) * Math.cos(lat2) * Math.cos(dLon);
                            bearing = (Math.toDegrees(Math.atan2(y, x)) + 360.0) % 360.0;
                        } else if (deltaD < 15.0) {
                            bearing = previous.bearing();
                            speedKmH = 0.0;
                        } else {
                            bearing = previous.bearing();
                            speedKmH = previous.speedKmH();
                        }
                    }

                    processedItem = liveItem.withBearingAndSpeed(bearing, speedKmH);
                    trackedUnits.put(cleanUnitId, processedItem);
                }

                consolidatedItems.add(processedItem);
            }

            Instant now = Instant.now();
            for (Map.Entry<String, BusArrivalItemDTO> entry : trackedUnits.entrySet()) {
                String unitId = entry.getKey();
                if (!activeUnitsInPoll.contains(unitId)) {
                    BusArrivalItemDTO lastConfirmed = entry.getValue();
                    BusArrivalItemDTO deadReckoned = extrapolationEngine.extrapolateVehicle(lastConfirmed, now);

                    if (deadReckoned != null
                            && deadReckoned.status() == TelemetryStatus.ESTIMATED_FALLBACK
                            && deadReckoned.remainingMinutes() != null
                            && deadReckoned.remainingMinutes() >= 0) {

                        boolean discardZeroMinute = deadReckoned.remainingMinutes() == 0
                                && (lastConfirmed.timestamp() == null
                                || Duration.between(lastConfirmed.timestamp(), now).toSeconds() > 180);

                        if (discardZeroMinute) {
                            trackedUnits.remove(unitId);
                        } else {
                            consolidatedItems.add(deadReckoned);
                        }
                    } else {
                        trackedUnits.remove(unitId);
                    }
                }
            }

            consolidatedItems.sort(Comparator.comparing(
                    item -> item.remainingMinutes() != null ? item.remainingMinutes() : Integer.MAX_VALUE
            ));

            String branch = consolidatedItems.isEmpty() ? null : consolidatedItems.getFirst().branch();

            Double stopLatitude = null;
            Double stopLongitude = null;
            for (MgpArriboRaw rawItem : rawList) {
                if (rawItem.latitudParada() != null && !rawItem.latitudParada().isBlank()
                        && rawItem.longitudParada() != null && !rawItem.longitudParada().isBlank()) {
                    Double lat = telemetryMapper.parseCoordinate(rawItem.latitudParada());
                    Double lon = telemetryMapper.parseCoordinate(rawItem.longitudParada());
                    if (lat != null && lon != null) {
                        stopLatitude = lat;
                        stopLongitude = lon;
                        harvestStopLocationAsync(stopId, stopLatitude, stopLongitude);
                        break;
                    }
                }
            }

            if ((stopLatitude == null || stopLongitude == null) && stopLocationRepository != null) {
                var cachedLoc = stopLocationRepository.findById(stopId);
                if (cachedLoc.isEmpty() && stopId != null) {
                    cachedLoc = stopLocationRepository.findById(stopId.trim());
                }
                if (cachedLoc.isPresent()) {
                    StopLocationEntity entity = cachedLoc.get();
                    if (stopLatitude == null) {
                        stopLatitude = entity.getLatitude();
                    }
                    if (stopLongitude == null) {
                        stopLongitude = entity.getLongitude();
                    }
                }
            }

            ArrivalResponseDTO liveResponse = new ArrivalResponseDTO(
                    lineCode,
                    stopId,
                    branch,
                    TelemetryStatus.LIVE,
                    Instant.now(),
                    0L,
                    consolidatedItems,
                    stopLatitude,
                    stopLongitude
            );

            if (arrivalsCache != null) {
                arrivalsCache.put(cacheKey, liveResponse);
            }

            if (fallbackCache != null) {
                fallbackCache.evict(cacheKey);
            }

            lastKnownTelemetryStore.put(cacheKey, liveResponse);

            return liveResponse;

        } catch (CallNotPermittedException ex) {
            log.warn("Circuit Breaker OPEN for stopId={} lineCode={}. Fast-failing to fallback.", stopId, lineCode);
            return resolveFallback(cacheKey, stopId, lineCode, fallbackCache, ex);
        } catch (Exception ex) {
            log.warn("Upstream proxy error for stopId={} lineCode={}: {}. Checking fallback telemetry store.",
                    stopId, lineCode, ex.getMessage());
            return resolveFallback(cacheKey, stopId, lineCode, fallbackCache, ex);
        }
    }

    private ArrivalResponseDTO resolveFallback(
            String cacheKey,
            String stopId,
            String lineCode,
            Cache fallbackCache,
            Exception ex
    ) {
        ArrivalResponseDTO lastKnown = lastKnownTelemetryStore.get(cacheKey);
        if (lastKnown != null) {
            log.info("Degrading to extrapolated fallback telemetry for key: {}", cacheKey);
            ArrivalResponseDTO degraded = extrapolationEngine.extrapolate(lastKnown, Instant.now());

            if (fallbackCache != null) {
                fallbackCache.put(cacheKey, lastKnown);
            }

            return degraded;
        }

        throw new UpstreamServiceException(
                "Upstream proxy failed and no cached telemetry exists for stop: " + stopId + ", line: " + lineCode,
                ex
        );
    }

    private void harvestStopLocationAsync(String stopId, Double latitude, Double longitude) {
        if (stopLocationRepository == null || stopId == null || latitude == null || longitude == null) {
            return;
        }
        CompletableFuture.runAsync(() -> {
            try {
                stopLocationRepository.save(new StopLocationEntity(stopId.trim(), latitude, longitude, Instant.now()));
                log.debug("Passively harvested stop coordinates: stopId={}, lat={}, lon={}", stopId, latitude, longitude);
            } catch (Exception ex) {
                log.warn("Failed to harvest stop coordinates for stopId {}: {}", stopId, ex.getMessage());
            }
        });
    }

    @Scheduled(fixedRate = 60000)
    public void cleanupStaleTrackedVehicles() {
        Instant cutoff = Instant.now().minus(Duration.ofMinutes(30));
        vehicleTrackingBuffer.forEach((stopLineKey, trackedUnits) -> {
            trackedUnits.entrySet().removeIf(entry -> {
                BusArrivalItemDTO item = entry.getValue();
                return item == null || item.timestamp() == null || item.timestamp().isBefore(cutoff);
            });
            if (trackedUnits.isEmpty()) {
                vehicleTrackingBuffer.remove(stopLineKey, trackedUnits);
            }
        });
        log.debug("Cleaned up stale vehicle units older than 30 minutes");
    }

    public ConcurrentHashMap<String, ArrivalResponseDTO> getLastKnownTelemetryStore() {
        return lastKnownTelemetryStore;
    }

    public ConcurrentHashMap<String, ConcurrentHashMap<String, BusArrivalItemDTO>> getVehicleTrackingBuffer() {
        return vehicleTrackingBuffer;
    }

    private double calculateHaversineMeters(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371000.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double rLat1 = Math.toRadians(lat1);
        double rLat2 = Math.toRadians(lat2);
        double a = Math.sin(dLat / 2.0) * Math.sin(dLat / 2.0)
                + Math.cos(rLat1) * Math.cos(rLat2) * Math.sin(dLon / 2.0) * Math.sin(dLon / 2.0);
        double c = 2.0 * Math.atan2(Math.sqrt(a), Math.sqrt(1.0 - a));
        return R * c;
    }
}