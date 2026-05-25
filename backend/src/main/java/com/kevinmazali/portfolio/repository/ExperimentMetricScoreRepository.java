package com.kevinmazali.portfolio.repository;

import com.kevinmazali.portfolio.model.experiment.ExperimentMetricScore;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExperimentMetricScoreRepository extends JpaRepository<ExperimentMetricScore, Long> {

  List<ExperimentMetricScore> findByExperimentResultId(Long experimentResultId);

  @Query("""
      SELECT ems.metric, AVG(ems.score)
      FROM ExperimentMetricScore ems
      WHERE ems.experimentResult.experimentRun.id = :runId
        AND ems.score IS NOT NULL
      GROUP BY ems.metric
      """)
  List<Object[]> averageScoresByMetricForRun(@Param("runId") Long runId);
}
