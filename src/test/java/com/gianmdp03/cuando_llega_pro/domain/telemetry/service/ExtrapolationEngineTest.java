package com.gianmdp03.cuando_llega_pro.domain.telemetry.service;

import com.gianmdp03.cuando_llega_pro.domain.telemetry.dto.ArrivalResponseDTO;
import com.gianmdp03.cuando_llega_pro.domain.telemetry.dto.BusArrivalItemDTO;
import com.gianmdp03.cuando_llega_pro.domain.telemetry.model.TelemetryStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ExtrapolationEngine Test Suite")
class ExtrapolationEngineTest {

    private ExtrapolationEngine engine;
    private Instant baseTimestamp;

    @BeforeEach
    void setUp() {
        engine = new ExtrapolationEngine();
        baseTimestamp = Instant.parse("2026-09-19T14:00:00Z");
    }

    @Nested
    @DisplayName("Live Scenario Tests (Delta t <= 0)")
    class LiveScenarioTests {

        @Test
        @DisplayName("Delta t = 0: Status is LIVE, remaining minutes unchanged, deltaMinutes is 0")
        void deltaZero_ReturnsLiveStatusAndUnchangedMinutes() {
            BusArrivalItemDTO bus1 = new BusArrivalItemDTO("bus-101", "511", "Terminal", 12L, 2500, TelemetryStatus.LIVE, baseTimestamp);
            BusArrivalItemDTO bus2 = new BusArrivalItemDTO("bus-102", "512", "Puerto", 4L, 900, TelemetryStatus.LIVE, baseTimestamp);
            ArrivalResponseDTO cached = new ArrivalResponseDTO("STOP-100", "511", TelemetryStatus.LIVE, baseTimestamp, List.of(bus1, bus2));

            Instant currentTime = baseTimestamp;

            ArrivalResponseDTO result = engine.extrapolate(cached, currentTime);

            assertThat(result).isNotNull();
            assertThat(result.status()).isEqualTo(TelemetryStatus.LIVE);
            assertThat(result.deltaMinutes()).isZero();
            assertThat(result.stopId()).isEqualTo("STOP-100");
            assertThat(result.lineCode()).isEqualTo("511");
            assertThat(result.timestamp()).isEqualTo(baseTimestamp);

            assertThat(result.arrivals()).hasSize(2);
            assertThat(result.arrivals().get(0).busId()).isEqualTo("bus-101");
            assertThat(result.arrivals().get(0).remainingMinutes()).isEqualTo(12L);
            assertThat(result.arrivals().get(0).status()).isEqualTo(TelemetryStatus.LIVE);
            assertThat(result.arrivals().get(0).destination()).isEqualTo("Terminal");
            assertThat(result.arrivals().get(0).distanceMeters()).isEqualTo(2500);

            assertThat(result.arrivals().get(1).busId()).isEqualTo("bus-102");
            assertThat(result.arrivals().get(1).remainingMinutes()).isEqualTo(4L);
            assertThat(result.arrivals().get(1).status()).isEqualTo(TelemetryStatus.LIVE);
            assertThat(result.arrivals().get(1).destination()).isEqualTo("Puerto");
            assertThat(result.arrivals().get(1).distanceMeters()).isEqualTo(900);
        }

        @Test
        @DisplayName("Delta t < 60 seconds (sub-minute): Delta evaluates to 0 minutes, status is LIVE")
        void subMinuteDelta_EvaluatesToZeroAndReturnsLive() {
            BusArrivalItemDTO bus = new BusArrivalItemDTO("bus-101", "511", 10L, TelemetryStatus.LIVE);
            ArrivalResponseDTO cached = new ArrivalResponseDTO("STOP-100", "511", TelemetryStatus.LIVE, baseTimestamp, List.of(bus));

            Instant currentTime = baseTimestamp.plusSeconds(45);

            ArrivalResponseDTO result = engine.extrapolate(cached, currentTime);

            assertThat(result.status()).isEqualTo(TelemetryStatus.LIVE);
            assertThat(result.deltaMinutes()).isZero();
            assertThat(result.arrivals().getFirst().remainingMinutes()).isEqualTo(10L);
            assertThat(result.arrivals().getFirst().status()).isEqualTo(TelemetryStatus.LIVE);
        }
    }

