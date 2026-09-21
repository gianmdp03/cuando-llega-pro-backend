package com.gianmdp03.cuando_llega_pro.domain.transit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gianmdp03.cuando_llega_pro.client.MgpProxyClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TransitLineResolver {
    private static final Logger log = LoggerFactory.getLogger(TransitLineResolver.class);
    private static final String ACCION_LINEAS = "RecuperarLineaPorCuandoLlega";

    private final MgpProxyClient mgpProxyClient;
    private final ObjectMapper objectMapper;

    // commercial -> internal (ej: "511" -> "98")
    private final Map<String, String> commercialToInternal = new ConcurrentHashMap<>();
    // internal -> commercial (ej: "98" -> "511")
    private final Map<String, String> internalToCommercial = new ConcurrentHashMap<>();

    // Fallback estático extraído del HAR real de Mar del Plata
    private static final Map<String, String> HAR_FALLBACK = Map.ofEntries(
            Map.entry("501", "93"),
            Map.entry("511", "98"),
            Map.entry("512", "99"),
            Map.entry("521", "100"),
            Map.entry("522", "101"),
            Map.entry("523", "102"),
            Map.entry("525", "103"),
            Map.entry("531", "104"),
            Map.entry("532", "105"),
            Map.entry("533", "106"),
            Map.entry("541", "107"),
            Map.entry("542", "108"),
            Map.entry("543", "109"),
            Map.entry("551", "110"),
            Map.entry("552", "111"),
            Map.entry("553", "112"),
            Map.entry("554", "116"),
            Map.entry("555", "117"),
            Map.entry("562", "119"),
            Map.entry("563", "120"),
            Map.entry("571", "121"),
            Map.entry("573", "122"),
            Map.entry("581", "123"),
            Map.entry("591", "124"),
            Map.entry("593", "125"),
            Map.entry("593C", "126"),
            Map.entry("717", "127"),
            Map.entry("BATAN", "344")
    );

    public TransitLineResolver(MgpProxyClient mgpProxyClient, ObjectMapper objectMapper) {
        this.mgpProxyClient = mgpProxyClient;
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
        // Precargar con fallback estático inmediatamente
        HAR_FALLBACK.forEach((comm, intern) -> {
            commercialToInternal.put(comm.toUpperCase().trim(), intern.trim());
            internalToCommercial.put(intern.trim(), comm.toUpperCase().trim());
        });
    }

    public TransitLineResolver() {
        this(null, new ObjectMapper());
    }

    @EventListener(ApplicationReadyEvent.class)
    public void warmupLineMappings() {
        if (mgpProxyClient == null) return;
        try {
            log.info("Iniciando warmup de líneas contra upstream municipal...");
            String rawJson = mgpProxyClient.getLines(UUID.randomUUID().toString(), ACCION_LINEAS);
            if (rawJson == null || rawJson.isBlank()) return;

            JsonNode root = objectMapper.readTree(rawJson);
            JsonNode lineasNode = root.hasNonNull("lineas") ? root.get("lineas") : root;
            if (lineasNode != null && lineasNode.isArray()) {
                for (JsonNode item : lineasNode) {
                    if (item.hasNonNull("Descripcion") && item.hasNonNull("CodigoLineaParada")) {
                        String commercial = item.get("Descripcion").asText().toUpperCase().trim();
                        String internal = item.get("CodigoLineaParada").asText().trim();
                        commercialToInternal.put(commercial, internal);
                        internalToCommercial.put(internal, commercial);
                    }
                }
                log.info("Warmup completado. {} líneas mapeadas en memoria.", commercialToInternal.size());
            }
        } catch (Exception ex) {
            log.warn("Warmup falló, utilizando respaldo estático del HAR: {}", ex.getMessage());
        }
    }

    public void register(String commercial, String internal) {
        if (commercial == null || internal == null) return;
        commercialToInternal.put(commercial.toUpperCase().trim(), internal.trim());
        internalToCommercial.put(internal.trim(), commercial.toUpperCase().trim());
    }

    public String toInternalCode(String commercialLine) {
        if (commercialLine == null || commercialLine.isBlank()) return commercialLine;
        String cleaned = commercialLine.toUpperCase().trim();
        return commercialToInternal.getOrDefault(cleaned, commercialLine);
    }

    public String toCommercialCode(String internalCode) {
        if (internalCode == null || internalCode.isBlank()) return internalCode;
        String cleaned = internalCode.trim();
        return internalToCommercial.getOrDefault(cleaned, internalCode);
    }

    public Map<String, String> getAllMappings() {
        return Map.copyOf(commercialToInternal);
    }
}
