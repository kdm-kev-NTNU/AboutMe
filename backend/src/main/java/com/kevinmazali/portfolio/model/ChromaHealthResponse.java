package com.kevinmazali.portfolio.model;

/**
 * Public health payload for ChromaDB connectivity checks.
 */
public record ChromaHealthResponse(
    boolean healthy,
    String collectionName,
    Long embeddingCount,
    String message
) {}
