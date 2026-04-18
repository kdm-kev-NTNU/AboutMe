package com.kevinmazali.portfolio.model;

import java.util.List;

/**
 * Active collection name plus embedding count and all collections in the Chroma database.
 */
public record ChromaCollectionsResponse(
    String activeCollectionName,
    Long activeCollectionEmbeddingCount,
    List<ChromaCollectionSummary> collections
) {}
