package com.kevinmazali.portfolio.service;

import com.kevinmazali.portfolio.config.AiBudgetProperties;
import com.kevinmazali.portfolio.config.AiLimitsProperties;
import com.kevinmazali.portfolio.config.RelevanceGateProperties;
import com.kevinmazali.portfolio.config.RetrievalProperties;
import com.kevinmazali.portfolio.model.Answer;
import com.kevinmazali.portfolio.model.Question;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OpenAIServiceImplTest {

  @Mock
  private OpenAiChatModel openAiChatModel;

  @Mock
  private ObjectProvider<OpenAiChatModel> openAiChatModelProvider;

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
  private RetrievalProperties retrievalProperties;

  @BeforeEach
  void setUp() {
    lenient().when(openAiChatModelProvider.getIfAvailable()).thenReturn(openAiChatModel);
    lenient()
        .when(promptVersionService.loadRagPrompt(anyString()))
        .thenReturn("Input: {input}\nDocs:\n{documents}");
    lenient().doNothing().when(aiCircuitBreaker).assertClosed();
    lenient().doNothing().when(aiBudgetService).assertWithinBudget(anyString(), anyBoolean());
    lenient()
        .doNothing()
        .when(aiBudgetService)
        .recordUsage(anyString(), anyString(), anyInt(), anyInt(), anyBoolean(), any(), any(), any());

    AiLimitsProperties limits = new AiLimitsProperties();
    AiBudgetProperties budgetProps = new AiBudgetProperties();
    retrievalProperties = new RetrievalProperties();
    openAIServiceImpl = new OpenAIServiceImpl(
        openAiChatModelProvider,
        anthropicChatModelProvider,
        vectorStore,
        "gpt-5.4-mini",
        promptVersionService,
        limits,
        budgetProps,
        aiBudgetService,
        aiCircuitBreaker,
        new PassThroughDocumentReranker(),
        retrievalProperties,
        disabledRelevanceGate(),
        new OffTopicRedirectMessages(),
        new PostHogTraceContext(),
        null,
        null);
  }

  private static RelevanceGateService disabledRelevanceGate() {
    RelevanceGateProperties props = new RelevanceGateProperties();
    props.setEnabled(false);
    return new RelevanceGateService(props);
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

  @Test
  void getAnswerUsesRerankerOutputForDocumentsInPrompt() {
    DocumentReranker reranker =
        (query, candidates, topN) ->
            candidates.isEmpty() ? List.of() : List.of(candidates.get(candidates.size() - 1));
    OpenAIServiceImpl svc =
        new OpenAIServiceImpl(
            openAiChatModelProvider,
            anthropicChatModelProvider,
            vectorStore,
            "gpt-5.4-mini",
            promptVersionService,
            new AiLimitsProperties(),
            new AiBudgetProperties(),
            aiBudgetService,
            aiCircuitBreaker,
            reranker,
            retrievalProperties,
            disabledRelevanceGate(),
            new OffTopicRedirectMessages(),
            new PostHogTraceContext(),
            null,
            null);

    when(vectorStore.similaritySearch(any(SearchRequest.class)))
        .thenReturn(
            List.of(
                new Document("doc-alpha-unique", new HashMap<>()),
                new Document("doc-beta-unique", new HashMap<>())));

    ChatResponse expand = mock(ChatResponse.class, RETURNS_DEEP_STUBS);
    when(expand.getResult().getOutput().getText()).thenReturn("{\"en\":\"Hello\",\"no\":\"Hei\"}");

    ChatResponse rag = mock(ChatResponse.class, RETURNS_DEEP_STUBS);
    when(rag.getResult().getOutput().getText()).thenReturn("ok");

    when(openAiChatModel.call(any(Prompt.class))).thenReturn(expand, rag);

    svc.getAnswer(new Question("Hi"));

    ArgumentCaptor<Prompt> promptCaptor = ArgumentCaptor.forClass(Prompt.class);
    verify(openAiChatModel, times(2)).call(promptCaptor.capture());
    String ragText = promptCaptor.getAllValues().get(1).toString();
    assertTrue(ragText.contains("doc-beta-unique"));
    assertTrue(!ragText.contains("doc-alpha-unique"));
  }

  @Test
  void getAnswerReturnsRedirectForOffTopicWithoutCallingLlm() {
    RelevanceGateProperties gateProps = new RelevanceGateProperties();
    gateProps.setEnabled(true);
    OpenAIServiceImpl svc =
        new OpenAIServiceImpl(
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
            retrievalProperties,
            new RelevanceGateService(gateProps),
            new OffTopicRedirectMessages(),
            new PostHogTraceContext(),
            null,
            null);

    Answer answer = svc.getAnswer(new Question("What is the meaning of life?"));

    assertTrue(answer.answer().contains("don't have information"));
    verify(openAiChatModel, never()).call(any(Prompt.class));
    verify(vectorStore, never()).similaritySearch(any(SearchRequest.class));
  }

  @Test
  void getAnswerReturnsNoContextRedirectWhenRetrievalIsEmpty() {
    when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());

    ChatResponse expand = mock(ChatResponse.class, RETURNS_DEEP_STUBS);
    when(expand.getResult().getOutput().getText()).thenReturn("{\"en\":\"Hello\",\"no\":\"Hei\"}");
    when(openAiChatModel.call(any(Prompt.class))).thenReturn(expand);

    Answer answer = openAIServiceImpl.getAnswer(new Question("Tell me about Kevin's rare unpublished hobby xyz"));

    assertTrue(answer.answer().contains("doesn't include") || answer.answer().contains("information"));
    verify(openAiChatModel, times(1)).call(any(Prompt.class));
  }
}
