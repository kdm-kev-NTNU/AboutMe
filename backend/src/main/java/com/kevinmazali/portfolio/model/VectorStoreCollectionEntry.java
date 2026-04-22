package com.kevinmazali.portfolio.model;

/** One logical collection / table entry for admin vector-store summary (replaces Chroma collection list). */
public record VectorStoreCollectionEntry(String id, String name) {}
