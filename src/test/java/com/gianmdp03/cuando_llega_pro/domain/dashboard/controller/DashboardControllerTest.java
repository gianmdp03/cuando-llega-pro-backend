package com.gianmdp03.cuando_llega_pro.domain.dashboard.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gianmdp03.cuando_llega_pro.domain.dashboard.DashboardService;
import com.gianmdp03.cuando_llega_pro.domain.dashboard.dto.DashboardPresetArrivalDTO;
import com.gianmdp03.cuando_llega_pro.domain.dashboard.dto.DashboardResponseDTO;
import com.gianmdp03.cuando_llega_pro.domain.preset.model.PresetConfig;
import com.gianmdp03.cuando_llega_pro.domain.telemetry.dto.ArrivalResponseDTO;
import com.gianmdp03.cuando_llega_pro.domain.telemetry.dto.BusArrivalItemDTO;
import com.gianmdp03.cuando_llega_pro.domain.telemetry.model.TelemetryStatus;
import com.gianmdp03.cuando_llega_pro.exception.GlobalExceptionHandler;
import com.gianmdp03.cuando_llega_pro.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.security.Principal;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class DashboardControllerTest {

    private MockMvc mockMvc;

    @Mock
    private DashboardService dashboardService;

    @InjectMocks
    private DashboardController dashboardController;

    private Principal principal;
    private DashboardResponseDTO sampleDashboardResponse;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();

        mockMvc = MockMvcBuilders.standaloneSetup(dashboardController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        principal = () -> "tester@example.com";

        PresetConfig sampleConfig = new PresetConfig(
                "Work",
                "bus-icon",
                "#00AAFF",
                new PresetConfig.ScheduleRange("07:00", "08:30", Set.of("MON", "TUE", "WED")),
                new PresetConfig.NotificationSettings(true, 5, true)
        );

        ArrivalResponseDTO sampleTelemetry = new ArrivalResponseDTO(
                "501",
                "P-101",
                "A",
                TelemetryStatus.LIVE,
                Instant.parse("2026-09-19T14:00:00Z"),
                0L,
                List.of(new BusArrivalItemDTO(
                        "501", "A", 5, 1200, "14:05", "Unit-42", true, TelemetryStatus.LIVE
                ))
        );

        DashboardPresetArrivalDTO presetArrival = new DashboardPresetArrivalDTO(
                10L,
                "501",
                "P-101",
                "A",
                sampleConfig,
                sampleTelemetry
        );

        sampleDashboardResponse = new DashboardResponseDTO(
                "tester@example.com",
                Instant.parse("2026-09-19T14:00:01Z"),
                1,
                List.of(presetArrival)
        );
    }

    @Nested
    @DisplayName("GET /api/v1/me/dashboard")
    class GetDashboardTests {

        @Test
        @DisplayName("Returns 200 OK with DashboardResponseDTO for authenticated user")
        void getDashboard_Success() throws Exception {
            when(dashboardService.getDashboardForUser("tester@example.com"))
                    .thenReturn(sampleDashboardResponse);

            mockMvc.perform(get("/api/v1/me/dashboard")
                            .principal(principal)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.userEmail", is("tester@example.com")))
                    .andExpect(jsonPath("$.totalPresets", is(1)))
                    .andExpect(jsonPath("$.generatedAt", notNullValue()))
                    .andExpect(jsonPath("$.presets", hasSize(1)))
                    .andExpect(jsonPath("$.presets[0].presetId", is(10)))
                    .andExpect(jsonPath("$.presets[0].codigoLinea", is("501")))
                    .andExpect(jsonPath("$.presets[0].identificadorParada", is("P-101")))
                    .andExpect(jsonPath("$.presets[0].bandera", is("A")))
                    .andExpect(jsonPath("$.presets[0].config.alias", is("Work")))
                    .andExpect(jsonPath("$.presets[0].config.icon", is("bus-icon")))
                    .andExpect(jsonPath("$.presets[0].config.color", is("#00AAFF")))
                    .andExpect(jsonPath("$.presets[0].telemetry.status", is("LIVE")))
                    .andExpect(jsonPath("$.presets[0].telemetry.arrivals[0].remainingMinutes", is(5)))
                    .andExpect(jsonPath("$.presets[0].telemetry.arrivals[0].vehicleUnit", is("Unit-42")))
                    .andExpect(jsonPath("$.presets[0].telemetry.arrivals[0].accessible", is(true)));

            verify(dashboardService).getDashboardForUser("tester@example.com");
        }

        @Test
        @DisplayName("Returns 200 OK with empty presets list when user has no presets")
        void getDashboard_EmptyPresets_ReturnsOk() throws Exception {
            DashboardResponseDTO emptyResponse = new DashboardResponseDTO(
                    "tester@example.com",
                    Instant.parse("2026-09-19T14:00:00Z"),
                    0,
                    List.of()
            );

            when(dashboardService.getDashboardForUser("tester@example.com"))
                    .thenReturn(emptyResponse);

            mockMvc.perform(get("/api/v1/me/dashboard")
                            .principal(principal)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.userEmail", is("tester@example.com")))
                    .andExpect(jsonPath("$.totalPresets", is(0)))
                    .andExpect(jsonPath("$.presets", hasSize(0)));

            verify(dashboardService).getDashboardForUser("tester@example.com");
        }

        @Test
        @DisplayName("Returns 404 Not Found ProblemDetail when user does not exist")
        void getDashboard_UserNotFound_ReturnsNotFound() throws Exception {
            when(dashboardService.getDashboardForUser("tester@example.com"))
                    .thenThrow(new ResourceNotFoundException("User not found with email: tester@example.com"));

            mockMvc.perform(get("/api/v1/me/dashboard")
                            .principal(principal)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status", is(404)))
                    .andExpect(jsonPath("$.title", is("Resource Not Found")))
                    .andExpect(jsonPath("$.detail", is("User not found with email: tester@example.com")));

            verify(dashboardService).getDashboardForUser("tester@example.com");
        }
    }
}
