package com.gianmdp03.cuando_llega_pro.domain.transit;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gianmdp03.cuando_llega_pro.client.MgpProxyClient;
import com.gianmdp03.cuando_llega_pro.config.CaffeineCacheConfig;
import com.gianmdp03.cuando_llega_pro.domain.transit.dto.TransitIntersectionDTO;
import com.gianmdp03.cuando_llega_pro.domain.transit.dto.TransitLineDTO;
import com.gianmdp03.cuando_llega_pro.domain.transit.dto.TransitStopWithFlagDTO;
import com.gianmdp03.cuando_llega_pro.domain.transit.dto.TransitStreetDTO;
import com.gianmdp03.cuando_llega_pro.exception.UpstreamServiceException;
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
import java.util.List;
import java.util.Comparator;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TransitCatalogService {
    private static final Logger log = LoggerFactory.getLogger(TransitCatalogService.class);

    public static final String CACHE_KEY_LINES_ALL = "lines:all";
    public static final String ACTION_RECUPERAR_LINEAS_FALLBACK = "RecuperarLineaPorCuandoLlega";
    public static final String ACTION_RECUPERAR_CALLES_PRINCIPAL = "RecuperarCallesPrincipalPorLinea";
    public static final String ACTION_RECUPERAR_INTERSECCION = "RecuperarInterseccionPorLineaYCalle";
    public static final String ACTION_RECUPERAR_PARADAS_BANDERA = "RecuperarParadasConBanderaPorLineaCalleEInterseccion";

    private final MgpProxyClient mgpProxyClient;
    private final TransitCatalogRepository catalogRepository;
    private final CacheManager cacheManager;
    private final ObjectMapper objectMapper;
    private final TransitLineResolver transitLineResolver;
    private final StopLocationRepository stopLocationRepository;
    private final Duration catalogTtl;
    private final ConcurrentHashMap<String, CompletableFuture<Object>> inFlightFetches = new ConcurrentHashMap<>();

    @Autowired
    public TransitCatalogService(
            MgpProxyClient mgpProxyClient,
            TransitCatalogRepository catalogRepository,
            CacheManager cacheManager,
            ObjectMapper objectMapper,
            TransitLineResolver transitLineResolver,
            @Autowired(required = false) StopLocationRepository stopLocationRepository,
            @org.springframework.beans.factory.annotation.Value("${app.cache.transit-catalog.ttl-hours:24}") long catalogTtlHours
    ) {
        this.mgpProxyClient = mgpProxyClient;
        this.catalogRepository = catalogRepository;
        this.cacheManager = cacheManager;
        this.objectMapper = objectMapper;
        this.transitLineResolver = transitLineResolver != null ? transitLineResolver : new TransitLineResolver();
        this.stopLocationRepository = stopLocationRepository;
        if (catalogTtlHours < 1) {
            throw new IllegalArgumentException("app.cache.transit-catalog.ttl-hours must be at least one");
        }
        this.catalogTtl = Duration.ofHours(catalogTtlHours);
    }

    public TransitCatalogService(
            MgpProxyClient mgpProxyClient,
            TransitCatalogRepository catalogRepository,
            CacheManager cacheManager,
            ObjectMapper objectMapper,
            TransitLineResolver transitLineResolver
    ) {
        this(mgpProxyClient, catalogRepository, cacheManager, objectMapper, transitLineResolver, null, 24);
    }

    public TransitCatalogService(
            MgpProxyClient mgpProxyClient,
            ObjectMapper objectMapper,
            TransitCatalogRepository catalogRepository,
            TransitLineResolver transitLineResolver
    ) {
        this(mgpProxyClient, catalogRepository, null, objectMapper, transitLineResolver, null, 24);
    }

    public List<TransitLineDTO> getLines() {
        return getOrFetch(CACHE_KEY_LINES_ALL, "LINES", new TypeReference<List<TransitLineDTO>>() {}, () -> {
            String raw = mgpProxyClient.getLines(UUID.randomUUID().toString(), ACTION_RECUPERAR_LINEAS_FALLBACK);
            JsonNode root = objectMapper.readTree(raw);
            JsonNode array = root.hasNonNull("lineas") ? root.get("lineas") : root;
            List<TransitLineDTO> list = new ArrayList<>();
            if (array != null && array.isArray()) {
                for (JsonNode n : array) {
                    list.add(new TransitLineDTO(
                            n.path("CodigoLineaParada").asText(),
                            n.path("Descripcion").asText(),
                            n.path("CodigoEntidad").asText(),
                            n.path("CodigoEmpresa").asInt(0)
                    ));
                }
            }
            return list;
        });
    }

    public List<TransitStreetDTO> getMainStreetsByLine(String lineCode) {
        String internalLine = transitLineResolver.toInternalCode(lineCode);
        String cacheKey = "streets:" + internalLine;
        return getOrFetch(cacheKey, "STREETS", new TypeReference<List<TransitStreetDTO>>() {}, () -> {
            String raw = mgpProxyClient.getMainStreetsByLine(UUID.randomUUID().toString(), ACTION_RECUPERAR_CALLES_PRINCIPAL, internalLine);
            JsonNode root = objectMapper.readTree(raw);
            JsonNode array = root.hasNonNull("calles") ? root.get("calles") : root;
            List<TransitStreetDTO> list = new ArrayList<>();
            if (array != null && array.isArray()) {
                for (JsonNode n : array) {
                    list.add(new TransitStreetDTO(n.path("Codigo").asText(), stripCitySuffix(n.path("Descripcion").asText())));
                }
            }
            list.sort(Comparator.comparing(TransitStreetDTO::descripcion));
            return list;
        });
    }

    public List<TransitIntersectionDTO> getIntersectionsByLineAndStreet(String lineCode, String streetCode) {
        String internalLine = transitLineResolver.toInternalCode(lineCode);
        String cacheKey = "intersections:" + internalLine + ":" + streetCode;
        return getOrFetch(cacheKey, "INTERSECTIONS", new TypeReference<List<TransitIntersectionDTO>>() {}, () -> {
            String raw = mgpProxyClient.getIntersectionsByLineAndStreet(
                    UUID.randomUUID().toString(), ACTION_RECUPERAR_INTERSECCION, internalLine, streetCode
            );
            JsonNode root = objectMapper.readTree(raw);
            JsonNode array = root.hasNonNull("calles") ? root.get("calles") : root;
            List<TransitIntersectionDTO> list = new ArrayList<>();
            if (array != null && array.isArray()) {
                for (JsonNode n : array) {
                    list.add(new TransitIntersectionDTO(n.path("Codigo").asText(), stripCitySuffix(n.path("Descripcion").asText())));
                }
            }
            list.sort(Comparator.comparing(TransitIntersectionDTO::descripcion));
            return list;
        });
    }

    public List<TransitStopWithFlagDTO> getStopsWithFlag(String lineCode, String streetCode, String intersectionCode) {
        String internalLine = transitLineResolver.toInternalCode(lineCode);
        String cacheKey = "stops_flag:" + internalLine + ":" + streetCode + ":" + intersectionCode;
        return getOrFetch(cacheKey, "STOPS_FLAG", new TypeReference<List<TransitStopWithFlagDTO>>() {}, () -> {
            String raw = mgpProxyClient.getStopsWithFlag(
                    UUID.randomUUID().toString(), ACTION_RECUPERAR_PARADAS_BANDERA,
                    internalLine, streetCode, intersectionCode
            );
            JsonNode root = objectMapper.readTree(raw);
            JsonNode array = root.hasNonNull("paradas") ? root.get("paradas") : root;
            List<TransitStopWithFlagDTO> list = new ArrayList<>();
            if (array != null && array.isArray()) {
                for (JsonNode n : array) {
                    String codigo = n.path("Codigo").asText();
                    String identificador = n.path("Identificador").asText();
                    Double lat = n.hasNonNull("LatitudParada") ? n.get("LatitudParada").asDouble() : null;
                    Double lon = n.hasNonNull("LongitudParada") ? n.get("LongitudParada").asDouble() : null;

                    if ((lat == null || lon == null) && stopLocationRepository != null) {
                        var loc = (identificador != null && !identificador.isBlank())
                                ? stopLocationRepository.findById(identificador).orElse(null)
                                : null;
                        if (loc == null && codigo != null && !codigo.isBlank()) {
                            loc = stopLocationRepository.findById(codigo).orElse(null);
                        }
                        if (loc != null) {
                            if (lat == null) lat = loc.getLatitude();
                            if (lon == null) lon = loc.getLongitude();
                        }
                    }

                    list.add(new TransitStopWithFlagDTO(
                            codigo,
                            identificador,
                            n.path("Descripcion").asText(),
                            n.path("AbreviaturaBandera").asText(),
                            n.path("AbreviaturaAmpliadaBandera").asText(),
                            lat,
                            lon
                    ));
                }
            }
            return list;
        });
    }

    public TransitLineResolver getTransitLineResolver() {
        return transitLineResolver;
    }

    @FunctionalInterface
    private interface UpstreamFetcher<T> {
        T fetch() throws Exception;
    }

    private <T> T getOrFetch(String cacheKey, String catalogType, TypeReference<T> typeRef, UpstreamFetcher<T> fetcher) {
        T cached = getCachedValue(cacheKey, typeRef);
        if (cached != null) {
            return cached;
        }

        CompletableFuture<Object> mine = new CompletableFuture<>();
        CompletableFuture<Object> inFlight = inFlightFetches.putIfAbsent(cacheKey, mine);
        if (inFlight != null) {
            try {
                return objectMapper.convertValue(inFlight.join(), typeRef);
            } catch (CompletionException exception) {
                if (exception.getCause() instanceof RuntimeException runtimeException) {
                    throw runtimeException;
                }
                throw new UpstreamServiceException("Fallo concurrente al obtener datos de catálogo para " + cacheKey, exception.getCause());
            }
        }

        try {
            T value = getCachedValue(cacheKey, typeRef);
            if (value == null) {
                value = fetchAndStore(cacheKey, catalogType, fetcher);
            }
            mine.complete(value);
            return value;
        } catch (RuntimeException | Error exception) {
            mine.completeExceptionally(exception);
            throw exception;
        } finally {
            inFlightFetches.remove(cacheKey, mine);
        }
    }

    private <T> T getCachedValue(String cacheKey, TypeReference<T> typeRef) {
        // 1. Check Caffeine L1
        Cache l1 = cacheManager != null ? cacheManager.getCache(CaffeineCacheConfig.TRANSIT_CATALOG_CACHE) : null;
        if (l1 != null) {
            Cache.ValueWrapper wrapper = l1.get(cacheKey);
            if (wrapper != null && wrapper.get() != null) {
                // Each cache key has one DTO type, so L1 can return its already materialized value.
                // Re-converting it through Jackson on every hit needlessly allocates a second object graph.
                @SuppressWarnings("unchecked")
                T cachedValue = (T) wrapper.get();
                return cachedValue;
            }
        }

        // 2. Check PostgreSQL L2 only while its entry remains fresh.
        if (catalogRepository != null) {
            var l2Entity = catalogRepository.findById(cacheKey);
            if (l2Entity.isPresent() && !isExpired(l2Entity.get())) {
                try {
                    T val = objectMapper.readValue(l2Entity.get().getPayload(), typeRef);
                    if (l1 != null) l1.put(cacheKey, val);
                    return val;
                } catch (Exception ex) {
                    log.warn("Error deserializando L2 postgres cache para {}", cacheKey);
                }
            }
        }

        return null;
    }

    private <T> T fetchAndStore(String cacheKey, String catalogType, UpstreamFetcher<T> fetcher) {
        Cache l1 = cacheManager != null ? cacheManager.getCache(CaffeineCacheConfig.TRANSIT_CATALOG_CACHE) : null;
        // 3. Fetch Upstream L3
        try {
            log.info("Cache Miss L1/L2 para key: {}. Consultando upstream municipal...", cacheKey);
            T upstreamData = fetcher.fetch();
            String serialized = objectMapper.writeValueAsString(upstreamData);

            // Guardar L2 Postgres solo si la respuesta no es una lista vacía
            // (evita persistir respuestas vacías que podrían enmascarar fallos upstream)
            boolean isEmptyList = upstreamData instanceof List<?> list && list.isEmpty();
            if (!isEmptyList) {
                saveL2(cacheKey, catalogType, serialized);
            }

            // Guardar L1 Caffeine siempre (incluso listas vacías) para evitar cache stampede inmediato
            if (l1 != null) l1.put(cacheKey, upstreamData);

            return upstreamData;
        } catch (Exception e) {
            log.error("Error consultando catálogo upstream para key {}: {}", cacheKey, e.getMessage());
            throw new UpstreamServiceException("Fallo al obtener datos de catálogo para " + cacheKey, e);
        }
    }

    private boolean isExpired(TransitCatalogEntity entry) {
        return entry.getUpdatedAt() == null || entry.getUpdatedAt().isBefore(Instant.now().minus(catalogTtl));
    }

    @Scheduled(cron = "0 0 4 * * SUN") // Domingos a las 04:00 AM
    public void purgeStaleCatalogEntries() {
        Instant cutoff = Instant.now().minus(Duration.ofDays(7));
        int deleted = catalogRepository.deleteStaleEntries(cutoff);
        if (deleted > 0) {
            log.info("Mantenimiento L2: Se purgaron {} entradas obsoletas de transit_catalog_entries", deleted);
        }
    }

    public void saveL2(String cacheKey, String catalogType, String payload) {
        if (catalogRepository == null) return;
        try {
            catalogRepository.save(new TransitCatalogEntity(cacheKey, catalogType, payload, Instant.now()));
        } catch (Exception ex) {
            log.warn("No se pudo persistir en L2 Postgres: {}", ex.getMessage());
        }
    }

    /**
     * Removes the city suffix appended by the upstream API (e.g. " - MAR DEL PLATA",
     * " - GENERAL PUEYRREDON") from street and intersection descriptions, returning
     * only the street name itself.
     */
    private String stripCitySuffix(String descripcion) {
        if (descripcion == null) return null;
        int idx = descripcion.lastIndexOf(" - ");
        return idx > 0 ? descripcion.substring(0, idx).trim() : descripcion.trim();
    }
}
