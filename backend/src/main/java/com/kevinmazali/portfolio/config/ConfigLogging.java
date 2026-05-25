package com.kevinmazali.portfolio.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;

/**
 * Startup diagnostics: logs non-secret config hints (ports, datasource URL, presence of {@code .env})
 * to speed up environment troubleshooting in dev and deployed environments.
 */
@Configuration
public class ConfigLogging implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ConfigLogging.class);

    private final Environment environment;

    public ConfigLogging(Environment environment) {
        this.environment = environment;
    }

    /** Emits one-time INFO lines after the application context is ready. */
    @Override
    public void run(ApplicationArguments args) {
        log.info("Config: user.dir={} (working directory)", System.getProperty("user.dir"));
        java.io.File envHere = new java.io.File(".env");
        log.info("Config: exists ./.env = {}", envHere.exists());
        // Report resolved values (mask secrets)
        log.info("Config: server.port={}", environment.getProperty("server.port"));
        log.info("Config: spring.datasource.url={}", environment.getProperty("spring.datasource.url"));
        log.info("Config: spring.datasource.username={}", environment.getProperty("spring.datasource.username"));
        String pwd = environment.getProperty("spring.datasource.password");
        log.info("Config: spring.datasource.password={} (masked)", pwd == null || pwd.isBlank() ? "<empty>" : "***");

        // Indicate whether .env was loaded
        if (environment instanceof ConfigurableEnvironment ce) {
            boolean hasDotEnv = false;
            for (PropertySource<?> ps : ce.getPropertySources()) {
                String name = ps.getName();
                if (name != null && name.toLowerCase().contains(".env")) {
                    hasDotEnv = true;
                    break;
                }
            }
            log.info("Config: .env property source loaded = {}", hasDotEnv);
        }

        log.info("Config: spring.ai.vectorstore.pgvector.schema-name={}",
            environment.getProperty("spring.ai.vectorstore.pgvector.schema-name", "<unset>"));
        log.info("Config: spring.ai.vectorstore.pgvector.table-name={}",
            environment.getProperty("spring.ai.vectorstore.pgvector.table-name", "<unset>"));
        log.info("Config: spring.ai.vectorstore.pgvector.index-type={}",
            environment.getProperty("spring.ai.vectorstore.pgvector.index-type", "<unset>"));
        log.info("Config: portfolio.posthog.enabled={}",
            environment.getProperty("portfolio.posthog.enabled", "<unset>"));
        log.info("Config: portfolio.posthog.host={}",
            environment.getProperty("portfolio.posthog.host", "<unset>"));
        String posthogKey = environment.getProperty("portfolio.posthog.api-key");
        log.info("Config: portfolio.posthog.api-key={}",
            posthogKey == null || posthogKey.isBlank() ? "<empty>" : "***");

        boolean realtimeOn =
            Boolean.parseBoolean(environment.getProperty("portfolio.realtime.enabled", "false"));
        String openaiKey = environment.getProperty("spring.ai.openai.api-key", "");
        if (realtimeOn && (openaiKey == null || openaiKey.isBlank())) {
            log.warn(
                "portfolio.realtime.enabled=true but spring.ai.openai.api-key is blank; "
                    + "voice status will show disabled and POST /realtime/session will return 503 until a key is set.");
        }
    }
}
