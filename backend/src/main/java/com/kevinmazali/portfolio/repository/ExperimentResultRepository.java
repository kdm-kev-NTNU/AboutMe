package com.kevinmazali.portfolio.repository;

import com.kevinmazali.portfolio.model.experiment.ExperimentResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExperimentResultRepository extends JpaRepository<ExperimentResult, Long> {

  List<ExperimentResult> findByExperimentRunIdOrderByIdAsc(Long experimentRunId);
}
