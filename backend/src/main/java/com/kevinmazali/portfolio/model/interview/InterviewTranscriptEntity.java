package com.kevinmazali.portfolio.model.interview;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "interview_transcripts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterviewTranscriptEntity {

  @Id
  @Column(length = 64)
  private String id;

  @Column(name = "session_id", nullable = false, length = 64)
  private String sessionId;

  @Column(name = "raw_text", columnDefinition = "TEXT")
  private String rawText;

  @Column(name = "cleaned_text", columnDefinition = "TEXT")
  private String cleanedText;

  @Column(name = "clean_status", nullable = false, length = 32)
  private String cleanStatus;

  @Column(name = "ingested_document_id", length = 64)
  private String ingestedDocumentId;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "cleaned_at")
  private Instant cleanedAt;
}
