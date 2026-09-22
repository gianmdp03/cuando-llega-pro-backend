package com.gianmdp03.cuando_llega_pro.security.jwt;

import com.gianmdp03.cuando_llega_pro.domain.user.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Filter that intercepts incoming HTTP requests, extracts JWT bearer tokens,
 * validates them against token signature and database existence, and populates
 * the Spring Security context with the authenticated principal.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider tokenProvider;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(JwtTokenProvider tokenProvider, UserRepository userRepository) {
        this.tokenProvider = tokenProvider;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        try {
            String jwt = extractJwtFromRequest(request);

            if (StringUtils.hasText(jwt) && tokenProvider.validateToken(jwt)) {
                String email = tokenProvider.getEmailFromToken(jwt);

                if (StringUtils.hasText(email) && userRepository.existsByEmail(email)) {
                    String role = tokenProvider.getRoleFromToken(jwt);

                    List<SimpleGrantedAuthority> authorities;
                    if (StringUtils.hasText(role)) {
                        if (role.startsWith("ROLE_")) {
                            authorities = List.of(new SimpleGrantedAuthority(role));
                        } else {
                            authorities = List.of(
                                    new SimpleGrantedAuthority(role),
                                    new SimpleGrantedAuthority("ROLE_" + role)
                            );
                        }
                    } else {
                        authorities = List.of();
                    }

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(email, null, authorities);
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                } else {
                    log.warn("JWT token valid but user with email '{}' does not exist in database", email);
                }
            }
        } catch (Exception ex) {
            log.error("Failed to establish security context from JWT: {}", ex.getMessage(), ex);
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Extracts the raw JWT bearer token from the Authorization header.
     *
     * @param request current HTTP request
     * @return bearer token string, or null if header is absent or does not start with "Bearer "
     */
    private String extractJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length()).trim();
        }
        return null;
    }
}
