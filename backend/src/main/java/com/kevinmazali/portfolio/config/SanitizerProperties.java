package com.kevinmazali.portfolio.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration for the PII sanitizer (Phileas + optional ph-eye NER).
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "portfolio.sanitizer")
public class SanitizerProperties {

    private boolean enabled = true;

    /** ph-eye NER service URL (e.g. http://ph-eye:5000). Empty disables NER-based person detection. */
    private String phEyeUrl = "";

    /** Valkey/Redis host for Phileas caching. Empty disables caching. */
    private String valkeyHost = "";

    /** Valkey/Redis port. */
    private int valkeyPort = 6379;

    /** Redaction format template. {@code %t} is replaced by entity type. */
    private String redactionFormat = "{{{REDACTED-%t}}}";
}