    @Nested
    @DisplayName("Fallback Scenario Tests (0 < Delta t <= 25)")
    class FallbackScenarioTests {

        @Test
        @DisplayName("Delta t = 5: 5 minutes elapsed reduces original 12 min to 7 min")
        void fiveMinutesElapsed_ReducesOriginal12MinTo7Min() {
            BusArrivalItemDTO bus = new BusArrivalItemDTO("bus-101", "511", "Centro", 12L, 1800, TelemetryStatus.LIVE, baseTimestamp);
            ArrivalResponseDTO cached = new ArrivalResponseDTO("STOP-100", "511", TelemetryStatus.LIVE, baseTimestamp, List.of(bus));

            Instant currentTime = baseTimestamp.plus(Duration.ofMinutes(5));

            ArrivalResponseDTO result = engine.extrapolate(cached, currentTime);

            assertThat(result.status()).isEqualTo(TelemetryStatus.ESTIMATED_FALLBACK);
            assertThat(result.deltaMinutes()).isEqualTo(5L);
            assertThat(result.arrivals()).hasSize(1);

            BusArrivalItemDTO item = result.arrivals().getFirst();
            assertThat(item.busId()).isEqualTo("bus-101");
            assertThat(item.line()).isEqualTo("511");
            assertThat(item.destination()).isEqualTo("Centro");
            assertThat(item.remainingMinutes()).isEqualTo(7L);
            assertThat(item.status()).isEqualTo(TelemetryStatus.ESTIMATED_FALLBACK);
            assertThat(item.distanceMeters()).isEqualTo(1800);
        }

        @Test
        @DisplayName("Delta t = 15: 15 minutes elapsed reduces original 10 min to 0 min clamped")
        void fifteenMinutesElapsed_ReducesOriginal10MinToZeroClamped() {
            BusArrivalItemDTO bus = new BusArrivalItemDTO("bus-101", "511", "Luro", 10L, 500, TelemetryStatus.LIVE, baseTimestamp);
            ArrivalResponseDTO cached = new ArrivalResponseDTO("STOP-100", "511", TelemetryStatus.LIVE, baseTimestamp, List.of(bus));

            Instant currentTime = baseTimestamp.plus(Duration.ofMinutes(15));

            ArrivalResponseDTO result = engine.extrapolate(cached, currentTime);

            assertThat(result.status()).isEqualTo(TelemetryStatus.ESTIMATED_FALLBACK);
            assertThat(result.deltaMinutes()).isEqualTo(15L);
            assertThat(result.arrivals()).hasSize(1);

            BusArrivalItemDTO item = result.arrivals().getFirst();
            assertThat(item.remainingMinutes()).isZero();
            assertThat(item.status()).isEqualTo(TelemetryStatus.ESTIMATED_FALLBACK);
        }

        @Test
        @DisplayName("Delta t = 25: Upper threshold boundary retains ESTIMATED_FALLBACK and clamps/reduces accordingly")
        void twentyFiveMinutesElapsed_AtThresholdBoundary_RetainsFallback() {
            BusArrivalItemDTO busFar = new BusArrivalItemDTO("bus-201", "522", 30L, TelemetryStatus.LIVE);
            BusArrivalItemDTO busNear = new BusArrivalItemDTO("bus-202", "522", 20L, TelemetryStatus.LIVE);
            BusArrivalItemDTO busExact = new BusArrivalItemDTO("bus-203", "522", 25L, TelemetryStatus.LIVE);
            ArrivalResponseDTO cached = new ArrivalResponseDTO("STOP-200", "522", TelemetryStatus.LIVE, baseTimestamp, List.of(busFar, busNear, busExact));

            Instant currentTime = baseTimestamp.plus(Duration.ofMinutes(25));

            ArrivalResponseDTO result = engine.extrapolate(cached, currentTime);

            assertThat(result.status()).isEqualTo(TelemetryStatus.ESTIMATED_FALLBACK);
            assertThat(result.deltaMinutes()).isEqualTo(25L);

            assertThat(result.arrivals()).hasSize(3);
            assertThat(result.arrivals().get(0).remainingMinutes()).isEqualTo(5L);
            assertThat(result.arrivals().get(0).status()).isEqualTo(TelemetryStatus.ESTIMATED_FALLBACK);

            assertThat(result.arrivals().get(1).remainingMinutes()).isZero();
            assertThat(result.arrivals().get(1).status()).isEqualTo(TelemetryStatus.ESTIMATED_FALLBACK);

            assertThat(result.arrivals().get(2).remainingMinutes()).isZero();
            assertThat(result.arrivals().get(2).status()).isEqualTo(TelemetryStatus.ESTIMATED_FALLBACK);
        }

