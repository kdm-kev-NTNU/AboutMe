package com.kevinmazali.portfolio.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import tools.jackson.databind.ObjectMapper;
import com.kevinmazali.portfolio.config.AiBudgetProperties;
import com.kevinmazali.portfolio.config.AiLimitsProperties;
import com.kevinmazali.portfolio.exception.AiCircuitOpenException;
import com.kevinmazali.portfolio.exception.BudgetExceededException;
import com.kevinmazali.portfolio.model.ChunkItem;
import com.kevinmazali.portfolio.model.ChunkListResponse;
import com.kevinmazali.portfolio.model.experiment.CreateEvalDatasetRequest;
import com.kevinmazali.portfolio.model.experiment.DatasetGeneration;
import com.kevinmazali.portfolio.model.experiment.DatasetGenerationStatus;
import com.kevinmazali.portfolio.model.experiment.EvalDatasetSummary;
import com.kevinmazali.portfolio.repository.DatasetGenerationRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.beans.factory.ObjectProvider;

@ExtendWith(MockitoExtension.class)
class DatasetGenerationAsyncRunnerTest {

  private static final String OPENAI_MODEL = "gpt-5.4-mini";
  private static final String ANTHROPIC_MODEL = "claude-haiku-4-5-20251001";

  @Mock private DatasetGenerationRepository datasetGenerationRepository;
  @Mock private DocumentIngestionService documentIngestionService;
  @Mock private EvalDatasetService evalDatasetService;
  @Mock private OpenAiChatModel openAiChatModel;
  @Mock private AnthropicChatModel anthropicChatModel;
  @SuppressWarnings("unchecked")
  private final ObjectProvider<OpenAiChatModel> openAiChatModelProvider =
      mock(ObjectProvider.class);

  @SuppressWarnings("unchecked")
  private final ObjectProvider<AnthropicChatModel> anthropicChatModelProvider =
      mock(ObjectProvider.class);
  @Mock private AiCircuitBreaker aiCircuitBreaker;
  @Mock private AiBudgetService aiBudgetService;

  private DatasetGenerationAsyncRunner runner;

  @BeforeEach
  void setUp() {
    lenient().doReturn(openAiChatModel).when(openAiChatModelProvider).getIfAvailable();
    lenient().doReturn(anthropicChatModel).when(anthropicChatModelProvider).getIfAvailable();
    lenient().doNothing().when(aiCircuitBreaker).assertClosed();
    lenient()
        .doNothing()
        .when(aiBudgetService)
        .recordUsage(
            anyString(),
            anyString(),
            anyInt(),
            anyInt(),
            anyBoolean(),
            anyDouble(),
            anyString(),
            any());

    runner =
        new DatasetGenerationAsyncRunner(
            datasetGenerationRepository,
            documentIngestionService,
            evalDatasetService,
            openAiChatModelProvider,
            anthropicChatModelProvider,
            new ObjectMapper(),
            new AiLimitsProperties(),
            new AiBudgetProperties(),
            aiCircuitBreaker,
            aiBudgetService);
  }

  @Test
  void noChunksFailsWithIngestMessage() {
    DatasetGeneration gen = baseGen(1L, OPENAI_MODEL);
    when(datasetGenerationRepository.findById(1L)).thenReturn(Optional.of(gen));
    when(documentIngestionService.getChunks(null, 200, 0))
        .thenReturn(new ChunkListResponse("c", 0, 0, 200, 0, List.of()));

    runner.executeGeneration(1L);

    ArgumentCaptor<DatasetGeneration> cap = ArgumentCaptor.forClass(DatasetGeneration.class);
    verify(datasetGenerationRepository, times(1)).save(cap.capture());
    assertEquals(DatasetGenerationStatus.FAILED, cap.getValue().getStatus());
    assertNotNull(cap.getValue().getErrorMessage());
    assertNotNull(cap.getValue().getCompletedAt());
    verify(evalDatasetService, never()).createDataset(any(CreateEvalDatasetRequest.class));
  }

