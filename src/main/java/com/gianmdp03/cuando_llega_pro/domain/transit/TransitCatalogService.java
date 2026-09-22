package com.gianmdp03.cuando_llega_pro.domain.transit;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gianmdp03.cuando_llega_pro.client.MgpProxyClient;
import com.gianmdp03.cuando_llega_pro.config.CaffeineCacheConfig;
import com.gianmdp03.cuando_llega_pro.domain.transit.dto.ConsolidatedStopDTO;
import com.gianmdp03.cuando_llega_pro.domain.transit.dto.TransitIntersectionDTO;
import com.gianmdp03.cuando_llega_pro.domain.transit.dto.TransitLineDTO;
import com.gianmdp03.cuando_llega_pro.domain.transit.dto.TransitRouteResponseDTO;
import com.gianmdp03.cuando_llega_pro.domain.transit.dto.TransitStopDTO;
import com.gianmdp03.cuando_llega_pro.domain.transit.dto.TransitStopWithFlagDTO;
import com.gianmdp03.cuando_llega_pro.domain.transit.dto.TransitStreetDTO;
import com.gianmdp03.cuando_llega_pro.exception.UpstreamServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Comparator;
import java.util.UUID;

@Service
public class TransitCatalogService {
    private static final Logger log = LoggerFactory.getLogger(TransitCatalogService.class);

    public static final String CACHE_KEY_LINES_ALL = "lines:all";
    public static final String ACTION_RECUPERAR_LINEAS_FALLBACK = "RecuperarLineaPorCuandoLlega";
    public static final String ACTION_RECUPERAR_CALLES_PRINCIPAL = "RecuperarCallesPrincipalPorLinea";
    public static final String ACTION_RECUPERAR_INTERSECCION = "RecuperarInterseccionPorLineaYCalle";
    public static final String ACTION_RECUPERAR_PARADAS_BANDERA = "RecuperarParadasConBanderaPorLineaCalleEInterseccion";
    public static final String ACTION_RECUPERAR_RECORRIDO_MAPA = "RecuperarRecorridoParaMapaAbrevYAmpliPorEntidadYLinea";
    public static final String ACTION_RECUPERAR_PARADAS_LEGACY = "RecuperarParadasPorLinea";

    private final MgpProxyClient mgpProxyClient;
    private final TransitCatalogRepository catalogRepository;
    private final CacheManager cacheManager;
    private final ObjectMapper objectMapper;
    private final TransitLineResolver transitLineResolver;
    private final StopLocationRepository stopLocationRepository;

    @Autowired
    public TransitCatalogService(
            MgpProxyClient mgpProxyClient,
            TransitCatalogRepository catalogRepository,
            CacheManager cacheManager,
            ObjectMapper objectMapper,
            TransitLineResolver transitLineResolver,
            @Autowired(required = false) StopLocationRepository stopLocationRepository
    ) {
        this.mgpProxyClient = mgpProxyClient;
        this.catalogRepository = catalogRepository;
        this.cacheManager = cacheManager;
        this.objectMapper = objectMapper;
        this.transitLineResolver = transitLineResolver != null ? transitLineResolver : new TransitLineResolver();
        this.stopLocationRepository = stopLocationRepository;
    }

    public TransitCatalogService(
            MgpProxyClient mgpProxyClient,
            TransitCatalogRepository catalogRepository,
            CacheManager cacheManager,
            ObjectMapper objectMapper,
            TransitLineResolver transitLineResolver
    ) {
        this(mgpProxyClient, catalogRepository, cacheManager, objectMapper, transitLineResolver, null);
    }

