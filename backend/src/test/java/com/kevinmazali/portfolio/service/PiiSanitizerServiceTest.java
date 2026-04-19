package com.kevinmazali.portfolio.service;

import ai.philterd.phileas.model.configuration.PhileasConfiguration;
import com.kevinmazali.portfolio.config.SanitizerProperties;
import com.kevinmazali.portfolio.model.SanitizeResult;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests PiiSanitizerService with Phileas running in-process (no ph-eye NER).
 * Covers regex-based filters: email addresses and phone numbers.
 * Name detection (FirstName/Surname) depends on Phileas dictionary data.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PiiSanitizerServiceTest {

    private PiiSanitizerService service;

    @BeforeAll
    void setup() throws Exception {
        Properties props = new Properties();
        PhileasConfiguration config = new PhileasConfiguration(props);

        SanitizerProperties sanitizerProps = new SanitizerProperties();
        sanitizerProps.setPhEyeUrl("");
        sanitizerProps.setValkeyHost("");

        service = new PiiSanitizerService(config, sanitizerProps);
    }

    @Test
    void sanitizeReturnsPassOrReviewForPlainText() {
        SanitizeResult result = service.sanitize("The quick brown fox jumps over the lazy dog.");
        assertNotNull(result);
        assertNotNull(result.sanitizedText());
    }

    @Test
    void sanitizeHandlesNull() {
        SanitizeResult result = service.sanitize(null);
        assertEquals("PASS", result.complianceStatus());
    }

    @Test
    void sanitizeHandlesBlank() {
        SanitizeResult result = service.sanitize("   ");
        assertEquals("PASS", result.complianceStatus());
    }

    @Test
    void sanitizeRedactsEmailAddress() {
        SanitizeResult result = service.sanitize("Contact us at john@example.com for details.");
        assertNotNull(result.sanitizedText());
        assertFalse(result.sanitizedText().contains("john@example.com"),
                "Email address should be redacted");
        assertTrue(result.piiDetectedCount() > 0);
        assertEquals("REVIEW", result.complianceStatus());
    }

    @Test
    void sanitizeRedactsPhoneNumber() {
        SanitizeResult result = service.sanitize("Call me at 555-123-4567 tomorrow.");
        assertNotNull(result.sanitizedText());
        assertFalse(result.sanitizedText().contains("555-123-4567"),
                "Phone number should be redacted");
        assertTrue(result.piiDetectedCount() > 0);
    }

    @Test
    void sanitizeRedactsMultiplePiiTypes() {
        SanitizeResult result = service.sanitize(
                "Email admin@corp.com or call 800-555-0199 for support.");
        assertNotNull(result.sanitizedText());
        assertFalse(result.sanitizedText().contains("admin@corp.com"));
        assertFalse(result.sanitizedText().contains("800-555-0199"));
        assertTrue(result.piiDetectedCount() >= 2);
        assertFalse(result.piiTypes().isEmpty());
    }
}
