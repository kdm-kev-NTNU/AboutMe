package com.kevinmazali.portfolio.security;

import com.kevinmazali.portfolio.model.User;
import com.kevinmazali.portfolio.repository.UserRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

/**
 * Loads {@link org.springframework.security.core.userdetails.UserDetails} from the JPA {@link User} table
 * for HTTP Basic authentication. Domain {@link User.Role} values are mapped to Spring authorities
 * {@code ROLE_ADMIN} or {@code ROLE_USER}, which {@link com.kevinmazali.portfolio.config.SecurityConfig} uses
 * in {@code requestMatchers(...).hasRole("ADMIN")}.
 */
@Service
public class JpaUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public JpaUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /** Resolves the account for HTTP Basic; password must match the bcrypt hash stored in {@link User}. */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
        Collection<? extends GrantedAuthority> authorities = mapAuthorities(user.getRole());
        return new org.springframework.security.core.userdetails.User(
            user.getUsername(),
            user.getPassword(),
            authorities
        );
    }

    /** Spring Security expects {@code ROLE_*} prefixes for {@code hasRole("ADMIN")} style matchers. */
    private List<GrantedAuthority> mapAuthorities(User.Role role) {
        String authority = (role == User.Role.ADMIN) ? "ROLE_ADMIN" : "ROLE_USER";
        return List.of(new SimpleGrantedAuthority(authority));
    }
}


