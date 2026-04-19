package com.kevinmazali.portfolio.model.prompt;

/**
 * Comparison between the active DB version and the classpath fallback for a prompt variant.
 */
public record PromptDiffResponse(
    String name,
    String language,
    String provider,
    boolean hasDbActive,
    boolean hasCodeFallback,
    boolean isEqual,
    String dbContent,
    String fallbackContent
) {}
