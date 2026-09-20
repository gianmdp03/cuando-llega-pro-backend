package com.gianmdp03.cuando_llega_pro.domain.user.dto;

public record AuthResponseDTO(
        String token,
        String tokenType,
        long expiresIn,
        UserDetailDTO user
) {
}
