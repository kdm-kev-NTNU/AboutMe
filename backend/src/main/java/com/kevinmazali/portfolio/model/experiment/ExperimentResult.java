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

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "experiment_results", indexes = {
    @Index(name = "idx_expres_run", columnList = "experiment_run_id"),
    @Index(name = "idx_expres_eval_example", columnList = "eval_example_id")
})
public class ExperimentResult {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "experiment_run_id", nullable = false)
  private ExperimentRun experimentRun;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "eval_example_id")
  private EvalDatasetExampleEntity evalExample;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String question;

  @Column(columnDefinition = "TEXT")
  private String referenceAnswer;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String ragResponse;

  @Column(name = "retrieved_context", columnDefinition = "TEXT")
  private String retrievedContext;
}
