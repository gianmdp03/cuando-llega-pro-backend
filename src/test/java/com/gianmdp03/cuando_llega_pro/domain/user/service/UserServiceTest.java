package com.gianmdp03.cuando_llega_pro.domain.user.service;

import com.gianmdp03.cuando_llega_pro.domain.preset.Preset;
import com.gianmdp03.cuando_llega_pro.domain.user.User;
import com.gianmdp03.cuando_llega_pro.domain.user.UserRepository;
import com.gianmdp03.cuando_llega_pro.domain.user.dto.AuthResponseDTO;
import com.gianmdp03.cuando_llega_pro.domain.user.dto.LoginRequestDTO;
import com.gianmdp03.cuando_llega_pro.domain.user.dto.UserDetailDTO;
import com.gianmdp03.cuando_llega_pro.exception.BadRequestException;
import com.gianmdp03.cuando_llega_pro.exception.ResourceNotFoundException;
import com.gianmdp03.cuando_llega_pro.security.jwt.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private UserService userService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = new User("tester@example.com", "encodedPassword123", "Tester Name", "ROLE_USER");
        sampleUser.setId(10L);
    }

    @Test
    @DisplayName("login: throws BadCredentialsException when email is not found")
    void login_UserNotFound_ThrowsBadCredentialsException() {
        LoginRequestDTO request = new LoginRequestDTO("unknown@example.com", "password123");
        when(userRepository.findByEmailWithPresets("unknown@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.login(request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid email or password");
    }

    @Test
    @DisplayName("login: throws BadCredentialsException when password does not match")
    void login_WrongPassword_ThrowsBadCredentialsException() {
        LoginRequestDTO request = new LoginRequestDTO("tester@example.com", "wrongPassword");
        when(userRepository.findByEmailWithPresets("tester@example.com")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("wrongPassword", "encodedPassword123")).thenReturn(false);

        assertThatThrownBy(() -> userService.login(request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid email or password");
    }

    @Test
    @DisplayName("login: successful authentication returns AuthResponseDTO")
    void login_Success() {
        LoginRequestDTO request = new LoginRequestDTO("tester@example.com", "correctPassword");
        when(userRepository.findByEmailWithPresets("tester@example.com")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("correctPassword", "encodedPassword123")).thenReturn(true);
        when(jwtTokenProvider.generateToken("tester@example.com", 10L, "ROLE_USER")).thenReturn("valid.jwt.token");
        when(jwtTokenProvider.getExpirationMs()).thenReturn(86400000L);

        AuthResponseDTO response = userService.login(request);

        assertThat(response).isNotNull();
        assertThat(response.token()).isEqualTo("valid.jwt.token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(86400000L);
        assertThat(response.user().id()).isEqualTo(10L);
        assertThat(response.user().email()).isEqualTo("tester@example.com");
    }

    @Test
    @DisplayName("getUserById: throws ResourceNotFoundException when user id not found")
    void getUserById_NotFound_ThrowsResourceNotFoundException() {
        when(userRepository.findByIdWithPresets(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User not found with id: 99");
    }

    @Test
    @DisplayName("getUserById: returns UserDetailDTO with presets count")
    void getUserById_Success() {
        sampleUser.addPreset(new Preset("511", "001", "A"));
        sampleUser.addPreset(new Preset("512", "002", "B"));
        when(userRepository.findByIdWithPresets(10L)).thenReturn(Optional.of(sampleUser));

        UserDetailDTO detail = userService.getUserById(10L);

        assertThat(detail).isNotNull();
        assertThat(detail.id()).isEqualTo(10L);
        assertThat(detail.email()).isEqualTo("tester@example.com");
        assertThat(detail.presetsCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("getUserByEmail: throws ResourceNotFoundException when email not found")
    void getUserByEmail_NotFound_ThrowsResourceNotFoundException() {
        when(userRepository.findByEmailWithPresets("unknown@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserByEmail("unknown@example.com"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User not found with email: unknown@example.com");
    }

    @Test
    @DisplayName("getUserByEmail: returns UserDetailDTO when found")
    void getUserByEmail_Success() {
        when(userRepository.findByEmailWithPresets("tester@example.com")).thenReturn(Optional.of(sampleUser));

        UserDetailDTO detail = userService.getUserByEmail("tester@example.com");

        assertThat(detail).isNotNull();
        assertThat(detail.id()).isEqualTo(10L);
        assertThat(detail.email()).isEqualTo("tester@example.com");
        assertThat(detail.presetsCount()).isEqualTo(0);
    }
}
