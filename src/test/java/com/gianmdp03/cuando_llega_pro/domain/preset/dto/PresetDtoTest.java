package com.gianmdp03.cuando_llega_pro.domain.preset.dto;

import com.gianmdp03.cuando_llega_pro.domain.preset.Preset;
import com.gianmdp03.cuando_llega_pro.domain.preset.model.PresetConfig;
import com.gianmdp03.cuando_llega_pro.domain.user.User;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PresetDtoTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    @Nested
    @DisplayName("PresetRequestDTO Validation")
    class RequestValidationTests {

        @Test
        @DisplayName("Valid PresetRequestDTO passes all validations")
        void validRequestPassesValidation() {
            PresetConfig config = new PresetConfig("Work", "work", "blue", null);
            PresetRequestDTO dto = new PresetRequestDTO("511", "100", "A", config);

            Set<ConstraintViolation<PresetRequestDTO>> violations = validator.validate(dto);

            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("Blank codigoLinea fails validation")
        void blankCodigoLineaFailsValidation() {
            PresetConfig config = new PresetConfig("Work", "work", "blue", null);
            PresetRequestDTO dto = new PresetRequestDTO("", "100", "A", config);

            Set<ConstraintViolation<PresetRequestDTO>> violations = validator.validate(dto);

            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("codigoLinea"));
        }

        @Test
        @DisplayName("Blank identificadorParada fails validation")
        void blankIdentificadorParadaFailsValidation() {
            PresetConfig config = new PresetConfig("Work", "work", "blue", null);
            PresetRequestDTO dto = new PresetRequestDTO("511", "  ", "A", config);

            Set<ConstraintViolation<PresetRequestDTO>> violations = validator.validate(dto);

            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("identificadorParada"));
        }

        @Test
        @DisplayName("Null config fails validation")
        void nullConfigFailsValidation() {
            PresetRequestDTO dto = new PresetRequestDTO("511", "100", "A", null);

            Set<ConstraintViolation<PresetRequestDTO>> violations = validator.validate(dto);

            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("config"));
        }
    }

    @Nested
    @DisplayName("PresetDetailDTO Mapping and Anti-Recursion")
    class DetailMappingTests {

        @Test
        @DisplayName("fromEntity maps all fields and strictly references userId as Long")
        void fromEntityMapsCorrectlyWithUser() {
            User user = new User("user@test.com", "pass", "User Test");
            ReflectionTestUtils.setField(user, "id", 42L);

            PresetConfig config = new PresetConfig("Home", "home", "#FF0000", null);
            Preset preset = new Preset(user, "522", "200", "B", config);
            ReflectionTestUtils.setField(preset, "id", 7L);
            Instant now = Instant.now();
            preset.setCreatedAt(now);

            PresetDetailDTO detail = PresetDetailDTO.fromEntity(preset);

            assertThat(detail.id()).isEqualTo(7L);
            assertThat(detail.userId()).isEqualTo(42L);
            assertThat(detail.codigoLinea()).isEqualTo("522");
            assertThat(detail.identificadorParada()).isEqualTo("200");
            assertThat(detail.bandera()).isEqualTo("B");
            assertThat(detail.config()).isEqualTo(config);
            assertThat(detail.createdAt()).isEqualTo(now);
        }

        @Test
        @DisplayName("fromEntity handles null user gracefully with null userId")
        void fromEntityHandlesNullUser() {
            Preset preset = new Preset("522", "200", null);
            ReflectionTestUtils.setField(preset, "id", 8L);

            PresetDetailDTO detail = PresetDetailDTO.fromEntity(preset);

            assertThat(detail.id()).isEqualTo(8L);
            assertThat(detail.userId()).isNull();
            assertThat(detail.codigoLinea()).isEqualTo("522");
            assertThat(detail.identificadorParada()).isEqualTo("200");
            assertThat(detail.bandera()).isNull();
        }
    }

    @Nested
    @DisplayName("PresetListDTO Mapping and Flattening")
    class ListMappingTests {

        @Test
        @DisplayName("fromEntity flattens alias, icon, color from config")
        void fromEntityFlattensConfigFields() {
            PresetConfig config = new PresetConfig("Office Stop", "office", "emerald", null);
            Preset preset = new Preset("533", "300", "C");
            preset.setConfig(config);
            ReflectionTestUtils.setField(preset, "id", 12L);

            PresetListDTO listDTO = PresetListDTO.fromEntity(preset);

            assertThat(listDTO.id()).isEqualTo(12L);
            assertThat(listDTO.codigoLinea()).isEqualTo("533");
            assertThat(listDTO.identificadorParada()).isEqualTo("300");
            assertThat(listDTO.bandera()).isEqualTo("C");
            assertThat(listDTO.alias()).isEqualTo("Office Stop");
            assertThat(listDTO.icon()).isEqualTo("office");
            assertThat(listDTO.color()).isEqualTo("emerald");
        }

        @Test
        @DisplayName("fromEntity handles null config safely with null fields")
        void fromEntityHandlesNullConfig() {
            Preset preset = new Preset("533", "300", "C");
            ReflectionTestUtils.setField(preset, "id", 13L);

            PresetListDTO listDTO = PresetListDTO.fromEntity(preset);

            assertThat(listDTO.id()).isEqualTo(13L);
            assertThat(listDTO.alias()).isNull();
            assertThat(listDTO.icon()).isNull();
            assertThat(listDTO.color()).isNull();
        }
    }
}
