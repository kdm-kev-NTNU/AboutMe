package com.kevinmazali.portfolio.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.Filter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletRequestWrapper;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.lang.NonNull;
import java.util.Collections;

/**
 * Web configuration including CORS and a lightweight rate limiter for the /ask endpoint.
 */
@Configuration
public class WebConfig {

    // CORS and headers are handled by Spring Security


    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    private Bucket newBucket() {
        Bandwidth limit = Bandwidth.builder()
            .capacity(5)
            .refillGreedy(5, Duration.ofSeconds(10))
            .build();
        return Bucket.builder().addLimit(limit).build();
    }

    private String key(HttpServletRequest req) {
        String user = req.getUserPrincipal() != null ? req.getUserPrincipal().getName() : null;
        String ip = req.getRemoteAddr();
        return "ask:" + (user != null ? "u:" + user : "ip:" + ip);
    }

    // Security headers are handled by Spring Security

    /**
     * Rate limiter for /ask endpoint (5 requests per 10 seconds).
     */
    @Bean
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


}


