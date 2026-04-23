package com.kevinmazali.portfolio.service;

import com.kevinmazali.portfolio.model.experiment.CreateEvalDatasetRequest;
import com.kevinmazali.portfolio.model.experiment.EvalDatasetEntity;
import com.kevinmazali.portfolio.model.experiment.EvalDatasetExampleEntity;
import com.kevinmazali.portfolio.model.experiment.EvalDatasetExampleRow;
import com.kevinmazali.portfolio.model.experiment.EvalDatasetSummary;
import com.kevinmazali.portfolio.repository.EvalDatasetExampleRepository;
import com.kevinmazali.portfolio.repository.EvalDatasetRepository;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class EvalDatasetService {

  private final EvalDatasetRepository datasetRepository;
  private final EvalDatasetExampleRepository exampleRepository;

  public EvalDatasetService(
      EvalDatasetRepository datasetRepository, EvalDatasetExampleRepository exampleRepository) {
    this.datasetRepository = datasetRepository;
    this.exampleRepository = exampleRepository;
  }

  public List<EvalDatasetSummary> listDatasets() {
    List<EvalDatasetSummary> out = new ArrayList<>();
    for (EvalDatasetEntity d : datasetRepository.findAllByOrderByCreatedAtDesc()) {
      int n = (int) Math.min(Integer.MAX_VALUE, exampleRepository.countByDataset_Id(d.getId()));
      out.add(new EvalDatasetSummary(Long.toString(d.getId()), d.getName(), n));
    }
    return out;
  }

  @Transactional
  public void deleteDataset(String datasetId) {
    long id = parseId(datasetId);
    datasetRepository.deleteById(id);
  }

  @Transactional(readOnly = true)
  public List<EvalDatasetExampleRow> getExamples(String datasetId) {
    long id = parseId(datasetId);
    return exampleRepository.findByDataset_IdOrderByIdAsc(id).stream()
        .map(e -> new EvalDatasetExampleRow(e.getQuestion(), e.getReferenceText() != null ? e.getReferenceText() : ""))
        .toList();
  }

  @Transactional
  public EvalDatasetSummary createDataset(CreateEvalDatasetRequest request) {
    if (request.examples() == null || request.examples().isEmpty()) {
      throw new IllegalArgumentException("examples required");
    }
    EvalDatasetEntity ds = new EvalDatasetEntity();
    ds.setName(request.name().trim());
    ds.setDescription(request.description() != null ? request.description() : "");
    for (CreateEvalDatasetRequest.DatasetExampleInput ex : request.examples()) {
      if (!StringUtils.hasText(ex.question())) {
        throw new IllegalArgumentException("Each example needs a question");
      }
      EvalDatasetExampleEntity row = new EvalDatasetExampleEntity();
      row.setDataset(ds);
      row.setQuestion(ex.question().trim());
      row.setReferenceText(ex.referenceText() != null ? ex.referenceText() : "");
      ds.getExamples().add(row);
    }
    ds = datasetRepository.save(ds);
    return new EvalDatasetSummary(Long.toString(ds.getId()), ds.getName(), ds.getExamples().size());
  }

  private static long parseId(String datasetId) {
    if (!StringUtils.hasText(datasetId)) {
      throw new IllegalArgumentException("dataset id required");
    }
    try {
      return Long.parseLong(datasetId.trim());
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("Invalid dataset id: " + datasetId);
    }
  }
}
