package com.kevinmazali.portfolio.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.nullable;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kevinmazali.portfolio.config.AiBudgetProperties;
import com.kevinmazali.portfolio.config.RealtimeProperties;
import com.kevinmazali.portfolio.exception.RealtimeErrorCode;
import com.kevinmazali.portfolio.exception.RealtimeSessionException;
import com.kevinmazali.portfolio.util.AiRequestContext;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RealtimeSessionServiceTest {

  private static final URI OPENAI_CALLS = URI.create("https://api.openai.com/v1/realtime/calls");

  @Mock private AiBudgetService aiBudgetService;

  @Mock private AiCircuitBreaker aiCircuitBreaker;

  @Mock private OpenAiRealtimeHttpInvoker openAiRealtimeHttpInvoker;

  @Mock private RealtimeModelCatalog realtimeModelCatalog;

  private AiBudgetProperties budgetProperties;
  private RealtimeProperties realtimeProperties;
  private RealtimeSessionService service;
  private final ObjectMapper mapper = new ObjectMapper();

  @BeforeEach
  void setUp() {
    realtimeProperties = new RealtimeProperties();
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
        new RealtimeSessionService(
            realtimeProperties,
            aiBudgetService,
            budgetProperties,
            aiCircuitBreaker,
            openAiRealtimeHttpInvoker,
            new RealtimeProfileService(),
            realtimeModelCatalog,
            "sk-test-openai-key");
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void summarizeOpenAiErrorBody_extractsMessageAndCode() {
    String body =
        "{\"error\":{\"message\":\"Invalid model\",\"type\":\"invalid_request_error\",\"code\":\"model_not_found\"}}";
    assertThat(service.summarizeOpenAiErrorBody(body)).contains("Invalid model").contains("model_not_found");
  }

  @Test
  void createRealtimeCall_returnsSdpOnSuccess_recordsUsage_invokesAssertions() throws Exception {
    String answer = "v=0\r\no=- realtime answer";
    stubUpstream(200, answer);

    assertThat(service.createRealtimeCall("v=0\r\no=offer", "en")).isEqualTo(answer);

    verify(aiCircuitBreaker).assertClosed();
    verify(aiBudgetService).assertWithinBudget(eq("anon:unknown"), eq(true));

    verify(aiBudgetService)
        .recordUsage(
            eq("anon:unknown"),
            eq("gpt-realtime-2"),
            eq(100),
            eq(200),
            eq(true),
            isNull(),
            eq("realtime_voice_session"));
  }

  @Test
  void createRealtimeCall_rejectsBlankSdp() throws Exception {

    assertThatThrownBy(() -> service.createRealtimeCall("", "en"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("SDP");

    assertThatThrownBy(() -> service.createRealtimeCall(null, "en"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("SDP");

    assertThatThrownBy(() -> service.createRealtimeCall("   ", "no"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("SDP");

    verify(openAiRealtimeHttpInvoker, never()).post(any(), any(), any());
  }

  @Test
  void createRealtimeCall_rejectsMissingApiKey() throws Exception {

    RealtimeSessionService svc =
        new RealtimeSessionService(
            realtimeProperties,
            aiBudgetService,
            budgetProperties,
            aiCircuitBreaker,
            openAiRealtimeHttpInvoker,
            new RealtimeProfileService(),
            realtimeModelCatalog,
            "  ");

    assertThatThrownBy(() -> svc.createRealtimeCall("v=0", "en"))
        .isInstanceOf(RealtimeSessionException.class)
        .hasFieldOrPropertyWithValue("errorCode", RealtimeErrorCode.API_KEY_MISSING)
        .hasFieldOrPropertyWithValue("httpStatus", HttpStatus.SERVICE_UNAVAILABLE);

    verify(openAiRealtimeHttpInvoker, never()).post(any(), any(), any());
  }

  @Test
  void createRealtimeCall_mapsHttp4xxFromOpenAi() throws Exception {
    stubUpstream(
        400,
        "{\"error\":{\"message\":\"bad sdp\",\"type\":\"invalid_request_error\",\"code\":\"invalid_value\"}}");

    assertThatThrownBy(() -> service.createRealtimeCall("v=0\r\no=x", null))
        .isInstanceOf(RealtimeSessionException.class)
        .hasFieldOrPropertyWithValue("httpStatus", HttpStatus.BAD_GATEWAY)
        .hasFieldOrPropertyWithValue("errorCode", RealtimeErrorCode.OPENAI_REJECTED)
        .hasMessageContaining("bad sdp");

    verify(aiBudgetService, never())
        .recordUsage(anyString(), anyString(), anyInt(), anyInt(), anyBoolean(), nullable(Double.class), anyString());
  }

  @Test
  void createRealtimeCall_mapsHttp5xxFromOpenAi() throws Exception {
    stubUpstream(503, "{}");

    assertThatThrownBy(() -> service.createRealtimeCall("v=0\r\no=x", null))
        .isInstanceOf(RealtimeSessionException.class)
        .hasFieldOrPropertyWithValue("errorCode", RealtimeErrorCode.OPENAI_SERVER_ERROR);

    verify(aiBudgetService, never())
        .recordUsage(anyString(), anyString(), anyInt(), anyInt(), anyBoolean(), nullable(Double.class), anyString());
  }

  /**
   * The transport reports "no response at all" as {@link IOException}, and that must surface to the
   * SPA as OPENAI_UNREACHABLE. This is the exact path that was live in production when voice broke:
   * the backend could not open a connection to api.openai.com.
   */
  @Test
  void createRealtimeCall_mapsTransportFailureToUnreachable() throws Exception {
    when(openAiRealtimeHttpInvoker.post(any(), any(), any()))
        .thenThrow(new IOException("Network is unreachable"));

    assertThatThrownBy(() -> service.createRealtimeCall("v=0\r\no=x", "en"))
        .isInstanceOf(RealtimeSessionException.class)
        .hasFieldOrPropertyWithValue("errorCode", RealtimeErrorCode.OPENAI_UNREACHABLE)
        .hasMessageContaining("Network is unreachable")
        .hasCauseInstanceOf(IOException.class);

    verify(aiBudgetService, never())
        .recordUsage(anyString(), anyString(), anyInt(), anyInt(), anyBoolean(), nullable(Double.class), anyString());
  }

  @Test
  void createRealtimeCall_includesMultipartSdpOffer() throws Exception {
    ArgumentCaptor<byte[]> body = bodyCaptor();
    when(openAiRealtimeHttpInvoker.post(any(), any(), body.capture()))
        .thenReturn(new OpenAiRealtimeHttpInvoker.Response(200, "ok"));

    service.createRealtimeCall("v=0\r\nCUSTOM_OFFER_MARK", null);

    String raw = utf8(body.getValue());
    assertThat(raw).contains("CUSTOM_OFFER_MARK");
    assertThat(raw).contains("name=\"sdp\"");
    assertThat(raw).contains("name=\"session\"");
  }

  @Test
  void createRealtimeCall_multipartUsesCrlfBeforeBoundary_afterLfOnlySdp() throws Exception {
    ArgumentCaptor<byte[]> body = bodyCaptor();
    when(openAiRealtimeHttpInvoker.post(any(), any(), body.capture()))
        .thenReturn(new OpenAiRealtimeHttpInvoker.Response(200, "ok"));

    service.createRealtimeCall("v=0\no=LF_ONLY_TERMINATOR\n", null);

    String raw = utf8(body.getValue());
    assertThat(raw).contains("o=LF_ONLY_TERMINATOR\r\n");
    int marker = raw.indexOf("o=LF_ONLY_TERMINATOR");
    assertThat(marker).isGreaterThan(0);
    assertThat(raw.substring(marker + "o=LF_ONLY_TERMINATOR".length())).startsWith("\r\n\r\n--");
  }

  @Test
  void createRealtimeCall_usesNorwegianInstructionsForNb() throws Exception {
    ArgumentCaptor<byte[]> body = bodyCaptor();
    when(openAiRealtimeHttpInvoker.post(any(), any(), body.capture()))
        .thenReturn(new OpenAiRealtimeHttpInvoker.Response(200, "ok"));

    service.createRealtimeCall("v=0", " NB ");

    JsonNode json = extractSessionJson(body.getValue());

    assertThat(json.get("instructions").asText()).contains("Du er en hjelpsom");
    assertThat(json.get("instructions").asText()).contains("Kevin studerer dataingeniør ved NTNU");
    assertThat(json.get("model").asText()).isEqualTo("gpt-realtime-2");
    assertThat(json.get("max_output_tokens").asInt()).isEqualTo(512);
    assertThat(json.at("/tools/0/type").asText()).isEqualTo("function");
    assertThat(json.at("/tools/0/name").asText()).isEqualTo("lookup_kevin_info");
    assertThat(json.at("/tools/0/parameters/properties/query/type").asText()).isEqualTo("string");
    assertThat(json.at("/tool_choice").asText()).isEqualTo("auto");
    assertThat(json.at("/reasoning/effort").asText()).isEqualTo("low");
    assertThat(json.at("/audio/output/voice").asText()).isEqualTo("marin");
    assertThat(json.at("/audio/input/transcription/model").asText()).isEqualTo("whisper-1");
    assertThat(json.at("/audio/input/turn_detection/type").asText()).isEqualTo("semantic_vad");
    assertThat(json.at("/audio/input/turn_detection/eagerness").asText()).isEqualTo("medium");
    assertThat(json.at("/audio/input/turn_detection/create_response").asBoolean()).isTrue();
    assertThat(json.at("/audio/input/turn_detection/interrupt_response").asBoolean()).isTrue();
    assertThat(json.at("/output_modalities").toString()).contains("audio");
    assertThat(json.has("reasoning_effort")).isFalse();
    assertThat(json.has("max_response_output_tokens")).isFalse();
    assertThat(json.has("modalities")).isFalse();
  }

  @Test
  void createRealtimeCall_usesRequestedCuratedVoiceAndReasoningEffort() throws Exception {
    ArgumentCaptor<byte[]> body = bodyCaptor();
    when(openAiRealtimeHttpInvoker.post(any(), any(), body.capture()))
        .thenReturn(new OpenAiRealtimeHttpInvoker.Response(200, "ok"));

    service.createRealtimeCall("v=0", "en", null, "cedar", "high", "high");

    JsonNode json = extractSessionJson(body.getValue());
    assertThat(json.at("/audio/output/voice").asText()).isEqualTo("cedar");
    assertThat(json.at("/reasoning/effort").asText()).isEqualTo("high");
    assertThat(json.at("/audio/input/turn_detection/eagerness").asText()).isEqualTo("high");
  }

  @Test
  void createRealtimeCall_defaultsToEnglishForUnsupportedLanguageHeaders() throws Exception {
    ArgumentCaptor<URI> uri = ArgumentCaptor.forClass(URI.class);
    ArgumentCaptor<Map<String, String>> headers = headerCaptor();
    ArgumentCaptor<byte[]> body = bodyCaptor();
    when(openAiRealtimeHttpInvoker.post(uri.capture(), headers.capture(), body.capture()))
        .thenReturn(new OpenAiRealtimeHttpInvoker.Response(200, "ok"));

    service.createRealtimeCall("v=0", "fr");

    JsonNode json = extractSessionJson(body.getValue());
    assertThat(json.get("instructions").asText()).contains("portfolio website").contains("third person");

    assertThat(uri.getValue()).isEqualTo(OPENAI_CALLS);
    assertThat(headers.getValue()).containsEntry("Authorization", "Bearer sk-test-openai-key");
    assertThat(headers.getValue().get("Content-Type")).startsWith("multipart/form-data; boundary=");

    verify(aiCircuitBreaker).assertClosed();
    verify(aiBudgetService).assertWithinBudget(anyString(), anyBoolean());
  }

  @Test
  void createRealtimeCall_hashesBudgetUserIdForOpenAiSafetyHeader() throws Exception {
    String username = "u".repeat(200);
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(username, "pw", List.of(new SimpleGrantedAuthority("ROLE_USER"))));

    ArgumentCaptor<Map<String, String>> headers = headerCaptor();
    when(openAiRealtimeHttpInvoker.post(any(), headers.capture(), any()))
        .thenReturn(new OpenAiRealtimeHttpInvoker.Response(200, "ok"));

    service.createRealtimeCall("v=0\r\noffer", null);

    String budgetId = "user:" + username;
    String safety = headers.getValue().get("OpenAI-Safety-Identifier");
    assertThat(safety).hasSize(64).matches("[0-9a-f]{64}").isEqualTo(AiRequestContext.openAiSafetyIdentifier(budgetId));
    verify(aiBudgetService).assertWithinBudget(eq(budgetId), eq(false));
  }

  private void stubUpstream(int status, String body) throws IOException {
    when(openAiRealtimeHttpInvoker.post(any(), any(), any()))
        .thenReturn(new OpenAiRealtimeHttpInvoker.Response(status, body));
  }

  private static ArgumentCaptor<byte[]> bodyCaptor() {
    return ArgumentCaptor.forClass(byte[].class);
  }

  @SuppressWarnings("unchecked")
  private static ArgumentCaptor<Map<String, String>> headerCaptor() {
    return ArgumentCaptor.forClass(Map.class);
  }

  private static String utf8(byte[] body) {
    return new String(body, StandardCharsets.UTF_8);
  }

  private JsonNode extractSessionJson(byte[] body) throws IOException {
    String raw = utf8(body);
    int sessionField = raw.indexOf("name=\"session\"");
    assertThat(sessionField).isGreaterThanOrEqualTo(0);
    int jsonStart = raw.indexOf('{', sessionField);
    assertThat(jsonStart).isGreaterThanOrEqualTo(0);
    int jsonEndExclusive = raw.indexOf("\r\n--", jsonStart);
    assertThat(jsonEndExclusive).isGreaterThan(jsonStart);
    return mapper.readTree(raw.substring(jsonStart, jsonEndExclusive).trim());
  }
}
