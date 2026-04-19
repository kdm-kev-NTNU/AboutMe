package com.kevinmazali.portfolio.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.Filter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.lang.NonNull;

/**
 * Registers servlet filters that rate-limit {@code POST /ask} and {@code POST /auth/login}
 * (token buckets per client key or IP).
 * CORS is configured in {@link SecurityConfig}.
 */
@Configuration
public class WebConfig {

    // CORS and security headers: see SecurityConfig (this class only registers rate limit filters).

    /** One token bucket per client key (authenticated username, else client IP) for /ask. */
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    /** One token bucket per client IP for /auth/login (credential stuffing mitigation). */
    private final Map<String, Bucket> loginBuckets = new ConcurrentHashMap<>();

    private Bucket newBucket() {
        Bandwidth limit = Bandwidth.builder()
            .capacity(5)
            .refillGreedy(5, Duration.ofSeconds(10))
            .build();
        return Bucket.builder().addLimit(limit).build();
    }

    /** Rate-limit bucket key: prefer principal name when present so logged-in users are not pooled with anonymous IPs. */
    private String key(HttpServletRequest req) {
        String user = req.getUserPrincipal() != null ? req.getUserPrincipal().getName() : null;
        String ip = req.getRemoteAddr();
        return "ask:" + (user != null ? "u:" + user : "ip:" + ip);
    }

    private Bucket newLoginBucket() {
        Bandwidth limit = Bandwidth.builder()
            .capacity(5)
            .refillGreedy(5, Duration.ofSeconds(60))
            .build();
        return Bucket.builder().addLimit(limit).build();
    }

    private String loginKey(HttpServletRequest req) {
        return "login:ip:" + req.getRemoteAddr();
    }

    /**
     * Rate limiter for /ask endpoint (5 requests per 10 seconds).
     */
    @Bean
    @ConditionalOnProperty(name = "portfolio.ask-rate-limit.enabled", havingValue = "true", matchIfMissing = true)
    public org.springframework.boot.web.servlet.FilterRegistrationBean<Filter> askRateLimitFilter() {
        var registration = new org.springframework.boot.web.servlet.FilterRegistrationBean<Filter>();
        registration.setFilter(new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain)
                throws ServletException, IOException {
                Bucket bucket = buckets.computeIfAbsent(key(request), k -> newBucket());
                if (bucket.tryConsume(1)) {
                    filterChain.doFilter(request, response);
                } else {
                    response.setStatus(429);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\":\"Too Many Requests\"}");
                }
            }
        });
        registration.addUrlPatterns("/ask");
        registration.setName("askRateLimitFilter");
        registration.setOrder(1);
        return registration;
    }

    /**
     * Rate limiter for {@code POST /auth/login} (5 attempts per 60 seconds per client IP).
     */
    @Bean
    @ConditionalOnProperty(name = "portfolio.login-rate-limit.enabled", havingValue = "true", matchIfMissing = true)
    public org.springframework.boot.web.servlet.FilterRegistrationBean<Filter> loginRateLimitFilter() {
        var registration = new org.springframework.boot.web.servlet.FilterRegistrationBean<Filter>();
        registration.setFilter(new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain)
                throws ServletException, IOException {
                Bucket bucket = loginBuckets.computeIfAbsent(loginKey(request), k -> newLoginBucket());
                if (bucket.tryConsume(1)) {
                    filterChain.doFilter(request, response);
                } else {
                    response.setStatus(429);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\":\"Too Many Requests\"}");
                }
            }
        });
        registration.addUrlPatterns("/auth/login");
        registration.setName("loginRateLimitFilter");
        registration.setOrder(0);
        return registration;
    }


}


