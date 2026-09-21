package com.gianmdp03.cuando_llega_pro.domain.transit;

import com.gianmdp03.cuando_llega_pro.domain.transit.dto.TransitIntersectionDTO;
import com.gianmdp03.cuando_llega_pro.domain.transit.dto.TransitLineDTO;
import com.gianmdp03.cuando_llega_pro.domain.transit.dto.TransitRouteResponseDTO;
import com.gianmdp03.cuando_llega_pro.domain.transit.dto.TransitStopDTO;
import com.gianmdp03.cuando_llega_pro.domain.transit.dto.TransitStopWithFlagDTO;
import com.gianmdp03.cuando_llega_pro.domain.transit.dto.TransitStreetDTO;
import com.gianmdp03.cuando_llega_pro.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("TransitCatalogController Unit Tests")
class TransitCatalogControllerTest {

    private MockMvc mockMvc;

    @Mock
    private TransitCatalogService transitCatalogService;

    @InjectMocks
    private TransitCatalogController transitCatalogController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(transitCatalogController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/transit/lines returns 200 with line catalog")
    void getLines_returns200() throws Exception {
        when(transitCatalogService.getLines()).thenReturn(List.of(
                new TransitLineDTO("98", "511", "511"),
                new TransitLineDTO("101", "522", "522")
        ));

        mockMvc.perform(get("/api/v1/transit/lines")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", is("98")))
                .andExpect(jsonPath("$[0].codigo", is("511")));
    }

    @Test
    @DisplayName("GET /api/v1/transit/lines/{lineCode}/streets returns 200 with main streets")
    void getMainStreets_returns200() throws Exception {
        when(transitCatalogService.getMainStreetsByLine("511")).thenReturn(List.of(
                new TransitStreetDTO("5449", "ALMAFUERTE - MAR DEL PLATA"),
                new TransitStreetDTO("5461", "12 DE OCTUBRE - MAR DEL PLATA")
        ));

        mockMvc.perform(get("/api/v1/transit/lines/511/streets")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].codigo", is("5449")))
                .andExpect(jsonPath("$[0].descripcion", is("ALMAFUERTE - MAR DEL PLATA")));
    }

    @Test
    @DisplayName("GET /api/v1/transit/lines/{lineCode}/streets/{streetCode}/intersections returns 200")
    void getIntersections_returns200() throws Exception {
        when(transitCatalogService.getIntersectionsByLineAndStreet("511", "5449")).thenReturn(List.of(
                new TransitIntersectionDTO("5625", "LEANDRO N. ALEM - MAR DEL PLATA")
        ));

        mockMvc.perform(get("/api/v1/transit/lines/511/streets/5449/intersections")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].codigo", is("5625")));
    }

    @Test
    @DisplayName("GET /api/v1/transit/lines/{lineCode}/streets/{streetCode}/intersections/{intersectionCode}/stops returns 200")
    void getStopsWithFlag_returns200() throws Exception {
        when(transitCatalogService.getStopsWithFlag("511", "5449", "5625")).thenReturn(List.of(
                new TransitStopWithFlagDTO("17453", "P4031", "P4031", "A ACANTILADOS", "A ACANTILADOS", null, null)
        ));

        mockMvc.perform(get("/api/v1/transit/lines/511/streets/5449/intersections/5625/stops")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].identificador", is("P4031")))
                .andExpect(jsonPath("$[0].abreviaturaBandera", is("A ACANTILADOS")));
    }

    @Test
    @DisplayName("GET /api/v1/transit/lines/{lineCode}/route returns 200 with polyline route")
    void getRoute_returns200() throws Exception {
        when(transitCatalogService.getRouteTrace("511")).thenReturn(
                new TransitRouteResponseDTO("511", List.of(), List.of(), List.of())
        );

        mockMvc.perform(get("/api/v1/transit/lines/511/route")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lineCode", is("511")));
    }

    @Test
    @DisplayName("GET /api/v1/transit/lines/{lineCode}/stops returns 200 with stops")
    void getStops_returns200() throws Exception {
        when(transitCatalogService.getStopsForLine("511")).thenReturn(List.of(
                new TransitStopDTO("P4031", "P4031", "ALMAFUERTE", -38.0, -57.5)
        ));

        mockMvc.perform(get("/api/v1/transit/lines/511/stops")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is("P4031")));
    }
}
