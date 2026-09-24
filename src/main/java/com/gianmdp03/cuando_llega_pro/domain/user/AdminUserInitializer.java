package com.gianmdp03.cuando_llega_pro.domain.user;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@Configuration
@ConditionalOnProperty(name = "app.admin.bootstrap.enabled", havingValue = "true", matchIfMissing = true)
public class AdminUserInitializer {

    private static final Logger log = LoggerFactory.getLogger(AdminUserInitializer.class);

    @Bean
    ApplicationRunner initializeAdminUser(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.admin.email:admin@local}") String email,
            @Value("${app.admin.password:admin1234}") String password,
            @Value("${app.admin.full-name:Administrator}") String fullName
    ) {
        return args -> {
            String cleanEmail = email.trim().toLowerCase();
            if (!userRepository.existsByEmail(cleanEmail)) {
                userRepository.save(new User(
                        cleanEmail,
                        passwordEncoder.encode(password.trim()),
                        fullName.trim(),
                        "ROLE_ADMIN"
                ));
                log.info("Usuario administrador inicial creado: {}", cleanEmail);
            }
        };
    }
}