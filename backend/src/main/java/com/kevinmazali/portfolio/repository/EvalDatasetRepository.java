package com.kevinmazali.portfolio.repository;

import com.kevinmazali.portfolio.model.experiment.EvalDatasetEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EvalDatasetRepository extends JpaRepository<EvalDatasetEntity, Long> {

  List<EvalDatasetEntity> findAllByOrderByCreatedAtDesc();
}
