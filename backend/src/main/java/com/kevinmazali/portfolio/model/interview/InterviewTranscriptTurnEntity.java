package com.kevinmazali.portfolio.model.interview;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "interview_transcript_turns")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterviewTranscriptTurnEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "session_id", nullable = false, length = 64)
  private String sessionId;

  @Column(nullable = false, length = 32)
  private String role;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String text;

  @Column(name = "sequence_no", nullable = false)
  private int sequenceNo;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;
}
