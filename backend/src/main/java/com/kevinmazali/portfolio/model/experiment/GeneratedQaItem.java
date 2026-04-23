package com.kevinmazali.portfolio.model.experiment;

/**
 * One synthetic Q/A row before persistence (QRA pipeline).
 */
public record GeneratedQaItem(
    String question,
    String answer,
    String sourceDocument,
    String documentId,
    Integer chunkIndex) {}
