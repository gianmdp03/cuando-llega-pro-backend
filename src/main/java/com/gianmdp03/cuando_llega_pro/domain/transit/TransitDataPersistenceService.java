package com.gianmdp03.cuando_llega_pro.domain.transit;

import com.fasterxml.jackson.databind.JsonNode;
import com.gianmdp03.cuando_llega_pro.domain.transit.model.TransitLine;
import com.gianmdp03.cuando_llega_pro.domain.transit.model.TransitStop;
import com.gianmdp03.cuando_llega_pro.domain.transit.model.StopLineDirection;
import com.gianmdp03.cuando_llega_pro.domain.transit.repository.StopLineDirectionRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Owns transactional writes to the normalized static transit catalogue. */
@Service
@RequiredArgsConstructor
public class TransitDataPersistenceService {

    private final EntityManager entityManager;
    private final StopLineDirectionRepository stopTransitLineRepository;

    @Transactional
    public void replaceEmptyCatalog(JsonNode dataset, int batchSize) {
        JsonNode lines = dataset.path("lines");
        if (!lines.isArray()) {
            throw new IllegalArgumentException("Dataset does not contain the lines array");
        }
        for (JsonNode lineNode : lines) {
            upsertTransitLine(lineNode.path("lineCode").asText(), lineNode.path("name").asText());
        }
        entityManager.flush();

        JsonNode stops = dataset.path("stops");
        if (!stops.isArray()) {
            throw new IllegalArgumentException("El dataset no contiene el arreglo stops");
        }
        int processed = 0;
        for (JsonNode stopNode : stops) {
            persistTransitStop(stopNode);
            if (++processed % batchSize == 0) {
                entityManager.flush();
                entityManager.clear();
            }
        }
        entityManager.flush();
    }

    @Transactional
    public void synchronizeLineStops(TransitLine line, JsonNode response) {
        // MGP's upstream response is intentionally parsed with its original Spanish field name.
        JsonNode stops = response.has("paradas") ? response.path("paradas") : response;
        if (!stops.isArray()) {
            return;
        }
        for (JsonNode stopNode : stops) {
            String identifier = firstText(stopNode, "Identificador", "identifier", "Codigo", "code");
            if (identifier == null) {
                continue;
            }

            TransitStop stop = entityManager.find(TransitStop.class, identifier);
            if (stop == null) {
                stop = new TransitStop(
                        identifier,
                        firstText(stopNode, "Codigo", "code"),
                        firstText(stopNode, "Descripcion", "description"),
                        firstDouble(stopNode, "LatitudParada", "latitude", "Latitud"),
                        firstDouble(stopNode, "LongitudParada", "longitude", "Longitud")
                );
                entityManager.persist(stop);
            } else {
                updateTransitStop(stop, stopNode);
            }

            String direction = firstText(stopNode, "AbreviaturaBandera", "direction");
            if (direction != null && stopTransitLineRepository
                    .findByStopIdentifierAndLineCodeAndDirection(identifier, line.getCode(), direction)
                    .isEmpty()) {
                stop.addTransitLine(new StopLineDirection(
                        entityManager.getReference(TransitLine.class, line.getCode()),
                        direction,
                        firstText(stopNode, "AbreviaturaAmpliadaBandera", "expandedDirection")
                ));
            }
        }
    }

    private void persistTransitStop(JsonNode stopNode) {
        String identifier = requiredText(stopNode, "identifier");
        TransitStop stop = new TransitStop(
                identifier,
                textOrNull(stopNode, "code"),
                textOrNull(stopNode, "description"),
                doubleOrNull(stopNode, "latitude"),
                doubleOrNull(stopNode, "longitude")
        );
        for (JsonNode lineNode : stopNode.path("lines")) {
            String lineCode = requiredText(lineNode, "lineCode");
            stop.addTransitLine(new StopLineDirection(
                    entityManager.getReference(TransitLine.class, lineCode),
                    requiredText(lineNode, "direction"),
                    textOrNull(lineNode, "expandedDirection")
            ));
        }
        entityManager.persist(stop);
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
