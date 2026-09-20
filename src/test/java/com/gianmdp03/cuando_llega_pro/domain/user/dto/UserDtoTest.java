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
    @DisplayName("UserRequestDTO Validation")
    class UserRequestDtoTests {

        @Test
        @DisplayName("Valid UserRequestDTO passes all bean validation rules")
        void validRequestDtoPassesValidation() {
            UserRequestDTO dto = new UserRequestDTO("valid@example.com", "password123", "Valid User");
            Set<ConstraintViolation<UserRequestDTO>> violations = validator.validate(dto);

            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("Invalid email causes constraint violation")
        void invalidEmailFailsValidation() {
            UserRequestDTO dto = new UserRequestDTO("not-an-email", "password123", "Valid User");
            Set<ConstraintViolation<UserRequestDTO>> violations = validator.validate(dto);

            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("email"));
        }

        @Test
        @DisplayName("Short password (< 6 chars) causes constraint violation")
        void shortPasswordFailsValidation() {
            UserRequestDTO dto = new UserRequestDTO("valid@example.com", "12345", "Valid User");
            Set<ConstraintViolation<UserRequestDTO>> violations = validator.validate(dto);

            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("password"));
        }

        @Test
        @DisplayName("Blank full name causes constraint violation")
        void blankFullNameFailsValidation() {
            UserRequestDTO dto = new UserRequestDTO("valid@example.com", "password123", "   ");
            Set<ConstraintViolation<UserRequestDTO>> violations = validator.validate(dto);

            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("fullName"));
        }
    }

    @Nested
    @DisplayName("UserDetailDTO and UserListDTO Mapping")
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

        @Test
        @DisplayName("UserListDTO fromEntity correctly extracts summary fields")
        void userListDtoFromEntity() {
            User user = new User("admin@example.com", "secret", "Admin User", "ROLE_ADMIN");
            ReflectionTestUtils.setField(user, "id", 202L);

            UserListDTO listDTO = UserListDTO.fromEntity(user);

            assertThat(listDTO.id()).isEqualTo(202L);
            assertThat(listDTO.email()).isEqualTo("admin@example.com");
            assertThat(listDTO.fullName()).isEqualTo("Admin User");
            assertThat(listDTO.role()).isEqualTo("ROLE_ADMIN");
        }
    }
}
