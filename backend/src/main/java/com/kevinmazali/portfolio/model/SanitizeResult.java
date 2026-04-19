package com.kevinmazali.portfolio.model;

import java.util.Map;

/**
 * Result of PII sanitization on a text segment.
 *
 * @param sanitizedText   text after PII replacement
 * @param piiDetectedCount total PII spans detected and redacted
 * @param piiTypes         entity-type to count, e.g. {"email-address": 2, "phone-number": 1}
 * @param complianceStatus PASS (no PII), REVIEW (PII found and removed), or FAIL (PII may remain)
 */
public record SanitizeResult(
        String sanitizedText,
        int piiDetectedCount,
        Map<String, Integer> piiTypes,
        String complianceStatus
) {}
