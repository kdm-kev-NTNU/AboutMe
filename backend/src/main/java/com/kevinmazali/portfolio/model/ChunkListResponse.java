package com.kevinmazali.portfolio.model;

import java.util.List;

/**
 * Paginated chunk listing from the active pgvector {@code vector_store} table.
 */
public record ChunkListResponse(
    String collectionName,
    long total,
    long totalMatching,
    int limit,
    int offset,
    List<ChunkItem> chunks
) {}
