package com.kevinmazali.portfolio.repository;

import com.kevinmazali.portfolio.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** Persistence for {@link com.kevinmazali.portfolio.model.User} accounts used by Spring Security. */
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
}


