package com.kevinmazali.portfolio.model.experiment;

/**
 * Result of one LLM-as-judge metric.
 */
public record EvaluationScore(
    double score,
    String label,
    String explanation
) {
  public static EvaluationScore failed(String reason) {
    return new EvaluationScore(Double.NaN, "error", reason != null ? reason : "unknown");
  }
}
