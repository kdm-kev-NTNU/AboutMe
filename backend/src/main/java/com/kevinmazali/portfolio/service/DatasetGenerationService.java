package com.kevinmazali.portfolio.service;

import com.kevinmazali.portfolio.model.chat.SupportedChatModel;
import com.kevinmazali.portfolio.model.experiment.DatasetGeneration;
import com.kevinmazali.portfolio.model.experiment.DatasetGenerationStatus;
import com.kevinmazali.portfolio.model.experiment.DatasetGenerationStatusResponse;
import com.kevinmazali.portfolio.model.experiment.GenerateDatasetRequest;
import com.kevinmazali.portfolio.repository.DatasetGenerationRepository;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class DatasetGenerationService {

  private final DatasetGenerationRepository datasetGenerationRepository;
  private final DatasetGenerationAsyncRunner datasetGenerationAsyncRunner;
  private final ChatModelCatalog chatModelCatalog;

  public DatasetGenerationService(
      DatasetGenerationRepository datasetGenerationRepository,
      DatasetGenerationAsyncRunner datasetGenerationAsyncRunner,
      ChatModelCatalog chatModelCatalog) {
    this.datasetGenerationRepository = datasetGenerationRepository;
    this.datasetGenerationAsyncRunner = datasetGenerationAsyncRunner;
    this.chatModelCatalog = chatModelCatalog;
  }

  /**
   * Persists a generation job and kicks off async QRA work. Not transactional as a whole so the job row is
   * committed before the async runner starts.
   */
  public long startGeneration(GenerateDatasetRequest request) {
    if (!StringUtils.hasText(request.name())) {
      throw new IllegalArgumentException("name is required");
    }
    SupportedChatModel model =
        SupportedChatModel.fromModelId(request.model())
            .orElseThrow(() -> new IllegalArgumentException("Unknown model: " + request.model()));
    if (!chatModelCatalog.isModelConfigured(model)) {
      throw new IllegalArgumentException("Model is not configured (API key / chat enabled).");
    }
    int qpc = request.questionsPerChunk() == null ? 1 : Math.max(1, request.questionsPerChunk());
    String docFilter = StringUtils.hasText(request.documentId()) ? request.documentId().trim() : null;

    DatasetGeneration entity =
        DatasetGeneration.builder()
            .name(request.name().trim())
            .description(request.description() != null ? request.description() : "")
            .documentIdFilter(docFilter)
            .model(model.modelId())
            .questionsPerChunk(qpc)
            .maxQuestions(request.maxQuestions())
            .seed(request.seed())
            .status(DatasetGenerationStatus.RUNNING)
            .questionsGenerated(0)
            .build();
    entity = datasetGenerationRepository.save(entity);
    datasetGenerationAsyncRunner.executeGeneration(entity.getId());
    return entity.getId();
  }

  public Optional<DatasetGenerationStatusResponse> getGenerationStatus(long id) {
    return datasetGenerationRepository
        .findById(id)
        .map(
            g ->
                new DatasetGenerationStatusResponse(
                    g.getId(),
                    g.getStatus(),
                    g.getQuestionsGenerated(),
                    g.getResultDatasetId() != null ? Long.toString(g.getResultDatasetId()) : null,
                    g.getErrorMessage(),
                    g.getCreatedAt() != null ? g.getCreatedAt().toString() : null,
                    g.getCompletedAt() != null ? g.getCompletedAt().toString() : null));
  }
}
