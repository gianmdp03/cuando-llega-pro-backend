package com.gianmdp03.cuando_llega_pro.domain.user.service;

import com.gianmdp03.cuando_llega_pro.domain.user.User;
import com.gianmdp03.cuando_llega_pro.domain.user.UserRepository;
import com.gianmdp03.cuando_llega_pro.domain.user.dto.AdminCreateUserRequestDTO;
import com.gianmdp03.cuando_llega_pro.domain.user.dto.AuthResponseDTO;
import com.gianmdp03.cuando_llega_pro.domain.user.dto.LoginRequestDTO;
import com.gianmdp03.cuando_llega_pro.domain.user.dto.UserDetailDTO;
import com.gianmdp03.cuando_llega_pro.exception.BadRequestException;
import com.gianmdp03.cuando_llega_pro.exception.ResourceNotFoundException;
import com.gianmdp03.cuando_llega_pro.security.jwt.JwtTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service managing user lifecycle, registration, authentication, and profile querying.
 */
@Service
@Transactional(readOnly = true)
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);
    private static final String DEFAULT_ROLE = "ROLE_USER";
    private static final String TOKEN_TYPE = "Bearer";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public UserService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Transactional
    public UserDetailDTO createUserByAdmin(AdminCreateUserRequestDTO request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new BadRequestException("Email is already registered");
        }

        String role = request.role() == null || request.role().isBlank()
                ? DEFAULT_ROLE
                : request.role();
        User savedUser = userRepository.save(new User(
                request.email(),
                passwordEncoder.encode(request.password()),
                request.fullName(),
                role
        ));

        return UserDetailDTO.fromEntity(savedUser);
    }

    /**
     * Authenticates a user by email and password, returning a signed JWT token upon success.
     *
     * @param request login credentials
     * @return authentication response containing JWT and user profile
     */
    public AuthResponseDTO login(LoginRequestDTO request) {
        log.info("Attempting login for email: {}", request.email());

        User user = userRepository.findByEmailWithPresets(request.email())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        String token = jwtTokenProvider.generateToken(
                user.getEmail(),
                user.getId(),
                user.getRole()
        );

        return new AuthResponseDTO(
                token,
                TOKEN_TYPE,
                jwtTokenProvider.getExpirationMs(),
                UserDetailDTO.fromEntity(user)
        );
    }

    /**
     * Retrieves user details by ID along with their presets count.
     *
     * @param id user ID
     * @return user detail DTO
     */
    public UserDetailDTO getUserById(Long id) {
        User user = userRepository.findByIdWithPresets(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        int presetsCount = user.getPresets() != null ? user.getPresets().size() : 0;
        return new UserDetailDTO(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole(),
                user.getCreatedAt(),
                presetsCount
        );
    }

    /**
     * Retrieves user details by email along with their presets count.
     *
     * @param email user email
     * @return user detail DTO
     */
    public UserDetailDTO getUserByEmail(String email) {
        User user = userRepository.findByEmailWithPresets(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        int presetsCount = user.getPresets() != null ? user.getPresets().size() : 0;
        return new UserDetailDTO(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole(),
                user.getCreatedAt(),
                presetsCount
        );
    }
}
