package com.gianmdp03.cuando_llega_pro.config;

import com.gianmdp03.cuando_llega_pro.domain.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Startup bootstrap runner ensuring designated administrative accounts hold ROLE_ADMIN.
 */
@Component
public class AdminBootstrapRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrapRunner.class);
    private static final String ADMIN_EMAIL = "gcastorina234@gmail.com";
    private static final String ROLE_ADMIN = "ROLE_ADMIN";

    private final UserRepository userRepository;

    public AdminBootstrapRunner(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        userRepository.findByEmail(ADMIN_EMAIL).ifPresent(user -> {
            if (!ROLE_ADMIN.equals(user.getRole())) {
                user.setRole(ROLE_ADMIN);
                userRepository.save(user);
                log.info("AdminBootstrapRunner: Updated user {} role to {}", ADMIN_EMAIL, ROLE_ADMIN);
            }
        });
    }
}
