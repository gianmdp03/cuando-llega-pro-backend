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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gianmdp03.cuando_llega_pro.config.JacksonConfig;
import com.gianmdp03.cuando_llega_pro.security.jwt.JwtAuthenticationFilter;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

@WebMvcTest(
        controllers = PresetController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class)
)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, JacksonConfig.class})
@DisplayName("PresetController WebMvc Sliced Tests (@MockitoBean)")
class PresetControllerSliceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PresetService presetService;

    private Principal principal;
    private PresetConfig sampleConfig;
    private PresetDetailDTO sampleDetailDTO;
    private PresetListDTO sampleListDTO;

    private static final String TEST_USER = "user@example.com";

    @BeforeEach
    void setUp() {
        principal = () -> TEST_USER;

        sampleConfig = new PresetConfig(
                "Office",
                "briefcase",
                "#336699",
                null
        );

        sampleDetailDTO = new PresetDetailDTO(
                42L,
                100L,
                "501",
                "P-4040",
                "Rapido",
                sampleConfig,
                Instant.parse("2026-03-15T08:00:00Z")
        );

        sampleListDTO = new PresetListDTO(
                42L,
                "501",
                "P-4040",
                "Rapido",
                "Office",
                "briefcase",
                "#336699"
        );
    }

    @Nested
    @DisplayName("GET /api/v1/presets")
    class GetPresetsSliced {

        @Test
        @DisplayName("Returns 200 OK with list of presets and verifies JSON serialization")
        void getPresets_ReturnsList() throws Exception {
            when(presetService.getPresetsForUser(TEST_USER)).thenReturn(List.of(sampleListDTO));

            mockMvc.perform(get("/api/v1/presets")
                            .principal(principal)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].id", is(42)))
                    .andExpect(jsonPath("$[0].codigoLinea", is("501")))
                    .andExpect(jsonPath("$[0].identificadorParada", is("P-4040")))
                    .andExpect(jsonPath("$[0].bandera", is("Rapido")))
                    .andExpect(jsonPath("$[0].alias", is("Office")))
                    .andExpect(jsonPath("$[0].icon", is("briefcase")))
                    .andExpect(jsonPath("$[0].color", is("#336699")));

            verify(presetService).getPresetsForUser(TEST_USER);
        }

        @Test
        @DisplayName("Returns 200 OK with empty array when no presets exist")
        void getPresets_ReturnsEmptyList() throws Exception {
            when(presetService.getPresetsForUser(TEST_USER)).thenReturn(List.of());

            mockMvc.perform(get("/api/v1/presets")
                            .principal(principal)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$", hasSize(0)));

            verify(presetService).getPresetsForUser(TEST_USER);
        }
    }

    @Nested
    @DisplayName("GET /api/v1/presets/{id}")
    class GetPresetByIdSliced {

        @Test
        @DisplayName("Returns 200 OK with detailed preset JSON structure")
        void getPresetById_ReturnsPresetDetail() throws Exception {
            when(presetService.getPresetByIdForUser(42L, TEST_USER)).thenReturn(sampleDetailDTO);

            mockMvc.perform(get("/api/v1/presets/42")
                            .principal(principal)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id", is(42)))
                    .andExpect(jsonPath("$.userId", is(100)))
                    .andExpect(jsonPath("$.codigoLinea", is("501")))
                    .andExpect(jsonPath("$.identificadorParada", is("P-4040")))
                    .andExpect(jsonPath("$.bandera", is("Rapido")))
                    .andExpect(jsonPath("$.config.alias", is("Office")))
                    .andExpect(jsonPath("$.config.icon", is("briefcase")))
                    .andExpect(jsonPath("$.config.color", is("#336699")))
                    .andExpect(jsonPath("$.createdAt", notNullValue()));

            verify(presetService).getPresetByIdForUser(42L, TEST_USER);
        }

        @Test
        @DisplayName("Returns 404 Not Found ProblemDetail when preset does not exist")
        void getPresetById_NotFound_Returns404() throws Exception {
            when(presetService.getPresetByIdForUser(999L, TEST_USER))
                    .thenThrow(new ResourceNotFoundException("Preset not found with id: 999"));

            mockMvc.perform(get("/api/v1/presets/999")
                            .principal(principal)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.status", is(404)))
                    .andExpect(jsonPath("$.title", is("Resource Not Found")))
                    .andExpect(jsonPath("$.detail", is("Preset not found with id: 999")))
                    .andExpect(jsonPath("$.timestamp", notNullValue()));

            verify(presetService).getPresetByIdForUser(999L, TEST_USER);
        }
    }

    @Nested
    @DisplayName("POST /api/v1/presets")
    class CreatePresetSliced {

        @Test
        @DisplayName("Returns 201 Created on valid request body")
        void createPreset_ValidBody_Returns201() throws Exception {
            PresetRequestDTO request = new PresetRequestDTO("501", "P-4040", "Rapido", sampleConfig);
            when(presetService.createPresetForUser(eq(TEST_USER), any(PresetRequestDTO.class)))
                    .thenReturn(sampleDetailDTO);

            mockMvc.perform(post("/api/v1/presets")
                            .principal(principal)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id", is(42)))
                    .andExpect(jsonPath("$.codigoLinea", is("501")))
                    .andExpect(jsonPath("$.identificadorParada", is("P-4040")))
                    .andExpect(jsonPath("$.bandera", is("Rapido")))
                    .andExpect(jsonPath("$.config.alias", is("Office")));

            verify(presetService).createPresetForUser(eq(TEST_USER), any(PresetRequestDTO.class));
        }

        @Test
        @DisplayName("Returns 400 Bad Request when codigoLinea is blank")
        void createPreset_BlankCodigoLinea_Returns400() throws Exception {
            PresetRequestDTO invalidRequest = new PresetRequestDTO("", "P-4040", "Rapido", sampleConfig);

            mockMvc.perform(post("/api/v1/presets")
                            .principal(principal)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.status", is(400)))
                    .andExpect(jsonPath("$.title", is("Validation Error")))
                    .andExpect(jsonPath("$.errors", hasSize(1)))
                    .andExpect(jsonPath("$.errors[0].field", is("codigoLinea")))
                    .andExpect(jsonPath("$.errors[0].message", is("Line code is required")));
        }

        @Test
        @DisplayName("Returns 400 Bad Request when identificadorParada is blank")
        void createPreset_BlankIdentificadorParada_Returns400() throws Exception {
            PresetRequestDTO invalidRequest = new PresetRequestDTO("501", "   ", "Rapido", sampleConfig);

            mockMvc.perform(post("/api/v1/presets")
                            .principal(principal)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.status", is(400)))
                    .andExpect(jsonPath("$.title", is("Validation Error")))
                    .andExpect(jsonPath("$.errors", hasSize(1)))
                    .andExpect(jsonPath("$.errors[0].field", is("identificadorParada")))
                    .andExpect(jsonPath("$.errors[0].message", is("Stop identifier is required")));
        }

        @Test
        @DisplayName("Returns 400 Bad Request when config is null")
        void createPreset_NullConfig_Returns400() throws Exception {
            PresetRequestDTO invalidRequest = new PresetRequestDTO("501", "P-4040", "Rapido", null);

            mockMvc.perform(post("/api/v1/presets")
                            .principal(principal)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.status", is(400)))
                    .andExpect(jsonPath("$.title", is("Validation Error")))
                    .andExpect(jsonPath("$.errors", hasSize(1)))
                    .andExpect(jsonPath("$.errors[0].field", is("config")))
                    .andExpect(jsonPath("$.errors[0].message", is("Config is required")));
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/presets/{id}")
    class UpdatePresetSliced {

        @Test
        @DisplayName("Returns 200 OK with updated details on valid payload")
        void updatePreset_ValidPayload_Returns200() throws Exception {
            PresetRequestDTO request = new PresetRequestDTO("502", "P-5050", "SemiRapido", sampleConfig);
            PresetDetailDTO updatedDetail = new PresetDetailDTO(
                    42L,
                    100L,
                    "502",
                    "P-5050",
                    "SemiRapido",
                    sampleConfig,
                    Instant.parse("2026-03-15T08:00:00Z")
            );

            when(presetService.updatePresetForUser(eq(42L), eq(TEST_USER), any(PresetRequestDTO.class)))
                    .thenReturn(updatedDetail);

            mockMvc.perform(put("/api/v1/presets/42")
                            .principal(principal)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id", is(42)))
                    .andExpect(jsonPath("$.codigoLinea", is("502")))
                    .andExpect(jsonPath("$.identificadorParada", is("P-5050")))
                    .andExpect(jsonPath("$.bandera", is("SemiRapido")));

            verify(presetService).updatePresetForUser(eq(42L), eq(TEST_USER), any(PresetRequestDTO.class));
        }

        @Test
        @DisplayName("Returns 404 Not Found when preset to update is not found")
        void updatePreset_NotFound_Returns404() throws Exception {
            PresetRequestDTO request = new PresetRequestDTO("502", "P-5050", "SemiRapido", sampleConfig);
            when(presetService.updatePresetForUser(eq(999L), eq(TEST_USER), any(PresetRequestDTO.class)))
                    .thenThrow(new ResourceNotFoundException("Preset not found with id: 999"));

            mockMvc.perform(put("/api/v1/presets/999")
                            .principal(principal)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.status", is(404)))
                    .andExpect(jsonPath("$.title", is("Resource Not Found")))
                    .andExpect(jsonPath("$.detail", is("Preset not found with id: 999")));

            verify(presetService).updatePresetForUser(eq(999L), eq(TEST_USER), any(PresetRequestDTO.class));
        }

        @Test
        @DisplayName("Returns 400 Bad Request when update payload has invalid fields")
        void updatePreset_InvalidPayload_Returns400() throws Exception {
            PresetRequestDTO invalidRequest = new PresetRequestDTO("", "", null, null);

            mockMvc.perform(put("/api/v1/presets/42")
                            .principal(principal)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.status", is(400)))
                    .andExpect(jsonPath("$.title", is("Validation Error")));
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/presets/{id}")
    class DeletePresetSliced {

        @Test
        @DisplayName("Returns 204 No Content when preset is deleted successfully")
        void deletePreset_Success_Returns204() throws Exception {
            doNothing().when(presetService).deletePresetForUser(42L, TEST_USER);

            mockMvc.perform(delete("/api/v1/presets/42")
                            .principal(principal))
                    .andExpect(status().isNoContent());

            verify(presetService).deletePresetForUser(42L, TEST_USER);
        }

        @Test
        @DisplayName("Returns 404 Not Found when preset to delete does not exist")
        void deletePreset_NotFound_Returns404() throws Exception {
            doThrow(new ResourceNotFoundException("Preset not found with id: 999"))
                    .when(presetService).deletePresetForUser(999L, TEST_USER);

            mockMvc.perform(delete("/api/v1/presets/999")
                            .principal(principal))
                    .andExpect(status().isNotFound())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.status", is(404)))
                    .andExpect(jsonPath("$.title", is("Resource Not Found")))
                    .andExpect(jsonPath("$.detail", is("Preset not found with id: 999")));

            verify(presetService).deletePresetForUser(999L, TEST_USER);
        }
    }
}
