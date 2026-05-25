package com.kevinmazali.portfolio.model.experiment;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "experiment_runs", indexes = {
    @Index(name = "idx_er_created", columnList = "createdAt")
})
public class ExperimentRun {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 256)
  private String name;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "eval_dataset_id")
  private EvalDatasetEntity evalDataset;

  @Column(nullable = false, length = 128)
  private String generatorModel;

  @Column(nullable = false, length = 128)
  private String evaluatorModel;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private ExperimentRunStatus status;

  @Column(nullable = false)
  @Builder.Default
  private Integer totalExamples = 0;

  @Column(columnDefinition = "TEXT")
  private String errorMessage;

  @Column(nullable = false)
  @Builder.Default
  private OffsetDateTime createdAt = OffsetDateTime.now();

  @Column
  private OffsetDateTime completedAt;

  @OneToMany(mappedBy = "experimentRun", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
  @Builder.Default
  private List<ExperimentResult> results = new ArrayList<>();

  public Long getEvalDatasetId() {
    return evalDataset != null ? evalDataset.getId() : null;
  }
}
