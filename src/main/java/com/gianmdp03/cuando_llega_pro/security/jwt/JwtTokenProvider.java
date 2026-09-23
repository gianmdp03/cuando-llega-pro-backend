package com.gianmdp03.cuando_llega_pro.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecurityException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Utility component for generating, parsing, and validating JSON Web Tokens (JWT).
 */
@Component
public class JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);

    private final long expirationMs;
    private final SecretKey key;

    public JwtTokenProvider(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-ms:86400000}") long expirationMs) {
        this.expirationMs = expirationMs;
        this.key = initSigningKey(secret);
    }

    public long getExpirationMs() {
        return expirationMs;
    }

    /**
     * Initializes the HMAC-SHA signing key using Base64 decoding, with UTF-8 bytes fallback.
     *
     * @param secret raw secret string from application properties
     * @return cryptographically validated SecretKey
     */
    private SecretKey initSigningKey(String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException("JWT secret cannot be null or blank");
        }

        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(secret);
            if (keyBytes.length < 32) {
                keyBytes = secret.getBytes(StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        }

        if (keyBytes.length < 32) {
            throw new IllegalArgumentException("JWT secret must contain at least 32 bytes of entropy");
        }

        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Generates a signed JWT with subject email, userId, and role claims.
     *
     * @param email  user email used as token subject
     * @param userId user identifier
     * @param role   assigned user role
     * @return compact signed JWT string
     */
    public String generateToken(String email, Long userId, String role) {
        Date now = new Date();
        var builder = Jwts.builder()
                .subject(email)
                .claim("userId", userId)
                .claim("role", role)
                .issuedAt(now)
                .signWith(key);

        if (expirationMs > 0) {
            builder.expiration(new Date(now.getTime() + expirationMs));
        }

        return builder.compact();
    }

    /**
     * Extracts user email (subject) from the JWT token.
     *
     * @param token JWT token string
     * @return email subject
     */
    public String getEmailFromToken(String token) {
        return getClaims(token).getSubject();
    }

    /**
     * Extracts user ID claim from the JWT token.
     *
     * @param token JWT token string
     * @return userId as Long, or null if absent
     */
    public Long getUserIdFromToken(String token) {
        Object userIdClaim = getClaims(token).get("userId");
        if (userIdClaim instanceof Number number) {
            return number.longValue();
        } else if (userIdClaim instanceof String str) {
            return Long.parseLong(str);
        }
        return null;
    }

    /**
     * Extracts role claim from the JWT token.
     *
     * @param token JWT token string
     * @return role as String
     */
    public String getRoleFromToken(String token) {
        return getClaims(token).get("role", String.class);
    }

    /**
     * Validates signature, structure, and expiration of the given JWT token.
     *
     * @param token JWT token string
     * @return true if token is valid and not expired, false otherwise
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (SecurityException | MalformedJwtException ex) {
            log.warn("Invalid JWT signature or malformed token: {}", ex.getMessage());
        } catch (ExpiredJwtException ex) {
            log.warn("Expired JWT token: {}", ex.getMessage());
        } catch (UnsupportedJwtException ex) {
            log.warn("Unsupported JWT token: {}", ex.getMessage());
        } catch (IllegalArgumentException ex) {
            log.warn("JWT claims string is empty or invalid: {}", ex.getMessage());
        } catch (JwtException ex) {
            log.warn("JWT validation error: {}", ex.getMessage());
        }
        return false;
    }

    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
