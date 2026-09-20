package com.gianmdp03.cuando_llega_pro.domain.transit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gianmdp03.cuando_llega_pro.client.MgpProxyClient;
import com.gianmdp03.cuando_llega_pro.config.CaffeineCacheConfig;
import com.gianmdp03.cuando_llega_pro.domain.transit.dto.RoutePointDTO;
import com.gianmdp03.cuando_llega_pro.domain.transit.dto.TransitBranchRouteDTO;
import com.gianmdp03.cuando_llega_pro.domain.transit.dto.TransitLineDTO;
import com.gianmdp03.cuando_llega_pro.domain.transit.dto.TransitRouteResponseDTO;
import com.gianmdp03.cuando_llega_pro.domain.transit.dto.TransitStopDTO;
import com.gianmdp03.cuando_llega_pro.exception.UpstreamServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service managing the transit catalog for lines, stops, and geographic route traces.
 * Results are cached indefinitely in the "transit-catalog" Caffeine cache bucket.
 */
@Service
public class TransitCatalogService {

    private static final Logger log = LoggerFactory.getLogger(TransitCatalogService.class);

    private static final String ACTION_RECUPERAR_LINEAS = "RecuperarLineas";
    private static final String ACTION_RECUPERAR_LINEAS_FALLBACK = "RecuperarLineaPorCuandoLlega";
    private static final String ACTION_RECUPERAR_PARADAS = "RecuperarParadasPorLinea";
    private static final String ACTION_RECUPERAR_RECORRIDOS = "RecuperarRecorridosPorLinea";

    private final MgpProxyClient mgpProxyClient;
    private final ObjectMapper objectMapper;

    public TransitCatalogService(MgpProxyClient mgpProxyClient, ObjectMapper objectMapper) {
        this.mgpProxyClient = mgpProxyClient;
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
    }

    /**
     * Retrieves all transit lines, cached with key 'all_lines'.
     *
     * @return list of clean TransitLineDTO objects
     */
    @Cacheable(value = CaffeineCacheConfig.TRANSIT_CATALOG_CACHE, key = "'all_lines'")
    public List<TransitLineDTO> getLines() {
        String requestId = UUID.randomUUID().toString();
        log.info("Fetching transit lines from upstream proxy (requestId={})", requestId);

        try {
            String rawJson = mgpProxyClient.getLines(requestId, ACTION_RECUPERAR_LINEAS);
            if (rawJson == null || rawJson.isBlank() || "[]".equals(rawJson.trim())) {
                rawJson = mgpProxyClient.getLines(requestId, ACTION_RECUPERAR_LINEAS_FALLBACK);
            }

            if (rawJson == null || rawJson.isBlank()) {
                return List.of();
            }

            JsonNode root = objectMapper.readTree(rawJson);
            List<JsonNode> lineNodes = extractArrayElements(root, "lineas", "lines", "data", "resultado", "items");

            List<TransitLineDTO> result = new ArrayList<>();
            for (JsonNode node : lineNodes) {
                String id = parseText(node, "id", "Id", "CodigoLineaParada", "codigoLineaParada", "Codigo", "codigo", "CodigoLinea");
                String codigo = parseText(node, "codigo", "Codigo", "codigoLinea", "CodigoLinea", "Descripcion", "descripcion");
                String descripcion = parseText(node, "descripcion", "Descripcion", "nombre", "Nombre", "DescripcionLinea", "descripcionLinea");

                if (id == null && codigo != null) {
                    id = codigo;
                }
                if (codigo == null && id != null) {
                    codigo = id;
                }
                if (descripcion == null) {
                    descripcion = (codigo != null) ? codigo : id;
                }

                if (id != null || codigo != null || descripcion != null) {
                    result.add(new TransitLineDTO(id, codigo, descripcion));
                }
            }

            return result;
        } catch (Exception ex) {
            log.error("Failed to retrieve transit lines from upstream: {}", ex.getMessage(), ex);
            throw new UpstreamServiceException("Failed to retrieve transit lines from upstream proxy", ex);
        }
    }

