package com.gianmdp03.cuando_llega_pro.domain.user.dto;

import com.gianmdp03.cuando_llega_pro.domain.preset.Preset;
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

class UserDtoTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    @Nested
    @DisplayName("AdminCreateUserRequestDTO Validation")
    class AdminCreateUserRequestDtoTests {

        @Test
        @DisplayName("Valid AdminCreateUserRequestDTO passes all bean validation rules")
        void validRequestDtoPassesValidation() {
            AdminCreateUserRequestDTO dto = new AdminCreateUserRequestDTO("valid@example.com", "password123", "Valid User", "ROLE_USER");
            Set<ConstraintViolation<AdminCreateUserRequestDTO>> violations = validator.validate(dto);

            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("Invalid email causes constraint violation")
        void invalidEmailFailsValidation() {
            AdminCreateUserRequestDTO dto = new AdminCreateUserRequestDTO("not-an-email", "password123", "Valid User", "ROLE_USER");
            Set<ConstraintViolation<AdminCreateUserRequestDTO>> violations = validator.validate(dto);

            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("email"));
        }

        @Test
        @DisplayName("Short password (< 6 chars) causes constraint violation")
        void shortPasswordFailsValidation() {
            AdminCreateUserRequestDTO dto = new AdminCreateUserRequestDTO("valid@example.com", "12345", "Valid User", "ROLE_USER");
            Set<ConstraintViolation<AdminCreateUserRequestDTO>> violations = validator.validate(dto);

            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("password"));
        }

        @Test
        @DisplayName("Blank full name causes constraint violation")
        void blankFullNameFailsValidation() {
            AdminCreateUserRequestDTO dto = new AdminCreateUserRequestDTO("valid@example.com", "password123", "   ", "ROLE_USER");
            Set<ConstraintViolation<AdminCreateUserRequestDTO>> violations = validator.validate(dto);

            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("fullName"));
        }
    }

    @Nested
    @DisplayName("UserDetailDTO Mapping")
    class DtoMappingTests {

        @Test
        @DisplayName("UserDetailDTO fromEntity correctly extracts all fields and counts presets")
        void userDetailDtoFromEntity() {
            User user = new User("user@example.com", "secret", "John Doe", "ROLE_USER");
            ReflectionTestUtils.setField(user, "id", 101L);
            Instant now = Instant.now();
            user.setCreatedAt(now);

            user.addPreset(new Preset("501", "P-1", "A"));
            user.addPreset(new Preset("502", "P-2", "B"));

            UserDetailDTO detailDTO = UserDetailDTO.fromEntity(user);

            assertThat(detailDTO.id()).isEqualTo(101L);
            assertThat(detailDTO.email()).isEqualTo("user@example.com");
            assertThat(detailDTO.fullName()).isEqualTo("John Doe");
            assertThat(detailDTO.role()).isEqualTo("ROLE_USER");
            assertThat(detailDTO.createdAt()).isEqualTo(now);
            assertThat(detailDTO.presetsCount()).isEqualTo(2);
        }
    }
}
