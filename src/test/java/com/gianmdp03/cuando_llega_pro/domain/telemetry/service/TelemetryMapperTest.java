package com.gianmdp03.cuando_llega_pro.domain.telemetry.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gianmdp03.cuando_llega_pro.domain.telemetry.dto.ArrivalDTO;
import com.gianmdp03.cuando_llega_pro.domain.telemetry.dto.BusArrivalItemDTO;
import com.gianmdp03.cuando_llega_pro.domain.telemetry.dto.MgpArriboRaw;
import com.gianmdp03.cuando_llega_pro.domain.telemetry.model.TelemetryStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TelemetryMapper Unit Tests")
class TelemetryMapperTest {

    private TelemetryMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new TelemetryMapper(new ObjectMapper());
    }

    @Nested
    @DisplayName("1. parseRemainingMinutes Tests")
    class ParseRemainingMinutesTests {

        @ParameterizedTest
        @ValueSource(strings = {
                "arribando",
                "ARRIBANDO",
                "Arribando",
                "coche arribando",
                "Vehículo arribando a la parada",
                "proximo",
                "PROXIMO",
                "Próximo",
                "próximo",
                "proximo a arribar"
        })
        @DisplayName("Returns 0 when arrival string contains 'arribando' or 'proximo' (case-insensitive)")
        void returnsZeroForArribandoOrProximo(String input) {
            Integer minutes = mapper.parseRemainingMinutes(input);
            assertThat(minutes).isEqualTo(0);
        }

        @Test
        @DisplayName("Extracts leading digits via Pattern '(\\d+)' from '53 min. aprox.' -> 53")
        void extractsLeadingDigitsFromSampleString() {
            Integer minutes = mapper.parseRemainingMinutes("53 min. aprox.");
            assertThat(minutes).isEqualTo(53);
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "5 min",
                "12 min.",
                "0 min",
                "99 minutos aproximadamente",
                "42",
                "   15 min   "
        })
        @DisplayName("Extracts leading digits from varied numeric arrival strings")
        void extractsDigitsSuccessfully(String input) {
            Integer minutes = mapper.parseRemainingMinutes(input);
            assertThat(minutes).isNotNull();
        }

        @Test
        @DisplayName("Returns null when arrival string contains no digits")
        void returnsNullWhenNoDigitsPresent() {
            assertThat(mapper.parseRemainingMinutes("Sin estimacion")).isNull();
            assertThat(mapper.parseRemainingMinutes("Fuera de servicio")).isNull();
            assertThat(mapper.parseRemainingMinutes("No disponible")).isNull();
        }

        @Test
        @DisplayName("Returns null for null or blank arrival string")
        void returnsNullForNullOrBlank() {
            assertThat(mapper.parseRemainingMinutes(null)).isNull();
            assertThat(mapper.parseRemainingMinutes("")).isNull();
            assertThat(mapper.parseRemainingMinutes("   ")).isNull();
        }
    }

    @Nested
    @DisplayName("2. parseGpsTimestamp Tests")
    class ParseGpsTimestampTests {

        @Test
        @DisplayName("Parses valid 'dd/MM/yyyy HH:mm:ss' to Instant in America/Argentina/Buenos_Aires timezone")
        void parsesValidGpsTimestamp() {
            String rawDate = "20/09/2026 11:15:00";
            Instant parsed = mapper.parseGpsTimestamp(rawDate);

            ZonedDateTime zdt = parsed.atZone(ZoneId.of("America/Argentina/Buenos_Aires"));
            assertThat(zdt.getYear()).isEqualTo(2026);
            assertThat(zdt.getMonthValue()).isEqualTo(9);
            assertThat(zdt.getDayOfMonth()).isEqualTo(20);
            assertThat(zdt.getHour()).isEqualTo(11);
            assertThat(zdt.getMinute()).isEqualTo(15);
            assertThat(zdt.getSecond()).isEqualTo(0);
        }

        @Test
        @DisplayName("Defaults to Instant.now() when timestamp string is null or blank")
        void defaultsToNowWhenTimestampMissing() {
            Instant before = Instant.now().minusSeconds(1);
            Instant resultNull = mapper.parseGpsTimestamp(null);
            Instant resultBlank = mapper.parseGpsTimestamp("   ");
            Instant after = Instant.now().plusSeconds(1);

            assertThat(resultNull).isBetween(before, after);
            assertThat(resultBlank).isBetween(before, after);
        }

        @Test
        @DisplayName("Defaults to Instant.now() when timestamp string is malformed")
        void defaultsToNowWhenTimestampMalformed() {
            Instant before = Instant.now().minusSeconds(1);
            Instant result = mapper.parseGpsTimestamp("not-a-date");
            Instant after = Instant.now().plusSeconds(1);

            assertThat(result).isBetween(before, after);
        }
    }

    @Nested
    @DisplayName("3. toArrivalDTO Mapping Tests")
    class ToArrivalDtoTests {

        @Test
        @DisplayName("Maps all fields accurately from raw MGP DTO")
        void mapsAllFieldsAccurately() {
            MgpArriboRaw raw = new MgpArriboRaw(
                    "511",
                    "A",
                    "53 min. aprox.",
                    "-38.000",
                    "-57.555",
                    "-38.001",
                    "-57.556",
                    "true",
                    "104",
                    "20/09/2026 11:15:00",
                    "511"
            );

            ArrivalDTO dto = mapper.toArrivalDTO(raw);

            assertThat(dto).isNotNull();
            assertThat(dto.lineCode()).isEqualTo("511");
            assertThat(dto.branch()).isEqualTo("A");
            assertThat(dto.vehicleUnit()).isEqualTo("104");
            assertThat(dto.accessible()).isTrue();
            assertThat(dto.remainingMinutes()).isEqualTo(53);
            assertThat(dto.timestamp()).isNotNull();
        }

        @Test
        @DisplayName("lineCode falls back to descripcionLinea when codigoLineaParada is null or blank")
        void lineCodeFallsBackToDescripcionLinea() {
            MgpArriboRaw rawWithoutCodigoLinea = new MgpArriboRaw(
                    "522",
                    "B",
                    "arribando",
                    null, null, null, null,
                    "false",
                    "099",
                    null,
                    null
            );

            ArrivalDTO dto = mapper.toArrivalDTO(rawWithoutCodigoLinea);

            assertThat(dto.lineCode()).isEqualTo("522");
            assertThat(dto.branch()).isEqualTo("B");
            assertThat(dto.remainingMinutes()).isEqualTo(0);
            assertThat(dto.accessible()).isFalse();
            assertThat(dto.vehicleUnit()).isEqualTo("099");
        }

        @Test
        @DisplayName("lineCode falls back to supplied fallbackLineCode when both raw fields are absent")
        void lineCodeFallsBackToMethodArgument() {
            MgpArriboRaw rawEmpty = new MgpArriboRaw(
                    null,
                    "C",
                    "10 min",
                    null, null, null, null,
                    "TRUE",
                    "050",
                    null,
                    "   "
            );

            ArrivalDTO dto = mapper.toArrivalDTO(rawEmpty, "543");

            assertThat(dto.lineCode()).isEqualTo("543");
            assertThat(dto.accessible()).isTrue();
            assertThat(dto.remainingMinutes()).isEqualTo(10);
        }

        @Test
        @DisplayName("Returns null when raw DTO is null")
        void returnsNullWhenRawIsNull() {
            assertThat(mapper.toArrivalDTO(null)).isNull();
        }
    }

    @Nested
    @DisplayName("4. Branch Filtering Tests")
    class BranchFilteringTests {

        @Test
        @DisplayName("matchesBranch compares case-insensitively")
        void matchesBranchCaseInsensitively() {
            assertThat(mapper.matchesBranch("A", "A")).isTrue();
            assertThat(mapper.matchesBranch("a", "A")).isTrue();
            assertThat(mapper.matchesBranch("A", "a")).isTrue();
            assertThat(mapper.matchesBranch("  Rapido  ", "rapido")).isTrue();
            assertThat(mapper.matchesBranch("A", "B")).isFalse();
        }

        @Test
        @DisplayName("matchesBranch includes all branches when preset bandera is empty or null")
        void matchesBranchIncludesAllWhenPresetBanderaEmptyOrNull() {
            assertThat(mapper.matchesBranch("A", null)).isTrue();
            assertThat(mapper.matchesBranch("A", "")).isTrue();
            assertThat(mapper.matchesBranch("A", "   ")).isTrue();
            assertThat(mapper.matchesBranch(null, null)).isTrue();
            assertThat(mapper.matchesBranch(null, "")).isTrue();
        }

        @Test
        @DisplayName("matchesBranch returns false when arrival branch is null and preset bandera is specified")
        void matchesBranchReturnsFalseWhenArrivalBranchNull() {
            assertThat(mapper.matchesBranch(null, "A")).isFalse();
        }

        @Test
        @DisplayName("filterByBranch filters ArrivalDTO list by target branch")
        void filterByBranchFiltersArrivalList() {
            ArrivalDTO itemA1 = new ArrivalDTO("511", "A", "01", true, 5, Instant.now());
            ArrivalDTO itemA2 = new ArrivalDTO("511", "a", "02", false, 12, Instant.now());
            ArrivalDTO itemB = new ArrivalDTO("511", "B", "03", true, 20, Instant.now());

            List<ArrivalDTO> list = List.of(itemA1, itemA2, itemB);

            List<ArrivalDTO> filteredA = mapper.filterByBranch(list, "A");
            assertThat(filteredA).containsExactly(itemA1, itemA2);

            List<ArrivalDTO> filteredAllNull = mapper.filterByBranch(list, null);
            assertThat(filteredAllNull).hasSize(3);

            List<ArrivalDTO> filteredAllBlank = mapper.filterByBranch(list, "");
            assertThat(filteredAllBlank).hasSize(3);
        }

        @Test
        @DisplayName("filterBusArrivalsByBranch filters BusArrivalItemDTO list by target branch")
        void filterBusArrivalsByBranchFiltersBusList() {
            BusArrivalItemDTO busA = new BusArrivalItemDTO("511", "A", 5, 1000, "5 min", "01", true, TelemetryStatus.LIVE);
            BusArrivalItemDTO busB = new BusArrivalItemDTO("511", "B", 10, 2000, "10 min", "02", false, TelemetryStatus.LIVE);

            List<BusArrivalItemDTO> filtered = mapper.filterBusArrivalsByBranch(List.of(busA, busB), "a");
            assertThat(filtered).containsExactly(busA);
        }
    }

    @Nested
    @DisplayName("5. parseRawArrivals JSON Tests")
    class ParseRawArrivalsTests {

        @Test
        @DisplayName("Parses upstream JSON wrapped in object with arribos array and PascalCase keys")
        void parsesUpstreamJsonObject() throws Exception {
            String json = """
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

            List<MgpArriboRaw> rawList = mapper.parseRawArrivals(json);

            assertThat(rawList).hasSize(1);
            MgpArriboRaw raw = rawList.getFirst();
            assertThat(raw.descripcionLinea()).isEqualTo("511");
            assertThat(raw.descripcionBandera()).isEqualTo("A");
            assertThat(raw.arribo()).isEqualTo("53 min. aprox.");
            assertThat(raw.identificadorCoche()).isEqualTo("104");
            assertThat(raw.esAdaptado()).isEqualTo("true");
            assertThat(raw.codigoLineaParada()).isEqualTo("511");
        }

        @Test
        @DisplayName("Parses upstream bare JSON array")
        void parsesUpstreamJsonArray() throws Exception {
            String json = """
                    [
                      {
                        "DescripcionLinea": "522",
                        "DescripcionBandera": "B",
                        "Arribo": "arribando",
                        "EsAdaptado": "false",
                        "IdentificadorCoche": "088"
                      }
                    ]
                    """;

            List<MgpArriboRaw> rawList = mapper.parseRawArrivals(json);

            assertThat(rawList).hasSize(1);
            assertThat(rawList.getFirst().descripcionLinea()).isEqualTo("522");
            assertThat(rawList.getFirst().arribo()).isEqualTo("arribando");
        }

        @Test
        @DisplayName("Parses enriched HAR telemetry JSON with driver ID, deviation, and stop coordinates")
        void parsesEnrichedHarJson() throws Exception {
            String json = """
                    {
                      "CodigoEstado": 0,
                      "MensajeEstado": "ok",
                      "arribos": [
                        {
                          "DescripcionLinea": "511",
                          "DescripcionBandera": "A EDISON",
                          "Arribo": "23 min. aprox.",
                          "Latitud": "-37.986935",
                          "Longitud": "-57.569492",
                          "LatitudParada": "-38.029881",
                          "LongitudParada": "-57.537932",
                          "DescripcionCortaBandera": "A EDISON",
                          "DescripcionCartelBandera": "A EDISON",
                          "EsAdaptado": "False",
                          "IdentificadorCoche": "1552",
                          "IdentificadorChofer": "PE,509",
                          "DesvioHorario": "+01:16",
                          "UltimaFechaHoraGPS": "20/09/2026 14:19:22",
                          "CodigoLineaParada": "98"
                        }
                      ]
                    }
                    """;

            List<MgpArriboRaw> rawList = mapper.parseRawArrivals(json);
            assertThat(rawList).hasSize(1);

            MgpArriboRaw raw = rawList.getFirst();
            assertThat(raw.identificadorCoche()).isEqualTo("1552");
            assertThat(raw.identificadorChofer()).isEqualTo("PE,509");
            assertThat(raw.desvioHorario()).isEqualTo("+01:16");
            assertThat(raw.latitudParada()).isEqualTo("-38.029881");
            assertThat(raw.longitudParada()).isEqualTo("-57.537932");

            BusArrivalItemDTO item = mapper.toBusArrivalItemDTO(raw, TelemetryStatus.LIVE, "511");
            assertThat(item.vehicleUnit()).isEqualTo("1552");
            assertThat(item.driverId()).isEqualTo("PE,509");
            assertThat(item.scheduleDeviation()).isEqualTo("+01:16");
            assertThat(item.accessible()).isFalse();
            assertThat(item.stopLatitude()).isEqualTo(-38.029881);
            assertThat(item.stopLongitude()).isEqualTo(-57.537932);
        }

        @Test
        @DisplayName("Normalizes boolean EsAdaptado correctly for diverse inputs")
        void normalizesBooleanEsAdaptado() {
            assertThat(mapper.parseBoolean("True")).isTrue();
            assertThat(mapper.parseBoolean("true")).isTrue();
            assertThat(mapper.parseBoolean("1")).isTrue();
            assertThat(mapper.parseBoolean("False")).isFalse();
            assertThat(mapper.parseBoolean("false")).isFalse();
            assertThat(mapper.parseBoolean("0")).isFalse();
            assertThat(mapper.parseBoolean(null)).isFalse();
            assertThat(mapper.parseBoolean("")).isFalse();
        }
    }
}
