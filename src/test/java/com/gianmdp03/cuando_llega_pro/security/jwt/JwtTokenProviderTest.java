package com.gianmdp03.cuando_llega_pro.security.jwt;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class JwtTokenProviderTest {

    private static final String BASE64_SECRET = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    private static final long EXPIRATION_MS = 3600000; // 1 hour

    private JwtTokenProvider tokenProvider;

    @BeforeEach
    void setUp() {
        tokenProvider = new JwtTokenProvider(BASE64_SECRET, EXPIRATION_MS);
    }

    @Test
    @DisplayName("Should generate valid token and extract claims correctly")
    void generateTokenAndExtractClaims() {
        String email = "testuser@example.com";
        Long userId = 42L;
        String role = "ROLE_USER";

        String token = tokenProvider.generateToken(email, userId, role);

        assertThat(token).isNotBlank();
        assertThat(tokenProvider.validateToken(token)).isTrue();
        assertThat(tokenProvider.getEmailFromToken(token)).isEqualTo(email);
        assertThat(tokenProvider.getUserIdFromToken(token)).isEqualTo(userId);
        assertThat(tokenProvider.getRoleFromToken(token)).isEqualTo(role);
    }

    @Test
    @DisplayName("Should reject token when signature is tampered")
    void validateTokenTampered() {
        String token = tokenProvider.generateToken("user@example.com", 1L, "ROLE_USER");
        String tamperedToken = token.substring(0, token.length() - 5) + "abcde";

        assertThat(tokenProvider.validateToken(tamperedToken)).isFalse();
    }

    @Test
    @DisplayName("Should reject malformed token string")
    void validateMalformedToken() {
        assertThat(tokenProvider.validateToken("not-a-valid-jwt-token")).isFalse();
        assertThat(tokenProvider.validateToken("")).isFalse();
    }

    @Test
    @DisplayName("Should reject expired token")
    void validateExpiredToken() {
        // Create token provider with negative expiration time
        JwtTokenProvider expiredTokenProvider = new JwtTokenProvider(BASE64_SECRET, -1000);
        String expiredToken = expiredTokenProvider.generateToken("user@example.com", 1L, "ROLE_USER");

        assertThat(tokenProvider.validateToken(expiredToken)).isFalse();
    }

    @Test
    @DisplayName("Should support raw ASCII secret fallback when not Base64 encoded")
    void rawAsciiSecretFallback() {
        String rawAsciiSecret = "this-is-a-very-long-raw-ascii-secret-key-for-jwt-testing-purposes-123456";
        JwtTokenProvider rawProvider = new JwtTokenProvider(rawAsciiSecret, EXPIRATION_MS);

        String token = rawProvider.generateToken("ascii@test.com", 99L, "ROLE_ADMIN");

        assertThat(rawProvider.validateToken(token)).isTrue();
        assertThat(rawProvider.getEmailFromToken(token)).isEqualTo("ascii@test.com");
        assertThat(rawProvider.getUserIdFromToken(token)).isEqualTo(99L);
        assertThat(rawProvider.getRoleFromToken(token)).isEqualTo("ROLE_ADMIN");
    }

    @Test
    @DisplayName("Should reject a secret shorter than the HS256 minimum")
    void rejectsShortSecret() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new JwtTokenProvider("short-secret", EXPIRATION_MS))
                .withMessageContaining("at least 32 bytes");
    }
}
