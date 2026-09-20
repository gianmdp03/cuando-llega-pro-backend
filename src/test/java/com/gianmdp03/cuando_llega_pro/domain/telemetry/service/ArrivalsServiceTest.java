package com.gianmdp03.cuando_llega_pro.domain.telemetry.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gianmdp03.cuando_llega_pro.client.MgpProxyClient;
import com.gianmdp03.cuando_llega_pro.config.CaffeineCacheConfig;
import com.gianmdp03.cuando_llega_pro.domain.telemetry.dto.ArrivalResponseDTO;
import com.gianmdp03.cuando_llega_pro.domain.telemetry.dto.BusArrivalItemDTO;
import com.gianmdp03.cuando_llega_pro.domain.telemetry.model.TelemetryStatus;
import com.gianmdp03.cuando_llega_pro.exception.UpstreamServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ArrivalsService Unit Tests")
class ArrivalsServiceTest {

    @Mock
    private MgpProxyClient mgpProxyClient;

    @Mock
    private ExtrapolationEngine extrapolationEngine;

    @Mock
    private CacheManager cacheManager;

    @Mock
    private Cache cache;

    @Mock
    private Cache.ValueWrapper valueWrapper;

    private ObjectMapper objectMapper;
    private ArrivalsService arrivalsService;

