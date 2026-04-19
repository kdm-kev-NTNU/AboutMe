package com.kevinmazali.portfolio.repository;

import com.kevinmazali.portfolio.model.experiment.ExperimentRun;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExperimentRunRepository extends JpaRepository<ExperimentRun, Long> {

  List<ExperimentRun> findAllByOrderByCreatedAtDesc();
}
