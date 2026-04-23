package com.kevinmazali.portfolio.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
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
import java.util.concurrent.TimeUnit;
import org.springframework.lang.NonNull;

/**
 * Registers servlet filters that rate-limit {@code POST /ask}, {@code POST /auth/login},
 * {@code POST /feedback}, {@code POST /admin/tools/experiments/run}, and
 * {@code POST /admin/tools/experiments/datasets/generate} (token buckets per client key or IP).
 * <p>
 * Admin experiment POSTs use {@code portfolio.experiments.*-rate-limit} defaults that are more generous than
 * {@code POST /ask}, because those routes already require {@code ROLE_ADMIN} (HTTP Basic) in
 * {@link SecurityConfig}; limits here mainly guard accidental bursts and shared credentials rather than anonymous abuse.
 * CORS is configured in {@link SecurityConfig}.
 */
@Configuration
public class WebConfig {

    private final AskRateLimitProperties askRateLimitProperties;
    private final ExperimentRunRateLimitProperties experimentRunRateLimitProperties;
    private final DatasetGenerateRateLimitProperties datasetGenerateRateLimitProperties;

    public WebConfig(
        AskRateLimitProperties askRateLimitProperties,
        ExperimentRunRateLimitProperties experimentRunRateLimitProperties,
        DatasetGenerateRateLimitProperties datasetGenerateRateLimitProperties) {
        this.askRateLimitProperties = askRateLimitProperties;
        this.experimentRunRateLimitProperties = experimentRunRateLimitProperties;
        this.datasetGenerateRateLimitProperties = datasetGenerateRateLimitProperties;
    }

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> loginBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> feedbackBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> experimentRunBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> datasetGenerateBuckets = new ConcurrentHashMap<>();

    private Bucket newAskBucketAuthenticated() {
        AskRateLimitProperties p = askRateLimitProperties;
        Bandwidth limit = Bandwidth.builder()
            .capacity(p.getAuthenticatedCapacity())
            .refillGreedy(p.getAuthenticatedCapacity(), Duration.ofSeconds(p.getAuthenticatedWindowSeconds()))
            .build();
        return Bucket.builder().addLimit(limit).build();
    }

    private Bucket newAskBucketAnonymous() {
        AskRateLimitProperties p = askRateLimitProperties;
        Bandwidth limit = Bandwidth.builder()
            .capacity(p.getAnonymousCapacity())
            .refillGreedy(p.getAnonymousCapacity(), Duration.ofSeconds(p.getAnonymousWindowSeconds()))
            .build();
        return Bucket.builder().addLimit(limit).build();
    }

    private String askKey(HttpServletRequest req) {
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

    private Bucket newFeedbackBucket() {
        Bandwidth limit = Bandwidth.builder()
            .capacity(3)
            .refillGreedy(3, Duration.ofSeconds(60))
            .build();
        return Bucket.builder().addLimit(limit).build();
    }

    private String feedbackKey(HttpServletRequest req) {
        return "feedback:ip:" + req.getRemoteAddr();
    }

    private Bucket newExperimentRunBucket() {
        ExperimentRunRateLimitProperties p = experimentRunRateLimitProperties;
        Bandwidth limit = Bandwidth.builder()
            .capacity(p.getCapacity())
            .refillGreedy(p.getCapacity(), Duration.ofSeconds(p.getWindowSeconds()))
            .build();
        return Bucket.builder().addLimit(limit).build();
    }

    private String experimentRunKey(HttpServletRequest req) {
        String user = req.getUserPrincipal() != null ? req.getUserPrincipal().getName() : "unknown";
        return "exp-run:" + user;
    }

    private Bucket newDatasetGenerateBucket() {
        DatasetGenerateRateLimitProperties p = datasetGenerateRateLimitProperties;
        Bandwidth limit = Bandwidth.builder()
            .capacity(p.getCapacity())
            .refillGreedy(p.getCapacity(), Duration.ofSeconds(p.getWindowSeconds()))
            .build();
        return Bucket.builder().addLimit(limit).build();
    }

    private String datasetGenerateKey(HttpServletRequest req) {
        String user = req.getUserPrincipal() != null ? req.getUserPrincipal().getName() : "unknown";
        return "dataset-gen:" + user;
    }

    private static void write429(HttpServletResponse response, ConsumptionProbe probe) throws IOException {
        response.setStatus(429);
        response.setContentType("application/json");
        long seconds = TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill()) + 1;
        response.setHeader("Retry-After", String.valueOf(Math.max(1, seconds)));
        response.getWriter().write("{\"error\":\"Too Many Requests\"}");
    }

