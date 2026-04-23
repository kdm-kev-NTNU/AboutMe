package com.kevinmazali.portfolio.repository;

import com.kevinmazali.portfolio.model.experiment.EvalDatasetExampleEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EvalDatasetExampleRepository extends JpaRepository<EvalDatasetExampleEntity, Long> {

  List<EvalDatasetExampleEntity> findByDataset_IdOrderByIdAsc(long datasetId);

  long countByDataset_Id(long datasetId);
}
