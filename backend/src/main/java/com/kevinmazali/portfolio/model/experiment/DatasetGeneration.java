package com.kevinmazali.portfolio.model.experiment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
    name = "dataset_generations",
    indexes = @Index(name = "idx_dataset_gen_created", columnList = "created_at"))
public class DatasetGeneration {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 512)
  private String name;

  @Column(columnDefinition = "TEXT")
  private String description;

  @Column(name = "document_id_filter", length = 128)
  private String documentIdFilter;

  @Column(nullable = false, length = 128)
  private String model;

  @Column(name = "questions_per_chunk", nullable = false)
  @Builder.Default
  private int questionsPerChunk = 1;

  @Column(name = "max_questions")
  private Integer maxQuestions;

  @Column(name = "seed")
  private Integer seed;

  @Column(nullable = false, length = 32)
  @Builder.Default
  private String status = DatasetGenerationStatus.RUNNING;

  @Column(name = "questions_generated")
  private Integer questionsGenerated;

  @Column(name = "result_dataset_id")
  private Long resultDatasetId;

  @Column(name = "error_message", columnDefinition = "TEXT")
  private String errorMessage;

  @Column(name = "created_at", nullable = false)
  @Builder.Default
  private OffsetDateTime createdAt = OffsetDateTime.now();

  @Column(name = "completed_at")
  private OffsetDateTime completedAt;
}
