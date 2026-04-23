package com.kevinmazali.portfolio.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kevinmazali.portfolio.config.AiLimitsProperties;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
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

  private void stubOpenAiReturns(String text) {
    ChatResponse r = mock(ChatResponse.class, RETURNS_DEEP_STUBS);
    when(r.getResult().getOutput().getText()).thenReturn(text);
    when(openAiChatModel.call(any(Prompt.class))).thenReturn(r);
  }
}
