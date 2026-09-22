package com.gianmdp03.cuando_llega_pro.domain.transit;

import com.fasterxml.jackson.databind.JsonNode;
import com.gianmdp03.cuando_llega_pro.domain.preset.PresetRepository;
import com.gianmdp03.cuando_llega_pro.domain.transit.model.TransitLine;
import com.gianmdp03.cuando_llega_pro.domain.transit.model.TransitStop;
import com.gianmdp03.cuando_llega_pro.domain.transit.model.StopLineDirection;
import com.gianmdp03.cuando_llega_pro.domain.transit.model.TransitRoute;
import com.gianmdp03.cuando_llega_pro.domain.transit.model.TransitRoutePoint;
import com.gianmdp03.cuando_llega_pro.domain.transit.repository.StopLineDirectionRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Owns transactional writes to the normalized static transit catalogue. */
@Service
@RequiredArgsConstructor
public class TransitDataPersistenceService {

    private final EntityManager entityManager;
    private final StopLineDirectionRepository stopTransitLineRepository;
    private final PresetRepository presetRepository;

    /**
     * Replaces the whole published catalogue as one database transaction. A malformed
     * dataset or a persistence error rolls the complete replacement back.
     */
    @Transactional
    public void replaceCatalogSnapshot(JsonNode dataset, int batchSize) {
        JsonNode lines = dataset.has("lineas") ? dataset.path("lineas") : dataset.path("lines");
        if (!lines.isArray()) {
            throw new IllegalArgumentException("Dataset does not contain the lines/lineas array");
        }
        JsonNode stops = dataset.has("paradas") ? dataset.path("paradas") : dataset.path("stops");
        if (!stops.isArray()) {
            throw new IllegalArgumentException("El dataset no contiene el arreglo stops/paradas");
        }

        clearCatalogueSnapshot();
        for (JsonNode lineNode : lines) {
            String code = firstText(lineNode, "codigoLinea", "lineCode");
            String name = firstText(lineNode, "nombre", "name");
            upsertTransitLine(code, name);
        }
        entityManager.flush();
        persistRoutes(dataset);
        entityManager.flush();

        int processed = 0;
        for (JsonNode stopNode : stops) {
            persistTransitStop(stopNode);
            if (++processed % batchSize == 0) {
                entityManager.flush();
                entityManager.clear();
            }
        }
        entityManager.flush();
        removeInvalidPresets(dataset);
    }

    /**
     * A catalogue is a snapshot, so it must replace deleted records as well as changed
     * ones; otherwise obsolete stops would keep appearing on the map.
     */
    @Transactional
    public void upsertCatalog(JsonNode dataset, int batchSize) {
        replaceCatalogSnapshot(dataset, batchSize);
    }

    private void persistRoutes(JsonNode dataset) {
        JsonNode routes = dataset.path("routes");
        if (!routes.isArray()) {
            return;
        }
        for (JsonNode routeNode : routes) {
            String id = requiredText(routeNode, "id");
            String lineCode = requiredText(routeNode, "lineCode");
            String branch = requiredText(routeNode, "branch");
            TransitRoute route = entityManager.find(TransitRoute.class, id);
            TransitLine line = entityManager.getReference(TransitLine.class, lineCode);
            if (route == null) {
                route = new TransitRoute(id, line, branch, textOrNull(routeNode, "description"));
                entityManager.persist(route);
            } else {
                route.updateMetadata(line, branch, textOrNull(routeNode, "description"));
            }
            java.util.List<TransitRoutePoint> points = new java.util.ArrayList<>();
            int sequence = 0;
            for (JsonNode point : routeNode.path("points")) {
                Double latitude = doubleOrNull(point, "latitude");
                Double longitude = doubleOrNull(point, "longitude");
                if (latitude != null && longitude != null) {
                    points.add(new TransitRoutePoint(sequence++, latitude, longitude,
                            point.path("isPassThrough").asBoolean(false)));
                }
            }
            route.replacePoints(points);
        }
    }

    private void clearCatalogueSnapshot() {
        // Children first: the FK graph intentionally prevents deleting the catalogue
        // in the wrong order.
        entityManager.createQuery("delete from TransitRoutePoint").executeUpdate();
        entityManager.createQuery("delete from TransitRoute").executeUpdate();
        entityManager.createQuery("delete from StopLineDirection").executeUpdate();
        entityManager.createQuery("delete from TransitStop").executeUpdate();
        entityManager.createQuery("delete from TransitLine").executeUpdate();
        entityManager.clear();
    }

