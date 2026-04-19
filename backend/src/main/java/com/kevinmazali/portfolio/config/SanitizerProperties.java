package com.kevinmazali.portfolio.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration for the PII sanitizer (regex + OpenNLP NER).
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "portfolio.sanitizer")
public class SanitizerProperties {

    private boolean enabled = true;

    /** Redaction format template. {@code %t} is replaced by entity type. */
    private String redactionFormat = "{{{REDACTED-%t}}}";
}