    /**
     * Retrieves all bus stops for a specific line, cached with key 'stops:' + lineCode.
     *
     * @param lineCode transit line code
     * @return list of clean TransitStopDTO objects
     */
    @Cacheable(value = CaffeineCacheConfig.TRANSIT_CATALOG_CACHE, key = "'stops:' + #lineCode")
    public List<TransitStopDTO> getStopsForLine(String lineCode) {
        if (lineCode == null || lineCode.isBlank()) {
            return List.of();
        }

        String requestId = UUID.randomUUID().toString();
        log.info("Fetching transit stops for line {} from upstream proxy (requestId={})", lineCode, requestId);

        try {
            String rawJson = mgpProxyClient.getStopsByLine(requestId, ACTION_RECUPERAR_PARADAS, lineCode);
            if (rawJson == null || rawJson.isBlank()) {
                return List.of();
            }

            JsonNode root = objectMapper.readTree(rawJson);
            List<JsonNode> stopNodes = extractArrayElements(root, "paradas", "stops", "items", "data", "resultado");

            List<TransitStopDTO> result = new ArrayList<>();
            for (JsonNode node : stopNodes) {
                String id = parseText(node, "Identificador", "identificador", "Codigo", "codigo", "id", "Id", "codigoParada");
                String nombre = parseText(node, "Descripcion", "descripcion", "nombre", "Nombre", "AbreviaturaBandera", "abreviaturaBandera");
                String calle = parseText(node, "Calle", "calle", "callePrincipal", "Interseccion", "interseccion", "nombreCalle");
                Double lat = parseCoordinate(node, "LatitudParada", "latitudParada", "Latitud", "latitud", "lat", "latitude");
                Double lon = parseCoordinate(node, "LongitudParada", "longitudParada", "Longitud", "longitud", "lon", "lng", "longitude");

                result.add(new TransitStopDTO(id, nombre, calle, lat, lon));
            }

            return result;
        } catch (Exception ex) {
            log.error("Failed to retrieve transit stops for line {}: {}", lineCode, ex.getMessage(), ex);
            throw new UpstreamServiceException("Failed to retrieve transit stops for line " + lineCode, ex);
        }
    }

