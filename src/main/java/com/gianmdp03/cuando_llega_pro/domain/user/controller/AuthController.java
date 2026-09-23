package com.gianmdp03.cuando_llega_pro.domain.user.controller;

import com.gianmdp03.cuando_llega_pro.domain.user.dto.AuthResponseDTO;
import com.gianmdp03.cuando_llega_pro.domain.user.dto.LoginRequestDTO;
import com.gianmdp03.cuando_llega_pro.domain.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller exposing authentication endpoints for user registration and login.
 * Never leaks domain entities; strictly interacts through records and DTOs.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Authenticates existing user credentials.
     *
     * @param request login details (email and password)
     * @return 200 OK with AuthResponseDTO containing token and user information
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody LoginRequestDTO request) {
        AuthResponseDTO response = userService.login(request);
        return ResponseEntity.ok(response);
    }
}
