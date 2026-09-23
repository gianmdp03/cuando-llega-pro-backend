package com.gianmdp03.cuando_llega_pro.domain.preset.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gianmdp03.cuando_llega_pro.domain.preset.dto.PresetDetailDTO;
import com.gianmdp03.cuando_llega_pro.domain.preset.dto.PresetListDTO;
import com.gianmdp03.cuando_llega_pro.domain.preset.dto.PresetRequestDTO;
import com.gianmdp03.cuando_llega_pro.domain.preset.model.PresetConfig;
import com.gianmdp03.cuando_llega_pro.domain.preset.service.PresetService;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PresetControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private PresetService presetService;

    @InjectMocks
    private PresetController presetController;

    private Principal principal;
    private PresetConfig sampleConfig;
    private PresetDetailDTO sampleDetailDTO;
    private PresetListDTO sampleListDTO;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();

        mockMvc = MockMvcBuilders.standaloneSetup(presetController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        principal = () -> "tester@example.com";

        sampleConfig = new PresetConfig(
                "Home",
                "home-icon",
                "#FF5500",
                null
        );

        sampleDetailDTO = new PresetDetailDTO(
                10L,
                1L,
                "501",
                "P-100",
                "A",
                sampleConfig,
                Instant.parse("2026-01-01T12:00:00Z")
        );

        sampleListDTO = new PresetListDTO(
                10L,
                "501",
                "P-100",
                "A",
                "Home",
                "home-icon",
                "#FF5500"
        );
    }

    @Nested
    @DisplayName("GET /api/v1/presets")
    class GetPresetsTests {

        @Test
        @DisplayName("Returns 200 OK with list of presets for authenticated user")
        void getPresets_Success() throws Exception {
            when(presetService.getPresetsForUser("tester@example.com")).thenReturn(List.of(sampleListDTO));

            mockMvc.perform(get("/api/v1/presets")
                            .principal(principal))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].id", is(10)))
                    .andExpect(jsonPath("$[0].codigoLinea", is("501")))
                    .andExpect(jsonPath("$[0].identificadorParada", is("P-100")))
                    .andExpect(jsonPath("$[0].bandera", is("A")))
                    .andExpect(jsonPath("$[0].alias", is("Home")))
                    .andExpect(jsonPath("$[0].icon", is("home-icon")))
                    .andExpect(jsonPath("$[0].color", is("#FF5500")));

            verify(presetService).getPresetsForUser("tester@example.com");
        }

        @Test
        @DisplayName("Returns 200 OK with empty array when user has no presets")
        void getPresets_EmptyList() throws Exception {
            when(presetService.getPresetsForUser("tester@example.com")).thenReturn(List.of());

            mockMvc.perform(get("/api/v1/presets")
                            .principal(principal))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/presets/{id}")
    class GetPresetByIdTests {

        @Test
        @DisplayName("Returns 200 OK with PresetDetailDTO when preset exists")
        void getPresetById_Success() throws Exception {
            when(presetService.getPresetByIdForUser(10L, "tester@example.com")).thenReturn(sampleDetailDTO);

            mockMvc.perform(get("/api/v1/presets/10")
                            .principal(principal))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(10)))
                    .andExpect(jsonPath("$.userId", is(1)))
                    .andExpect(jsonPath("$.codigoLinea", is("501")))
                    .andExpect(jsonPath("$.identificadorParada", is("P-100")))
                    .andExpect(jsonPath("$.bandera", is("A")))
                    .andExpect(jsonPath("$.config.alias", is("Home")))
                    .andExpect(jsonPath("$.createdAt", notNullValue()));

            verify(presetService).getPresetByIdForUser(10L, "tester@example.com");
        }

        @Test
        @DisplayName("Returns 404 Not Found ProblemDetail when preset does not exist")
        void getPresetById_NotFound() throws Exception {
            when(presetService.getPresetByIdForUser(99L, "tester@example.com"))
                    .thenThrow(new ResourceNotFoundException("Preset not found with id: 99"));

            mockMvc.perform(get("/api/v1/presets/99")
                            .principal(principal))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status", is(404)))
                    .andExpect(jsonPath("$.title", is("Resource Not Found")))
                    .andExpect(jsonPath("$.detail", is("Preset not found with id: 99")));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/presets")
    class CreatePresetTests {

        @Test
        @DisplayName("Returns 201 Created with PresetDetailDTO on valid request")
        void createPreset_Success() throws Exception {
            PresetRequestDTO request = new PresetRequestDTO("501", "P-100", "A", sampleConfig);
            when(presetService.createPresetForUser(eq("tester@example.com"), any(PresetRequestDTO.class)))
                    .thenReturn(sampleDetailDTO);

            mockMvc.perform(post("/api/v1/presets")
                            .principal(principal)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id", is(10)))
                    .andExpect(jsonPath("$.codigoLinea", is("501")))
                    .andExpect(jsonPath("$.identificadorParada", is("P-100")))
                    .andExpect(jsonPath("$.config.alias", is("Home")));

            verify(presetService).createPresetForUser(eq("tester@example.com"), any(PresetRequestDTO.class));
        }

        @Test
        @DisplayName("Returns 400 Bad Request when codigoLinea is blank")
        void createPreset_BlankCodigoLinea_ReturnsBadRequest() throws Exception {
            PresetRequestDTO invalidRequest = new PresetRequestDTO("", "P-100", "A", sampleConfig);

            mockMvc.perform(post("/api/v1/presets")
                            .principal(principal)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)))
                    .andExpect(jsonPath("$.title", is("Validation Error")))
                    .andExpect(jsonPath("$.errors", notNullValue()));
        }

        @Test
        @DisplayName("Returns 400 Bad Request when identificadorParada is blank")
        void createPreset_BlankIdentificadorParada_ReturnsBadRequest() throws Exception {
            PresetRequestDTO invalidRequest = new PresetRequestDTO("501", "  ", "A", sampleConfig);

            mockMvc.perform(post("/api/v1/presets")
                            .principal(principal)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)))
                    .andExpect(jsonPath("$.title", is("Validation Error")));
        }

        @Test
        @DisplayName("Returns 400 Bad Request when config is null")
        void createPreset_NullConfig_ReturnsBadRequest() throws Exception {
            PresetRequestDTO invalidRequest = new PresetRequestDTO("501", "P-100", "A", null);

            mockMvc.perform(post("/api/v1/presets")
                            .principal(principal)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)))
                    .andExpect(jsonPath("$.title", is("Validation Error")));
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/presets/{id}")
    class UpdatePresetTests {

        @Test
        @DisplayName("Returns 200 OK with updated PresetDetailDTO on valid request")
        void updatePreset_Success() throws Exception {
            PresetRequestDTO request = new PresetRequestDTO("502", "P-200", "B", sampleConfig);
            PresetDetailDTO updatedDTO = new PresetDetailDTO(
                    10L, 1L, "502", "P-200", "B", sampleConfig, Instant.parse("2026-01-01T12:00:00Z")
            );

            when(presetService.updatePresetForUser(eq(10L), eq("tester@example.com"), any(PresetRequestDTO.class)))
                    .thenReturn(updatedDTO);

            mockMvc.perform(put("/api/v1/presets/10")
                            .principal(principal)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(10)))
                    .andExpect(jsonPath("$.codigoLinea", is("502")))
                    .andExpect(jsonPath("$.identificadorParada", is("P-200")))
                    .andExpect(jsonPath("$.bandera", is("B")));

            verify(presetService).updatePresetForUser(eq(10L), eq("tester@example.com"), any(PresetRequestDTO.class));
        }

        @Test
        @DisplayName("Returns 404 Not Found when preset to update does not exist")
        void updatePreset_NotFound() throws Exception {
            PresetRequestDTO request = new PresetRequestDTO("502", "P-200", "B", sampleConfig);
            when(presetService.updatePresetForUser(eq(99L), eq("tester@example.com"), any(PresetRequestDTO.class)))
                    .thenThrow(new ResourceNotFoundException("Preset not found with id: 99"));

            mockMvc.perform(put("/api/v1/presets/99")
                            .principal(principal)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status", is(404)))
                    .andExpect(jsonPath("$.title", is("Resource Not Found")))
                    .andExpect(jsonPath("$.detail", is("Preset not found with id: 99")));
        }

        @Test
        @DisplayName("Returns 400 Bad Request when update payload is invalid")
        void updatePreset_InvalidPayload() throws Exception {
            PresetRequestDTO invalidRequest = new PresetRequestDTO("", "", null, null);

            mockMvc.perform(put("/api/v1/presets/10")
                            .principal(principal)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)))
                    .andExpect(jsonPath("$.title", is("Validation Error")));
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/presets/{id}")
    class DeletePresetTests {

        @Test
        @DisplayName("Returns 204 No Content when preset is successfully deleted")
        void deletePreset_Success() throws Exception {
            doNothing().when(presetService).deletePresetForUser(10L, "tester@example.com");

            mockMvc.perform(delete("/api/v1/presets/10")
                            .principal(principal))
                    .andExpect(status().isNoContent());

            verify(presetService).deletePresetForUser(10L, "tester@example.com");
        }

        @Test
        @DisplayName("Returns 404 Not Found when preset to delete does not exist")
        void deletePreset_NotFound() throws Exception {
            doThrow(new ResourceNotFoundException("Preset not found with id: 99"))
                    .when(presetService).deletePresetForUser(99L, "tester@example.com");

            mockMvc.perform(delete("/api/v1/presets/99")
                            .principal(principal))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status", is(404)))
                    .andExpect(jsonPath("$.title", is("Resource Not Found")))
                    .andExpect(jsonPath("$.detail", is("Preset not found with id: 99")));
        }
    }
}
