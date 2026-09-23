package com.gianmdp03.cuando_llega_pro.integration;

import com.gianmdp03.cuando_llega_pro.client.MgpProxyClient;
import com.gianmdp03.cuando_llega_pro.config.CaffeineCacheConfig;
import com.gianmdp03.cuando_llega_pro.domain.preset.Preset;
import com.gianmdp03.cuando_llega_pro.domain.preset.PresetRepository;
import com.gianmdp03.cuando_llega_pro.domain.preset.model.PresetConfig;
import com.gianmdp03.cuando_llega_pro.domain.telemetry.dto.ArrivalResponseDTO;
import com.gianmdp03.cuando_llega_pro.domain.telemetry.service.ArrivalsService;
import com.gianmdp03.cuando_llega_pro.domain.user.User;
import com.gianmdp03.cuando_llega_pro.domain.user.UserRepository;
import com.gianmdp03.cuando_llega_pro.exception.UpstreamServiceException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Phase 5 End-to-End Integration Fallback Test.
 * <p>
 * Validates that when municipal upstream telemetry proxy experiences connectivity failure
 * or timeouts, the predictive extrapolation engine seamlessly degrades to cached telemetry,
 * preserving high system availability and returning estimated or expired ETAs with HTTP 200 OK.
 * </p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Import(TelemetryFallbackIntegrationTest.TestTxConfig.class)
@DisplayName("Phase 5 Telemetry Fallback Integration Test")
class TelemetryFallbackIntegrationTest {

    @TestConfiguration
    static class TestTxConfig {
        @Bean
        @Primary
        PlatformTransactionManager transactionManager() {
            return new AbstractPlatformTransactionManager() {
                @Override
                protected Object doGetTransaction() {
                    return new Object();
                }

                @Override
                protected void doBegin(Object transaction, TransactionDefinition definition) {
                }

                @Override
                protected void doCommit(DefaultTransactionStatus status) {
                }

                @Override
                protected void doRollback(DefaultTransactionStatus status) {
                }
            };
        }
    }

    private static final String COMMUTER_EMAIL = "commuter@example.com";
    private static final String LINE_511 = "511";
    private static final String INTERNAL_LINE_511 = "98";
    private static final String STOP_100 = "100";
    private static final String CACHE_KEY = STOP_100 + ":" + INTERNAL_LINE_511;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private ArrivalsService arrivalsService;

    @Autowired
    private CircuitBreaker circuitBreaker;

    @MockitoBean
    private MgpProxyClient mgpProxyClient;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private PresetRepository presetRepository;

    @BeforeEach
    void setUp() {
        // Reset Caffeine L1 cache and internal in-memory fallback store before each test run
        Cache arrivalsCache = cacheManager.getCache(CaffeineCacheConfig.ARRIVALS_CACHE);
        if (arrivalsCache != null) {
            arrivalsCache.clear();
        }
        Cache fallbackCache = cacheManager.getCache(CaffeineCacheConfig.FALLBACK_ARRIVALS_CACHE);
        if (fallbackCache != null) {
            fallbackCache.clear();
        }
        arrivalsService.getLastKnownTelemetryStore().clear();
        circuitBreaker.reset();

        // Setup commuter user entity
        User commuter = new User(COMMUTER_EMAIL, "hashedPassword", "Commuter User");
        commuter.setId(1L);

        // Setup transit preset for this user
        PresetConfig presetConfig = new PresetConfig(
                "Home to Work",
                "bus-route",
                "#0088CC",
                null
        );

        Preset preset = new Preset(commuter, LINE_511, STOP_100, "A", presetConfig);
        preset.setId(42L);

        // Mock repository lookups
        when(userRepository.findByEmail(COMMUTER_EMAIL)).thenReturn(Optional.of(commuter));
        when(presetRepository.findAllByUserIdWithUser(commuter.getId())).thenReturn(List.of(preset));
    }

