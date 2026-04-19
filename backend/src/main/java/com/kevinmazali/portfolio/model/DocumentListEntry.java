package com.kevinmazali.portfolio.model;

/**
 * Aggregated view of one logical document (all chunks sharing the same {@code document_id}).
 */
public record DocumentListEntry(
    String documentId,
    String filename,
    int chunkCount,
    String lastIngestedAt
) {}
