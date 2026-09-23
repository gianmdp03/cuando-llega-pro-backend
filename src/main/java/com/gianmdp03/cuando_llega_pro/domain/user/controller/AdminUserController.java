package com.gianmdp03.cuando_llega_pro.domain.user.controller;

import com.gianmdp03.cuando_llega_pro.domain.user.dto.AdminCreateUserRequestDTO;
import com.gianmdp03.cuando_llega_pro.domain.user.dto.UserDetailDTO;
import com.gianmdp03.cuando_llega_pro.domain.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/users")
public class AdminUserController {

    private final UserService userService;

    public AdminUserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<UserDetailDTO> createUser(@Valid @RequestBody AdminCreateUserRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createUserByAdmin(request));
    }
}