    /**
     * Retrieves geographic polyline vertices for rendering the line trajectory, cached with key 'route:' + lineCode.
     *
     * @param lineCode transit line code
     * @return structured TransitRouteResponseDTO with branches and coordinate lists
     */
    @Cacheable(value = CaffeineCacheConfig.TRANSIT_CATALOG_CACHE, key = "'route:' + #lineCode")
    public TransitRouteResponseDTO getRouteTrace(String lineCode) {
        if (lineCode == null || lineCode.isBlank()) {
            return new TransitRouteResponseDTO(lineCode, List.of(), List.of(), List.of());
        }

        String requestId = UUID.randomUUID().toString();
        log.info("Fetching route trace for line {} from upstream proxy (requestId={})", lineCode, requestId);

        try {
            String rawJson = mgpProxyClient.getRouteByLine(requestId, ACTION_RECUPERAR_RECORRIDOS, lineCode);
            if (rawJson == null || rawJson.isBlank()) {
                return new TransitRouteResponseDTO(lineCode, List.of(), List.of(), List.of());
            }

            JsonNode root = objectMapper.readTree(rawJson);
            List<JsonNode> pointNodes = extractArrayElements(root, "puntos", "recorridos", "points", "routes", "items", "data");

            Map<String, List<RoutePointDTO>> branchPointsMap = new LinkedHashMap<>();
            Map<String, String> branchDescMap = new LinkedHashMap<>();
            List<RoutePointDTO> allPoints = new ArrayList<>();
            List<List<Double>> allCoordinates = new ArrayList<>();

            for (JsonNode node : pointNodes) {
                Double lat = parseCoordinate(node, "Latitud", "latitud", "lat", "latitude");
                Double lon = parseCoordinate(node, "Longitud", "longitud", "lon", "lng", "longitude");

                if (lat == null || lon == null) {
                    continue;
                }

                String bandera = parseText(node, "AbreviaturaBanderaSMP", "bandera", "Bandera", "ramal", "Ramal");
                if (bandera == null || bandera.isBlank()) {
                    bandera = "Principal";
                }

                String descripcion = parseText(node, "Descripcion", "descripcion", "nombre");
                if (descripcion != null && !branchDescMap.containsKey(bandera)) {
                    branchDescMap.put(bandera, descripcion);
                }

                Boolean isPuntoPaso = parseBoolean(node, "IsPuntoPaso", "isPuntoPaso");

                RoutePointDTO point = new RoutePointDTO(lat, lon, descripcion, isPuntoPaso);
                allPoints.add(point);
                allCoordinates.add(List.of(lat, lon));

                branchPointsMap.computeIfAbsent(bandera, k -> new ArrayList<>()).add(point);
            }

            List<TransitBranchRouteDTO> branches = new ArrayList<>();
            for (Map.Entry<String, List<RoutePointDTO>> entry : branchPointsMap.entrySet()) {
                String bandera = entry.getKey();
                List<RoutePointDTO> bPoints = entry.getValue();
                List<List<Double>> bCoords = bPoints.stream()
                        .map(p -> List.of(p.latitude(), p.longitude()))
                        .toList();
                String desc = branchDescMap.getOrDefault(bandera, bandera);
                branches.add(new TransitBranchRouteDTO(bandera, desc, bPoints, bCoords));
            }

            return new TransitRouteResponseDTO(lineCode, branches, allPoints, allCoordinates);
        } catch (Exception ex) {
            log.error("Failed to retrieve route trace for line {}: {}", lineCode, ex.getMessage(), ex);
            throw new UpstreamServiceException("Failed to retrieve route trace for line " + lineCode, ex);
        }
    }

    private Double parseCoordinate(JsonNode node, String... fieldNames) {
        for (String field : fieldNames) {
            if (node.hasNonNull(field)) {
                JsonNode val = node.get(field);
                if (val.isNumber()) {
                    return val.asDouble();
                } else if (val.isTextual()) {
                    String text = val.asText().trim().replace(',', '.');
                    try {
                        return Double.parseDouble(text);
                    } catch (NumberFormatException ignored) {}
                }
            }
        }
        return null;
    }

    private String parseText(JsonNode node, String... fieldNames) {
        for (String field : fieldNames) {
            if (node.hasNonNull(field)) {
                String text = node.get(field).asText().trim();
                if (!text.isEmpty()) {
                    return text;
                }
            }
        }
        return null;
    }

    private Boolean parseBoolean(JsonNode node, String... fieldNames) {
        for (String field : fieldNames) {
            if (node.hasNonNull(field)) {
                JsonNode val = node.get(field);
                if (val.isBoolean()) {
                    return val.asBoolean();
                } else if (val.isTextual()) {
                    return "true".equalsIgnoreCase(val.asText().trim()) || "1".equals(val.asText().trim());
                } else if (val.isNumber()) {
                    return val.asInt() != 0;
                }
            }
        }
        return null;
    }

    private List<JsonNode> extractArrayElements(JsonNode root, String... candidateKeys) {
        if (root == null || root.isNull()) {
            return List.of();
        }
        if (root.isArray()) {
            List<JsonNode> list = new ArrayList<>();
            root.forEach(list::add);
            return list;
        }
        if (root.isObject()) {
            for (String key : candidateKeys) {
                if (root.hasNonNull(key) && root.get(key).isArray()) {
                    List<JsonNode> list = new ArrayList<>();
                    root.get(key).forEach(list::add);
                    return list;
                }
            }
            Iterator<Map.Entry<String, JsonNode>> fields = root.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                if (entry.getValue().isArray()) {
                    List<JsonNode> list = new ArrayList<>();
                    entry.getValue().forEach(list::add);
                    return list;
                }
            }
        }
        return List.of();
    }
}
