package com.gianmdp03.cuando_llega_pro.security;

import com.gianmdp03.cuando_llega_pro.security.jwt.JwtTokenProvider;
import com.gianmdp03.cuando_llega_pro.domain.user.User;
import com.gianmdp03.cuando_llega_pro.domain.user.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(SecurityConfigTest.DummySecurityController.class)
class SecurityConfigTest {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private WebApplicationContext context;

    @MockitoBean
    private UserRepository userRepository;

    @RestController
    static class DummySecurityController {
        @GetMapping("/api/v1/auth/ping")
        public String authPing() {
            return "PONG";
        }

        @GetMapping("/api/v1/protected-data")
        public String protectedData() {
            return "SECRET_DATA";
        }
    }

    @Test
    @DisplayName("Should encode and match passwords using BCryptPasswordEncoder")
    void passwordEncoderWorks() {
        assertThat(passwordEncoder).isNotNull();
        String rawPassword = "mySecurePassword123";
        String encoded = passwordEncoder.encode(rawPassword);

        assertThat(encoded).isNotEqualTo(rawPassword);
        assertThat(passwordEncoder.matches(rawPassword, encoded)).isTrue();
        assertThat(passwordEncoder.matches("wrongPassword", encoded)).isFalse();
    }

    @Test
    @DisplayName("Should provide configured AuthenticationManager bean")
    void authenticationManagerBeanExists() {
        assertThat(authenticationManager).isNotNull();
    }

    @Test
    @DisplayName("Should return 401 ProblemDetail when unauthenticated request hits protected endpoint")
    void unauthorizedAccessReturnsProblemDetail() throws Exception {
        MockMvc mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        mockMvc.perform(get("/api/v1/protected-data"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.title", is("Unauthorized")))
                .andExpect(jsonPath("$.detail", notNullValue()))
                .andExpect(jsonPath("$.timestamp", notNullValue()));
    }

    @Test
    @DisplayName("Should permit access to /healthz without authentication")
    void permitAllHealthz() throws Exception {
        MockMvc mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        mockMvc.perform(get("/healthz"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should require authentication for auth endpoints other than login")
    void protectsNonLoginAuthEndpoints() throws Exception {
        MockMvc mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        mockMvc.perform(get("/api/v1/auth/ping"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should permit access to protected endpoint when valid JWT is supplied")
    void authenticatedWithValidJwt() throws Exception {
        MockMvc mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        String token = tokenProvider.generateToken("test@example.com", 1L, "ROLE_USER");
        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(new User("test@example.com", "password", "Test", "ROLE_USER")));

        mockMvc.perform(get("/api/v1/protected-data")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should handle CORS preflight OPTIONS request for configured origin")
    void corsOptionsRequest() throws Exception {
        MockMvc mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        mockMvc.perform(options("/api/v1/protected-data")
                        .header("Origin", "http://localhost:8400")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:8400"));
    }
}