    public TransitCatalogService(
            MgpProxyClient mgpProxyClient,
            ObjectMapper objectMapper,
            TransitCatalogRepository catalogRepository,
            TransitLineResolver transitLineResolver
    ) {
        this(mgpProxyClient, catalogRepository, null, objectMapper, transitLineResolver, null);
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

    @Cacheable(CaffeineCacheConfig.TRANSIT_CATALOG_CACHE)
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

    @Cacheable(CaffeineCacheConfig.TRANSIT_CATALOG_CACHE)
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

    @Cacheable(CaffeineCacheConfig.TRANSIT_CATALOG_CACHE)
    public ConsolidatedStopDTO getConsolidatedStop(String lineCode, String streetCode, String intersectionCode) {
        String internalLine = transitLineResolver.toInternalCode(lineCode);
        String cacheKey = "stops_consolidated:" + internalLine + ":" + streetCode + ":" + intersectionCode;
        return getOrFetch(cacheKey, "STOPS_CONSOLIDATED", new TypeReference<ConsolidatedStopDTO>() {}, () -> {
            String raw = mgpProxyClient.getStopsWithFlag(
                    UUID.randomUUID().toString(), ACTION_RECUPERAR_PARADAS_BANDERA,
                    internalLine, streetCode, intersectionCode
            );
            JsonNode root = objectMapper.readTree(raw);
            JsonNode array = root.hasNonNull("paradas") ? root.get("paradas") : root;
            String stopId = null;
            List<String> banderas = new ArrayList<>();
            if (array != null && array.isArray()) {
                for (JsonNode n : array) {
                    if (stopId == null) {
                        String id = n.path("Identificador").asText();
                        if (id == null || id.isBlank()) {
                            id = n.path("Codigo").asText();
                        }
                        if (id != null && !id.isBlank()) {
                            stopId = id;
                        }
                    }
                    String bandera = n.path("AbreviaturaBandera").asText();
                    if (bandera == null || bandera.isBlank()) {
                        bandera = n.path("AbreviaturaAmpliadaBandera").asText();
                    }
                    if (bandera != null && !bandera.isBlank() && !banderas.contains(bandera)) {
                        banderas.add(bandera);
                    }
                }
            }
            return new ConsolidatedStopDTO(stopId, streetCode, intersectionCode, banderas);
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

    public TransitRouteResponseDTO getRouteTrace(String lineCode) {
        String internalLine = transitLineResolver.toInternalCode(lineCode);
        String cacheKey = "route:" + internalLine;
        return getOrFetch(cacheKey, "ROUTE", new TypeReference<TransitRouteResponseDTO>() {}, () -> {
            String raw = mgpProxyClient.getRouteMapByLine(
                    UUID.randomUUID().toString(), ACTION_RECUPERAR_RECORRIDO_MAPA, internalLine, "0"
            );
            JsonNode root = objectMapper.readTree(raw);
            JsonNode array = root.hasNonNull("puntos") ? root.get("puntos") : (root.isArray() ? root : null);
            java.util.Map<String, List<com.gianmdp03.cuando_llega_pro.domain.transit.dto.RoutePointDTO>> branchPointsMap = new java.util.LinkedHashMap<>();
            java.util.Map<String, String> branchDescMap = new java.util.LinkedHashMap<>();
            List<com.gianmdp03.cuando_llega_pro.domain.transit.dto.RoutePointDTO> allPoints = new ArrayList<>();
            List<List<Double>> allCoordinates = new ArrayList<>();

            if (array != null && array.isArray()) {
                for (JsonNode node : array) {
                    Double lat = node.hasNonNull("Latitud") ? node.get("Latitud").asDouble() : null;
                    Double lon = node.hasNonNull("Longitud") ? node.get("Longitud").asDouble() : null;
                    if (lat == null || lon == null) continue;

                    String bandera = node.hasNonNull("AbreviaturaBanderaSMP") ? node.get("AbreviaturaBanderaSMP").asText() : "Principal";
                    String descripcion = node.hasNonNull("Descripcion") ? node.get("Descripcion").asText() : "";
                    if (!branchDescMap.containsKey(bandera)) {
                        branchDescMap.put(bandera, descripcion);
                    }
                    Boolean isPuntoPaso = node.path("IsPuntoPaso").asBoolean(false);

                    var point = new com.gianmdp03.cuando_llega_pro.domain.transit.dto.RoutePointDTO(lat, lon, descripcion, isPuntoPaso);
                    allPoints.add(point);
                    allCoordinates.add(List.of(lat, lon));
                    branchPointsMap.computeIfAbsent(bandera, k -> new ArrayList<>()).add(point);
                }
            }

            List<com.gianmdp03.cuando_llega_pro.domain.transit.dto.TransitBranchRouteDTO> branches = new ArrayList<>();
            for (java.util.Map.Entry<String, List<com.gianmdp03.cuando_llega_pro.domain.transit.dto.RoutePointDTO>> entry : branchPointsMap.entrySet()) {
                String bandera = entry.getKey();
                List<com.gianmdp03.cuando_llega_pro.domain.transit.dto.RoutePointDTO> bPoints = entry.getValue();
                List<List<Double>> bCoords = bPoints.stream()
                        .map(p -> List.of(p.latitude(), p.longitude()))
                        .toList();
                String desc = branchDescMap.getOrDefault(bandera, bandera);
                branches.add(new com.gianmdp03.cuando_llega_pro.domain.transit.dto.TransitBranchRouteDTO(bandera, desc, bPoints, bCoords));
            }

            return new TransitRouteResponseDTO(lineCode, branches, allPoints, allCoordinates);
        });
    }

    public List<TransitStopDTO> getStopsForLine(String lineCode) {
        String internalLine = transitLineResolver.toInternalCode(lineCode);
        String cacheKey = "stops:" + internalLine;
        return getOrFetch(cacheKey, "STOPS", new TypeReference<List<TransitStopDTO>>() {}, () -> {
            String raw = mgpProxyClient.getStopsByLine(UUID.randomUUID().toString(), ACTION_RECUPERAR_PARADAS_LEGACY, internalLine);
            JsonNode root = objectMapper.readTree(raw);
            JsonNode array = root.hasNonNull("paradas") ? root.get("paradas") : (root.isArray() ? root : null);
            List<TransitStopDTO> list = new ArrayList<>();
            if (array != null && array.isArray()) {
                for (JsonNode n : array) {
                    String id = n.hasNonNull("Identificador") ? n.get("Identificador").asText() : n.path("Codigo").asText();
                    String nombre = n.hasNonNull("Descripcion") ? n.get("Descripcion").asText() : n.path("AbreviaturaBandera").asText();
                    String calle = n.path("Calle").asText(null);
                    Double lat = null;
                    if (n.hasNonNull("LatitudParada")) {
                        lat = n.get("LatitudParada").asDouble();
                    } else if (n.hasNonNull("Latitud")) {
                        lat = n.get("Latitud").asDouble();
                    }

                    Double lon = null;
                    if (n.hasNonNull("LongitudParada")) {
                        lon = n.get("LongitudParada").asDouble();
                    } else if (n.hasNonNull("Longitud")) {
                        lon = n.get("Longitud").asDouble();
                    }

                    list.add(new TransitStopDTO(id, nombre, calle, lat, lon));
                }
            }
            if (stopLocationRepository != null) {
                list = list.stream().map(stop -> {
                    if (stop.latitude() == null || stop.longitude() == null) {
                        return stopLocationRepository.findById(stop.id())
                                .map(loc -> new TransitStopDTO(
                                        stop.id(),
                                        stop.nombre(),
                                        stop.calle(),
                                        stop.latitude() != null ? stop.latitude() : loc.getLatitude(),
                                        stop.longitude() != null ? stop.longitude() : loc.getLongitude()
                                ))
                                .orElse(stop);
                    }
                    return stop;
                }).toList();
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
        // 1. Check Caffeine L1
        Cache l1 = cacheManager != null ? cacheManager.getCache(CaffeineCacheConfig.TRANSIT_CATALOG_CACHE) : null;
        if (l1 != null) {
            Cache.ValueWrapper wrapper = l1.get(cacheKey);
            if (wrapper != null && wrapper.get() != null) {
                try {
                    return objectMapper.convertValue(wrapper.get(), typeRef);
                } catch (Exception ignored) {}
            }
        }

        // 2. Check PostgreSQL L2
        if (catalogRepository != null) {
            var l2Entity = catalogRepository.findById(cacheKey);
            if (l2Entity.isPresent()) {
                try {
                    T val = objectMapper.readValue(l2Entity.get().getPayload(), typeRef);
                    if (l1 != null) l1.put(cacheKey, val);
                    return val;
                } catch (Exception ex) {
                    log.warn("Error deserializando L2 postgres cache para {}", cacheKey);
                }
            }
        }

        // 3. Fetch Upstream L3
        try {
            log.info("Cache Miss L1/L2 para key: {}. Consultando upstream municipal...", cacheKey);
            T upstreamData = fetcher.fetch();
            String serialized = objectMapper.writeValueAsString(upstreamData);

            // Guardar L2 Postgres
            saveL2Async(cacheKey, catalogType, serialized);

            // Guardar L1 Caffeine
            if (l1 != null) l1.put(cacheKey, upstreamData);

            return upstreamData;
        } catch (Exception e) {
            log.error("Error consultando catálogo upstream para key {}: {}", cacheKey, e.getMessage());
            throw new UpstreamServiceException("Fallo al obtener datos de catálogo para " + cacheKey, e);
        }
    }

    @Transactional
    public void saveL2Async(String cacheKey, String catalogType, String payload) {
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

