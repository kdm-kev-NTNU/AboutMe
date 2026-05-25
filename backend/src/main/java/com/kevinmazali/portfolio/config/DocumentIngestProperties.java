package com.kevinmazali.portfolio.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Limits for admin document parsing (Tika) to reduce DoS risk from malicious files.
 */
@ConfigurationProperties(prefix = "portfolio.document-ingest")
public class DocumentIngestProperties {

    private long maxParseBytes = 52_428_800;
    private int parseTimeoutSeconds = 120;

    public long getMaxParseBytes() {
        return maxParseBytes;
    }

    public void setMaxParseBytes(long maxParseBytes) {
        this.maxParseBytes = maxParseBytes;
    }

    public int getParseTimeoutSeconds() {
        return parseTimeoutSeconds;
    }

    public void setParseTimeoutSeconds(int parseTimeoutSeconds) {
        this.parseTimeoutSeconds = parseTimeoutSeconds;
    }
}
