package com.kevinmazali.portfolio;

import com.kevinmazali.portfolio.config.SessionCookiePropertiesConfiguration;
import com.kevinmazali.portfolio.security.JwtCookieAuthenticationFilter;
import com.kevinmazali.portfolio.security.JwtService;
import com.kevinmazali.portfolio.security.SessionCookieSupport;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

/**
 * Loads session-cookie JWT beans required by {@link com.kevinmazali.portfolio.config.SecurityConfig}
 * in {@code @WebMvcTest} slices.
 */
@TestConfiguration
@TestPropertySource(
    properties = {
      "portfolio.session.jwt-secret=test-jwt-secret-for-mvc-slices-minimum-32-characters-long",
      "portfolio.test.disable-csrf=true",
    })
@Import({
  SessionCookiePropertiesConfiguration.class,
  JwtService.class,
  SessionCookieSupport.class,
  JwtCookieAuthenticationFilter.class
})
public class MvcTestSessionAuthConfig {}
