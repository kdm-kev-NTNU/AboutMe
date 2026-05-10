package com.kevinmazali.portfolio.service;

import tools.jackson.databind.ObjectMapper;
import com.kevinmazali.portfolio.config.AiLimitsProperties;
import com.kevinmazali.portfolio.exception.AiCircuitOpenException;
import com.kevinmazali.portfolio.model.experiment.EvaluationScore;
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

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EvaluatorServiceTest {

  private static final String OPENAI_EVAL = "gpt-5.4-mini";

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
    AiLimitsProperties limits = new AiLimitsProperties();
    evaluatorService = new EvaluatorService(
        openAiChatModelProvider,
        anthropicChatModelProvider,
        new ObjectMapper(),
        limits,
        aiCircuitBreaker,
        aiBudgetService);
  }

  @Test
  void evaluateRelevanceParsesJsonFromModelOutput() {
    stubOpenAiReturns("prefix text {\"score\": 0.72, \"label\": \"high\", \"explanation\": \"on topic\"} suffix");

    EvaluationScore s = evaluatorService.evaluateRelevance(OPENAI_EVAL, "What is X?", "X is a thing.");

    assertEquals(0.72, s.score(), 1e-6);
    assertEquals("high", s.label());
    assertEquals("on topic", s.explanation());
  }

  @Test
  void evaluateFaithfulnessUsesPlaceholderWhenNoDocuments() {
    stubOpenAiReturns("{\"score\": 0.4, \"label\": \"partial\", \"explanation\": \"no ctx\"}");

    EvaluationScore s = evaluatorService.evaluateFaithfulness(OPENAI_EVAL, "Q?", "R", List.of());

    assertEquals(0.4, s.score(), 1e-6);
  }

  @Test
  void evaluateCorrectnessClampsScoreAboveOne() {
    stubOpenAiReturns("{\"score\": 1.9, \"label\": \"correct\", \"explanation\": \"clamped\"}");

    EvaluationScore s = evaluatorService.evaluateCorrectness(OPENAI_EVAL, "Q", "A", "ref");

    assertEquals(1.0, s.score(), 1e-6);
  }

  @Test
  void evaluateConcisenessReturnsFailedWhenScoreMissing() {
    stubOpenAiReturns("{\"label\": \"verbose\", \"explanation\": \"no score\"}");

    EvaluationScore s = evaluatorService.evaluateConciseness(OPENAI_EVAL, "Q", "long answer");

    assertTrue(Double.isNaN(s.score()));
    assertEquals("error", s.label());
  }

  @Test
  void unknownModelIdFailsGracefully() {
    EvaluationScore s = evaluatorService.evaluateRelevance("unknown-model-xyz", "q", "r");

    assertTrue(Double.isNaN(s.score()));
    assertEquals("error", s.label());
    assertTrue(s.explanation().contains("Unknown evaluator model"));
  }

  @Test
  void invokeJudge_propagatesCircuitOpen() {
    doThrow(new AiCircuitOpenException("circuit")).when(aiCircuitBreaker).assertClosed();

    assertThrows(
        AiCircuitOpenException.class,
        () -> evaluatorService.evaluateRelevance(OPENAI_EVAL, "q", "r"));
  }

  // --- Language consistency evaluator tests ---

  @Test
  void evaluateLanguageConsistencyConsistentNorwegian() {
    stubOpenAiReturns("{\"score\": 1.0, \"label\": \"consistent\", \"explanation\": \"Both question and response are in Norwegian.\"}");

    EvaluationScore s = evaluatorService.evaluateLanguageConsistency(
        OPENAI_EVAL, "Hva studerer Kevin?", "Kevin studerer dataingeniør ved NTNU.");

    assertEquals(1.0, s.score(), 1e-6);
    assertEquals("consistent", s.label());
  }

  @Test
  void evaluateLanguageConsistencyConsistentEnglish() {
    stubOpenAiReturns("{\"score\": 1.0, \"label\": \"consistent\", \"explanation\": \"Both in English.\"}");

    EvaluationScore s = evaluatorService.evaluateLanguageConsistency(
        OPENAI_EVAL, "What does Kevin study?", "Kevin studies data engineering at NTNU.");

    assertEquals(1.0, s.score(), 1e-6);
    assertEquals("consistent", s.label());
  }

  @Test
  void evaluateLanguageConsistencyWrongLanguageEnglishResponseToNorwegian() {
    stubOpenAiReturns("{\"score\": 0.0, \"label\": \"wrong_language\", \"explanation\": \"Norwegian question but English response.\"}");

    EvaluationScore s = evaluatorService.evaluateLanguageConsistency(
        OPENAI_EVAL, "Hva studerer Kevin?", "Kevin studies data engineering at NTNU.");

    assertEquals(0.0, s.score(), 1e-6);
    assertEquals("wrong_language", s.label());
  }

  @Test
  void evaluateLanguageConsistencyWrongLanguageNorwegianResponseToEnglish() {
    stubOpenAiReturns("{\"score\": 0.0, \"label\": \"wrong_language\", \"explanation\": \"English question but Norwegian response.\"}");

    EvaluationScore s = evaluatorService.evaluateLanguageConsistency(
        OPENAI_EVAL, "What does Kevin study?", "Kevin studerer dataingeniør ved NTNU.");

    assertEquals(0.0, s.score(), 1e-6);
    assertEquals("wrong_language", s.label());
  }

  @Test
  void evaluateLanguageConsistencyMixedResponse() {
    stubOpenAiReturns("{\"score\": 0.3, \"label\": \"mixed\", \"explanation\": \"Response mixes both languages.\"}");

    EvaluationScore s = evaluatorService.evaluateLanguageConsistency(
        OPENAI_EVAL, "Hva studerer Kevin?", "Kevin studerer data engineering at NTNU. He er veldig flink.");

    assertEquals(0.3, s.score(), 1e-6);
    assertEquals("mixed", s.label());
  }

  @Test
  void evaluateLanguageConsistencyMostlyConsistent() {
    stubOpenAiReturns("{\"score\": 0.7, \"label\": \"mostly_consistent\", \"explanation\": \"Mostly Norwegian with a few English terms.\"}");

    EvaluationScore s = evaluatorService.evaluateLanguageConsistency(
        OPENAI_EVAL, "Hva jobber Kevin med?", "Kevin jobber med Spring Boot og machine learning.");

    assertEquals(0.7, s.score(), 1e-6);
    assertEquals("mostly_consistent", s.label());
  }

  @Test
  void evaluateLanguageConsistencyHandlesUnknownModel() {
    EvaluationScore s = evaluatorService.evaluateLanguageConsistency(
        "unknown-model-xyz", "Hva studerer Kevin?", "Kevin studerer data.");

    assertTrue(Double.isNaN(s.score()));
    assertEquals("error", s.label());
    assertTrue(s.explanation().contains("Unknown evaluator model"));
  }

  @Test
  void evaluateLanguageConsistencyHandlesEmptyResponse() {
    stubOpenAiReturns("{\"score\": 0.0, \"label\": \"wrong_language\", \"explanation\": \"empty response\"}");

    EvaluationScore s = evaluatorService.evaluateLanguageConsistency(
        OPENAI_EVAL, "Fortell om Kevin", "");

    assertEquals(0.0, s.score(), 1e-6);
  }

  @Test
  void evaluateLanguageConsistencyHandlesNullFields() {
    stubOpenAiReturns("{\"score\": 1.0, \"label\": \"consistent\", \"explanation\": \"ok\"}");

    EvaluationScore s = evaluatorService.evaluateLanguageConsistency(
        OPENAI_EVAL, "", "");

    assertEquals(1.0, s.score(), 1e-6);
  }

  @Test
  void evaluateLanguageConsistencyClampsScoreAboveOne() {
    stubOpenAiReturns("{\"score\": 1.5, \"label\": \"consistent\", \"explanation\": \"clamped\"}");

    EvaluationScore s = evaluatorService.evaluateLanguageConsistency(
        OPENAI_EVAL, "Hei", "Hei på deg");

    assertEquals(1.0, s.score(), 1e-6);
  }

  @Test
  void evaluateLanguageConsistencyClampsScoreBelowZero() {
    stubOpenAiReturns("{\"score\": -0.5, \"label\": \"wrong_language\", \"explanation\": \"clamped\"}");

    EvaluationScore s = evaluatorService.evaluateLanguageConsistency(
        OPENAI_EVAL, "Hello", "Hei");

    assertEquals(0.0, s.score(), 1e-6);
  }

  @Test
  void evaluateLanguageConsistencyReturnsFailedOnMalformedJson() {
    stubOpenAiReturns("this is not json at all");

    EvaluationScore s = evaluatorService.evaluateLanguageConsistency(
        OPENAI_EVAL, "Hva studerer Kevin?", "Kevin studerer data.");

    assertTrue(Double.isNaN(s.score()));
    assertEquals("error", s.label());
  }

  @Test
  void evaluateLanguageConsistencyReturnsFailedOnMissingScore() {
    stubOpenAiReturns("{\"label\": \"consistent\", \"explanation\": \"no score field\"}");

    EvaluationScore s = evaluatorService.evaluateLanguageConsistency(
        OPENAI_EVAL, "Hva?", "svar");

    assertTrue(Double.isNaN(s.score()));
    assertEquals("error", s.label());
  }

  @Test
  void evaluateLanguageConsistencyParsesJsonWithSurroundingText() {
    stubOpenAiReturns("Here is my evaluation: {\"score\": 0.7, \"label\": \"mostly_consistent\", \"explanation\": \"ok\"} end.");

    EvaluationScore s = evaluatorService.evaluateLanguageConsistency(
        OPENAI_EVAL, "Hvem er Kevin?", "Kevin er en student. He studies at NTNU.");

    assertEquals(0.7, s.score(), 1e-6);
    assertEquals("mostly_consistent", s.label());
  }

  @Test
  void evaluateLanguageConsistencyNorwegianQuestionWithBokmaalDialect() {
    stubOpenAiReturns("{\"score\": 1.0, \"label\": \"consistent\", \"explanation\": \"Both in Norwegian bokmål.\"}");

    EvaluationScore s = evaluatorService.evaluateLanguageConsistency(
        OPENAI_EVAL, "Hva slags prosjekter har Kevin jobbet med?", "Kevin har jobbet med flere prosjekter, inkludert denne porteføljesiden.");

    assertEquals(1.0, s.score(), 1e-6);
  }

  @Test
  void evaluateLanguageConsistencyNorwegianQuestionWithNynorskResponse() {
    stubOpenAiReturns("{\"score\": 0.7, \"label\": \"mostly_consistent\", \"explanation\": \"Question in bokmål, response in nynorsk.\"}");

    EvaluationScore s = evaluatorService.evaluateLanguageConsistency(
        OPENAI_EVAL, "Kva studerer Kevin?", "Kevin studerer dataingeniør ved NTNU.");

    assertEquals(0.7, s.score(), 1e-6);
  }

  @Test
  void evaluateLanguageConsistencyTechnicalTermsAcceptable() {
    stubOpenAiReturns("{\"score\": 1.0, \"label\": \"consistent\", \"explanation\": \"Technical terms in English are acceptable in Norwegian context.\"}");

    EvaluationScore s = evaluatorService.evaluateLanguageConsistency(
        OPENAI_EVAL,
        "Hvilke teknologier bruker Kevin?",
        "Kevin bruker Spring Boot, Vue.js, PostgreSQL og pgvector i sine prosjekter.");

    assertEquals(1.0, s.score(), 1e-6);
  }

  @Test
  void evaluateLanguageConsistencyLongNorwegianResponse() {
    stubOpenAiReturns("{\"score\": 1.0, \"label\": \"consistent\", \"explanation\": \"Full Norwegian response.\"}");

    EvaluationScore s = evaluatorService.evaluateLanguageConsistency(
        OPENAI_EVAL,
        "Kan du fortelle meg om Kevin sin bacheloroppgave?",
        "Kevin jobber med en bacheloroppgave innen dataingeniør ved NTNU. Prosjektet handler om å bygge en interaktiv porteføljeside med AI-drevet chat. Han bruker Spring Boot på backend og Vue.js på frontend, med PostgreSQL og pgvector for lagring av data og vektorembeddings.");

    assertEquals(1.0, s.score(), 1e-6);
  }

  @Test
  void evaluateLanguageConsistencyLongEnglishResponse() {
    stubOpenAiReturns("{\"score\": 1.0, \"label\": \"consistent\", \"explanation\": \"Full English response.\"}");

    EvaluationScore s = evaluatorService.evaluateLanguageConsistency(
        OPENAI_EVAL,
        "Can you tell me about Kevin's bachelor thesis?",
        "Kevin is working on a bachelor thesis in data engineering at NTNU. The project involves building an interactive portfolio site with AI-powered chat. He uses Spring Boot on the backend and Vue.js on the frontend, with PostgreSQL and pgvector for data and vector embeddings.");

    assertEquals(1.0, s.score(), 1e-6);
  }

  private void stubOpenAiReturns(String text) {
    ChatResponse r = mock(ChatResponse.class, RETURNS_DEEP_STUBS);
    when(r.getResult().getOutput().getText()).thenReturn(text);
    when(openAiChatModel.call(any(Prompt.class))).thenReturn(r);
  }
}
