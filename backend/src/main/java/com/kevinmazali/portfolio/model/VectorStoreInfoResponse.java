package com.kevinmazali.portfolio.model;

import java.util.List;

/** Active vector table plus embedding count (same JSON shape as the former Chroma collections response). */
public record VectorStoreInfoResponse(
    String activeCollectionName,
    Long activeCollectionEmbeddingCount,
    List<VectorStoreCollectionEntry> collections
) {}
