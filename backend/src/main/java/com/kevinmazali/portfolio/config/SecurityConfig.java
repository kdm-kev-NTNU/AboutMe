package com.kevinmazali.portfolio.config;

import com.kevinmazali.portfolio.security.JwtCookieAuthenticationFilter;
import com.kevinmazali.portfolio.security.JsonAccessDeniedHandler;
import com.kevinmazali.portfolio.security.JsonAuthenticationEntryPoint;
import io.micrometer.tracing.Tracer;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import tools.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Spring Security: httpOnly session cookie JWT for admins, optional HTTP Basic for tooling,
 * role-based rules for {@code /admin/**}, and a CORS allow-list aligned with the Vue SPA.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    AuthenticationEntryPoint jsonAuthenticationEntryPoint(
            ObjectMapper objectMapper, ObjectProvider<Tracer> tracer) {
        return new JsonAuthenticationEntryPoint(objectMapper, tracer);
    }

    @Bean
    AccessDeniedHandler jsonAccessDeniedHandler(ObjectMapper objectMapper, ObjectProvider<Tracer> tracer) {
        return new JsonAccessDeniedHandler(objectMapper, tracer);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            AuthenticationEntryPoint jsonAuthenticationEntryPoint,
            AccessDeniedHandler jsonAccessDeniedHandler,
            JwtCookieAuthenticationFilter jwtCookieAuthenticationFilter,
            @Value("${portfolio.test.disable-csrf:false}") boolean disableCsrfForTests)
            throws Exception {

        if (disableCsrfForTests) {
            http.csrf(AbstractHttpConfigurer::disable);
        } else {
            // spa(): CookieCsrfTokenRepository (XSRF-TOKEN / X-XSRF-TOKEN) + SpaCsrfTokenRequestHandler
            // so the SPA can send the raw cookie value in the header (not the BREACH-masked form token).
            http.csrf(
                csrf ->
                    csrf.spa()
                        .ignoringRequestMatchers(
                            "/ask",
                            "/feedback",
                            "/transcribe",
                            "/auth/login",
                            "/realtime/**",
                            "/health/**",
                            "/actuator/health",
                            "/actuator/info",
                            "/v3/api-docs/**",
                            "/swagger-ui/**",
                            "/swagger-ui.html",
                            "/chat/models"));
        }

        http.cors(Customizer.withDefaults())
            .exceptionHandling(
                ex ->
                    ex.authenticationEntryPoint(jsonAuthenticationEntryPoint)
                        .accessDeniedHandler(jsonAccessDeniedHandler))
            .httpBasic(Customizer.withDefaults())
            .addFilterBefore(jwtCookieAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .headers(headers -> headers
                .contentTypeOptions(Customizer.withDefaults())
                .frameOptions(frame -> frame.deny())
                .httpStrictTransportSecurity(hsts -> hsts
                    .maxAgeInSeconds(31536000)
                    .includeSubDomains(true))
                .contentSecurityPolicy(csp -> csp
                    .policyDirectives("default-src 'none'; frame-ancestors 'none'")))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                .requestMatchers("/health/**").permitAll()
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                .requestMatchers("/actuator/**").hasRole("ADMIN")
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/auth/me").authenticated()
                .requestMatchers(HttpMethod.POST, "/auth/logout").permitAll()
                .anyRequest().permitAll());
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(
            "http://localhost:5173",
            "http://127.0.0.1:5173",
            "http://localhost:5174",
            "http://127.0.0.1:5174",
            "http://localhost:4173",
            "http://127.0.0.1:4173",
            "https://kevindmazali.me",
            "https://www.kevindmazali.me"
        ));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of(
            "Content-Type",
            "Authorization",
            "X-Requested-With",
            "Accept",
            "Origin",
            "Access-Control-Request-Method",
            "Access-Control-Request-Headers",
            "X-Chat-Language",
            "X-Realtime-Model",
            "X-Realtime-Voice",
            "X-Realtime-Reasoning-Effort",
            "X-Realtime-Vad-Eagerness",
            "X-Conversation-Id",
            "X-XSRF-TOKEN"
        ));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }
}
