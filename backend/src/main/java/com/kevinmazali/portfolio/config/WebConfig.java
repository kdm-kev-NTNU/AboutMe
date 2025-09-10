package com.kevinmazali.portfolio.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
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
import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Web configuration including CORS and a lightweight rate limiter for the /ask endpoint.
 */
@Configuration
public class WebConfig {

    /**
     * Configures permissive CORS for the known front-end origins.
     */
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                    .allowedOrigins(
                        "http://localhost:5173",
                        "http://localhost:4173",
                        "https://kevindmazali.me",
                        "https://www.kevindmazali.me"
                    )
                    .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                    .allowedHeaders("*")
                    .allowCredentials(true);
            }
        };
    }

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    private Bucket newBucket() {
        Bandwidth limit = Bandwidth.builder()
            .capacity(5)
            .refillGreedy(5, Duration.ofSeconds(10))
            .initialTokens(5)
            .build();
        return Bucket.builder().addLimit(limit).build();
    }

    private String key(HttpServletRequest req) {
        String user = req.getUserPrincipal() != null ? req.getUserPrincipal().getName() : null;
        String ip = req.getRemoteAddr();
        return "ask:" + (user != null ? "u:" + user : "ip:" + ip);
    }

    /**
     * Simple per-user/IP rate limiter using Bucket4j for the /ask endpoint.
     */
    @Bean
    public Filter rateLimitFilter() {
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
                throws ServletException, IOException {
                if (!request.getRequestURI().startsWith("/ask")) {
                    filterChain.doFilter(request, response);
                    return;
                }

                Bucket bucket = buckets.computeIfAbsent(key(request), k -> newBucket());
                if (bucket.tryConsume(1)) {
                    filterChain.doFilter(request, response);
                } else {
                    response.setStatus(429);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\":\"Too Many Requests\"}");
                }
            }
        };
    }
}


