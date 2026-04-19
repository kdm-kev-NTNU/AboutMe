package com.kevinmazali.portfolio.model;

import java.util.Map;

/**
 * One vector-store chunk row returned for admin inspection.
 */
public record ChunkItem(
    String id,
    String documentTitle,
    Integer chunkIndex,
    String text,
    Map<String, Object> metadata
) {}
