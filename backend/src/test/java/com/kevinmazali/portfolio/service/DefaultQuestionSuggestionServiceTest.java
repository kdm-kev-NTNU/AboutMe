package com.kevinmazali.portfolio.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kevinmazali.portfolio.config.AiBudgetProperties;
import com.kevinmazali.portfolio.config.AiLimitsProperties;
import com.kevinmazali.portfolio.model.ChunkItem;
import com.kevinmazali.portfolio.model.ChunkListResponse;
import com.kevinmazali.portfolio.model.DefaultQuestionSuggestionRequest;
import com.kevinmazali.portfolio.model.DefaultQuestionSuggestionResponse;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.beans.factory.ObjectProvider;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class DefaultQuestionSuggestionServiceTest {

  private static final String MODEL_ID = "gpt-5.4-nano";

  @Mock
  private DocumentIngestionService documentIngestionService;

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Mock
  private ObjectProvider<OpenAiChatModel> openAiChatModelProvider;

  @Mock
  private ObjectProvider<AnthropicChatModel> anthropicChatModelProvider;

  private final AiLimitsProperties aiLimitsProperties = new AiLimitsProperties();
  private final AiBudgetProperties aiBudgetProperties = new AiBudgetProperties();

  @Mock
  private AiCircuitBreaker aiCircuitBreaker;

  @Mock
  private AiBudgetService aiBudgetService;

  @Mock
  private ChatModelCatalog chatModelCatalog;

  @Mock
  private OpenAiChatModel openAiChatModel;

  private DefaultQuestionSuggestionService service;

  @BeforeEach
  void setUp() {
    lenient().doNothing().when(aiCircuitBreaker).assertClosed();
    lenient()
        .doNothing()
        .when(aiBudgetService)
        .assertWithinBudget(anyString(), anyBoolean());
    lenient()
        .doNothing()
        .when(aiBudgetService)
        .recordUsage(
            anyString(),
            anyString(),
            anyInt(),
            anyInt(),
            anyBoolean(),
            any(),
            any(),
            any());
    lenient().when(openAiChatModelProvider.getIfAvailable()).thenReturn(openAiChatModel);
    lenient().when(chatModelCatalog.isModelConfigured(any())).thenReturn(true);

    service =
        new DefaultQuestionSuggestionService(
            documentIngestionService,
            objectMapper,
            openAiChatModelProvider,
            anthropicChatModelProvider,
            aiLimitsProperties,
            aiBudgetProperties,
            aiCircuitBreaker,
            aiBudgetService,
            chatModelCatalog);
  }

  @Test
  void suggest_nullBody() {
    assertThrows(IllegalArgumentException.class, () -> service.suggest(null));
  }

  @Test
  void suggest_invalidSource() {
    var req =
        new DefaultQuestionSuggestionRequest("bogus", null, null, MODEL_ID, 12, "Norwegian");
    assertThrows(IllegalArgumentException.class, () -> service.suggest(req));
  }

  @Test
  void suggest_uploadedJsonMissingChunksJson() {
    var req =
        new DefaultQuestionSuggestionRequest(
            DefaultQuestionSuggestionService.SOURCE_UPLOADED,
            null,
            "   ",
            MODEL_ID,
            12,
            "Norwegian");
    assertThrows(IllegalArgumentException.class, () -> service.suggest(req));
  }

  @Test
  void suggest_chunksJsonExceedsMaxSize() {
    String huge = "x".repeat(2_000_001);
    var req =
        new DefaultQuestionSuggestionRequest(
            DefaultQuestionSuggestionService.SOURCE_UPLOADED,
            null,
            huge,
            MODEL_ID,
            12,
            "Norwegian");
    assertThrows(IllegalArgumentException.class, () -> service.suggest(req));
  }

  @Test
  void suggest_currentChunksEmpty() {
    when(documentIngestionService.getChunks(null, 200, 0))
        .thenReturn(new ChunkListResponse("c", 0, 0, 200, 0, List.of()));

    var req =
        new DefaultQuestionSuggestionRequest(
            DefaultQuestionSuggestionService.SOURCE_CURRENT,
            null,
            null,
            MODEL_ID,
            12,
            "Norwegian");
    assertThrows(IllegalArgumentException.class, () -> service.suggest(req));
  }

  @Test
  void suggest_unknownModel() {
    when(documentIngestionService.getChunks(null, 200, 0))
        .thenReturn(
            new ChunkListResponse(
                "c",
                1,
                1,
                200,
                0,
                List.of(new ChunkItem("id1", "t", 0, "hello", Map.of()))));

    var req =
        new DefaultQuestionSuggestionRequest(
            DefaultQuestionSuggestionService.SOURCE_CURRENT,
            null,
            null,
            "not-a-real-model",
            12,
            "Norwegian");
    assertThrows(IllegalArgumentException.class, () -> service.suggest(req));
  }

  @Test
  void suggest_modelNotConfigured() {
    when(chatModelCatalog.isModelConfigured(any())).thenReturn(false);
    when(documentIngestionService.getChunks(null, 200, 0))
        .thenReturn(
            new ChunkListResponse(
                "c",
                1,
                1,
                200,
                0,
                List.of(new ChunkItem("id1", "t", 0, "hello corpus", Map.of()))));

    var req =
        new DefaultQuestionSuggestionRequest(
            DefaultQuestionSuggestionService.SOURCE_CURRENT,
            null,
            null,
            MODEL_ID,
            12,
            "Norwegian");
    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.suggest(req));
    assertTrue(ex.getMessage().contains("not configured"));
  }

  @Test
  void suggest_missingModelField() {
    when(documentIngestionService.getChunks(null, 200, 0))
        .thenReturn(
            new ChunkListResponse(
                "c",
                1,
                1,
                200,
                0,
                List.of(new ChunkItem("id1", "t", 0, "hello", Map.of()))));

    var req =
        new DefaultQuestionSuggestionRequest(
            DefaultQuestionSuggestionService.SOURCE_CURRENT,
            null,
            null,
            "  ",
            12,
            "Norwegian");
    assertThrows(IllegalArgumentException.class, () -> service.suggest(req));
  }

  @Test
  void parseChunksFromExportJson_valid() {
    String json =
        "{\"chunks\":[{\"id\":\"c1\",\"documentTitle\":\"D\",\"chunkIndex\":0,\"text\":\"x\",\"metadata\":{}}]}";
    List<ChunkItem> items = service.parseChunksFromExportJson(json);
    assertEquals(1, items.size());
    assertEquals("c1", items.getFirst().id());
  }

  @Test
  void parseChunksFromExportJson_missingChunksArray() {
    String json = "{\"other\":[]}";
    assertThrows(IllegalArgumentException.class, () -> service.parseChunksFromExportJson(json));
  }

  @Test
  void parseChunksFromExportJson_invalidJson() {
    assertThrows(IllegalArgumentException.class, () -> service.parseChunksFromExportJson("{"));
  }

  @Test
  void suggest_happyPath_openAi() throws Exception {
    when(documentIngestionService.getChunks(null, 200, 0))
        .thenReturn(
            new ChunkListResponse(
                "c",
                1,
                1,
                200,
                0,
                List.of(new ChunkItem("id1", "Doc", 0, "Some chunk text for corpus.", Map.of()))));

    ChatResponse r = mock(ChatResponse.class, RETURNS_DEEP_STUBS);
    when(r.getResult().getOutput().getText())
        .thenReturn("{\"questions\":[\"First unique?\", \"Second unique?\"]}");
    when(openAiChatModel.call(any(Prompt.class))).thenReturn(r);

    var req =
        new DefaultQuestionSuggestionRequest(
            DefaultQuestionSuggestionService.SOURCE_CURRENT,
            null,
            null,
            MODEL_ID,
            10,
            "English");

    DefaultQuestionSuggestionResponse out = service.suggest(req);
    assertEquals(List.of("First unique?", "Second unique?"), out.suggestions());
    assertEquals(MODEL_ID, out.modelUsed());
    verify(aiBudgetService).recordUsage(anyString(), eq(MODEL_ID), anyInt(), anyInt(), anyBoolean(), any(), eq("default_question_suggestions"), any());
  }

  @Test
  void suggest_uploadedJson_happyPath() throws Exception {
    String export =
        "{\"chunks\":[{\"id\":\"u1\",\"documentTitle\":\"U\",\"chunkIndex\":0,\"text\":\"uploaded body\",\"metadata\":{}}]}";

    ChatResponse r = mock(ChatResponse.class, RETURNS_DEEP_STUBS);
    when(r.getResult().getOutput().getText()).thenReturn("{\"questions\":[\"Q one?\"]}");
    when(openAiChatModel.call(any(Prompt.class))).thenReturn(r);

    var req =
        new DefaultQuestionSuggestionRequest(
            DefaultQuestionSuggestionService.SOURCE_UPLOADED,
            null,
            export,
            MODEL_ID,
            5,
            "Norwegian");

    DefaultQuestionSuggestionResponse out = service.suggest(req);
    assertEquals(List.of("Q one?"), out.suggestions());
    verify(documentIngestionService, never()).getChunks(any(), anyInt(), anyInt());
  }

  @Test
  void suggest_modelOutputEmptyQuestionsFails() {
    when(documentIngestionService.getChunks(null, 200, 0))
        .thenReturn(
            new ChunkListResponse(
                "c",
                1,
                1,
                200,
                0,
                List.of(new ChunkItem("id1", "D", 0, "text", Map.of()))));

    ChatResponse r = mock(ChatResponse.class, RETURNS_DEEP_STUBS);
    when(r.getResult().getOutput().getText()).thenReturn("{\"questions\":[]}");
    when(openAiChatModel.call(any(Prompt.class))).thenReturn(r);

    var req =
        new DefaultQuestionSuggestionRequest(
            DefaultQuestionSuggestionService.SOURCE_CURRENT,
            null,
            null,
            MODEL_ID,
            12,
            "Norwegian");

    assertThrows(IllegalArgumentException.class, () -> service.suggest(req));
  }

  @Test
  void suggest_modelOutputMissingQuestionsKeyFails() {
    when(documentIngestionService.getChunks(null, 200, 0))
        .thenReturn(
            new ChunkListResponse(
                "c",
                1,
                1,
                200,
                0,
                List.of(new ChunkItem("id1", "D", 0, "text", Map.of()))));

    ChatResponse r = mock(ChatResponse.class, RETURNS_DEEP_STUBS);
    when(r.getResult().getOutput().getText()).thenReturn("{\"items\":[\"x\"]}");
    when(openAiChatModel.call(any(Prompt.class))).thenReturn(r);

    var req =
        new DefaultQuestionSuggestionRequest(
            DefaultQuestionSuggestionService.SOURCE_CURRENT,
            null,
            null,
            MODEL_ID,
            12,
            "Norwegian");

    assertThrows(IllegalArgumentException.class, () -> service.suggest(req));
  }

  @Test
  void suggest_deduplicatesCaseInsensitive() throws Exception {
    when(documentIngestionService.getChunks(null, 200, 0))
        .thenReturn(
            new ChunkListResponse(
                "c",
                1,
                1,
                200,
                0,
                List.of(new ChunkItem("id1", "D", 0, "body", Map.of()))));

    ChatResponse r = mock(ChatResponse.class, RETURNS_DEEP_STUBS);
    when(r.getResult().getOutput().getText())
        .thenReturn("{\"questions\":[\"Same?\", \"same?\", \"Other?\"]}");
    when(openAiChatModel.call(any(Prompt.class))).thenReturn(r);

    var req =
        new DefaultQuestionSuggestionRequest(
            DefaultQuestionSuggestionService.SOURCE_CURRENT,
            null,
            null,
            MODEL_ID,
            10,
            "Norwegian");

    DefaultQuestionSuggestionResponse out = service.suggest(req);
    assertEquals(2, out.suggestions().size());
    assertEquals("Same?", out.suggestions().getFirst());
    assertEquals("Other?", out.suggestions().get(1));
  }

  @Test
  void suggest_blankModelOutputFails() {
    when(documentIngestionService.getChunks(null, 200, 0))
        .thenReturn(
            new ChunkListResponse(
                "c",
                1,
                1,
                200,
                0,
                List.of(new ChunkItem("id1", "D", 0, "text", Map.of()))));

    ChatResponse r = mock(ChatResponse.class, RETURNS_DEEP_STUBS);
    when(r.getResult().getOutput().getText()).thenReturn("   ");
    when(openAiChatModel.call(any(Prompt.class))).thenReturn(r);

    var req =
        new DefaultQuestionSuggestionRequest(
            DefaultQuestionSuggestionService.SOURCE_CURRENT,
            null,
            null,
            MODEL_ID,
            12,
            "Norwegian");

    assertThrows(IllegalArgumentException.class, () -> service.suggest(req));
  }

  @Test
  void suggest_callsCircuitBreakerAndBudget() {
    when(documentIngestionService.getChunks(null, 200, 0))
        .thenReturn(
            new ChunkListResponse(
                "c",
                1,
                1,
                200,
                0,
                List.of(new ChunkItem("id1", "D", 0, "content", Map.of()))));

    ChatResponse r = mock(ChatResponse.class, RETURNS_DEEP_STUBS);
    when(r.getResult().getOutput().getText()).thenReturn("{\"questions\":[\"Ok?\"]}");
    when(openAiChatModel.call(any(Prompt.class))).thenReturn(r);

    var req =
        new DefaultQuestionSuggestionRequest(
            DefaultQuestionSuggestionService.SOURCE_CURRENT,
            null,
            null,
            MODEL_ID,
            12,
            "Norwegian");

    service.suggest(req);
    verify(aiCircuitBreaker).assertClosed();
    verify(aiBudgetService).assertWithinBudget(anyString(), anyBoolean());
  }
}
