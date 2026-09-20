package com.gianmdp03.cuando_llega_pro.domain.telemetry.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Models the top-level upstream telemetry response payload received from MGP via Go proxy.
 * Supports both standard PascalCase wrapped responses and raw JSON array payloads.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonDeserialize(using = MgpResponse.Deserializer.class)
public record MgpResponse(
        @JsonProperty("CodigoEstado") @JsonAlias({"codigoEstado"}) Integer codigoEstado,
        @JsonProperty("MensajeEstado") @JsonAlias({"mensajeEstado"}) String mensajeEstado,
        @JsonProperty("arribos") @JsonAlias({"Arribos", "arrivals", "proximosArribos"}) List<MgpArriboRaw> arribos
) {

    public MgpResponse {
        if (arribos == null) {
            arribos = List.of();
        } else {
            arribos = List.copyOf(arribos);
        }
    }

    /**
     * Custom deserializer supporting both polymorphic JSON structures:
     * 1. Standard object: {"CodigoEstado": 0, "MensajeEstado": "Ok", "arribos": [...]}
     * 2. Direct array: [{...}, {...}]
     */
    public static class Deserializer extends JsonDeserializer<MgpResponse> {
        @Override
        public MgpResponse deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            ObjectMapper mapper = (ObjectMapper) p.getCodec();
            JsonNode root = mapper.readTree(p);

            if (root == null || root.isNull()) {
                return new MgpResponse(0, "OK", List.of());
            }

            if (root.isArray()) {
                List<MgpArriboRaw> items = new ArrayList<>();
                for (JsonNode node : root) {
                    items.add(mapper.treeToValue(node, MgpArriboRaw.class));
                }
                return new MgpResponse(0, "OK", items);
            }

            if (root.isObject()) {
                Integer codigoEstado = root.hasNonNull("CodigoEstado") ? root.get("CodigoEstado").asInt()
                        : (root.hasNonNull("codigoEstado") ? root.get("codigoEstado").asInt() : 0);
                String mensajeEstado = root.hasNonNull("MensajeEstado") ? root.get("MensajeEstado").asText()
                        : (root.hasNonNull("mensajeEstado") ? root.get("mensajeEstado").asText() : "OK");

                List<MgpArriboRaw> items = new ArrayList<>();
                List<String> candidateKeys = List.of(
                        "arribos", "Arribos", "proximosArribos", "arrivals", "items", "data", "resultado"
                );
                boolean foundList = false;
                for (String key : candidateKeys) {
                    if (root.hasNonNull(key) && root.get(key).isArray()) {
                        for (JsonNode node : root.get(key)) {
                            items.add(mapper.treeToValue(node, MgpArriboRaw.class));
                        }
                        foundList = true;
                        break;
                    }
                }

                if (!foundList && (root.has("DescripcionLinea") || root.has("linea") || root.has("Arribo") || root.has("arribo"))) {
                    items.add(mapper.treeToValue(root, MgpArriboRaw.class));
                }

                return new MgpResponse(codigoEstado, mensajeEstado, items);
            }

            return new MgpResponse(0, "OK", List.of());
        }
    }
}
