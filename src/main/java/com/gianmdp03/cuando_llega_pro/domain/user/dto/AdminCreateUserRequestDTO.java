package com.gianmdp03.cuando_llega_pro.domain.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminCreateUserRequestDTO(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 6) String password,
        @NotBlank String fullName,
        String role
) {
}
