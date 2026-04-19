package com.kevinmazali.portfolio.config;

import com.kevinmazali.portfolio.model.User;
import com.kevinmazali.portfolio.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Objects;

/**
 * Creates an admin user once if env vars are set and the username is not yet registered.
 * Set {@code ADMIN_BOOTSTRAP_USERNAME} and {@code ADMIN_BOOTSTRAP_PASSWORD} in Railway or
 * a local {@code .env}, then remove or clear the password variable after first successful login.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 50)
public class AdminBootstrapRunner implements ApplicationRunner {

    private static final String PROP_USER = "ADMIN_BOOTSTRAP_USERNAME";
    private static final String PROP_PASSWORD = "ADMIN_BOOTSTRAP_PASSWORD";

    private final Environment environment;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminBootstrapRunner(
            Environment environment,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        this.environment = environment;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /** No-op when bootstrap env vars are unset; otherwise creates one ADMIN user if the username is new. */
    @Override
    public void run(ApplicationArguments args) {
        String username = environment.getProperty(PROP_USER);
        String rawPassword = environment.getProperty(PROP_PASSWORD);
        if (username == null || rawPassword == null || !StringUtils.hasText(username) || !StringUtils.hasText(rawPassword)) {
            return;
        }
        final String trimmedUser = username.trim();
        final String trimmedPassword = rawPassword;
        if (userRepository.existsByUsername(trimmedUser)) {
            log.debug("Admin bootstrap skipped: user '{}' already exists", trimmedUser);
            return;
        }
        User admin = User.builder()
                .username(trimmedUser)
                .password(passwordEncoder.encode(trimmedPassword))
                .role(User.Role.ADMIN)
                .build();
        userRepository.save(Objects.requireNonNull(admin));
        log.info("Admin bootstrap: created user '{}' (clear {} in production after use)", trimmedUser, PROP_PASSWORD);
    }
}
