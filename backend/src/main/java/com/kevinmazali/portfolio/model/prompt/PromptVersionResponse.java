package com.kevinmazali.portfolio.model.prompt;

/**
 * Full prompt version snapshot returned by history and create endpoints.
 */
public record PromptVersionResponse(
    long id,
    String name,
    int version,
    String language,
    String provider,
    String content,
    String contentHash,
    boolean isActive,
    String description,
    String createdAt
) {}
