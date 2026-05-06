package com.kevinmazali.portfolio.model;

import java.time.Instant;
import java.util.List;

/**
 * Full export of chunk rows from pgvector for backup / offline tooling (matches {@link ChunkListResponse} items).
 */
public record ChunkExportResponse(
    Instant exportedAt,
    String collectionName,
    String documentId,
    long totalChunks,
    List<ChunkItem> chunks
) {}
