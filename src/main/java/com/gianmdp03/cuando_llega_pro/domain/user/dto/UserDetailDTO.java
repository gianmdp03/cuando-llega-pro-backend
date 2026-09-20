package com.gianmdp03.cuando_llega_pro.domain.user.dto;

import com.gianmdp03.cuando_llega_pro.domain.user.User;

import java.time.Instant;

public record UserDetailDTO(
        Long id,
        String email,
        String fullName,
        String role,
        Instant createdAt,
        int presetsCount
) {
    public static UserDetailDTO fromEntity(User user) {
        int count = user.getPresets() != null ? user.getPresets().size() : 0;
        return new UserDetailDTO(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole(),
                user.getCreatedAt(),
                count
        );
    }
}
