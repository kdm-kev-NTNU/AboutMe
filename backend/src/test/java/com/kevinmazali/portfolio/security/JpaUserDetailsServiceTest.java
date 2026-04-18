package com.kevinmazali.portfolio.security;

import com.kevinmazali.portfolio.model.User;
import com.kevinmazali.portfolio.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JpaUserDetailsServiceTest {

	@Mock
	private UserRepository userRepository;

	@InjectMocks
	private JpaUserDetailsService jpaUserDetailsService;

	@Test
	void loadUserByUsernameReturnsAdminAuthorities() {
		User user = User.builder()
			.username("admin")
			.password("{noop}x")
			.role(User.Role.ADMIN)
			.build();
		when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));

		UserDetails details = jpaUserDetailsService.loadUserByUsername("admin");

		assertEquals("admin", details.getUsername());
		assertTrue(details.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority())));
	}

	@Test
	void loadUserByUsernameReturnsUserAuthorities() {
		User user = User.builder()
			.username("u")
			.password("{noop}x")
			.role(User.Role.USER)
			.build();
		when(userRepository.findByUsername("u")).thenReturn(Optional.of(user));

		UserDetails details = jpaUserDetailsService.loadUserByUsername("u");

		assertTrue(details.getAuthorities().stream().anyMatch(a -> "ROLE_USER".equals(a.getAuthority())));
	}

	@Test
	void loadUserByUsernameThrowsWhenMissing() {
		when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

		assertThrows(UsernameNotFoundException.class, () -> jpaUserDetailsService.loadUserByUsername("ghost"));
	}
}
