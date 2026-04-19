package com.kevinmazali.portfolio.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Always binds {@link SanitizerProperties} so the enabled flag is available
 * even when PII scrubbing is disabled and {@link PhileasConfig} is not active.
 */
@Configuration
@EnableConfigurationProperties(SanitizerProperties.class)
public class SanitizerPropertiesConfiguration {
}
