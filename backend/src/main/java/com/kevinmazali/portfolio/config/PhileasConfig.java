package com.kevinmazali.portfolio.config;

import ai.philterd.phileas.model.configuration.PhileasConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.util.Properties;

/**
 * Wires Phileas PII detection/redaction beans when sanitizer is enabled.
 * Properties are always bound via {@link SanitizerPropertiesConfiguration}.
 */
@Configuration
@ConditionalOnProperty(name = "portfolio.sanitizer.enabled", havingValue = "true", matchIfMissing = true)
public class PhileasConfig {

    @Bean
    public PhileasConfiguration phileasConfiguration(SanitizerProperties props) throws IOException {
        Properties properties = new Properties();

        if (props.getPhEyeUrl() != null && !props.getPhEyeUrl().isBlank()) {
            properties.setProperty("ner.endpoint", props.getPhEyeUrl());
        }

        if (props.getValkeyHost() != null && !props.getValkeyHost().isBlank()) {
            properties.setProperty("cache.redis.host", props.getValkeyHost());
            properties.setProperty("cache.redis.port", String.valueOf(props.getValkeyPort()));
        }

        return new PhileasConfiguration(properties);
    }
}
