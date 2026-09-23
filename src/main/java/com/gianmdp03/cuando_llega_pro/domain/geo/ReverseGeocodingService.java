package com.gianmdp03.cuando_llega_pro.domain.geo;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class ReverseGeocodingService {

    private static final Logger log = LoggerFactory.getLogger(ReverseGeocodingService.class);
    private static final long MIN_INTERVAL_NANOS = 1_500_000_000L; // 1.5 seconds
    private final GeoCacheRepository geoCacheRepository;
    private final RestClient restClient;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;
    private final ReentrantLock pacerLock = new ReentrantLock();
    /** Coalesces simultaneous requests for the same coordinate before the OSM call is made. */
    private final ConcurrentHashMap<String, ReentrantLock> addressLocks = new ConcurrentHashMap<>();
    private long lastRequestNanos = 0L;

    public ReverseGeocodingService(
            GeoCacheRepository geoCacheRepository,
            @org.springframework.beans.factory.annotation.Autowired(required = false) RestClient.Builder restClientBuilder,
            @org.springframework.beans.factory.annotation.Autowired(required = false) com.fasterxml.jackson.databind.ObjectMapper objectMapper) {
        this.geoCacheRepository = geoCacheRepository;
        this.objectMapper = objectMapper != null ? objectMapper : new com.fasterxml.jackson.databind.ObjectMapper();

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(5));
        requestFactory.setReadTimeout(Duration.ofSeconds(8));

        RestClient.Builder builder = restClientBuilder != null ? restClientBuilder : RestClient.builder();
        this.restClient = builder
                .requestFactory(requestFactory)
                .defaultHeader("User-Agent", "CuandoLlegaPro/1.0 (gcastorina234@gmail.com)")
                .build();
    }

    /** Resolves and permanently caches the street and number for a coordinate. */
    public String resolveAddress(double latitude, double longitude) {
        String gridKey = String.format(Locale.US, "%.4f:%.4f", latitude, longitude);
        ReentrantLock addressLock = addressLocks.computeIfAbsent(gridKey, ignored -> new ReentrantLock());
        addressLock.lock();
        try {
            return resolveAddressForGridKey(latitude, longitude, gridKey);
        } finally {
            addressLock.unlock();
            addressLocks.remove(gridKey, addressLock);
        }
    }

    private String resolveAddressForGridKey(double latitude, double longitude, String gridKey) {
        Optional<GeoCacheEntity> cached = geoCacheRepository.findById(gridKey);
        if (cached.isPresent()) {
            GeoCacheEntity entry = cached.get();
            String address = removeIntersectionContext(entry.getFormattedAddress());
            if (!address.equals(entry.getFormattedAddress())) {
                entry.setFormattedAddress(address);
                geoCacheRepository.save(entry);
            }
            log.info("L2 Cache HIT for gridKey {}: {}", gridKey, address);
            return address;
        }

        log.info("L2 Cache MISS for gridKey {}. Resolving via OSM...", gridKey);

        // 1. Nominatim Reverse Geocoding
        String mainStreet = null;
        String houseNumber = null;
        try {
            awaitOsmSlot();
            byte[] nominatimBytes = restClient.get()
                    .uri("https://nominatim.openstreetmap.org/reverse?format=jsonv2&lat={lat}&lon={lon}&addressdetails=1",
                            latitude, longitude)
                    .header("Accept-Language", "es")
                    .retrieve()
                    .body(byte[].class);

            if (nominatimBytes != null && nominatimBytes.length > 0) {
                JsonNode nominatimResponse = objectMapper.readTree(nominatimBytes);
                if (nominatimResponse.has("address")) {
                    JsonNode addressNode = nominatimResponse.get("address");
                    if (addressNode.hasNonNull("road") && !addressNode.get("road").asText().isBlank()) {
                        mainStreet = addressNode.get("road").asText().trim();
                    } else if (addressNode.hasNonNull("pedestrian") && !addressNode.get("pedestrian").asText().isBlank()) {
                        mainStreet = addressNode.get("pedestrian").asText().trim();
                    }

                    if (addressNode.hasNonNull("house_number") && !addressNode.get("house_number").asText().isBlank()) {
                        houseNumber = addressNode.get("house_number").asText().trim();
                    }
                }

                if ((mainStreet == null || mainStreet.isBlank()) && nominatimResponse.hasNonNull("name")) {
                    mainStreet = nominatimResponse.get("name").asText().trim();
                }
            }
        } catch (Exception e) {
            log.warn("Nominatim reverse geocoding failed for ({}, {}): {}", latitude, longitude, e.getMessage());
        }

        if (mainStreet == null || mainStreet.isBlank()) {
            mainStreet = "Ubicación s/n";
        }

        String baseStreet = (houseNumber != null && !houseNumber.isBlank())
                ? mainStreet + " " + houseNumber
                : mainStreet;

        if (baseStreet.length() > 300) {
            baseStreet = baseStreet.substring(0, 300);
        }

        try {
            GeoCacheEntity entity = new GeoCacheEntity(gridKey, baseStreet);
            geoCacheRepository.save(entity);
            log.info("Persisted geo_cache for gridKey {}: {}", gridKey, baseStreet);
        } catch (Exception e) {
            log.error("Failed to persist geo_cache entry for gridKey {}: {}", gridKey, e.getMessage());
        }

        return baseStreet;
    }

    /** Enforces a minimum interval of 1.5 seconds between outbound OSM requests. */
    private void awaitOsmSlot() {
        pacerLock.lock();
        try {
            long now = System.nanoTime();
            long elapsed = now - lastRequestNanos;
            if (lastRequestNanos != 0L && elapsed < MIN_INTERVAL_NANOS) {
                long sleepNanos = MIN_INTERVAL_NANOS - elapsed;
                long millis = sleepNanos / 1_000_000L;
                int nanos = (int) (sleepNanos % 1_000_000L);
                try {
                    Thread.sleep(millis, nanos);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            lastRequestNanos = System.nanoTime();
        } finally {
            pacerLock.unlock();
        }
    }

    private String removeIntersectionContext(String address) {
        return address.replaceFirst("\\s*\\((?:esq|entre|casi)\\s+.*\\)$", "");
    }
}
