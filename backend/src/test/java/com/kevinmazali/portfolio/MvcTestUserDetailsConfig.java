package com.kevinmazali.portfolio;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

/**
 * In-memory users for {@link org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest}
 * slices that {@link com.kevinmazali.portfolio.config.SecurityConfig} imports; the main app uses
 * {@link com.kevinmazali.portfolio.security.JpaUserDetailsService} instead.
 */
@TestConfiguration(proxyBeanMethods = false)
public class MvcTestUserDetailsConfig {

	@Bean
	UserDetailsService mvcTestUserDetailsService() {
		return new InMemoryUserDetailsManager(
			User.withUsername("user").password("{noop}pass").roles("USER").build(),
			User.withUsername("admin").password("{noop}pass").roles("ADMIN").build()
		);
	}
}