    private static final String LINE_511 = "511";
    private static final String STOP_1024 = "1024";
    private static final String CACHE_KEY = "1024:511";

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        arrivalsService = new ArrivalsService(mgpProxyClient, objectMapper, extrapolationEngine, cacheManager);
    }

    @Nested
    @DisplayName("Input Validation Tests")
    class InputValidationTests {

        @Test
        @DisplayName("Throws IllegalArgumentException when lineCode is null or blank")
        void throwsExceptionWhenLineCodeInvalid() {
            assertThatThrownBy(() -> arrivalsService.getArrivals(null, STOP_1024))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("lineCode must not be null or blank");

            assertThatThrownBy(() -> arrivalsService.getArrivals("   ", STOP_1024))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("lineCode must not be null or blank");
        }

        @Test
        @DisplayName("Throws IllegalArgumentException when stopId is null or blank")
        void throwsExceptionWhenStopIdInvalid() {
            assertThatThrownBy(() -> arrivalsService.getArrivals(LINE_511, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("stopId must not be null or blank");

            assertThatThrownBy(() -> arrivalsService.getArrivals(LINE_511, ""))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("stopId must not be null or blank");
        }
    }

    @Nested
    @DisplayName("Caffeine Cache Hit Tests")
    class CacheHitTests {

        @Test
        @DisplayName("Returns cached live arrival DTO on cache hit without invoking upstream proxy")
        void returnsCachedLiveArrivalsWhenCacheHits() {
            BusArrivalItemDTO bus = new BusArrivalItemDTO(LINE_511, "A", 6, 1500, "6 min", "012", true, TelemetryStatus.LIVE);
            ArrivalResponseDTO cachedResponse = new ArrivalResponseDTO(LINE_511, STOP_1024, "A", TelemetryStatus.LIVE,
                    Instant.now(), 0L, List.of(bus));

            when(cacheManager.getCache(CaffeineCacheConfig.ARRIVALS_CACHE)).thenReturn(cache);
            when(cache.get(CACHE_KEY)).thenReturn(valueWrapper);
            when(valueWrapper.get()).thenReturn(cachedResponse);

            ArrivalResponseDTO result = arrivalsService.getArrivals(LINE_511, STOP_1024);

            assertThat(result).isSameAs(cachedResponse);
            verify(mgpProxyClient, never()).getArrivals(any(), any(), any(), any());
        }
    }

    @Nested
    @DisplayName("Live Upstream Integration Tests (Cache Miss)")
    class LiveUpstreamTests {

        @Test
        @DisplayName("Parses JSON array response, stores in Caffeine and fallback cache, and returns LIVE DTO")
        void parsesJsonArraySuccessfully() {
            String upstreamJson = """
                    [
                      {
                        "linea": "511",
                        "bandera": "A",
                        "minutos": 7,
                        "distancia": 1800,
                        "arribo": "7 min",
                        "coche": "045",
                        "adaptado": true
                      }
                    ]
                    """;

            when(cacheManager.getCache(CaffeineCacheConfig.ARRIVALS_CACHE)).thenReturn(cache);
            when(cache.get(CACHE_KEY)).thenReturn(null);
            when(mgpProxyClient.getArrivals(any(), eq("RecuperarProximosArribosW"), eq(STOP_1024), eq(LINE_511)))
                    .thenReturn(upstreamJson);

            ArrivalResponseDTO result = arrivalsService.getArrivals(LINE_511, STOP_1024);

            assertThat(result).isNotNull();
            assertThat(result.lineCode()).isEqualTo(LINE_511);
            assertThat(result.stopId()).isEqualTo(STOP_1024);
            assertThat(result.branch()).isEqualTo("A");
            assertThat(result.status()).isEqualTo(TelemetryStatus.LIVE);
            assertThat(result.deltaMinutes()).isZero();
            assertThat(result.arrivals()).hasSize(1);

            BusArrivalItemDTO item = result.arrivals().getFirst();
            assertThat(item.lineCode()).isEqualTo(LINE_511);
            assertThat(item.branch()).isEqualTo("A");
            assertThat(item.remainingMinutes()).isEqualTo(7);
            assertThat(item.distanceMeters()).isEqualTo(1800);
            assertThat(item.estimatedArrivalTime()).isEqualTo("7 min");
            assertThat(item.vehicleUnit()).isEqualTo("045");
            assertThat(item.accessible()).isTrue();
            assertThat(item.status()).isEqualTo(TelemetryStatus.LIVE);

            // Verify cache put and fallback store population
            verify(cache).put(CACHE_KEY, result);
            assertThat(arrivalsService.getLastKnownTelemetryStore()).containsKey(CACHE_KEY);
            assertThat(arrivalsService.getLastKnownTelemetryStore().get(CACHE_KEY)).isSameAs(result);
        }

        @Test
        @DisplayName("Parses wrapped JSON object response with arribos array")
        void parsesWrappedJsonObjectSuccessfully() {
            String upstreamJson = """
                    {
                      "arribos": [
                        {
                          "linea": "511",
                          "bandera": "B",
                          "minutos": 12,
                          "distancia": 3200,
                          "arribo": "12 min",
                          "coche": "108",
                          "adaptado": false
                        }
                      ]
                    }
                    """;

            when(cacheManager.getCache(CaffeineCacheConfig.ARRIVALS_CACHE)).thenReturn(cache);
            when(cache.get(CACHE_KEY)).thenReturn(null);
            when(mgpProxyClient.getArrivals(any(), eq("RecuperarProximosArribosW"), eq(STOP_1024), eq(LINE_511)))
                    .thenReturn(upstreamJson);

            ArrivalResponseDTO result = arrivalsService.getArrivals(LINE_511, STOP_1024);

            assertThat(result).isNotNull();
            assertThat(result.branch()).isEqualTo("B");
            assertThat(result.status()).isEqualTo(TelemetryStatus.LIVE);
            assertThat(result.arrivals()).hasSize(1);
            assertThat(result.arrivals().getFirst().remainingMinutes()).isEqualTo(12);
            assertThat(result.arrivals().getFirst().accessible()).isFalse();
        }

        @Test
        @DisplayName("Handles empty upstream array cleanly with empty arrivals list and null branch")
        void handlesEmptyUpstreamArrayCleanly() {
            when(cacheManager.getCache(CaffeineCacheConfig.ARRIVALS_CACHE)).thenReturn(cache);
            when(cache.get(CACHE_KEY)).thenReturn(null);
            when(mgpProxyClient.getArrivals(any(), eq("RecuperarProximosArribosW"), eq(STOP_1024), eq(LINE_511)))
                    .thenReturn("[]");

            ArrivalResponseDTO result = arrivalsService.getArrivals(LINE_511, STOP_1024);

            assertThat(result).isNotNull();
            assertThat(result.lineCode()).isEqualTo(LINE_511);
            assertThat(result.stopId()).isEqualTo(STOP_1024);
            assertThat(result.branch()).isNull();
            assertThat(result.status()).isEqualTo(TelemetryStatus.LIVE);
            assertThat(result.arrivals()).isEmpty();
            assertThat(arrivalsService.getLastKnownTelemetryStore().get(CACHE_KEY)).isSameAs(result);
        }

        @Test
        @DisplayName("Parses real MGP PascalCase upstream JSON response accurately")
        void parsesPascalCaseUpstreamSuccessfully() {
            String pascalJson = """
                    {
                      "CodigoEstado": 0,
                      "MensajeEstado": "Ok",
                      "arribos": [
                        {
                          "DescripcionLinea": "511",
                          "DescripcionBandera": "A",
                          "Arribo": "53 min. aprox.",
                          "Latitud": "-38.000",
                          "Longitud": "-57.555",
                          "LatitudParada": "-38.001",
                          "LongitudParada": "-57.556",
                          "EsAdaptado": "true",
                          "IdentificadorCoche": "104",
                          "UltimaFechaHoraGPS": "20/09/2026 11:15:00",
                          "CodigoLineaParada": "511"
                        }
                      ]
                    }
                    """;

            when(cacheManager.getCache(CaffeineCacheConfig.ARRIVALS_CACHE)).thenReturn(cache);
            when(cache.get(CACHE_KEY)).thenReturn(null);
            when(mgpProxyClient.getArrivals(any(), eq("RecuperarProximosArribosW"), eq(STOP_1024), eq(LINE_511)))
                    .thenReturn(pascalJson);

            ArrivalResponseDTO result = arrivalsService.getArrivals(LINE_511, STOP_1024);

            assertThat(result).isNotNull();
            assertThat(result.lineCode()).isEqualTo(LINE_511);
            assertThat(result.branch()).isEqualTo("A");
            assertThat(result.arrivals()).hasSize(1);

            BusArrivalItemDTO item = result.arrivals().getFirst();
            assertThat(item.lineCode()).isEqualTo("511");
            assertThat(item.branch()).isEqualTo("A");
            assertThat(item.remainingMinutes()).isEqualTo(53);
            assertThat(item.vehicleUnit()).isEqualTo("104");
            assertThat(item.accessible()).isTrue();
        }

        @Test
        @DisplayName("Filters arrivals by target branch when getArrivals with bandera is called")
        void filtersByBranchWhenBanderaProvided() {
            String upstreamJson = """
                    {
                      "arribos": [
                        {
                          "DescripcionLinea": "511",
                          "DescripcionBandera": "A",
                          "Arribo": "5 min",
                          "IdentificadorCoche": "01",
                          "EsAdaptado": "true"
                        },
                        {
                          "DescripcionLinea": "511",
                          "DescripcionBandera": "B",
                          "Arribo": "10 min",
                          "IdentificadorCoche": "02",
                          "EsAdaptado": "false"
                        }
                      ]
                    }
                    """;

            when(cacheManager.getCache(CaffeineCacheConfig.ARRIVALS_CACHE)).thenReturn(cache);
            when(cache.get(CACHE_KEY)).thenReturn(null);
            when(mgpProxyClient.getArrivals(any(), eq("RecuperarProximosArribosW"), eq(STOP_1024), eq(LINE_511)))
                    .thenReturn(upstreamJson);

            ArrivalResponseDTO result = arrivalsService.getArrivals(LINE_511, STOP_1024, "a");

            assertThat(result).isNotNull();
            assertThat(result.branch()).isEqualTo("a");
            assertThat(result.arrivals()).hasSize(1);
            assertThat(result.arrivals().getFirst().branch()).isEqualTo("A");
            assertThat(result.arrivals().getFirst().vehicleUnit()).isEqualTo("01");
        }
    }

    @Nested
    @DisplayName("Fallback Extrapolation & Upstream Failure Tests")
    class FallbackAndFailureTests {

        @Test
        @DisplayName("When upstream throws exception and last known telemetry exists, delegates to ExtrapolationEngine")
        void upstreamExceptionWithFallback_InvokesExtrapolationEngine() {
            BusArrivalItemDTO oldItem = new BusArrivalItemDTO(LINE_511, "A", 15, 3000, "15 min", "001", true, TelemetryStatus.LIVE);
            ArrivalResponseDTO previousTelemetry = new ArrivalResponseDTO(LINE_511, STOP_1024, "A", TelemetryStatus.LIVE,
                    Instant.now().minusSeconds(300), 0L, List.of(oldItem));

            // Seed fallback store
            arrivalsService.getLastKnownTelemetryStore().put(CACHE_KEY, previousTelemetry);

            when(cacheManager.getCache(CaffeineCacheConfig.ARRIVALS_CACHE)).thenReturn(cache);
            when(cache.get(CACHE_KEY)).thenReturn(null);
            when(mgpProxyClient.getArrivals(any(), eq("RecuperarProximosArribosW"), eq(STOP_1024), eq(LINE_511)))
                    .thenThrow(new RuntimeException("Connection timed out to Go proxy"));

            BusArrivalItemDTO extrapolatedItem = new BusArrivalItemDTO(LINE_511, "A", 10, 3000, "15 min", "001", true, TelemetryStatus.ESTIMATED_FALLBACK);
            ArrivalResponseDTO extrapolatedResponse = new ArrivalResponseDTO(LINE_511, STOP_1024, "A", TelemetryStatus.ESTIMATED_FALLBACK,
                    previousTelemetry.timestamp(), 5L, List.of(extrapolatedItem));

            when(extrapolationEngine.extrapolate(eq(previousTelemetry), any(Instant.class)))
                    .thenReturn(extrapolatedResponse);

            ArrivalResponseDTO result = arrivalsService.getArrivals(LINE_511, STOP_1024);

            assertThat(result).isNotNull();
            assertThat(result.status()).isEqualTo(TelemetryStatus.ESTIMATED_FALLBACK);
            assertThat(result.deltaMinutes()).isEqualTo(5L);
            assertThat(result.arrivals().getFirst().remainingMinutes()).isEqualTo(10);
            assertThat(result.arrivals().getFirst().status()).isEqualTo(TelemetryStatus.ESTIMATED_FALLBACK);

            verify(extrapolationEngine).extrapolate(eq(previousTelemetry), any(Instant.class));
        }

        @Test
        @DisplayName("When upstream throws exception and no fallback telemetry exists, throws UpstreamServiceException")
        void upstreamExceptionWithoutFallback_ThrowsUpstreamServiceException() {
            when(cacheManager.getCache(CaffeineCacheConfig.ARRIVALS_CACHE)).thenReturn(cache);
            when(cache.get(CACHE_KEY)).thenReturn(null);
            when(mgpProxyClient.getArrivals(any(), eq("RecuperarProximosArribosW"), eq(STOP_1024), eq(LINE_511)))
                    .thenThrow(new RuntimeException("502 Bad Gateway"));

            assertThatThrownBy(() -> arrivalsService.getArrivals(LINE_511, STOP_1024))
                    .isInstanceOf(UpstreamServiceException.class)
                    .hasMessageContaining("Upstream proxy failed and no cached telemetry exists for stop: 1024, line: 511");
        }

        @Test
        @DisplayName("When upstream returns invalid JSON and fallback telemetry exists, extrapolates from fallback")
        void malformedJsonWithFallback_InvokesExtrapolationEngine() {
            BusArrivalItemDTO oldItem = new BusArrivalItemDTO(LINE_511, "A", 8, 1200, "8 min", "002", false, TelemetryStatus.LIVE);
            ArrivalResponseDTO previousTelemetry = new ArrivalResponseDTO(LINE_511, STOP_1024, "A", TelemetryStatus.LIVE,
                    Instant.now().minusSeconds(180), 0L, List.of(oldItem));

            arrivalsService.getLastKnownTelemetryStore().put(CACHE_KEY, previousTelemetry);

            when(cacheManager.getCache(CaffeineCacheConfig.ARRIVALS_CACHE)).thenReturn(cache);
            when(cache.get(CACHE_KEY)).thenReturn(null);
            when(mgpProxyClient.getArrivals(any(), eq("RecuperarProximosArribosW"), eq(STOP_1024), eq(LINE_511)))
                    .thenReturn("{this is not valid json");

            ArrivalResponseDTO fallbackResponse = new ArrivalResponseDTO(LINE_511, STOP_1024, "A", TelemetryStatus.ESTIMATED_FALLBACK,
                    previousTelemetry.timestamp(), 3L, List.of(oldItem));

            when(extrapolationEngine.extrapolate(eq(previousTelemetry), any(Instant.class)))
                    .thenReturn(fallbackResponse);

            ArrivalResponseDTO result = arrivalsService.getArrivals(LINE_511, STOP_1024);

            assertThat(result).isNotNull();
            assertThat(result.status()).isEqualTo(TelemetryStatus.ESTIMATED_FALLBACK);
            verify(extrapolationEngine).extrapolate(eq(previousTelemetry), any(Instant.class));
        }

        @Test
        @DisplayName("When upstream returns empty string and no fallback exists, throws UpstreamServiceException")
        void emptyResponseWithoutFallback_ThrowsUpstreamServiceException() {
            when(cacheManager.getCache(CaffeineCacheConfig.ARRIVALS_CACHE)).thenReturn(cache);
            when(cache.get(CACHE_KEY)).thenReturn(null);
            when(mgpProxyClient.getArrivals(any(), eq("RecuperarProximosArribosW"), eq(STOP_1024), eq(LINE_511)))
                    .thenReturn("   ");

            assertThatThrownBy(() -> arrivalsService.getArrivals(LINE_511, STOP_1024))
                    .isInstanceOf(UpstreamServiceException.class)
                    .hasMessageContaining("Upstream proxy failed and no cached telemetry exists for stop: 1024, line: 511");
        }
    }
}
