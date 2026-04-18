package com.kevinmazali.portfolio.model;

/**
 * Basic metadata for a ChromaDB collection visible to the admin tools API.
 */
public record ChromaCollectionSummary(
    String id,
    String name
) {}