    @Bean
    @ConditionalOnProperty(name = "portfolio.ask-rate-limit.enabled", havingValue = "true", matchIfMissing = true)
    public org.springframework.boot.web.servlet.FilterRegistrationBean<Filter> askRateLimitFilter() {
        var registration = new org.springframework.boot.web.servlet.FilterRegistrationBean<Filter>();
        registration.setFilter(new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain)
                throws ServletException, IOException {
                if (!"POST".equalsIgnoreCase(request.getMethod())) {
                    filterChain.doFilter(request, response);
                    return;
                }
                String k = askKey(request);
                Bucket bucket = buckets.computeIfAbsent(k, key -> key.contains(":ip:") ? newAskBucketAnonymous() : newAskBucketAuthenticated());
                ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
                if (probe.isConsumed()) {
                    filterChain.doFilter(request, response);
                } else {
                    write429(response, probe);
                }
            }
        });
        registration.addUrlPatterns("/ask");
        registration.setName("askRateLimitFilter");
        registration.setOrder(1);
        return registration;
    }

    @Bean
    @ConditionalOnProperty(name = "portfolio.login-rate-limit.enabled", havingValue = "true", matchIfMissing = true)
    public org.springframework.boot.web.servlet.FilterRegistrationBean<Filter> loginRateLimitFilter() {
        var registration = new org.springframework.boot.web.servlet.FilterRegistrationBean<Filter>();
        registration.setFilter(new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain)
                throws ServletException, IOException {
                if (!"POST".equalsIgnoreCase(request.getMethod())) {
                    filterChain.doFilter(request, response);
                    return;
                }
                Bucket bucket = loginBuckets.computeIfAbsent(loginKey(request), k -> newLoginBucket());
                ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
                if (probe.isConsumed()) {
                    filterChain.doFilter(request, response);
                } else {
                    write429(response, probe);
                }
            }
        });
        registration.addUrlPatterns("/auth/login");
        registration.setName("loginRateLimitFilter");
        registration.setOrder(0);
        return registration;
    }

    @Bean
    @ConditionalOnProperty(name = "portfolio.feedback-rate-limit.enabled", havingValue = "true", matchIfMissing = true)
    public org.springframework.boot.web.servlet.FilterRegistrationBean<Filter> feedbackRateLimitFilter() {
        var registration = new org.springframework.boot.web.servlet.FilterRegistrationBean<Filter>();
        registration.setFilter(new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain)
                throws ServletException, IOException {
                if (!"POST".equalsIgnoreCase(request.getMethod())) {
                    filterChain.doFilter(request, response);
                    return;
                }
                Bucket bucket = feedbackBuckets.computeIfAbsent(feedbackKey(request), k -> newFeedbackBucket());
                ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
                if (probe.isConsumed()) {
                    filterChain.doFilter(request, response);
                } else {
                    write429(response, probe);
                }
            }
        });
        registration.addUrlPatterns("/feedback");
        registration.setName("feedbackRateLimitFilter");
        registration.setOrder(2);
        return registration;
    }

    @Bean
    @ConditionalOnProperty(name = "portfolio.experiments.run-rate-limit.enabled", havingValue = "true", matchIfMissing = true)
    public org.springframework.boot.web.servlet.FilterRegistrationBean<Filter> experimentRunRateLimitFilter() {
        var registration = new org.springframework.boot.web.servlet.FilterRegistrationBean<Filter>();
        registration.setFilter(new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain)
                throws ServletException, IOException {
                String uri = request.getRequestURI();
                if (!"POST".equalsIgnoreCase(request.getMethod())
                    || uri == null
                    || !uri.endsWith("/admin/tools/experiments/run")) {
                    filterChain.doFilter(request, response);
                    return;
                }
                Bucket bucket = experimentRunBuckets.computeIfAbsent(experimentRunKey(request), k -> newExperimentRunBucket());
                ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
                if (probe.isConsumed()) {
                    filterChain.doFilter(request, response);
                } else {
                    write429(response, probe);
                }
            }
        });
        registration.addUrlPatterns("/admin/tools/experiments/run");
        registration.setName("experimentRunRateLimitFilter");
        registration.setOrder(3);
        return registration;
    }

    @Bean
    @ConditionalOnProperty(name = "portfolio.experiments.dataset-generate-rate-limit.enabled", havingValue = "true", matchIfMissing = true)
    public org.springframework.boot.web.servlet.FilterRegistrationBean<Filter> datasetGenerateRateLimitFilter() {
        var registration = new org.springframework.boot.web.servlet.FilterRegistrationBean<Filter>();
        registration.setFilter(new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain)
                throws ServletException, IOException {
                String uri = request.getRequestURI();
                if (!"POST".equalsIgnoreCase(request.getMethod())
                    || uri == null
                    || !uri.endsWith("/admin/tools/experiments/datasets/generate")) {
                    filterChain.doFilter(request, response);
                    return;
                }
                Bucket bucket = datasetGenerateBuckets.computeIfAbsent(datasetGenerateKey(request), k -> newDatasetGenerateBucket());
                ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
                if (probe.isConsumed()) {
                    filterChain.doFilter(request, response);
                } else {
                    write429(response, probe);
                }
            }
        });
        registration.addUrlPatterns("/admin/tools/experiments/datasets/generate");
        registration.setName("datasetGenerateRateLimitFilter");
        registration.setOrder(4);
        return registration;
    }
}