        @Test
        @DisplayName("Multiple items with mixed remaining times extrapolated accurately")
        void multipleItems_MixedRemainingTimes_ExtrapolatedAccurately() {
            BusArrivalItemDTO bus1 = new BusArrivalItemDTO("b1", "541", 20L, TelemetryStatus.LIVE);
            BusArrivalItemDTO bus2 = new BusArrivalItemDTO("b2", "541", 8L, TelemetryStatus.LIVE);
            BusArrivalItemDTO bus3 = new BusArrivalItemDTO("b3", "541", 2L, TelemetryStatus.LIVE);
            ArrivalResponseDTO cached = new ArrivalResponseDTO("STOP-300", "541", TelemetryStatus.LIVE, baseTimestamp, List.of(bus1, bus2, bus3));

            Instant currentTime = baseTimestamp.plus(Duration.ofMinutes(7));

            ArrivalResponseDTO result = engine.extrapolate(cached, currentTime);

            assertThat(result.status()).isEqualTo(TelemetryStatus.ESTIMATED_FALLBACK);
            assertThat(result.deltaMinutes()).isEqualTo(7L);

            assertThat(result.arrivals().get(0).remainingMinutes()).isEqualTo(13L);
            assertThat(result.arrivals().get(1).remainingMinutes()).isEqualTo(1L);
            assertThat(result.arrivals().get(2).remainingMinutes()).isZero();

            result.arrivals().forEach(item -> assertThat(item.status()).isEqualTo(TelemetryStatus.ESTIMATED_FALLBACK));
        }
    }

    @Nested
    @DisplayName("Expired Scenario Tests (Delta t > 25)")
    class ExpiredScenarioTests {

        @Test
        @DisplayName("Delta t = 26: Signal lost for 26 minutes tags overall response and all items EXPIRED with remaining minutes 0")
        void twentySixMinutesElapsed_StatusExpiredAndMinutesSetToZero() {
            BusArrivalItemDTO bus1 = new BusArrivalItemDTO("bus-301", "551", "Casino", 18L, 3000, TelemetryStatus.LIVE, baseTimestamp);
            BusArrivalItemDTO bus2 = new BusArrivalItemDTO("bus-302", "551", "Playa Grande", 35L, 6500, TelemetryStatus.LIVE, baseTimestamp);
            ArrivalResponseDTO cached = new ArrivalResponseDTO("STOP-400", "551", TelemetryStatus.LIVE, baseTimestamp, List.of(bus1, bus2));

            Instant currentTime = baseTimestamp.plus(Duration.ofMinutes(26));

            ArrivalResponseDTO result = engine.extrapolate(cached, currentTime);

            assertThat(result.status()).isEqualTo(TelemetryStatus.EXPIRED);
            assertThat(result.deltaMinutes()).isEqualTo(26L);
            assertThat(result.arrivals()).hasSize(2);

            for (BusArrivalItemDTO item : result.arrivals()) {
                assertThat(item.status()).isEqualTo(TelemetryStatus.EXPIRED);
                assertThat(item.remainingMinutes()).isZero();
            }
        }

        @Test
        @DisplayName("Delta t = 120 (far expired): Status EXPIRED, all items tagged EXPIRED and remainingMinutes set to 0")
        void farExpired_TagsAllExpiredWithZeroMinutes() {
            BusArrivalItemDTO bus = new BusArrivalItemDTO("bus-301", "551", 45L, TelemetryStatus.LIVE);
            ArrivalResponseDTO cached = new ArrivalResponseDTO("STOP-400", "551", TelemetryStatus.LIVE, baseTimestamp, List.of(bus));

            Instant currentTime = baseTimestamp.plus(Duration.ofHours(2));

            ArrivalResponseDTO result = engine.extrapolate(cached, currentTime);

            assertThat(result.status()).isEqualTo(TelemetryStatus.EXPIRED);
            assertThat(result.deltaMinutes()).isEqualTo(120L);
            assertThat(result.arrivals().getFirst().status()).isEqualTo(TelemetryStatus.EXPIRED);
            assertThat(result.arrivals().getFirst().remainingMinutes()).isZero();
        }
    }

