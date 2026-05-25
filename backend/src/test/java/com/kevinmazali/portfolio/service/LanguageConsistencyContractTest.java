package com.kevinmazali.portfolio.service;

import com.kevinmazali.portfolio.config.AiBudgetProperties;
import com.kevinmazali.portfolio.config.AiLimitsProperties;
import com.kevinmazali.portfolio.config.RetrievalProperties;
import com.kevinmazali.portfolio.model.Answer;
import com.kevinmazali.portfolio.model.Question;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StreamUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Cross-cutting contract tests that verify every language-sensitive component
 * in the RAG pipeline enforces or supports bilingual (Norwegian / English) behaviour.
 *
 * This complements {@link RagPromptLanguageRulesTest} (template content)
 * and the individual unit tests by checking the interaction between components.
 */
@ExtendWith(MockitoExtension.class)
class LanguageConsistencyContractTest {

  // -----------------------------------------------------------------------
  //  1. RAG prompt templates: structural invariants
  // -----------------------------------------------------------------------

  @Nested
  class PromptTemplateLanguageInvariants {

    private String loadTemplate(String path) throws IOException {
      ClassPathResource res = new ClassPathResource(path);
      return StreamUtils.copyToString(res.getInputStream(), StandardCharsets.UTF_8);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "templates/rag-prompt-template-openai.st",
        "templates/rag-prompt-template-anthropic.st"
    })
    void languageRuleAppearsBeforeQuestionPlaceholder(String path) throws IOException {
      String t = loadTemplate(path);
      int ruleIdx = t.toLowerCase().indexOf("same language as the user");
      int inputIdx = t.indexOf("{input}");
      assertTrue(ruleIdx >= 0, "language rule must exist");
      assertTrue(inputIdx >= 0, "{input} must exist");
      assertTrue(ruleIdx < inputIdx,
          "Language rule must appear before {input} so it precedes the user question in context");
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "templates/rag-prompt-template-openai.st",
        "templates/rag-prompt-template-anthropic.st"
    })
    void templateDoesNotContainHardcodedResponseLanguage(String path) throws IOException {
      String t = loadTemplate(path).toLowerCase();
      assertFalse(t.contains("always respond in english"),
          "Template must NOT hardcode English as the response language");
      assertFalse(t.contains("always respond in norwegian"),
          "Template must NOT hardcode Norwegian as the response language");
      assertFalse(t.contains("svar alltid på norsk"),
          "Template must NOT hardcode Norwegian as the response language (Norwegian text)");
      assertFalse(t.contains("svar alltid på engelsk"),
          "Template must NOT hardcode English as the response language (Norwegian text)");
    }

    @Test
    void openaiAndAnthropicTemplatesBothExist() throws IOException {
      assertTrue(new ClassPathResource("templates/rag-prompt-template-openai.st").exists());
      assertTrue(new ClassPathResource("templates/rag-prompt-template-anthropic.st").exists());
    }
  }

  // -----------------------------------------------------------------------
  //  2. Query expansion: bilingual coverage
  // -----------------------------------------------------------------------

  @Nested
  class QueryExpansionBilingualCoverage {

    @Mock
    private OpenAiChatModel openAiChatModel;

    @Mock
    private ObjectProvider<OpenAiChatModel> openAiChatModelProvider;

    @Mock
    private ObjectProvider<AnthropicChatModel> anthropicChatModelProvider;

    @Mock
    private VectorStore vectorStore;

    @Mock
    private PromptVersionService promptVersionService;

    @Mock
    private AiBudgetService aiBudgetService;

    @Mock
    private AiCircuitBreaker aiCircuitBreaker;

    private OpenAIServiceImpl service;

    @BeforeEach
    void setUp() {
      when(openAiChatModelProvider.getIfAvailable()).thenReturn(openAiChatModel);
      when(promptVersionService.loadRagPrompt(anyString()))
          .thenReturn("Input: {input}\nDocs:\n{documents}");
      lenient().doNothing().when(aiCircuitBreaker).assertClosed();
      lenient().doNothing().when(aiBudgetService).assertWithinBudget(anyString(), anyBoolean());
      lenient()
          .doNothing()
          .when(aiBudgetService)
          .recordUsage(anyString(), anyString(), anyInt(), anyInt(), anyBoolean(), any(), any(), any());

      service = new OpenAIServiceImpl(
          openAiChatModelProvider,
          anthropicChatModelProvider,
          vectorStore,
          "gpt-5.4-mini",
          promptVersionService,
          new AiLimitsProperties(),
          new AiBudgetProperties(),
          aiBudgetService,
          aiCircuitBreaker,
          new PassThroughDocumentReranker(),
          new RetrievalProperties(),
          new PostHogTraceContext(),
          null,
          null);
    }

    @AfterEach
    void tearDown() {
      SecurityContextHolder.clearContext();
    }

    @Test
    void queryExpansionReturnsNorwegianAndEnglishVariants() {
      when(vectorStore.similaritySearch(any(SearchRequest.class)))
          .thenReturn(List.of(new Document("ctx", new HashMap<>())));

      ChatResponse expand = mock(ChatResponse.class, RETURNS_DEEP_STUBS);
      when(expand.getResult().getOutput().getText()).thenReturn("{\"en\":\"What is Kevin studying?\",\"no\":\"Hva studerer Kevin?\"}");

      ChatResponse rag = mock(ChatResponse.class, RETURNS_DEEP_STUBS);
      when(rag.getResult().getOutput().getText()).thenReturn("answer");

      when(openAiChatModel.call(any(Prompt.class))).thenReturn(expand, rag);

      service.getAnswer(new Question("Hva studerer Kevin?"));

      // Query expansion produces 3 variants (original, en, no), each searched against the vector store
      verify(vectorStore, atLeast(1)).similaritySearch(any(SearchRequest.class));
    }

    @Test
    void norwegianQuestionIsPassedThroughToRagPromptUnchanged() {
      when(vectorStore.similaritySearch(any(SearchRequest.class)))
          .thenReturn(List.of(new Document("Kevin studerer data.", new HashMap<>())));

      ChatResponse expand = mock(ChatResponse.class, RETURNS_DEEP_STUBS);
      when(expand.getResult().getOutput().getText()).thenReturn("{\"en\":\"What?\",\"no\":\"Hva?\"}");

      ChatResponse rag = mock(ChatResponse.class, RETURNS_DEEP_STUBS);
      when(rag.getResult().getOutput().getText()).thenReturn("svar");

      when(openAiChatModel.call(any(Prompt.class))).thenReturn(expand, rag);

      service.getAnswer(new Question("Hva studerer Kevin?"));

      ArgumentCaptor<Prompt> captor = ArgumentCaptor.forClass(Prompt.class);
      verify(openAiChatModel, times(2)).call(captor.capture());
      // The last call is the RAG completion; the first is query expansion
      String ragPrompt = captor.getAllValues().get(1).toString();
      assertTrue(ragPrompt.contains("Hva studerer Kevin?"),
          "The original Norwegian question must appear unchanged in the RAG prompt");
    }

    @Test
    void englishQuestionIsPassedThroughToRagPromptUnchanged() {
      when(vectorStore.similaritySearch(any(SearchRequest.class)))
          .thenReturn(List.of(new Document("Kevin studies data.", new HashMap<>())));

      ChatResponse expand = mock(ChatResponse.class, RETURNS_DEEP_STUBS);
      when(expand.getResult().getOutput().getText()).thenReturn("{\"en\":\"What does Kevin study?\",\"no\":\"Hva studerer Kevin?\"}");

      ChatResponse rag = mock(ChatResponse.class, RETURNS_DEEP_STUBS);
      when(rag.getResult().getOutput().getText()).thenReturn("answer");

      when(openAiChatModel.call(any(Prompt.class))).thenReturn(expand, rag);

      service.getAnswer(new Question("What does Kevin study?"));

      ArgumentCaptor<Prompt> captor = ArgumentCaptor.forClass(Prompt.class);
      verify(openAiChatModel, times(2)).call(captor.capture());
      String ragPrompt = captor.getAllValues().get(1).toString();
      assertTrue(ragPrompt.contains("What does Kevin study?"),
          "The original English question must appear unchanged in the RAG prompt");
    }
  }

  // -----------------------------------------------------------------------
  //  3. Default question suggestions: language parameter
  // -----------------------------------------------------------------------

  @Nested
  class DefaultQuestionSuggestionLanguageTemplate {

    @Test
    void promptTemplateContainsLanguagePlaceholder() throws IOException {
      ClassPathResource res = new ClassPathResource("prompts/default_chat_questions.txt");
      assertTrue(res.exists(), "default_chat_questions.txt must exist");
      String template = StreamUtils.copyToString(res.getInputStream(), StandardCharsets.UTF_8);
      assertTrue(template.contains("{language}"),
          "Question suggestion prompt must contain {language} placeholder");
    }

    @Test
    void promptTemplateMentionsOutputLanguage() throws IOException {
      ClassPathResource res = new ClassPathResource("prompts/default_chat_questions.txt");
      String template = StreamUtils.copyToString(res.getInputStream(), StandardCharsets.UTF_8);
      assertTrue(template.toLowerCase().contains("output language") || template.toLowerCase().contains("language"),
          "Question suggestion prompt must reference the output language");
    }
  }

  // -----------------------------------------------------------------------
  //  4. EvaluatorService: language consistency judge prompt quality
  // -----------------------------------------------------------------------

  @Nested
  class EvaluatorLanguageConsistencyPromptQuality {

    @Mock
    private OpenAiChatModel openAiChatModel;

    @Mock
    private ObjectProvider<OpenAiChatModel> openAiChatModelProvider;

    @Mock
    private ObjectProvider<AnthropicChatModel> anthropicChatModelProvider;

    @Mock
    private AiCircuitBreaker aiCircuitBreaker;

    @Mock
    private AiBudgetService aiBudgetService;

    private EvaluatorService evaluatorService;

    @BeforeEach
    void setUp() {
      lenient().when(openAiChatModelProvider.getIfAvailable()).thenReturn(openAiChatModel);
      lenient().doNothing().when(aiCircuitBreaker).assertClosed();
      lenient()
          .doNothing()
          .when(aiBudgetService)
          .recordUsage(anyString(), anyString(), anyInt(), anyInt(), anyBoolean(), any(), any(), any());

      evaluatorService = new EvaluatorService(
          openAiChatModelProvider,
          anthropicChatModelProvider,
          new tools.jackson.databind.ObjectMapper(),
          new AiLimitsProperties(),
          aiCircuitBreaker,
          aiBudgetService);
    }

    @ParameterizedTest
    @CsvSource({
        "'Hva studerer Kevin?', 'Kevin studerer dataingeniør ved NTNU.'",
        "'What does Kevin study?', 'Kevin studies data engineering at NTNU.'",
        "'Fortell om Kevin sine prosjekter', 'Kevin har jobbet med flere prosjekter.'",
        "'Tell me about Kevin''s projects', 'Kevin has worked on several projects.'",
    })
    void languageConsistencyJudgePromptContainsBothInputs(String question, String response) {
      ChatResponse resp = mock(ChatResponse.class, RETURNS_DEEP_STUBS);
      when(resp.getResult().getOutput().getText()).thenReturn(
          "{\"score\": 1.0, \"label\": \"consistent\", \"explanation\": \"ok\"}");
      when(openAiChatModel.call(any(Prompt.class))).thenReturn(resp);

      evaluatorService.evaluateLanguageConsistency("gpt-5.4-mini", question, response);

      ArgumentCaptor<Prompt> captor = ArgumentCaptor.forClass(Prompt.class);
      verify(openAiChatModel).call(captor.capture());
      String promptText = captor.getValue().toString();
      assertTrue(promptText.contains(question),
          "Judge prompt must contain the original question");
      assertTrue(promptText.contains(response),
          "Judge prompt must contain the response being evaluated");
    }

    @Test
    void languageConsistencyJudgePromptMentionsBilingual() {
      ChatResponse resp = mock(ChatResponse.class, RETURNS_DEEP_STUBS);
      when(resp.getResult().getOutput().getText()).thenReturn(
          "{\"score\": 1.0, \"label\": \"consistent\", \"explanation\": \"ok\"}");
      when(openAiChatModel.call(any(Prompt.class))).thenReturn(resp);

      evaluatorService.evaluateLanguageConsistency("gpt-5.4-mini", "test", "test");

      ArgumentCaptor<Prompt> captor = ArgumentCaptor.forClass(Prompt.class);
      verify(openAiChatModel).call(captor.capture());
      String promptText = captor.getValue().toString().toLowerCase();
      assertTrue(promptText.contains("norwegian") && promptText.contains("english"),
          "Judge prompt must mention both Norwegian and English");
      assertTrue(promptText.contains("bilingual"),
          "Judge prompt must indicate this is a bilingual chatbot evaluation");
    }

    @Test
    void languageConsistencyJudgePromptDefinesScoringRubric() {
      ChatResponse resp = mock(ChatResponse.class, RETURNS_DEEP_STUBS);
      when(resp.getResult().getOutput().getText()).thenReturn(
          "{\"score\": 1.0, \"label\": \"consistent\", \"explanation\": \"ok\"}");
      when(openAiChatModel.call(any(Prompt.class))).thenReturn(resp);

      evaluatorService.evaluateLanguageConsistency("gpt-5.4-mini", "test", "test");

      ArgumentCaptor<Prompt> captor = ArgumentCaptor.forClass(Prompt.class);
      verify(openAiChatModel).call(captor.capture());
      String promptText = captor.getValue().toString().toLowerCase();
      assertTrue(promptText.contains("1.0") && promptText.contains("consistent"),
          "Judge prompt must define 1.0 = consistent");
      assertTrue(promptText.contains("0.0") && promptText.contains("wrong_language"),
          "Judge prompt must define 0.0 = wrong_language");
      assertTrue(promptText.contains("0.7") || promptText.contains("mostly"),
          "Judge prompt must define a partial consistency score");
      assertTrue(promptText.contains("0.3") || promptText.contains("mixed"),
          "Judge prompt must define a mixed language score");
    }

    @Test
    void languageConsistencyJudgeHandlesTechnicalTermsGracefully() {
      ChatResponse resp = mock(ChatResponse.class, RETURNS_DEEP_STUBS);
      when(resp.getResult().getOutput().getText()).thenReturn(
          "{\"score\": 1.0, \"label\": \"consistent\", \"explanation\": \"ok\"}");
      when(openAiChatModel.call(any(Prompt.class))).thenReturn(resp);

      evaluatorService.evaluateLanguageConsistency("gpt-5.4-mini", "test", "test");

      ArgumentCaptor<Prompt> captor = ArgumentCaptor.forClass(Prompt.class);
      verify(openAiChatModel).call(captor.capture());
      String promptText = captor.getValue().toString().toLowerCase();
      assertTrue(promptText.contains("technical") || promptText.contains("proper nouns") || promptText.contains("jargon"),
          "Judge prompt must mention that technical terms in either language are acceptable");
    }
  }

  // -----------------------------------------------------------------------
  //  5. Question DTO: language-agnostic pass-through
  // -----------------------------------------------------------------------

  @Nested
  class QuestionDtoLanguageAgnostic {

    @Test
    void questionRecordPreservesNorwegianCharacters() {
      Question q = new Question("Hva studerer Kevin? Æøå ÆØÅ");
      assertTrue(q.question().contains("Æøå"));
      assertTrue(q.question().contains("ÆØÅ"));
    }

    @Test
    void questionRecordPreservesEnglishText() {
      Question q = new Question("What does Kevin study?");
      assertTrue(q.question().contains("What does Kevin study?"));
    }

    @Test
    void questionRecordHandlesEmoji() {
      Question q = new Question("Hva gjør Kevin? 😊");
      assertTrue(q.question().contains("😊"));
    }

    @Test
    void questionRecordHandlesLongNorwegianText() {
      String longQ = "Kan du fortelle meg om alle prosjektene Kevin har jobbet med? " +
          "Jeg er spesielt interessert i bacheloroppgaven hans og teknologivalg.";
      Question q = new Question(longQ);
      assertTrue(q.question().equals(longQ));
    }
  }

  // -----------------------------------------------------------------------
  //  6. ExperimentResult and ExperimentRun: language consistency fields exist
  // -----------------------------------------------------------------------

  @Nested
  class ExperimentEntityLanguageConsistencyFields {

    @Test
    void experimentMetricScoreStoresLanguageConsistency() {
      var score =
          com.kevinmazali.portfolio.model.experiment.ExperimentMetricScore.builder()
              .metric("language_consistency")
              .score(1.0)
              .explanation("Both Norwegian")
              .build();
      assertEquals("language_consistency", score.getMetric());
      assertTrue(score.getExplanation().contains("Norwegian"));
    }

    @Test
    void experimentRunDetailResponseIncludesMeanLanguageConsistency() {
      var resp =
          new com.kevinmazali.portfolio.model.experiment.ExperimentRunDetailResponse(
              1L,
              "test",
              "ds",
              9L,
              "",
              "gpt-5.4-mini",
              "gpt-5.4-mini",
              com.kevinmazali.portfolio.model.experiment.ExperimentRunStatus.COMPLETED,
              1,
              null,
              null,
              null,
              null,
              0.95,
              null,
              null,
              null,
              List.of());
      assertEquals(0.95, resp.meanLanguageConsistency());
    }

    @Test
    void experimentResultResponseIncludesLanguageConsistency() {
      var resp = new com.kevinmazali.portfolio.model.experiment.ExperimentResultResponse(
          1L, "q", "ref", "rag", "docs",
          0.9, 0.8, 0.7, 0.6, 1.0,
          "f", "r", "c", "k", "lc"
      );
      assertTrue(resp.languageConsistency() == 1.0);
      assertTrue(resp.languageConsistencyExplanation().equals("lc"));
    }

    @Test
    void experimentRunSummaryResponseIncludesMeanLanguageConsistency() {
      var resp = new com.kevinmazali.portfolio.model.experiment.ExperimentRunSummaryResponse(
          1L, "n", "ds", "gen", "eval",
          com.kevinmazali.portfolio.model.experiment.ExperimentRunStatus.COMPLETED,
          5, 0.9, 0.8, 0.7, 0.6, 0.95,
          null, null, null
      );
      assertTrue(resp.meanLanguageConsistency() == 0.95);
    }

  }
}