  @Test
  void completedRunCreatesDatasetAndPersistsResult() {
    DatasetGeneration gen = baseGen(2L, OPENAI_MODEL);
    gen.setMaxQuestions(1);
    when(datasetGenerationRepository.findById(2L)).thenReturn(Optional.of(gen));

    ChunkItem chunk =
        new ChunkItem(
            "id1",
            "readme.md",
            0,
            "Oslo is the capital of Norway.",
            Map.of("document_id", "doc-1"));
    when(documentIngestionService.getChunks(null, 200, 0))
        .thenReturn(new ChunkListResponse("c", 1, 1, 200, 0, List.of(chunk)));

    stubOpenAiReturns(
        "[{\"question\":\"What is the capital of Norway?\",\"answer\":\"The capital is Oslo, a coastal city.\"}]");

    when(evalDatasetService.createDataset(any(CreateEvalDatasetRequest.class)))
        .thenReturn(new EvalDatasetSummary("42", "My set", 1));

    runner.executeGeneration(2L);

    ArgumentCaptor<DatasetGeneration> cap = ArgumentCaptor.forClass(DatasetGeneration.class);
    verify(datasetGenerationRepository, times(2)).save(cap.capture());
    DatasetGeneration last = cap.getAllValues().get(cap.getAllValues().size() - 1);
    assertEquals(DatasetGenerationStatus.COMPLETED, last.getStatus());
    assertEquals(42L, last.getResultDatasetId());
    assertEquals(1, last.getQuestionsGenerated());
    assertNull(last.getErrorMessage());
    verify(evalDatasetService).createDataset(any(CreateEvalDatasetRequest.class));
  }

  @Test
  void anthropicModelUsesAnthropicClient() {
    DatasetGeneration gen = baseGen(3L, ANTHROPIC_MODEL);
    gen.setMaxQuestions(1);
    when(datasetGenerationRepository.findById(3L)).thenReturn(Optional.of(gen));

    ChunkItem chunk =
        new ChunkItem("id1", "x.pdf", 1, "Some context about databases.", Map.of("document_id", "d2"));
    when(documentIngestionService.getChunks(null, 200, 0))
        .thenReturn(new ChunkListResponse("c", 1, 1, 200, 0, List.of(chunk)));

    ChatResponse r = mock(ChatResponse.class, RETURNS_DEEP_STUBS);
    when(r.getResult().getOutput().getText())
        .thenReturn(
            "{\"question\":\"How do indexes speed up queries?\",\"answer\":\"Indexes reduce scanned rows by ordering keys for fast lookup.\"}");
    when(anthropicChatModel.call(any(Prompt.class))).thenReturn(r);

    when(evalDatasetService.createDataset(any(CreateEvalDatasetRequest.class)))
        .thenReturn(new EvalDatasetSummary("9", "Anthropic set", 1));

    runner.executeGeneration(3L);

    verify(anthropicChatModel).call(any(Prompt.class));
    verify(openAiChatModel, never()).call(any(Prompt.class));
    ArgumentCaptor<DatasetGeneration> cap = ArgumentCaptor.forClass(DatasetGeneration.class);
    verify(datasetGenerationRepository, times(2)).save(cap.capture());
    assertEquals(DatasetGenerationStatus.COMPLETED, cap.getValue().getStatus());
    assertEquals(9L, cap.getValue().getResultDatasetId());
  }

  @Test
  void zeroTargetFailsEarly() {
    DatasetGeneration gen = baseGen(4L, OPENAI_MODEL);
    gen.setMaxQuestions(0);
    when(datasetGenerationRepository.findById(4L)).thenReturn(Optional.of(gen));
    ChunkItem chunk =
        new ChunkItem("id1", "f.txt", 0, "hello", Map.of("document_id", "d"));
    when(documentIngestionService.getChunks(null, 200, 0))
        .thenReturn(new ChunkListResponse("c", 1, 1, 200, 0, List.of(chunk)));

    runner.executeGeneration(4L);

    ArgumentCaptor<DatasetGeneration> cap = ArgumentCaptor.forClass(DatasetGeneration.class);
    verify(datasetGenerationRepository).save(cap.capture());
    assertEquals(DatasetGenerationStatus.FAILED, cap.getValue().getStatus());
    assertNotNull(cap.getValue().getErrorMessage());
    verify(openAiChatModel, never()).call(any(Prompt.class));
  }

  @Test
  void budgetExceededMarksFailed() {
    DatasetGeneration gen = baseGen(5L, OPENAI_MODEL);
    gen.setMaxQuestions(1);
    when(datasetGenerationRepository.findById(5L)).thenReturn(Optional.of(gen));
    ChunkItem chunk =
        new ChunkItem("id1", "f.txt", 0, "content for budget test", Map.of("document_id", "d"));
    when(documentIngestionService.getChunks(null, 200, 0))
        .thenReturn(new ChunkListResponse("c", 1, 1, 200, 0, List.of(chunk)));
    when(openAiChatModel.call(any(Prompt.class))).thenThrow(new BudgetExceededException("cap"));

    runner.executeGeneration(5L);

    ArgumentCaptor<DatasetGeneration> cap = ArgumentCaptor.forClass(DatasetGeneration.class);
    verify(datasetGenerationRepository).save(cap.capture());
    assertEquals(DatasetGenerationStatus.FAILED, cap.getValue().getStatus());
    assertEquals("cap", cap.getValue().getErrorMessage());
  }

