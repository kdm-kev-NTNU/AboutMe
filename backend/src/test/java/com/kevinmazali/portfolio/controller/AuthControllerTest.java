package com.kevinmazali.portfolio.controller;


import com.kevinmazali.portfolio.MvcTestSessionAuthConfig;import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.kevinmazali.portfolio.MvcTestUserDetailsConfig;
import com.kevinmazali.portfolio.config.SecurityConfig;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthController.class)
@Import({SecurityConfig.class, MvcTestSessionAuthConfig.class, MvcTestUserDetailsConfig.class})
class AuthControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private AuthenticationManager authenticationManager;

	@Test
	void loginReturnsRoleWhenAuthenticationSucceeds() throws Exception {
		var auth = new UsernamePasswordAuthenticationToken(
			"alice",
			"secret",
			List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
		);
		when(authenticationManager.authenticate(any())).thenReturn(auth);

		mockMvc.perform(post("/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"username\":\"alice\",\"password\":\"secret\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.username").value("alice"))
			.andExpect(jsonPath("$.role").value("ADMIN"));
	}

	@Test
	void loginReturns401OnBadCredentials() throws Exception {
		when(authenticationManager.authenticate(any()))
			.thenThrow(new BadCredentialsException("bad"));

		mockMvc.perform(post("/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"username\":\"x\",\"password\":\"y\"}"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.error").value("Invalid credentials"));
	}

	@Test
	void loginReturnsUserRoleWhenNotAdmin() throws Exception {
		var auth = new UsernamePasswordAuthenticationToken(
			"bob",
			"pw",
			List.of(new SimpleGrantedAuthority("ROLE_USER"))
		);
		when(authenticationManager.authenticate(any())).thenReturn(auth);

		mockMvc.perform(post("/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"username\":\"bob\",\"password\":\"pw\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.role").value("USER"));
	}
}
