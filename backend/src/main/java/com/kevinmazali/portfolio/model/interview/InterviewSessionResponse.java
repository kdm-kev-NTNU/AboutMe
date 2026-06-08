package com.kevinmazali.portfolio.model.interview;

import java.time.Instant;
import java.util.List;

public record InterviewSessionResponse(
    String id,
    String documentId,
    String language,
    String status,
    String voice,
    Instant startedAt,
    Instant endedAt,
    String transcriptId,
    String cleanStatus,
    String ingestedDocumentId,
    List<InterviewTurnDto> turns) {}
