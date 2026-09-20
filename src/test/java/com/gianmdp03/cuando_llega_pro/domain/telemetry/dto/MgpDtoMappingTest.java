package com.gianmdp03.cuando_llega_pro.domain.telemetry.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MGP Telemetry DTO PascalCase Mapping Tests")
class MgpDtoMappingTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("Deserializes upstream PascalCase JSON into MgpResponse without null fields")
    void deserializesPascalCaseUpstreamJsonSuccessfully() throws Exception {
        String upstreamJson = """
                {
                  "CodigoEstado": 0,
                  "MensajeEstado": "Ok",
                  "arribos": [
                    {
                      "DescripcionLinea": "511",
                      "DescripcionBandera": "A",
                      "Arribo": "53 min. aprox.",
                      "Latitud": "-38.0001",
                      "Longitud": "-57.5552",
                      "LatitudParada": "-38.0050",
                      "LongitudParada": "-57.5590",
                      "EsAdaptado": "true",
                      "IdentificadorCoche": "104",
                      "UltimaFechaHoraGPS": "20/09/2026 11:15:00",
                      "CodigoLineaParada": "511"
                    }
                  ]
                }
                """;

        MgpResponse response = objectMapper.readValue(upstreamJson, MgpResponse.class);

        assertThat(response).isNotNull();
        assertThat(response.codigoEstado()).isEqualTo(0);
        assertThat(response.mensajeEstado()).isEqualTo("Ok");
        assertThat(response.arribos()).hasSize(1);

        MgpArriboRaw raw = response.arribos().getFirst();
        assertThat(raw.descripcionLinea()).isEqualTo("511");
        assertThat(raw.descripcionBandera()).isEqualTo("A");
        assertThat(raw.arribo()).isEqualTo("53 min. aprox.");
        assertThat(raw.latitud()).isEqualTo("-38.0001");
        assertThat(raw.longitud()).isEqualTo("-57.5552");
        assertThat(raw.latitudParada()).isEqualTo("-38.0050");
        assertThat(raw.longitudParada()).isEqualTo("-57.5590");
        assertThat(raw.esAdaptado()).isEqualTo("true");
        assertThat(raw.identificadorCoche()).isEqualTo("104");
        assertThat(raw.ultimaFechaHoraGps()).isEqualTo("20/09/2026 11:15:00");
        assertThat(raw.codigoLineaParada()).isEqualTo("511");
    }

    @Test
    @DisplayName("Deserializes direct array of PascalCase items into MgpResponse via polymorphic deserializer")
    void deserializesDirectArrayIntoMgpResponse() throws Exception {
        String arrayJson = """
                [
                  {
                    "DescripcionLinea": "522",
                    "DescripcionBandera": "B",
                    "Arribo": "proximo",
                    "EsAdaptado": "false",
                    "IdentificadorCoche": "022",
                    "CodigoLineaParada": "522"
                  }
                ]
                """;

        MgpResponse response = objectMapper.readValue(arrayJson, MgpResponse.class);

        assertThat(response).isNotNull();
        assertThat(response.arribos()).hasSize(1);
        assertThat(response.arribos().getFirst().descripcionLinea()).isEqualTo("522");
        assertThat(response.arribos().getFirst().arribo()).isEqualTo("proximo");
        assertThat(response.arribos().getFirst().identificadorCoche()).isEqualTo("022");
    }

    @Test
    @DisplayName("MgpArriboRawDTO converts to and from MgpArriboRaw")
    void rawDtoConversionWorks() {
        MgpArriboRaw raw = new MgpArriboRaw(
                "511", "A", "10 min", "lat1", "lon1",
                "lat2", "lon2", "true", "42", "20/09/2026 12:00:00", "511"
        );

        MgpArriboRawDTO dto = MgpArriboRawDTO.fromRaw(raw);
        assertThat(dto).isNotNull();
        assertThat(dto.descripcionLinea()).isEqualTo("511");
        assertThat(dto.identificadorCoche()).isEqualTo("42");

        MgpArriboRaw convertedBack = dto.toRaw();
        assertThat(convertedBack.descripcionLinea()).isEqualTo("511");
        assertThat(convertedBack.identificadorCoche()).isEqualTo("42");
        assertThat(convertedBack.esAdaptado()).isEqualTo("true");
    }
}
