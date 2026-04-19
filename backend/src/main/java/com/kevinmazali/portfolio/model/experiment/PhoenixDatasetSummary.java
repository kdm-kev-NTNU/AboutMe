package com.kevinmazali.portfolio.model.experiment;

/**
 * One Phoenix dataset row for admin UI.
 */
public record PhoenixDatasetSummary(
    String id,
    String name,
    int exampleCount
) {
}