    private void removeInvalidPresets(JsonNode dataset) {
        Map<String, String> commercialToInternal = new java.util.HashMap<>();
        JsonNode lines = dataset.has("lineas") ? dataset.path("lineas") : dataset.path("lines");
        for (JsonNode line : lines) {
            String commercial = firstText(line, "nombre", "name");
            String internal = firstText(line, "codigoLinea", "lineCode");
            if (commercial != null && internal != null) {
                commercialToInternal.put(normalize(commercial), internal);
            }
        }

        Set<StopLine> validStopLines = new java.util.HashSet<>();
        Set<PresetTarget> validTargets = new java.util.HashSet<>();
        JsonNode stops = dataset.has("paradas") ? dataset.path("paradas") : dataset.path("stops");
        for (JsonNode stop : stops) {
            String identifier = textOrNull(stop, "identifier");
            if (identifier == null) continue;
            for (JsonNode line : stop.path("lines")) {
                String lineCode = textOrNull(line, "lineCode");
                String direction = textOrNull(line, "direction");
                if (lineCode == null || direction == null) continue;
                validStopLines.add(new StopLine(identifier, lineCode));
                validTargets.add(new PresetTarget(identifier, lineCode, normalize(direction)));
            }
        }

        var invalid = presetRepository.findAll().stream()
                .filter(preset -> {
                    String internalLine = commercialToInternal.getOrDefault(
                            normalize(preset.getCodigoLinea()), preset.getCodigoLinea());
                    if (preset.getBandera() == null || preset.getBandera().isBlank()) {
                        return !validStopLines.contains(new StopLine(preset.getIdentificadorParada(), internalLine));
                    }
                    return !validTargets.contains(new PresetTarget(
                            preset.getIdentificadorParada(), internalLine, normalize(preset.getBandera())));
                })
                .toList();
        if (!invalid.isEmpty()) {
            presetRepository.deleteAll(invalid);
        }
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private record StopLine(String stopIdentifier, String lineCode) {}

    private record PresetTarget(String stopIdentifier, String lineCode, String direction) {}

    private void persistTransitStop(JsonNode stopNode) {
        String identifier = requiredText(stopNode, "identifier");
        TransitStop stop = entityManager.find(TransitStop.class, identifier);
        if (stop == null) {
            stop = new TransitStop(
                    identifier,
                    textOrNull(stopNode, "code"),
                    textOrNull(stopNode, "description"),
                    doubleOrNull(stopNode, "latitude"),
                    doubleOrNull(stopNode, "longitude")
            );
            entityManager.persist(stop);
        } else {
            updateTransitStop(stop, stopNode);
        }
        int lineSequenceIndex = 0;
        for (JsonNode lineNode : stopNode.path("lines")) {
            String lineCode = requiredText(lineNode, "lineCode");
            String direction = requiredText(lineNode, "direction");
            int stopOrder = intOrDefault(lineNode, "stopOrder", lineSequenceIndex++);
            var existing = stopTransitLineRepository
                    .findByStopIdentifierAndLineCodeAndDirection(identifier, lineCode, direction);
            if (existing.isPresent()) {
                existing.get().setExpandedDirection(textOrNull(lineNode, "expandedDirection"));
                existing.get().setStopOrder(stopOrder);
            } else {
                stop.addTransitLine(new StopLineDirection(
                        entityManager.getReference(TransitLine.class, lineCode),
                        direction,
                        textOrNull(lineNode, "expandedDirection"),
                        stopOrder
                ));
            }
        }
    }

    private void upsertTransitLine(String code, String name) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("A dataset line has no lineCode");
        }
        TransitLine line = entityManager.find(TransitLine.class, code);
        if (line == null) {
            entityManager.persist(new TransitLine(code, name));
        } else {
            line.setName(name);
        }
    }

    private void updateTransitStop(TransitStop stop, JsonNode source) {
        String code = firstText(source, "Codigo", "code");
        String description = firstText(source, "Descripcion", "description");
        Double latitude = firstDouble(source, "LatitudParada", "latitude", "Latitud");
        Double longitude = firstDouble(source, "LongitudParada", "longitude", "Longitud");
        if (code != null) stop.setCode(code);
        if (description != null) stop.setDescription(description);
        if (latitude != null) stop.setLatitude(latitude);
        if (longitude != null) stop.setLongitude(longitude);
    }

    private static String requiredText(JsonNode node, String field) {
        String value = textOrNull(node, field);
        if (value == null) {
            throw new IllegalArgumentException("Campo obligatorio ausente: " + field);
        }
        return value;
    }

    private static String firstText(JsonNode node, String... fields) {
        for (String field : fields) {
            String value = textOrNull(node, field);
            if (value != null) return value;
        }
        return null;
    }

    private static String textOrNull(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() || value.asText().isBlank() ? null : value.asText();
    }

    private static int intOrDefault(JsonNode node, String field, int defaultValue) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? defaultValue : value.asInt(defaultValue);
    }

    private static Double firstDouble(JsonNode node, String... fields) {
        for (String field : fields) {
            Double value = doubleOrNull(node, field);
            if (value != null) return value;
        }
        return null;
    }

    private static Double doubleOrNull(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asDouble();
    }
}
