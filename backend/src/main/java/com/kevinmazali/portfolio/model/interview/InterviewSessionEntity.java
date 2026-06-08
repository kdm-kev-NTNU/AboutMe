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
@Table(name = "interview_sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterviewSessionEntity {

  @Id
  @Column(length = 64)
  private String id;

  @Column(name = "document_id", nullable = false, length = 64)
  private String documentId;

  @Column(nullable = false, length = 8)
  private String language;

  @Column(nullable = false, length = 32)
  private String status;

  @Column(length = 32)
  private String voice;

  @Column(name = "started_at", nullable = false)
  private Instant startedAt;

  @Column(name = "ended_at")
  private Instant endedAt;

  @Column(name = "deleted_at")
  private Instant deletedAt;
}
