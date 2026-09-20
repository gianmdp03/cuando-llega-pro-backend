package com.gianmdp03.cuando_llega_pro.domain.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gianmdp03.cuando_llega_pro.domain.user.dto.AuthResponseDTO;
import com.gianmdp03.cuando_llega_pro.domain.user.dto.LoginRequestDTO;
import com.gianmdp03.cuando_llega_pro.domain.user.dto.UserDetailDTO;
import com.gianmdp03.cuando_llega_pro.domain.user.dto.UserRequestDTO;
import com.gianmdp03.cuando_llega_pro.domain.user.service.UserService;
import com.gianmdp03.cuando_llega_pro.exception.BadRequestException;
import com.gianmdp03.cuando_llega_pro.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private UserService userService;

    @InjectMocks
    private AuthController authController;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/auth/register: returns 201 Created on valid request")
    void register_ValidRequest_ReturnsCreated() throws Exception {
        UserRequestDTO request = new UserRequestDTO("tester@example.com", "password123", "Tester");
        UserDetailDTO userDetail = new UserDetailDTO(1L, "tester@example.com", "Tester", "ROLE_USER", Instant.now(), 0);
        AuthResponseDTO authResponse = new AuthResponseDTO("jwt.token.here", "Bearer", 86400000L, userDetail);

        when(userService.register(any(UserRequestDTO.class))).thenReturn(authResponse);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token", is("jwt.token.here")))
                .andExpect(jsonPath("$.tokenType", is("Bearer")))
                .andExpect(jsonPath("$.expiresIn", is(86400000)))
                .andExpect(jsonPath("$.user.id", is(1)))
                .andExpect(jsonPath("$.user.email", is("tester@example.com")))
                .andExpect(jsonPath("$.user.fullName", is("Tester")));
    }

    @Test
    @DisplayName("POST /api/v1/auth/register: returns 400 Bad Request on validation failure")
    void register_InvalidPayload_ReturnsBadRequest() throws Exception {
        UserRequestDTO invalidRequest = new UserRequestDTO("invalid-email", "123", "");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title", is("Validation Error")))
                .andExpect(jsonPath("$.errors", notNullValue()));
    }

    @Test
    @DisplayName("POST /api/v1/auth/register: returns 400 when email already exists")
    void register_EmailConflict_ReturnsBadRequest() throws Exception {
        UserRequestDTO request = new UserRequestDTO("exists@example.com", "password123", "Tester");
        when(userService.register(any(UserRequestDTO.class)))
                .thenThrow(new BadRequestException("Email is already registered"));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title", is("Bad Request")))
                .andExpect(jsonPath("$.detail", is("Email is already registered")));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login: returns 200 OK on valid credentials")
    void login_ValidCredentials_ReturnsOk() throws Exception {
        LoginRequestDTO request = new LoginRequestDTO("tester@example.com", "secret123");
        UserDetailDTO userDetail = new UserDetailDTO(1L, "tester@example.com", "Tester", "ROLE_USER", Instant.now(), 0);
        AuthResponseDTO authResponse = new AuthResponseDTO("jwt.token.login", "Bearer", 86400000L, userDetail);

        when(userService.login(any(LoginRequestDTO.class))).thenReturn(authResponse);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", is("jwt.token.login")))
                .andExpect(jsonPath("$.tokenType", is("Bearer")))
                .andExpect(jsonPath("$.expiresIn", is(86400000)))
                .andExpect(jsonPath("$.user.email", is("tester@example.com")));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login: returns 401 Unauthorized on bad credentials")
    void login_BadCredentials_ReturnsUnauthorized() throws Exception {
        LoginRequestDTO request = new LoginRequestDTO("tester@example.com", "wrongPassword");
        when(userService.login(any(LoginRequestDTO.class)))
                .thenThrow(new BadCredentialsException("Invalid email or password"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title", is("Authentication Failed")))
                .andExpect(jsonPath("$.detail", is("Invalid email or password")));
    }
}
