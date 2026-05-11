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
        java.io.File envBackend = new java.io.File("backend/.env");
        log.info("Config: exists ./.env = {}", envHere.exists());
        log.info("Config: exists backend/.env = {}", envBackend.exists());
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

        boolean elevenLabsOn =
            realtimeOn
                && Boolean.parseBoolean(
                    environment.getProperty("portfolio.realtime.providers.elevenlabs.enabled", "false"));
        if (elevenLabsOn) {
            String elApiKey = environment.getProperty("ELEVENLABS_API_KEY", "");
            String elAgentId = environment.getProperty("ELEVENLABS_AGENT_ID", "");
            log.info("Config: ElevenLabs provider enabled=true, agent-id={}, api-key={}",
                elAgentId == null || elAgentId.isBlank() ? "<empty>" : elAgentId,
                elApiKey == null || elApiKey.isBlank() ? "<empty>" : "***");
            if (elAgentId == null || elAgentId.isBlank()) {
                log.warn(
                    "ElevenLabs provider is enabled but ELEVENLABS_AGENT_ID is blank; "
                        + "token requests will fail with VOICE_MODEL_NOT_CONFIGURED. "
                        + "Set a valid agent ID from the ElevenLabs dashboard.");
            }
            if (elApiKey == null || elApiKey.isBlank()) {
                log.warn(
                    "ElevenLabs provider is enabled but ELEVENLABS_API_KEY is blank; "
                        + "token requests will fail with API_KEY_MISSING.");
            }
            String elEnv = environment.getProperty("ELEVENLABS_AGENT_ENVIRONMENT", "");
            String elBranch = environment.getProperty("ELEVENLABS_AGENT_BRANCH_ID", "");
            if (elEnv != null && !elEnv.isBlank()) {
                log.info("Config: ElevenLabs agent environment={}", elEnv);
            }
            if (elBranch != null && !elBranch.isBlank()) {
                log.info("Config: ElevenLabs agent branch-id={}", elBranch);
            }
        }
    }
}
