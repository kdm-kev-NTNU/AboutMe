package com.kevinmazali.portfolio.model.interview;

import java.time.Instant;

public record InterviewDocumentResponse(
    String id,
    String originalFilename,
    String mimeType,
    int charCount,
    String createdBy,
    Instant createdAt) {}
