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

    /**
     * Configures permissive CORS for the known front-end origins.
     */
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(@NonNull CorsRegistry registry) {
                registry.addMapping("/**")
                    .allowedOrigins(
                        "http://localhost:5173",
                        "http://localhost:4173",
                        "https://kevindmazali.me",
                        "https://www.kevindmazali.me"
                    )
                    .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                    .allowedHeaders(
                        "Content-Type",
                        "Authorization",
                        "X-Requested-With",
                        "Accept",
                        "Origin",
                        "Access-Control-Request-Method",
                        "Access-Control-Request-Headers"
                    )
                    .allowCredentials(true);
            }
        };
    }

    /**
     * Ensures each client has a stable chatId. If missing in request (header or cookie),
     * generates a UUID, exposes it as request attribute "chatId", sets it in response header
     * X-Chat-Id and a cookie "chatId"; controllers can read from request attribute as fallback.
     */
    // X-Chat-Id handling removed.

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

    /**
     * Adds security headers to all responses.
     */
    @Bean
    public Filter securityHeadersFilter() {
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain)
                throws ServletException, IOException {
                
                // Prevent clickjacking
                response.setHeader("X-Frame-Options", "DENY");
                
                // Prevent MIME type sniffing
                response.setHeader("X-Content-Type-Options", "nosniff");
                
                // XSS Protection
                response.setHeader("X-XSS-Protection", "1; mode=block");
                
                // Referrer Policy
                response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
                
                // Content Security Policy
                response.setHeader("Content-Security-Policy", 
                    "default-src 'self'; " +
                    "script-src 'self' 'unsafe-inline'; " +
                    "style-src 'self' 'unsafe-inline'; " +
                    "img-src 'self' data: https:; " +
                    "font-src 'self'; " +
                    "connect-src 'self' https://api.openai.com; " +
                    "frame-ancestors 'none'; " +
                    "base-uri 'self'; " +
                    "form-action 'self'");
                
                // Strict Transport Security (only for HTTPS)
                if (request.isSecure()) {
                    response.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
                }
                
                // Permissions Policy
                response.setHeader("Permissions-Policy", 
                    "camera=(), microphone=(), geolocation=(), payment=(), usb=()");
                
                filterChain.doFilter(request, response);
            }
        };
    }

    /**
     * Rate limiter for /ask endpoint (5 requests per 10 seconds).
     */
    @Bean
    public Filter askRateLimitFilter() {
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain)
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

    // Conversations rate limiter removed.
}


