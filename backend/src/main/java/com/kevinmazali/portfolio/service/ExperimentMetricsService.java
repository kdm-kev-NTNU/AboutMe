package com.kevinmazali.portfolio.service;

import com.kevinmazali.portfolio.model.experiment.EvaluationScore;
import com.kevinmazali.portfolio.model.experiment.ExperimentMetricScore;
import com.kevinmazali.portfolio.model.experiment.ExperimentResult;
import com.kevinmazali.portfolio.repository.ExperimentMetricScoreRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ExperimentMetricsService {

  private final ExperimentMetricScoreRepository metricScoreRepository;

  @Transactional
  public void saveScores(ExperimentResult result, Map<String, EvaluationScore> scores) {
    for (var entry : scores.entrySet()) {
      EvaluationScore eval = entry.getValue();
      if (eval == null || Double.isNaN(eval.score())) {
        continue;
      }
      metricScoreRepository.save(
          ExperimentMetricScore.builder()
              .experimentResult(result)
              .metric(entry.getKey())
              .score(eval.score())
              .explanation(eval.explanation())
              .build());
    }
  }

  @Transactional(readOnly = true)
  public Map<String, Double> meanScoresForRun(long runId) {
    List<Object[]> rows = metricScoreRepository.averageScoresByMetricForRun(runId);
    Map<String, Double> means = new HashMap<>();
    for (Object[] row : rows) {
      means.put((String) row[0], (Double) row[1]);
    }
    return means;
  }

  @Transactional(readOnly = true)
  public List<ExperimentMetricScore> scoresForResult(long resultId) {
    return metricScoreRepository.findByExperimentResultId(resultId);
  }
}
