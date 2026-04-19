package com.kevinmazali.portfolio.service;

import com.kevinmazali.portfolio.model.RequestLog;
import com.kevinmazali.portfolio.repository.RequestLogRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RequestLogServiceTest {

	@Mock
	private RequestLogRepository requestLogRepository;

	@InjectMocks
	private RequestLogService requestLogService;

	@AfterEach
	void clearSecurityContext() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void saveUsesExplicitRequesterIdWhenProvided() {
		requestLogService.save("/ask", "POST", "payload", "custom-id");

		ArgumentCaptor<RequestLog> captor = ArgumentCaptor.forClass(RequestLog.class);
		verify(requestLogRepository).save(captor.capture());
		assertEquals("custom-id", captor.getValue().getRequesterId());
	}

	@Test
	void saveUsesBrukerWhenUnauthenticated() {
		requestLogService.save("/ask", "POST", "payload", null);

		verify(requestLogRepository).save(argThat(log -> "Bruker".equals(log.getRequesterId())));
	}

	@Test
	void saveUsesGoatWhenPrincipalNameIsGoat() {
		var auth = new UsernamePasswordAuthenticationToken(
			"GOAT",
			"n/a",
			List.of(new SimpleGrantedAuthority("ROLE_USER"))
		);
		SecurityContextHolder.getContext().setAuthentication(auth);

		requestLogService.save("/ask", "POST", "payload", null);

		verify(requestLogRepository).save(argThat(log -> "GOAT".equals(log.getRequesterId())));
	}

	@Test
	void saveUsesBrukerWhenAuthenticatedNonGoat() {
		var auth = new UsernamePasswordAuthenticationToken(
			"someone",
			"n/a",
			List.of(new SimpleGrantedAuthority("ROLE_USER"))
		);
		SecurityContextHolder.getContext().setAuthentication(auth);

		requestLogService.save("/ask", "POST", "payload", null);

		verify(requestLogRepository).save(argThat(log -> "Bruker".equals(log.getRequesterId())));
	}
}