    @Nested
    @DisplayName("Edge Conditions Tests")
    class EdgeConditionsTests {

        @Test
        @DisplayName("Empty arrivals list: Returns empty extrapolated items with corresponding status")
        void emptyArrivalsList_HandledSafely() {
            ArrivalResponseDTO cachedEmpty = new ArrivalResponseDTO("STOP-EMPTY", "500", TelemetryStatus.LIVE, baseTimestamp, List.of());

            ArrivalResponseDTO liveResult = engine.extrapolate(cachedEmpty, baseTimestamp);
            assertThat(liveResult.status()).isEqualTo(TelemetryStatus.LIVE);
            assertThat(liveResult.deltaMinutes()).isZero();
            assertThat(liveResult.arrivals()).isEmpty();
            assertThat(liveResult.items()).isEmpty();

            ArrivalResponseDTO fallbackResult = engine.extrapolate(cachedEmpty, baseTimestamp.plus(Duration.ofMinutes(10)));
            assertThat(fallbackResult.status()).isEqualTo(TelemetryStatus.ESTIMATED_FALLBACK);
            assertThat(fallbackResult.deltaMinutes()).isEqualTo(10L);
            assertThat(fallbackResult.arrivals()).isEmpty();

            ArrivalResponseDTO expiredResult = engine.extrapolate(cachedEmpty, baseTimestamp.plus(Duration.ofMinutes(30)));
            assertThat(expiredResult.status()).isEqualTo(TelemetryStatus.EXPIRED);
            assertThat(expiredResult.deltaMinutes()).isEqualTo(30L);
            assertThat(expiredResult.arrivals()).isEmpty();
        }

        @Test
        @DisplayName("Negative delta clock skew: Current time before cached timestamp returns LIVE with unchanged remaining minutes")
        void negativeDeltaClockSkew_ReturnsLiveAndUnchangedMinutes() {
            BusArrivalItemDTO bus = new BusArrivalItemDTO("bus-skew", "562", 15L, TelemetryStatus.LIVE);
            ArrivalResponseDTO cached = new ArrivalResponseDTO("STOP-SKEW", "562", TelemetryStatus.LIVE, baseTimestamp, List.of(bus));

            Instant currentTime = baseTimestamp.minus(Duration.ofMinutes(5));

            ArrivalResponseDTO result = engine.extrapolate(cached, currentTime);

            assertThat(result.status()).isEqualTo(TelemetryStatus.LIVE);
            assertThat(result.deltaMinutes()).isEqualTo(-5L);
            assertThat(result.arrivals()).hasSize(1);
            assertThat(result.arrivals().getFirst().remainingMinutes()).isEqualTo(15L);
            assertThat(result.arrivals().getFirst().status()).isEqualTo(TelemetryStatus.LIVE);
        }

        @Test
        @DisplayName("Null safety: Null cachedTelemetry throws IllegalArgumentException")
        void nullCachedTelemetry_ThrowsIllegalArgumentException() {
            assertThatThrownBy(() -> engine.extrapolate(null, baseTimestamp))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("cachedTelemetry must not be null");
        }

