package com.kevinmazali.portfolio.model.prompt;

/**
 * Summary of an active prompt variant, used in the "names" listing.
 */
public record PromptNameEntry(
    String name,
    String language,
    String provider,
    int activeVersion,
    long activeId,
    String createdAt
) {}
