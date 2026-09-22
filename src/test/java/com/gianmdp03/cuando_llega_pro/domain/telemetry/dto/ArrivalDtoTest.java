package com.gianmdp03.cuando_llega_pro.domain.telemetry.dto;

import com.gianmdp03.cuando_llega_pro.domain.telemetry.model.TelemetryStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Arrival DTO and Telemetry Models Test Suite")
class ArrivalDtoTest {

    @Nested
    @DisplayName("TelemetryStatus Enum")
    class TelemetryStatusTests {

        @Test
        @DisplayName("TelemetryStatus enum values check")
        void enumContainsRequiredValues() {
            assertThat(TelemetryStatus.values()).containsExactly(
                    TelemetryStatus.LIVE,
                    TelemetryStatus.ESTIMATED_FALLBACK,
                    TelemetryStatus.EXPIRED,
                    TelemetryStatus.UNAVAILABLE
            );
        }
    }

    @Nested
    @DisplayName("BusArrivalItemDTO Tests")
    class BusArrivalItemDtoTests {

        @Test
        @DisplayName("Full constructor and getters")
        void fullConstructorMapsAllFields() {
            Instant now = Instant.now();
            BusArrivalItemDTO item = new BusArrivalItemDTO(
                    "bus-01",
                    "511",
                    "Puerto",
                    14L,
                    3200,
                    TelemetryStatus.LIVE,
                    now
            );

            assertThat(item.busId()).isEqualTo("bus-01");
            assertThat(item.line()).isEqualTo("511");
            assertThat(item.destination()).isEqualTo("Puerto");
            assertThat(item.remainingMinutes()).isEqualTo(14L);
            assertThat(item.distanceMeters()).isEqualTo(3200);
            assertThat(item.status()).isEqualTo(TelemetryStatus.LIVE);
            assertThat(item.timestamp()).isEqualTo(now);
        }

        @Test
        @DisplayName("Overloaded constructors instantiate valid instances")
        void overloadedConstructorsWork() {
            BusArrivalItemDTO item1 = new BusArrivalItemDTO("bus-02", "512", "Centro", 10L, TelemetryStatus.LIVE);
            assertThat(item1.busId()).isEqualTo("bus-02");
            assertThat(item1.destination()).isEqualTo("Centro");
            assertThat(item1.remainingMinutes()).isEqualTo(10L);

            BusArrivalItemDTO item2 = new BusArrivalItemDTO("bus-03", "512", 8L, TelemetryStatus.LIVE);
            assertThat(item2.busId()).isEqualTo("bus-03");
            assertThat(item2.line()).isEqualTo("512");
            assertThat(item2.remainingMinutes()).isEqualTo(8L);

            BusArrivalItemDTO item3 = new BusArrivalItemDTO("bus-04", 5L, TelemetryStatus.LIVE);
            assertThat(item3.busId()).isEqualTo("bus-04");
            assertThat(item3.remainingMinutes()).isEqualTo(5L);
        }

        @Test
        @DisplayName("withStatusAndRemainingMinutes copies attributes while altering status and remaining minutes")
        void withStatusAndRemainingMinutesCreatesModifiedCopy() {
            Instant now = Instant.now();
            BusArrivalItemDTO item = new BusArrivalItemDTO(
                    "bus-01",
                    "511",
                    "Puerto",
                    14L,
                    3200,
                    TelemetryStatus.LIVE,
                    now
            );

            BusArrivalItemDTO updated = item.withStatusAndRemainingMinutes(TelemetryStatus.ESTIMATED_FALLBACK, 9L);

            assertThat(updated.busId()).isEqualTo("bus-01");
            assertThat(updated.line()).isEqualTo("511");
            assertThat(updated.destination()).isEqualTo("Puerto");
            assertThat(updated.distanceMeters()).isEqualTo(3200);
            assertThat(updated.timestamp()).isEqualTo(now);
            assertThat(updated.remainingMinutes()).isEqualTo(9L);
            assertThat(updated.status()).isEqualTo(TelemetryStatus.ESTIMATED_FALLBACK);
        }
    }

