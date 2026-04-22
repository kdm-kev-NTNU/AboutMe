package com.kevinmazali.portfolio.service;

import com.kevinmazali.portfolio.config.AiBudgetProperties;
import com.kevinmazali.portfolio.config.AiLimitsProperties;
import com.kevinmazali.portfolio.model.Answer;
import com.kevinmazali.portfolio.model.Question;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OpenAIServiceImplTest {

  @Mock
  private OpenAiChatModel openAiChatModel;

  @Mock
  private VectorStore vectorStore;

  @Mock
  private ObjectProvider<AnthropicChatModel> anthropicChatModelProvider;

  @Mock
  private PromptVersionService promptVersionService;

  @Mock
  private AiBudgetService aiBudgetService;

  @Mock
  private AiCircuitBreaker aiCircuitBreaker;

  private OpenAIServiceImpl openAIServiceImpl;

  @BeforeEach
  void setUp() {
    when(promptVersionService.loadRagPrompt(anyString()))
        .thenReturn("Input: {input}\nDocs:\n{documents}");
    lenient().doNothing().when(aiCircuitBreaker).assertClosed();
    lenient().doNothing().when(aiBudgetService).assertWithinBudget(anyString(), anyBoolean());
    lenient().doNothing().when(aiBudgetService).recordUsage(anyString(), anyString(), anyInt(), anyInt(), anyBoolean());

    AiLimitsProperties limits = new AiLimitsProperties();
    AiBudgetProperties budgetProps = new AiBudgetProperties();
    openAIServiceImpl = new OpenAIServiceImpl(
        openAiChatModel,
        anthropicChatModelProvider,
        vectorStore,
        "gpt-5.4-mini",
        promptVersionService,
        limits,
        budgetProps,
        aiBudgetService,
        aiCircuitBreaker);
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void getAnswerReturnsChatModelOutput() {
    when(vectorStore.similaritySearch(any(SearchRequest.class)))
        .thenReturn(List.of(new Document("ctx", new HashMap<>())));

    ChatResponse expand = mock(ChatResponse.class, RETURNS_DEEP_STUBS);
    when(expand.getResult().getOutput().getText()).thenReturn("{\"en\":\"Hello\",\"no\":\"Hei\"}");

    ChatResponse rag = mock(ChatResponse.class, RETURNS_DEEP_STUBS);
    when(rag.getResult().getOutput().getText()).thenReturn("final answer text");

    when(openAiChatModel.call(any(Prompt.class))).thenReturn(expand, rag);

    Answer answer = openAIServiceImpl.getAnswer(new Question("Hi"));

    assertEquals("final answer text", answer.answer());
  }
}
