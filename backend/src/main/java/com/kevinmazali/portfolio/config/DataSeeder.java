package com.kevinmazali.portfolio.config;

import com.kevinmazali.portfolio.model.User;
import com.kevinmazali.portfolio.repository.UserRepository;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataSeeder {

    @Bean
    public ApplicationRunner seedAdminUser(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            if (!userRepository.existsByUsername("GOAT")) {
                User admin = User.builder()
                    .username("GOAT")
                    .password(passwordEncoder.encode("MAZALI"))
                    .role(User.Role.ADMIN)
                    .build();
                userRepository.save(admin);
            }
        };
    }
}