    @Nested
    @DisplayName("ArrivalResponseDTO Tests")
    class ArrivalResponseDtoTests {

        @Test
        @DisplayName("Compact constructor filters out null arrival items and treats null list as empty list")
        void compactConstructorEnsuresNullSafety() {
            ArrivalResponseDTO dtoWithNull = new ArrivalResponseDTO(
                    "STOP-1",
                    "511",
                    TelemetryStatus.LIVE,
                    Instant.now(),
                    0L,
                    null
            );

            assertThat(dtoWithNull.arrivals()).isNotNull().isEmpty();
            assertThat(dtoWithNull.items()).isNotNull().isEmpty();

            List<BusArrivalItemDTO> listWithNulls = new ArrayList<>();
            listWithNulls.add(new BusArrivalItemDTO("bus-1", 10L, TelemetryStatus.LIVE));
            listWithNulls.add(null);

            ArrivalResponseDTO dtoWithListNulls = new ArrivalResponseDTO(
                    "STOP-1",
                    "511",
                    TelemetryStatus.LIVE,
                    Instant.now(),
                    0L,
                    listWithNulls
            );

            assertThat(dtoWithListNulls.arrivals()).hasSize(1);
            assertThat(dtoWithListNulls.arrivals().getFirst().busId()).isEqualTo("bus-1");
        }

        @Test
        @DisplayName("Overloaded constructors set sensible defaults")
        void overloadedConstructorsSetDefaults() {
            Instant now = Instant.now();
            BusArrivalItemDTO bus = new BusArrivalItemDTO("b1", 10L, TelemetryStatus.LIVE);

            ArrivalResponseDTO dto1 = new ArrivalResponseDTO("STOP-1", "511", TelemetryStatus.LIVE, now, List.of(bus));
            assertThat(dto1.deltaMinutes()).isZero();
            assertThat(dto1.stopId()).isEqualTo("STOP-1");

            ArrivalResponseDTO dto2 = new ArrivalResponseDTO(TelemetryStatus.LIVE, now, 5L, List.of(bus));
            assertThat(dto2.stopId()).isNull();
            assertThat(dto2.deltaMinutes()).isEqualTo(5L);

            ArrivalResponseDTO dto3 = new ArrivalResponseDTO(TelemetryStatus.LIVE, now, List.of(bus));
            assertThat(dto3.deltaMinutes()).isZero();
            assertThat(dto3.arrivals()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("ArrivalItemDTO Interoperability Tests")
    class ArrivalItemDtoTests {

        @Test
        @DisplayName("Conversion between ArrivalItemDTO and BusArrivalItemDTO")
        void conversionWorksAccurately() {
            Instant now = Instant.now();
            BusArrivalItemDTO bus = new BusArrivalItemDTO("b1", "511", "Dest", 10L, 500, TelemetryStatus.LIVE, now);

            ArrivalItemDTO itemDto = ArrivalItemDTO.from(bus);
            assertThat(itemDto).isNotNull();
            assertThat(itemDto.busId()).isEqualTo("b1");
            assertThat(itemDto.line()).isEqualTo("511");
            assertThat(itemDto.destination()).isEqualTo("Dest");
            assertThat(itemDto.remainingMinutes()).isEqualTo(10L);
            assertThat(itemDto.distanceMeters()).isEqualTo(500);
            assertThat(itemDto.status()).isEqualTo(TelemetryStatus.LIVE);
            assertThat(itemDto.timestamp()).isEqualTo(now);

            BusArrivalItemDTO convertedBack = itemDto.toBusArrivalItemDTO();
            assertThat(convertedBack).isEqualTo(bus);

            assertThat(ArrivalItemDTO.from(null)).isNull();

            ArrivalItemDTO itemConstructors1 = new ArrivalItemDTO("b2", "512", 15L, TelemetryStatus.LIVE);
            assertThat(itemConstructors1.busId()).isEqualTo("b2");
            ArrivalItemDTO itemConstructors2 = new ArrivalItemDTO("b3", 20L, TelemetryStatus.LIVE);
            assertThat(itemConstructors2.busId()).isEqualTo("b3");
        }
    }
}
