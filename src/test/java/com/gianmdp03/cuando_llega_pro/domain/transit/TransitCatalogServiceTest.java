package com.gianmdp03.cuando_llega_pro.domain.transit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gianmdp03.cuando_llega_pro.client.MgpProxyClient;
import com.gianmdp03.cuando_llega_pro.domain.transit.dto.TransitIntersectionDTO;
import com.gianmdp03.cuando_llega_pro.domain.transit.dto.TransitLineDTO;
import com.gianmdp03.cuando_llega_pro.domain.transit.dto.TransitStopWithFlagDTO;
import com.gianmdp03.cuando_llega_pro.domain.transit.dto.TransitStreetDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@DisplayName("TransitCatalogService Unit Tests (HAR Methods & Cache-Aside)")
class TransitCatalogServiceTest {

    @Mock
    private MgpProxyClient mgpProxyClient;

    @Mock
    private TransitCatalogRepository transitCatalogRepository;

    private ObjectMapper objectMapper;
    private TransitLineResolver transitLineResolver;
    private TransitCatalogService transitCatalogService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        transitLineResolver = new TransitLineResolver();
        transitCatalogService = new TransitCatalogService(
                mgpProxyClient,
                objectMapper,
                transitCatalogRepository,
                transitLineResolver
        );
    }

    @Test
    @DisplayName("L2 Cache Hit: Returns lines from PostgreSQL without invoking upstream proxy")
    void getLines_returnsFromPostgresL2Hit() {
        String dbPayload = """
                [
                    {"id":"98","codigo":"511","descripcion":"511"},
                    {"id":"101","codigo":"522","descripcion":"522"}
                ]
                """;
        when(transitCatalogRepository.findById(TransitCatalogService.CACHE_KEY_LINES_ALL))
                .thenReturn(Optional.of(new TransitCatalogEntity(
                        TransitCatalogService.CACHE_KEY_LINES_ALL,
                        "LINES",
                        dbPayload,
                        Instant.now()
                )));

        List<TransitLineDTO> lines = transitCatalogService.getLines();

        assertThat(lines).hasSize(2);
        assertThat(lines.get(0).codigo()).isEqualTo("511");
        assertThat(lines.get(0).id()).isEqualTo("98");

        verify(mgpProxyClient, never()).getLines(any(), any());
        assertThat(transitLineResolver.toInternalCode("511")).isEqualTo("98");
    }

    @Test
    @DisplayName("L2 Cache Miss: Calls upstream proxy, parses lines, saves to PostgreSQL, and updates resolver")
    void getLines_callsUpstreamOnL2MissAndSavesToPostgres() {
        when(transitCatalogRepository.findById(TransitCatalogService.CACHE_KEY_LINES_ALL))
                .thenReturn(Optional.empty());

        String upstreamHarResponse = """
                {
                    "CodigoEstado": 0,
                    "MensajeEstado": "ok",
                    "lineas": [
                        {"CodigoLineaParada":"98","Descripcion":"511","CodigoEntidad":"10","CodigoEmpresa":13},
                        {"CodigoLineaParada":"101","Descripcion":"522","CodigoEntidad":"10","CodigoEmpresa":13}
                    ]
                }
                """;
        when(mgpProxyClient.getLines(any(), eq(TransitCatalogService.ACTION_RECUPERAR_LINEAS_FALLBACK)))
                .thenReturn(upstreamHarResponse);

        List<TransitLineDTO> lines = transitCatalogService.getLines();

        assertThat(lines).hasSize(2);
        assertThat(lines.get(0).id()).isEqualTo("98");
        assertThat(lines.get(0).codigo()).isEqualTo("511");

        verify(transitCatalogRepository).save(any(TransitCatalogEntity.class));
        assertThat(transitLineResolver.toInternalCode("511")).isEqualTo("98");
    }

    @Test
    @DisplayName("getMainStreetsByLine resolves line code to internal code and returns streets")
    void getMainStreetsByLine_resolvesAndCaches() {
        when(transitCatalogRepository.findById("streets:98")).thenReturn(Optional.empty());

        String upstreamHarResponse = """
                {
                    "CodigoEstado": 0,
                    "MensajeEstado": "ok",
                    "calles": [
                        {"Codigo":"5461","Descripcion":"12 DE OCTUBRE - MAR DEL PLATA"},
                        {"Codigo":"5449","Descripcion":"ALMAFUERTE - MAR DEL PLATA"}
                    ]
                }
                """;
        when(mgpProxyClient.getMainStreetsByLine(any(), eq(TransitCatalogService.ACTION_RECUPERAR_CALLES_PRINCIPAL), eq("98")))
                .thenReturn(upstreamHarResponse);

        List<TransitStreetDTO> streets = transitCatalogService.getMainStreetsByLine("511");

        assertThat(streets).hasSize(2);
        assertThat(streets.get(0).codigo()).isEqualTo("5461");
        assertThat(streets.get(1).descripcion()).isEqualTo("ALMAFUERTE");
        verify(transitCatalogRepository).save(any(TransitCatalogEntity.class));
    }

    @Test
    @DisplayName("getIntersectionsByLineAndStreet returns intersections for given line and street")
    void getIntersectionsByLineAndStreet_returnsIntersections() {
        when(transitCatalogRepository.findById("intersections:98:5449")).thenReturn(Optional.empty());

        String upstreamHarResponse = """
                {
                    "CodigoEstado": 0,
                    "MensajeEstado": "ok",
                    "calles": [
                        {"Codigo":"5456","Descripcion":"MARTÍN MIGUEL DE GÜEMES - MAR DEL PLATA"},
                        {"Codigo":"5625","Descripcion":"LEANDRO N. ALEM - MAR DEL PLATA"}
                    ]
                }
                """;
        when(mgpProxyClient.getIntersectionsByLineAndStreet(
                any(), eq(TransitCatalogService.ACTION_RECUPERAR_INTERSECCION), eq("98"), eq("5449")
        )).thenReturn(upstreamHarResponse);

        List<TransitIntersectionDTO> intersections = transitCatalogService.getIntersectionsByLineAndStreet("511", "5449");

        assertThat(intersections).hasSize(2);
        assertThat(intersections.get(0).codigo()).isEqualTo("5625");
        verify(transitCatalogRepository).save(any(TransitCatalogEntity.class));
    }

    @Test
    @DisplayName("getStopsWithFlag returns stops with flag information")
    void getStopsWithFlag_returnsStops() {
        when(transitCatalogRepository.findById("stops_flag:98:5449:5625")).thenReturn(Optional.empty());

        String upstreamHarResponse = """
                {
                    "CodigoEstado": 0,
                    "MensajeEstado": "ok",
                    "paradas": [
                        {
                            "Codigo": "17453",
                            "Identificador": "P4031",
                            "Descripcion": "P4031",
                            "AbreviaturaBandera": "A ACANTILADOS",
                            "AbreviaturaAmpliadaBandera": "A ACANTILADOS",
                            "LatitudParada": null,
                            "LongitudParada": null
                        }
                    ]
                }
                """;
        when(mgpProxyClient.getStopsWithFlag(
                any(), eq(TransitCatalogService.ACTION_RECUPERAR_PARADAS_BANDERA), eq("98"), eq("5449"), eq("5625")
        )).thenReturn(upstreamHarResponse);

        List<TransitStopWithFlagDTO> stops = transitCatalogService.getStopsWithFlag("511", "5449", "5625");

        assertThat(stops).hasSize(1);
        assertThat(stops.get(0).identificador()).isEqualTo("P4031");
        assertThat(stops.get(0).abreviaturaBandera()).isEqualTo("A ACANTILADOS");
        verify(transitCatalogRepository).save(any(TransitCatalogEntity.class));
    }

    @Test
    @DisplayName("Concurrent cache misses for the same catalog key share one upstream request")
    void getMainStreetsByLine_coalescesConcurrentMisses() throws Exception {
        var cacheManager = new com.gianmdp03.cuando_llega_pro.config.CaffeineCacheConfig().cacheManager();
        transitCatalogService = new TransitCatalogService(
                mgpProxyClient, transitCatalogRepository, cacheManager, objectMapper, transitLineResolver
        );
        when(transitCatalogRepository.findById("streets:98")).thenReturn(Optional.empty());
        CountDownLatch upstreamStarted = new CountDownLatch(1);
        CountDownLatch allowUpstreamResponse = new CountDownLatch(1);
        when(mgpProxyClient.getMainStreetsByLine(any(), eq(TransitCatalogService.ACTION_RECUPERAR_CALLES_PRINCIPAL), eq("98")))
                .thenAnswer(invocation -> {
                    upstreamStarted.countDown();
                    allowUpstreamResponse.await(2, TimeUnit.SECONDS);
                    return "{\"calles\":[{\"Codigo\":\"1\",\"Descripcion\":\"A - MAR DEL PLATA\"}]}";
                });

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var first = executor.submit(() -> transitCatalogService.getMainStreetsByLine("511"));
            assertThat(upstreamStarted.await(1, TimeUnit.SECONDS)).isTrue();
            var second = executor.submit(() -> transitCatalogService.getMainStreetsByLine("511"));
            allowUpstreamResponse.countDown();

            assertThat(first.get(2, TimeUnit.SECONDS)).hasSize(1);
            assertThat(second.get(2, TimeUnit.SECONDS)).hasSize(1);
        }

        verify(mgpProxyClient, times(1)).getMainStreetsByLine(any(), any(), any());
    }

    @Test
    @DisplayName("L1 cache hit returns the already materialized DTO list")
    void getMainStreetsByLine_returnsSameL1ValueWithoutJacksonCopy() {
        var cacheManager = new com.gianmdp03.cuando_llega_pro.config.CaffeineCacheConfig().cacheManager();
        transitCatalogService = new TransitCatalogService(
                mgpProxyClient, transitCatalogRepository, cacheManager, objectMapper, transitLineResolver
        );
        when(transitCatalogRepository.findById("streets:98")).thenReturn(Optional.empty());
        when(mgpProxyClient.getMainStreetsByLine(any(), eq(TransitCatalogService.ACTION_RECUPERAR_CALLES_PRINCIPAL), eq("98")))
                .thenReturn("{\"calles\":[{\"Codigo\":\"1\",\"Descripcion\":\"A - MAR DEL PLATA\"}]}");

        List<TransitStreetDTO> first = transitCatalogService.getMainStreetsByLine("511");
        List<TransitStreetDTO> second = transitCatalogService.getMainStreetsByLine("511");

        assertThat(second).isSameAs(first);
        verify(mgpProxyClient, times(1)).getMainStreetsByLine(any(), any(), any());
    }
}
