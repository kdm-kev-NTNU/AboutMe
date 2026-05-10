package com.kevinmazali.portfolio.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kevinmazali.portfolio.config.AiBudgetProperties;
import com.kevinmazali.portfolio.config.RealtimeProperties;
import com.kevinmazali.portfolio.exception.RealtimeErrorCode;
import com.kevinmazali.portfolio.exception.RealtimeSessionException;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class ElevenLabsRealtimeTokenServiceTest {

  @Mock private AiBudgetService aiBudgetService;
  @Mock private AiCircuitBreaker aiCircuitBreaker;
  @Mock private ElevenLabsRealtimeHttpInvoker elevenLabsRealtimeHttpInvoker;

  private ElevenLabsRealtimeTokenService service;
  private RealtimeProperties props;

  @BeforeEach
  void setUp() {
    props = new RealtimeProperties();
    props.setEnabled(true);
    props.getProviders().getElevenlabs().setEnabled(true);
    props.getProviders().getElevenlabs().setApiKey("xi-test");
    RealtimeProperties.ElevenLabsAgent agent = new RealtimeProperties.ElevenLabsAgent();
    agent.setAgentId("agent_123");
    agent.setEnvironment("staging");
    agent.setBranchId("branch_1");
    agent.setDefaultAgent(true);
    props.getProviders().getElevenlabs().setAgents(List.of(agent));

    AiBudgetProperties budgetProperties = new AiBudgetProperties();
    budgetProperties.setAnonIdentitySalt("test-salt");
    budgetProperties.setEnabled(false);

    service = new ElevenLabsRealtimeTokenService(
        props,
        new RealtimeModelCatalog(props, ""),
        budgetProperties,
        aiBudgetService,
        aiCircuitBreaker,
        elevenLabsRealtimeHttpInvoker);
  }

  @Test
  void createConversationTokenReturnsTokenAndRecordsZeroCostUsage() throws Exception {
    HttpResponse<String> response = mock(HttpResponse.class);
    when(response.statusCode()).thenReturn(200);
    when(response.body()).thenReturn("{\"token\":\"token_abc\"}");
    ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
    when(elevenLabsRealtimeHttpInvoker.invoke(captor.capture())).thenReturn(response);

    assertThat(service.createConversationToken("agent_123")).isEqualTo("token_abc");

    HttpRequest request = captor.getValue();
    assertThat(request.uri()).isEqualTo(URI.create(
        "https://api.elevenlabs.io/v1/convai/conversation/token?agent_id=agent_123&environment=staging&branch_id=branch_1"));
    assertThat(request.headers().firstValue("xi-api-key")).hasValue("xi-test");
    verify(aiCircuitBreaker).assertClosed();
    verify(aiBudgetService).assertWithinBudget(eq("anon:unknown"), eq(true));
    verify(aiBudgetService).recordUsage(
        eq("anon:unknown"),
        eq("elevenlabs:agent_123"),
        eq(0),
        eq(0),
        eq(true),
        isNull(),
        eq("elevenlabs_voice_session"));
  }

  @Test
  void createConversationTokenRejectsUnknownAgentBeforeNetwork() throws Exception {
    assertThatThrownBy(() -> service.createConversationToken("missing"))
        .isInstanceOf(RealtimeSessionException.class)
        .hasFieldOrPropertyWithValue("errorCode", RealtimeErrorCode.VOICE_MODEL_NOT_CONFIGURED)
        .hasFieldOrPropertyWithValue("httpStatus", HttpStatus.BAD_REQUEST);

    verify(elevenLabsRealtimeHttpInvoker, never()).invoke(any());
  }

  @Test
  void createConversationTokenRejectsMissingApiKeyBeforeNetwork() throws Exception {
    props.getProviders().getElevenlabs().setApiKey(" ");

    assertThatThrownBy(() -> service.createConversationToken("agent_123"))
        .isInstanceOf(RealtimeSessionException.class)
        .hasFieldOrPropertyWithValue("errorCode", RealtimeErrorCode.API_KEY_MISSING)
        .hasFieldOrPropertyWithValue("httpStatus", HttpStatus.SERVICE_UNAVAILABLE);

    verify(elevenLabsRealtimeHttpInvoker, never()).invoke(any());
  }

  @Test
  void createConversationTokenMapsUpstream4xx() throws Exception {
    HttpResponse<String> response = mock(HttpResponse.class);
    when(response.statusCode()).thenReturn(422);
    when(response.body()).thenReturn("{\"detail\":\"invalid agent\"}");
    when(elevenLabsRealtimeHttpInvoker.invoke(any())).thenReturn(response);

    assertThatThrownBy(() -> service.createConversationToken("agent_123"))
        .isInstanceOf(RealtimeSessionException.class)
        .hasFieldOrPropertyWithValue("errorCode", RealtimeErrorCode.ELEVENLABS_REJECTED)
        .hasMessageContaining("invalid agent");
  }

  @Test
  void createConversationTokenMapsNetworkFailure() throws Exception {
    when(elevenLabsRealtimeHttpInvoker.invoke(any())).thenThrow(new IOException("reset"));

    assertThatThrownBy(() -> service.createConversationToken("agent_123"))
        .isInstanceOf(RealtimeSessionException.class)
        .hasFieldOrPropertyWithValue("errorCode", RealtimeErrorCode.ELEVENLABS_UNREACHABLE)
        .hasCauseInstanceOf(IOException.class);
  }
}
