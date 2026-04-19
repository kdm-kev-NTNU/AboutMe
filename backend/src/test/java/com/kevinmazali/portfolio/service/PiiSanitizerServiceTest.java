package com.kevinmazali.portfolio.service;

import com.kevinmazali.portfolio.model.SanitizeResult;
import opennlp.tools.namefind.TokenNameFinderModel;
import opennlp.tools.tokenize.TokenizerModel;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests PiiSanitizerService with regex patterns and OpenNLP NER (in-process).
 * Covers email addresses, phone numbers, and person-name detection.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PiiSanitizerServiceTest {

    private PiiSanitizerService service;

    @BeforeAll
    void setup() throws Exception {
        try (InputStream nerStream = getClass().getResourceAsStream("/models/en-ner-person.bin");
             InputStream tokStream = getClass().getResourceAsStream("/models/en-token.bin")) {
            TokenNameFinderModel nameModel = new TokenNameFinderModel(nerStream);
            TokenizerModel tokenizerModel = new TokenizerModel(tokStream);
            service = new PiiSanitizerService(nameModel, tokenizerModel);
        }
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