    @Test
    @DisplayName("Step 1 to 5: Upstream succeeds (LIVE), upstream fails after cache eviction, serves ESTIMATED_FALLBACK with decayed ETAs")
    void testTelemetryFallback_ServesEstimatedFallbackWithDecayedMinutes() throws Exception {
        // --- STEP 1: Upstream succeeds ---
        String upstreamSuccessJson = """
                [{"linea":"511","bandera":"A","minutos":15,"distancia":3200,"arribo":"10:15","coche":"42","adaptado":true}]
                """;

        when(mgpProxyClient.getArrivals(any(), eq("RecuperarProximosArribosW"), eq(STOP_100), eq(INTERNAL_LINE_511)))
                .thenReturn(upstreamSuccessJson);

        // Make initial request as commuter@example.com
        mockMvc.perform(get("/api/v1/me/dashboard")
                        .with(user(COMMUTER_EMAIL))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userEmail", is(COMMUTER_EMAIL)))
                .andExpect(jsonPath("$.totalPresets", is(1)))
                .andExpect(jsonPath("$.presets[0].codigoLinea", is(LINE_511)))
                .andExpect(jsonPath("$.presets[0].identificadorParada", is(STOP_100)))
                .andExpect(jsonPath("$.presets[0].bandera", is("A")))
                .andExpect(jsonPath("$.presets[0].telemetry.status", is("LIVE")))
                .andExpect(jsonPath("$.presets[0].telemetry.deltaMinutes", is(0)))
                .andExpect(jsonPath("$.presets[0].telemetry.arrivals", hasSize(1)))
                .andExpect(jsonPath("$.presets[0].telemetry.arrivals[0].lineCode", is(LINE_511)))
                .andExpect(jsonPath("$.presets[0].telemetry.arrivals[0].branch", is("A")))
                .andExpect(jsonPath("$.presets[0].telemetry.arrivals[0].remainingMinutes", is(15)))
                .andExpect(jsonPath("$.presets[0].telemetry.arrivals[0].distanceMeters", is(3200)))
                .andExpect(jsonPath("$.presets[0].telemetry.arrivals[0].vehicleUnit", is("42")))
                .andExpect(jsonPath("$.presets[0].telemetry.arrivals[0].accessible", is(true)))
                .andExpect(jsonPath("$.presets[0].telemetry.arrivals[0].status", is("LIVE")));

        // --- STEP 2: Clear or evict Caffeine "arrivals" cache ---
        Objects.requireNonNull(cacheManager.getCache(CaffeineCacheConfig.ARRIVALS_CACHE)).clear();

        // --- STEP 3: Upstream proxy failure simulation ---
        when(mgpProxyClient.getArrivals(any(), eq("RecuperarProximosArribosW"), eq(STOP_100), eq(INTERNAL_LINE_511)))
                .thenThrow(new RuntimeException("Upstream timeout 504 Gateway Timeout"));

        // Simulate 5 minutes elapsed on the cached snapshot
        ArrivalResponseDTO liveSnapshot = arrivalsService.getLastKnownTelemetryStore().get(CACHE_KEY);
        assertThat(liveSnapshot).isNotNull();

        ArrivalResponseDTO agedSnapshot = new ArrivalResponseDTO(
                liveSnapshot.lineCode(),
                liveSnapshot.stopId(),
                liveSnapshot.branch(),
                liveSnapshot.status(),
                Instant.now().minus(Duration.ofMinutes(5)),
                liveSnapshot.deltaMinutes(),
                liveSnapshot.arrivals()
        );
        arrivalsService.getLastKnownTelemetryStore().put(CACHE_KEY, agedSnapshot);

        // --- STEP 4: Make request GET /api/v1/me/dashboard again ---
        // --- STEP 5: Verify that despite upstream failure, extrapolation engine serves ESTIMATED_FALLBACK with decayed minutes ---
        mockMvc.perform(get("/api/v1/me/dashboard")
                        .with(user(COMMUTER_EMAIL))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userEmail", is(COMMUTER_EMAIL)))
                .andExpect(jsonPath("$.totalPresets", is(1)))
                .andExpect(jsonPath("$.presets[0].codigoLinea", is(LINE_511)))
                .andExpect(jsonPath("$.presets[0].identificadorParada", is(STOP_100)))
                .andExpect(jsonPath("$.presets[0].bandera", is("A")))
                .andExpect(jsonPath("$.presets[0].telemetry.status", is("ESTIMATED_FALLBACK")))
                .andExpect(jsonPath("$.presets[0].telemetry.deltaMinutes", is(5)))
                .andExpect(jsonPath("$.presets[0].telemetry.arrivals", hasSize(1)))
                .andExpect(jsonPath("$.presets[0].telemetry.arrivals[0].lineCode", is(LINE_511)))
                .andExpect(jsonPath("$.presets[0].telemetry.arrivals[0].branch", is("A")))
                .andExpect(jsonPath("$.presets[0].telemetry.arrivals[0].remainingMinutes", is(10))) // Decayed: 15 - 5 = 10
                .andExpect(jsonPath("$.presets[0].telemetry.arrivals[0].distanceMeters", is(3200)))
                .andExpect(jsonPath("$.presets[0].telemetry.arrivals[0].vehicleUnit", is("42")))
                .andExpect(jsonPath("$.presets[0].telemetry.arrivals[0].status", is("ESTIMATED_FALLBACK")));
    }

    @Test
    @DisplayName("Step 1 to 5: Upstream proxy failure with elapsed time exceeding 25m degrades to EXPIRED status")
    void testTelemetryFallback_ServesExpiredWhenElapsedExceedsThreshold() throws Exception {
        // Seed initial live telemetry
        String upstreamSuccessJson = """
                [{"linea":"511","bandera":"A","minutos":15,"distancia":3200,"arribo":"10:15","coche":"42","adaptado":true}]
                """;

        when(mgpProxyClient.getArrivals(any(), eq("RecuperarProximosArribosW"), eq(STOP_100), eq(INTERNAL_LINE_511)))
                .thenReturn(upstreamSuccessJson);

        mockMvc.perform(get("/api/v1/me/dashboard")
                        .with(user(COMMUTER_EMAIL))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.presets[0].telemetry.status", is("LIVE")));

        // Evict Caffeine L1 cache
        Objects.requireNonNull(cacheManager.getCache(CaffeineCacheConfig.ARRIVALS_CACHE)).clear();

        // Simulate upstream failure via UpstreamServiceException
        when(mgpProxyClient.getArrivals(any(), eq("RecuperarProximosArribosW"), eq(STOP_100), eq(INTERNAL_LINE_511)))
                .thenThrow(new UpstreamServiceException("Upstream timeout 504 Gateway Timeout"));

        // Simulate 30 minutes elapsed (exceeding the 25-minute threshold)
        ArrivalResponseDTO liveSnapshot = arrivalsService.getLastKnownTelemetryStore().get(CACHE_KEY);
        assertThat(liveSnapshot).isNotNull();

        ArrivalResponseDTO expiredSnapshot = new ArrivalResponseDTO(
                liveSnapshot.lineCode(),
                liveSnapshot.stopId(),
                liveSnapshot.branch(),
                liveSnapshot.status(),
                Instant.now().minus(Duration.ofMinutes(30)),
                liveSnapshot.deltaMinutes(),
                liveSnapshot.arrivals()
        );
        arrivalsService.getLastKnownTelemetryStore().put(CACHE_KEY, expiredSnapshot);

        // Verify status degrades to EXPIRED with 0 remaining minutes
        mockMvc.perform(get("/api/v1/me/dashboard")
                        .with(user(COMMUTER_EMAIL))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.presets[0].telemetry.status", is("EXPIRED")))
                .andExpect(jsonPath("$.presets[0].telemetry.deltaMinutes", is(30)))
                .andExpect(jsonPath("$.presets[0].telemetry.arrivals[0].remainingMinutes", is(0)))
                .andExpect(jsonPath("$.presets[0].telemetry.arrivals[0].status", is("EXPIRED")));
    }
}
