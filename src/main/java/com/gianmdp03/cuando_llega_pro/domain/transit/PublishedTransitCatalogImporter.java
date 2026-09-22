package com.gianmdp03.cuando_llega_pro.domain.transit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gianmdp03.cuando_llega_pro.config.CaffeineCacheConfig;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/** Imports the public, versioned catalogue maintained by this project; it never contacts MGP. */
@Component
@ConditionalOnProperty(name = "app.transit.source.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class PublishedTransitCatalogImporter {

    private static final Logger log = LoggerFactory.getLogger(PublishedTransitCatalogImporter.class);
    private static final String SOURCE_METADATA_KEY = "source:published-transit-catalog";
    private static final String SOURCE_METADATA_TYPE = "SOURCE_METADATA";
    private static final int BATCH_SIZE = 200;

    private final TransitCatalogRepository catalogRepository;
    private final TransitDataPersistenceService persistenceService;
    private final ObjectMapper objectMapper;
    private final CacheManager cacheManager;
    private final AtomicBoolean importInProgress = new AtomicBoolean();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    @Value("${app.transit.source.url:https://raw.githubusercontent.com/gianmdp03/paradas_mgp/main/paradas_mgp.json}")
    private String sourceUrl;

    @Scheduled(
            fixedDelayString = "${app.transit.source.poll-interval-ms:21600000}",
            initialDelayString = "${app.transit.source.initial-delay-ms:60000}"
    )
    public void importIfChanged() {
        importNow();
    }

    /** Returns true only when this call applied a newly downloaded catalogue. */
    public boolean importNow() {
        if (!importInProgress.compareAndSet(false, true)) {
            log.info("La importación del catálogo publicado ya está en curso; se omite esta ejecución");
            return false;
        }
        try {
            return importPublishedCatalog();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            log.warn("Se interrumpió la descarga del catálogo publicado");
            return false;
        } catch (Exception exception) {
            // A failed remote download must never damage the last known good local catalogue.
            log.warn("No se pudo actualizar el catálogo publicado; se conserva la versión local", exception);
            return false;
        } finally {
            importInProgress.set(false);
        }
    }

    private boolean importPublishedCatalog() throws Exception {
        JsonNode previousMetadata = catalogRepository.findById(SOURCE_METADATA_KEY)
                .map(entry -> readMetadata(entry.getPayload()))
                .orElse(objectMapper.createObjectNode());
        String etag = previousMetadata.path("etag").asText(null);

        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(sourceUrl))
                .GET()
                .timeout(Duration.ofSeconds(60))
                .header("Accept", "application/json")
                .header("User-Agent", "cuando-llega-pro-catalog-importer");
        if (etag != null && !etag.isBlank()) {
            request.header("If-None-Match", etag);
        }

        HttpResponse<String> response = httpClient.send(request.build(), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 304) {
            log.info("El catálogo publicado no cambió (ETag)");
            return false;
        }
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("GitHub respondió HTTP " + response.statusCode());
        }

        String body = response.body();
        String sha256 = sha256(body);
        if (sha256.equals(previousMetadata.path("sha256").asText())) {
            saveMetadata(response, sha256);
            log.info("El catálogo publicado no cambió (hash)");
            return false;
        }

        JsonNode dataset = objectMapper.readTree(body);
        validateDataset(dataset);
        persistenceService.upsertCatalog(dataset, BATCH_SIZE);
        clearMapCache();
        saveMetadata(response, sha256);
        log.info("Catálogo publicado importado correctamente: líneas={}, paradas={}, recorridos={}",
                dataset.path("lineas").size(), dataset.path("paradas").size(), dataset.path("routes").size());
        return true;
    }

    private JsonNode readMetadata(String payload) {
        try {
            return objectMapper.readTree(payload);
        } catch (Exception ignored) {
            return objectMapper.createObjectNode();
        }
    }

    private void validateDataset(JsonNode dataset) {
        if (!dataset.path("lineas").isArray() || !dataset.path("paradas").isArray() || !dataset.path("routes").isArray()) {
            throw new IllegalArgumentException("El catálogo publicado no tiene los arreglos lineas, paradas y routes requeridos");
        }
        if (dataset.path("lineas").isEmpty() || dataset.path("paradas").isEmpty() || dataset.path("routes").isEmpty()) {
            throw new IllegalArgumentException("El catálogo publicado no puede tener líneas, paradas o recorridos vacíos");
        }
    }

    private void saveMetadata(HttpResponse<String> response, String sha256) throws Exception {
        String etag = response.headers().firstValue("ETag").orElse(null);
        String payload = objectMapper.writeValueAsString(Map.of(
                "etag", etag == null ? "" : etag,
                "sha256", sha256
        ));
        catalogRepository.save(new TransitCatalogEntity(
                SOURCE_METADATA_KEY, SOURCE_METADATA_TYPE, payload, Instant.now()
        ));
    }

    private void clearMapCache() {
        var cache = cacheManager.getCache(CaffeineCacheConfig.TRANSIT_MAP_CACHE);
        if (cache != null) {
            cache.clear();
        }
    }

    private static String sha256(String content) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(content.getBytes(StandardCharsets.UTF_8));
        StringBuilder result = new StringBuilder(digest.length * 2);
        for (byte value : digest) {
            result.append(String.format("%02x", value));
        }
        return result.toString();
    }
}