  @Test
  void circuitOpenMarksFailed() {
    DatasetGeneration gen = baseGen(6L, OPENAI_MODEL);
    gen.setMaxQuestions(1);
    when(datasetGenerationRepository.findById(6L)).thenReturn(Optional.of(gen));
    ChunkItem chunk =
        new ChunkItem("id1", "f.txt", 0, "content", Map.of("document_id", "d"));
    when(documentIngestionService.getChunks(null, 200, 0))
        .thenReturn(new ChunkListResponse("c", 1, 1, 200, 0, List.of(chunk)));
    org.mockito.Mockito.doThrow(new AiCircuitOpenException("open"))
        .when(aiCircuitBreaker)
        .assertClosed();

    runner.executeGeneration(6L);

    ArgumentCaptor<DatasetGeneration> cap = ArgumentCaptor.forClass(DatasetGeneration.class);
    verify(datasetGenerationRepository).save(cap.capture());
    assertEquals(DatasetGenerationStatus.FAILED, cap.getValue().getStatus());
    assertEquals("open", cap.getValue().getErrorMessage());
  }

  @Test
  void unknownModelFailsRun() {
    DatasetGeneration gen = baseGen(7L, "not-a-real-model");
    when(datasetGenerationRepository.findById(7L)).thenReturn(Optional.of(gen));
    ChunkItem chunk =
        new ChunkItem("id1", "f.txt", 0, "content", Map.of("document_id", "d"));
    when(documentIngestionService.getChunks(null, 200, 0))
        .thenReturn(new ChunkListResponse("c", 1, 1, 200, 0, List.of(chunk)));

    runner.executeGeneration(7L);

    ArgumentCaptor<DatasetGeneration> cap = ArgumentCaptor.forClass(DatasetGeneration.class);
    verify(datasetGenerationRepository).save(cap.capture());
    assertEquals(DatasetGenerationStatus.FAILED, cap.getValue().getStatus());
    assertNotNull(cap.getValue().getErrorMessage());
  }

  @Test
  void allLowQualityOutputsFailsSanitization() {
    DatasetGeneration gen = baseGen(8L, OPENAI_MODEL);
    gen.setMaxQuestions(1);
    when(datasetGenerationRepository.findById(8L)).thenReturn(Optional.of(gen));
    ChunkItem chunk =
        new ChunkItem("id1", "f.txt", 0, "content", Map.of("document_id", "d"));
    when(documentIngestionService.getChunks(null, 200, 0))
        .thenReturn(new ChunkListResponse("c", 1, 1, 200, 0, List.of(chunk)));
    stubOpenAiReturns("{\"question\":\"short\",\"answer\":\"no\"}");

    runner.executeGeneration(8L);

    ArgumentCaptor<DatasetGeneration> cap = ArgumentCaptor.forClass(DatasetGeneration.class);
    verify(datasetGenerationRepository, atLeast(1)).save(cap.capture());
    List<DatasetGeneration> saves = cap.getAllValues();
    DatasetGeneration last = saves.get(saves.size() - 1);
    assertEquals(DatasetGenerationStatus.FAILED, last.getStatus());
    assertNotNull(last.getErrorMessage());
    verify(evalDatasetService, never()).createDataset(any(CreateEvalDatasetRequest.class));
  }

  private static DatasetGeneration baseGen(long id, String model) {
    return DatasetGeneration.builder()
        .id(id)
        .name("eval-set")
        .description("desc")
        .documentIdFilter(null)
        .model(model)
        .questionsPerChunk(1)
        .maxQuestions(null)
        .seed(1)
        .status(DatasetGenerationStatus.RUNNING)
        .build();
  }

  private void stubOpenAiReturns(String text) {
    ChatResponse r = mock(ChatResponse.class, RETURNS_DEEP_STUBS);
    when(r.getResult().getOutput().getText()).thenReturn(text);
    when(openAiChatModel.call(any(Prompt.class))).thenReturn(r);
  }
}
