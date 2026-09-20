package com.gianmdp03.cuando_llega_pro.domain.user.dto;

import com.gianmdp03.cuando_llega_pro.domain.user.User;

public record UserListDTO(
        Long id,
        String email,
        String fullName,
        String role
) {
    public static UserListDTO fromEntity(User user) {
        return new UserListDTO(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole()
        );
    }
}
