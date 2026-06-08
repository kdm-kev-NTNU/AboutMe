package com.kevinmazali.portfolio.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kevinmazali.portfolio.config.AiBudgetProperties;
import com.kevinmazali.portfolio.config.RealtimeProperties;
import com.kevinmazali.portfolio.exception.RealtimeErrorCode;
import com.kevinmazali.portfolio.exception.RealtimeSessionException;
import com.kevinmazali.portfolio.model.interview.InterviewSessionEntity;
import com.kevinmazali.portfolio.repository.InterviewSessionRepository;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InterviewRealtimeSessionServiceTest {

  @Mock private AiBudgetService aiBudgetService;
  @Mock private AiCircuitBreaker aiCircuitBreaker;
  @Mock private OpenAiRealtimeHttpInvoker openAiRealtimeHttpInvoker;
  @Mock private RealtimeModelCatalog realtimeModelCatalog;
  @Mock private InterviewDocumentService interviewDocumentService;
  @Mock private InterviewSessionRepository sessionRepository;

  private RealtimeProperties realtimeProperties;
  private AiBudgetProperties budgetProperties;
  private InterviewRealtimeSessionService service;

  @BeforeEach
  void setUp() {
    realtimeProperties = new RealtimeProperties();
    realtimeProperties.setEnabled(true);
    realtimeProperties.setModel("gpt-realtime-2");
    realtimeProperties.setVoice("marin");
    realtimeProperties.setReasoningEffort("low");
    realtimeProperties.setMaxResponseOutputTokens(512);
    realtimeProperties.setReservationInputTokens(100);
    realtimeProperties.setReservationOutputTokens(200);

    budgetProperties = new AiBudgetProperties();
    budgetProperties.setAnonIdentitySalt("test-salt");
    budgetProperties.setEnabled(false);

    doNothing().when(aiCircuitBreaker).assertClosed();
    doNothing().when(aiBudgetService).assertWithinBudget(anyString(), anyBoolean());
    when(realtimeModelCatalog.resolveOpenAiModelId(any())).thenReturn("gpt-realtime-2");
    when(realtimeModelCatalog.isOpenAiModelConfigured(anyString())).thenReturn(true);

    service =
        new InterviewRealtimeSessionService(
            realtimeProperties,
            aiBudgetService,
            budgetProperties,
            aiCircuitBreaker,
            openAiRealtimeHttpInvoker,
            realtimeModelCatalog,
            interviewDocumentService,
            sessionRepository,
            "sk-test-openai-key");
    service.preloadInstructionPrompts();
  }

  @Test
  void createInterviewCall_returnsSdpOnSuccess() throws Exception {
    stubActiveSession("sess1", "doc1");
    when(interviewDocumentService.contextForSession("doc1")).thenReturn("Document context");
    HttpResponse<String> response = mock(HttpResponse.class);
    when(response.statusCode()).thenReturn(200);
    when(response.body()).thenReturn("v=0\r\no=- interview answer");
    when(openAiRealtimeHttpInvoker.invoke(any())).thenReturn(response);

    assertThat(service.createInterviewCall("sess1", "v=0\r\no=offer", "no", null, "cedar", "high"))
        .isEqualTo("v=0\r\no=- interview answer");

    verify(aiBudgetService)
        .recordUsage(anyString(), eq("gpt-realtime-2"), eq(100), eq(200), eq(false), isNull(), eq("interview_realtime_session"));
  }

  @Test
  void createInterviewCall_rejectsWhenRealtimeDisabled() {
    realtimeProperties.setEnabled(false);

    assertThatThrownBy(() -> service.createInterviewCall("sess1", "v=0", "en", null, null, null))
        .isInstanceOf(RealtimeSessionException.class)
        .hasFieldOrPropertyWithValue("errorCode", RealtimeErrorCode.REALTIME_DISABLED);

    verify(openAiRealtimeHttpInvoker, never()).invoke(any());
  }

  @Test
  void createInterviewCall_rejectsMissingApiKey() {
    InterviewRealtimeSessionService svc =
        new InterviewRealtimeSessionService(
            realtimeProperties,
            aiBudgetService,
            budgetProperties,
            aiCircuitBreaker,
            openAiRealtimeHttpInvoker,
            realtimeModelCatalog,
            interviewDocumentService,
            sessionRepository,
            "  ");

    assertThatThrownBy(() -> svc.createInterviewCall("sess1", "v=0", "en", null, null, null))
        .isInstanceOf(RealtimeSessionException.class)
        .hasFieldOrPropertyWithValue("errorCode", RealtimeErrorCode.API_KEY_MISSING)
        .hasFieldOrPropertyWithValue("httpStatus", HttpStatus.SERVICE_UNAVAILABLE);
  }

  @Test
  void createInterviewCall_rejectsInactiveSession() {
    when(sessionRepository.findById("sess1"))
        .thenReturn(
            Optional.of(
                InterviewSessionEntity.builder()
                    .id("sess1")
                    .documentId("doc1")
                    .language("en")
                    .status(InterviewSessionService.STATUS_FINALIZED)
                    .startedAt(Instant.now())
                    .build()));

    assertThatThrownBy(() -> service.createInterviewCall("sess1", "v=0", "en", null, null, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("not active");
  }

  @Test
  void createInterviewCall_mapsOpenAi4xx() throws Exception {
    stubActiveSession("sess1", "doc1");
    when(interviewDocumentService.contextForSession("doc1")).thenReturn("ctx");
    HttpResponse<String> response = mock(HttpResponse.class);
    when(response.statusCode()).thenReturn(400);
    when(response.body()).thenReturn("{\"error\":\"bad sdp\"}");
    when(openAiRealtimeHttpInvoker.invoke(any())).thenReturn(response);

    assertThatThrownBy(() -> service.createInterviewCall("sess1", "v=0", "en", null, null, null))
        .isInstanceOf(RealtimeSessionException.class)
        .hasFieldOrPropertyWithValue("errorCode", RealtimeErrorCode.OPENAI_REJECTED);

    verify(aiBudgetService, never())
        .recordUsage(anyString(), anyString(), anyInt(), anyInt(), anyBoolean(), isNull(), anyString());
  }

  private void stubActiveSession(String sessionId, String documentId) {
    when(sessionRepository.findById(sessionId))
        .thenReturn(
            Optional.of(
                InterviewSessionEntity.builder()
                    .id(sessionId)
                    .documentId(documentId)
                    .language("en")
                    .status(InterviewSessionService.STATUS_ACTIVE)
                    .voice("marin")
                    .startedAt(Instant.now())
                    .build()));
  }
}
