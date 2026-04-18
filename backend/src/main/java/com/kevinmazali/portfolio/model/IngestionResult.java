package com.kevinmazali.portfolio.model;

/**
 * Result of a single document ingestion run into the vector store.
 */
public record IngestionResult(
    String documentId,
    String filename,
    int chunksIngested,
    boolean skipped,
    String message
) {}
