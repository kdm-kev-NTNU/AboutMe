package com.kevinmazali.portfolio.model.interview;

import java.time.Instant;

public record InterviewTranscriptResponse(
    String id,
    String sessionId,
    String rawText,
    String cleanedText,
    String cleanStatus,
    String ingestedDocumentId,
    Instant createdAt,
    Instant cleanedAt) {}