        @Test
        @DisplayName("Null safety: Null currentTime throws IllegalArgumentException")
        void nullCurrentTime_ThrowsIllegalArgumentException() {
            ArrivalResponseDTO cached = new ArrivalResponseDTO("STOP-1", "501", TelemetryStatus.LIVE, baseTimestamp, List.of());

            assertThatThrownBy(() -> engine.extrapolate(cached, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("currentTime must not be null");
        }

        @Test
        @DisplayName("Null safety: Null timestamp on cachedTelemetry throws IllegalArgumentException")
        void nullCachedTimestamp_ThrowsIllegalArgumentException() {
            ArrivalResponseDTO cachedWithNullTimestamp = new ArrivalResponseDTO("STOP-1", "501", TelemetryStatus.LIVE, null, 0L, List.of());

            assertThatThrownBy(() -> engine.extrapolate(cachedWithNullTimestamp, baseTimestamp))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("cachedTelemetry timestamp must not be null");
        }

        @Test
        @DisplayName("Null safety: Null arrivals in cachedTelemetry handled gracefully without NPE")
        void nullArrivalsListInCachedTelemetry_HandledSafely() {
            ArrivalResponseDTO cachedWithNullArrivals = new ArrivalResponseDTO(
                    "STOP-NULL",
                    "501",
                    TelemetryStatus.LIVE,
                    baseTimestamp,
                    0L,
                    null
            );

            ArrivalResponseDTO result = engine.extrapolate(cachedWithNullArrivals, baseTimestamp.plus(Duration.ofMinutes(5)));

            assertThat(result).isNotNull();
            assertThat(result.arrivals()).isEmpty();
            assertThat(result.status()).isEqualTo(TelemetryStatus.ESTIMATED_FALLBACK);
            assertThat(result.deltaMinutes()).isEqualTo(5L);
        }

        @Test
        @DisplayName("Null safety: Null element inside arrivals collection is filtered out safely")
        void nullElementInsideArrivals_FilteredSafely() {
            List<BusArrivalItemDTO> listWithNull = new ArrayList<>();
            listWithNull.add(new BusArrivalItemDTO("bus-valid", "511", 12L, TelemetryStatus.LIVE));
            listWithNull.add(null);

            ArrivalResponseDTO cached = new ArrivalResponseDTO(
                    "STOP-1",
                    "511",
                    TelemetryStatus.LIVE,
                    baseTimestamp,
                    0L,
                    listWithNull
            );

            ArrivalResponseDTO result = engine.extrapolate(cached, baseTimestamp.plus(Duration.ofMinutes(4)));

            assertThat(result.arrivals()).hasSize(1);
            assertThat(result.arrivals().getFirst().busId()).isEqualTo("bus-valid");
            assertThat(result.arrivals().getFirst().remainingMinutes()).isEqualTo(8L);
        }

        @Test
        @DisplayName("Single-argument extrapolate overload executes against Instant.now()")
        void singleArgExtrapolate_ExecutesAgainstCurrentTime() {
            Instant recentTimestamp = Instant.now().minusSeconds(120);
            BusArrivalItemDTO bus = new BusArrivalItemDTO("bus-now", "511", 10L, TelemetryStatus.LIVE);
            ArrivalResponseDTO cached = new ArrivalResponseDTO("STOP-NOW", "511", TelemetryStatus.LIVE, recentTimestamp, List.of(bus));

            ArrivalResponseDTO result = engine.extrapolate(cached);

            assertThat(result).isNotNull();
            assertThat(result.deltaMinutes()).isGreaterThanOrEqualTo(2L);
            assertThat(result.status()).isIn(TelemetryStatus.ESTIMATED_FALLBACK, TelemetryStatus.LIVE);
        }

        @Test
        @DisplayName("extrapolateArrivals alias methods delegate accurately")
        void extrapolateArrivalsAliases_DelegateAccurately() {
            BusArrivalItemDTO bus = new BusArrivalItemDTO("bus-alias", "511", 12L, TelemetryStatus.LIVE);
            ArrivalResponseDTO cached = new ArrivalResponseDTO("STOP-ALIAS", "511", TelemetryStatus.LIVE, baseTimestamp, List.of(bus));

            ArrivalResponseDTO resultWithTime = engine.extrapolateArrivals(cached, baseTimestamp.plus(Duration.ofMinutes(3)));
            assertThat(resultWithTime.status()).isEqualTo(TelemetryStatus.ESTIMATED_FALLBACK);
            assertThat(resultWithTime.deltaMinutes()).isEqualTo(3L);
            assertThat(resultWithTime.arrivals().getFirst().remainingMinutes()).isEqualTo(9L);

            ArrivalResponseDTO resultNow = engine.extrapolateArrivals(cached);
            assertThat(resultNow).isNotNull();
        }
    }
}
