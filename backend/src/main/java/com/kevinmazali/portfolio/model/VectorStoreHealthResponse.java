package com.kevinmazali.portfolio.model;

/**
 * Public health payload for the pgvector-backed {@code vector_store} table (same JSON shape as the former Chroma health endpoint).
 */
public record VectorStoreHealthResponse(
    boolean healthy,
    String collectionName,
    Long embeddingCount,
    String message
) {}
