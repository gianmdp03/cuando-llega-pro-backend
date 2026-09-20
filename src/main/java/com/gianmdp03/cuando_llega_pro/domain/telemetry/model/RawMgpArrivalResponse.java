package com.gianmdp03.cuando_llega_pro.domain.telemetry.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Models the upstream telemetry response from the Go proxy for accion=RecuperarProximosArribosW.
 * Supports parsing upstream JSON payloads structured as either a JSON array or a JSON object containing an arrivals list.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonDeserialize(using = RawMgpArrivalResponse.Deserializer.class)
public record RawMgpArrivalResponse(
        List<RawMgpArrivalItem> arrivals
) {

    public RawMgpArrivalResponse {
        if (arrivals == null) {
            arrivals = List.of();
        } else {
            arrivals = List.copyOf(arrivals);
        }
    }

    /**
     * Individual transit bus arrival item from the municipal telemetry feed.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RawMgpArrivalItem(
            @JsonProperty("linea") @com.fasterxml.jackson.annotation.JsonAlias({"DescripcionLinea", "descripcionLinea", "CodigoLineaParada"}) String linea,
            @JsonProperty("bandera") @com.fasterxml.jackson.annotation.JsonAlias({"DescripcionBandera", "descripcionBandera"}) String bandera,
            @JsonProperty("minutos") @com.fasterxml.jackson.annotation.JsonAlias({"Minutos"}) Integer minutos,
            @JsonProperty("distancia") @com.fasterxml.jackson.annotation.JsonAlias({"Distancia"}) Integer distancia,
            @JsonProperty("arribo") @com.fasterxml.jackson.annotation.JsonAlias({"Arribo"}) String arribo,
            @JsonProperty("coche") @com.fasterxml.jackson.annotation.JsonAlias({"IdentificadorCoche", "identificadorCoche"}) String coche,
            @JsonProperty("adaptado") @com.fasterxml.jackson.annotation.JsonAlias({"EsAdaptado", "esAdaptado"}) Boolean adaptado
    ) {}

    /**
     * Parses raw JSON string into a RawMgpArrivalResponse instance.
     *
     * @param mapper  configured ObjectMapper instance
     * @param rawJson raw JSON payload string
     * @return parsed RawMgpArrivalResponse
     * @throws JsonProcessingException if JSON is malformed
     */
    public static RawMgpArrivalResponse fromJson(ObjectMapper mapper, String rawJson) throws JsonProcessingException {
        if (rawJson == null || rawJson.isBlank()) {
            return new RawMgpArrivalResponse(List.of());
        }
        return mapper.readValue(rawJson, RawMgpArrivalResponse.class);
    }

    /**
     * Custom Jackson deserializer to handle polymorphic response shapes (JSON array vs. wrapped object).
     */
    public static class Deserializer extends JsonDeserializer<RawMgpArrivalResponse> {
        @Override
        public RawMgpArrivalResponse deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            ObjectMapper mapper = (ObjectMapper) p.getCodec();
            JsonNode root = mapper.readTree(p);
            List<RawMgpArrivalItem> items = new ArrayList<>();

            if (root == null || root.isNull()) {
                return new RawMgpArrivalResponse(items);
            }

            if (root.isArray()) {
                for (JsonNode node : root) {
                    items.add(mapper.treeToValue(node, RawMgpArrivalItem.class));
                }
            } else if (root.isObject()) {
                List<String> candidateKeys = List.of(
                        "arribos", "proximosArribos", "arrivals", "items",
                        "data", "resultado", "proximos", "arribo"
                );
                boolean foundList = false;
                for (String key : candidateKeys) {
                    if (root.hasNonNull(key) && root.get(key).isArray()) {
                        for (JsonNode node : root.get(key)) {
                            items.add(mapper.treeToValue(node, RawMgpArrivalItem.class));
                        }
                        foundList = true;
                        break;
                    }
                }

                if (!foundList) {
                    Iterator<JsonNode> elements = root.elements();
                    while (elements.hasNext()) {
                        JsonNode node = elements.next();
                        if (node.isArray()) {
                            for (JsonNode child : node) {
                                items.add(mapper.treeToValue(child, RawMgpArrivalItem.class));
                            }
                            foundList = true;
                            break;
                        }
                    }
                }

                if (!foundList && (root.has("linea") || root.has("arribo") || root.has("minutos"))) {
                    items.add(mapper.treeToValue(root, RawMgpArrivalItem.class));
                }
            }

            return new RawMgpArrivalResponse(items);
        }
    }
}
