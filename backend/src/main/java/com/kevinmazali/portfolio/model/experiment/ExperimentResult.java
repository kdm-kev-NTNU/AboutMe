package com.kevinmazali.portfolio.model.experiment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * One row per dataset example: RAG output and per-metric judge scores.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "experiment_results", indexes = {
    @Index(name = "idx_expres_run", columnList = "experiment_run_id")
})
public class ExperimentResult {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "experiment_run_id", nullable = false)
  private ExperimentRun experimentRun;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String question;

  @Column(columnDefinition = "TEXT")
  private String referenceAnswer;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String ragResponse;

  @Column(columnDefinition = "TEXT")
  private String documents;

  @Column
  private Double faithfulness;

  @Column
  private Double relevance;

  @Column
  private Double correctness;

  @Column
  private Double conciseness;

  @Column(columnDefinition = "TEXT")
  private String faithfulnessExplanation;

  @Column(columnDefinition = "TEXT")
  private String relevanceExplanation;

  @Column(columnDefinition = "TEXT")
  private String correctnessExplanation;

  @Column(columnDefinition = "TEXT")
  private String concisenessExplanation;

  @Column
  private Double languageConsistency;

  @Column(columnDefinition = "TEXT")
  private String languageConsistencyExplanation;
}
