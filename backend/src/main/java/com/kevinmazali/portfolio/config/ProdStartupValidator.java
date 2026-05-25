package com.kevinmazali.portfolio.config;

import com.kevinmazali.portfolio.repository.UserRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Fails fast in production when unsafe defaults or leftover bootstrap secrets are detected.
 */
@Component
@Profile("prod")
@Order(100)
public class ProdStartupValidator implements ApplicationRunner {

    static final String DEFAULT_ANON_SALT = "portfolio-ai-budget";
    static final String DEFAULT_DB_PASSWORD = "postgres";

    private final Environment environment;
    private final AiBudgetProperties aiBudgetProperties;
    private final SessionCookieProperties sessionCookieProperties;
    private final UserRepository userRepository;

    public ProdStartupValidator(
            Environment environment,
            AiBudgetProperties aiBudgetProperties,
            SessionCookieProperties sessionCookieProperties,
            UserRepository userRepository) {
        this.environment = environment;
        this.aiBudgetProperties = aiBudgetProperties;
        this.sessionCookieProperties = sessionCookieProperties;
        this.userRepository = userRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (DEFAULT_ANON_SALT.equals(aiBudgetProperties.getAnonIdentitySalt())) {
            throw new IllegalStateException(
                    "Production requires a unique AI_BUDGET_ANON_SALT (default salt is not allowed)");
        }

        String dbPassword = environment.getProperty("spring.datasource.password");
        if (!StringUtils.hasText(dbPassword) || DEFAULT_DB_PASSWORD.equals(dbPassword)) {
            throw new IllegalStateException(
                    "Production requires a strong SPRING_DATASOURCE_PASSWORD (default 'postgres' is not allowed)");
        }

        String jwtSecret = sessionCookieProperties.getJwtSecret();
        if (!StringUtils.hasText(jwtSecret) || jwtSecret.length() < 32) {
            throw new IllegalStateException(
                    "Production requires PORTFOLIO_JWT_SECRET with at least 32 characters");
        }

        String bootstrapPassword = environment.getProperty("ADMIN_BOOTSTRAP_PASSWORD");
        if (StringUtils.hasText(bootstrapPassword) && userRepository.count() > 0) {
            throw new IllegalStateException(
                    "Clear ADMIN_BOOTSTRAP_PASSWORD after the first admin user was created");
        }
    }
}
