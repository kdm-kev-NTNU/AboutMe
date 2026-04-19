package com.kevinmazali.portfolio.model.experiment;

import java.util.Map;

/**
 * One example from Phoenix {@code GET /v1/datasets/{id}/examples}.
 */
public record PhoenixDatasetExample(
    String question,
    String referenceText,
    Map<String, Object> rawInput,
    Map<String, Object> rawOutput
) {
}
