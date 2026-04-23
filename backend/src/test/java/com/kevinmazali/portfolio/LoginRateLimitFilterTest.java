package com.kevinmazali.portfolio;

import com.kevinmazali.portfolio.config.AskRateLimitProperties;
import com.kevinmazali.portfolio.config.DatasetGenerateRateLimitProperties;
import com.kevinmazali.portfolio.config.ExperimentRunRateLimitProperties;
import com.kevinmazali.portfolio.config.WebConfig;
import com.kevinmazali.portfolio.controller.AuthController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthController.class, excludeAutoConfiguration = SecurityAutoConfiguration.class)
@TestPropertySource(properties = "portfolio.login-rate-limit.enabled=true")
@EnableConfigurationProperties({
  AskRateLimitProperties.class,
  ExperimentRunRateLimitProperties.class,
  DatasetGenerateRateLimitProperties.class
})
@Import(WebConfig.class)
class LoginRateLimitFilterTest {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private AuthenticationManager authenticationManager;

	@Test
	void loginRateLimiterReturns429AfterFiveAttemptsPerWindow() throws Exception {
		when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("bad"));

		String body = "{\"username\":\"x\",\"password\":\"y\"}";

		for (int i = 0; i < 5; i++) {
			mockMvc.perform(post("/auth/login")
					.contentType(MediaType.APPLICATION_JSON)
					.content(body))
				.andExpect(status().isUnauthorized());
		}

		mockMvc.perform(post("/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(body))
			.andExpect(status().isTooManyRequests());
	}
}
