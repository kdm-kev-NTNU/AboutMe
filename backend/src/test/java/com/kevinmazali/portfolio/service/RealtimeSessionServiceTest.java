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
import static org.mockito.Mockito.mock;
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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;
import java.util.concurrent.Flow.Subscription;
import java.util.concurrent.TimeUnit;
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

  @Mock private AiBudgetService aiBudgetService;

  @Mock private AiCircuitBreaker aiCircuitBreaker;

  @Mock private OpenAiRealtimeHttpInvoker openAiRealtimeHttpInvoker;

  private AiBudgetProperties budgetProperties;
  private RealtimeProperties realtimeProperties;
  private RealtimeSessionService service;
  private final ObjectMapper mapper = new ObjectMapper();

  @BeforeEach
  void setUp() {
    // clear interrupt flag leaked from interrupt tests on some JVM/thread pool setups
    // noinspection ResultOfMethodCallIgnored
    Thread.interrupted();

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

    service =
        new RealtimeSessionService(
            realtimeProperties,
            aiBudgetService,
            budgetProperties,
            aiCircuitBreaker,
            openAiRealtimeHttpInvoker,
            mapper,
            "sk-test-openai-key");
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
    // noinspection ResultOfMethodCallIgnored
    Thread.interrupted();
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
    HttpResponse<String> response = mock(HttpResponse.class);
    when(response.statusCode()).thenReturn(200);
    when(response.body()).thenReturn(answer);
    when(openAiRealtimeHttpInvoker.invoke(any())).thenReturn(response);

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

    verify(openAiRealtimeHttpInvoker, never()).invoke(any());
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
            mapper,
            "  ");

    assertThatThrownBy(() -> svc.createRealtimeCall("v=0", "en"))
        .isInstanceOf(RealtimeSessionException.class)
        .hasFieldOrPropertyWithValue("errorCode", RealtimeErrorCode.API_KEY_MISSING)
        .hasFieldOrPropertyWithValue("httpStatus", HttpStatus.SERVICE_UNAVAILABLE);

    verify(openAiRealtimeHttpInvoker, never()).invoke(any());
  }

  @Test
  void createRealtimeCall_mapsHttp4xxFromOpenAi() throws Exception {
    HttpResponse<String> response = mock(HttpResponse.class);
    when(response.statusCode()).thenReturn(400);
    when(response.body())
        .thenReturn(
            "{\"error\":{\"message\":\"bad sdp\",\"type\":\"invalid_request_error\",\"code\":\"invalid_value\"}}");
    when(openAiRealtimeHttpInvoker.invoke(any())).thenReturn(response);

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
    HttpResponse<String> response = mock(HttpResponse.class);
    when(response.statusCode()).thenReturn(503);
    when(response.body()).thenReturn("{}");
    when(openAiRealtimeHttpInvoker.invoke(any())).thenReturn(response);

    assertThatThrownBy(() -> service.createRealtimeCall("v=0\r\no=x", null))
        .isInstanceOf(RealtimeSessionException.class)
        .hasFieldOrPropertyWithValue("errorCode", RealtimeErrorCode.OPENAI_SERVER_ERROR);

    verify(aiBudgetService, never())
        .recordUsage(anyString(), anyString(), anyInt(), anyInt(), anyBoolean(), nullable(Double.class), anyString());
  }

  @Test
  void createRealtimeCall_mapsIoExceptionFromInvoker() throws Exception {
    when(openAiRealtimeHttpInvoker.invoke(any())).thenThrow(new IOException("reset"));

    assertThatThrownBy(() -> service.createRealtimeCall("v=0\r\no=x", "en"))
        .isInstanceOf(RealtimeSessionException.class)
        .hasFieldOrPropertyWithValue("errorCode", RealtimeErrorCode.OPENAI_UNREACHABLE)
        .hasCauseInstanceOf(IOException.class);
  }

  @Test
  void createRealtimeCall_restoresInterruptOnInterruptedInvoker() throws Exception {
    when(openAiRealtimeHttpInvoker.invoke(any())).thenThrow(new InterruptedException("boom"));

    assertThatThrownBy(() -> service.createRealtimeCall("v=0\r\no=x", "en"))
        .isInstanceOf(RealtimeSessionException.class)
        .hasFieldOrPropertyWithValue("errorCode", RealtimeErrorCode.OPENAI_UNREACHABLE)
        .hasCauseInstanceOf(InterruptedException.class);

    assertThat(Thread.interrupted()).isTrue();
  }

  @Test
  void createRealtimeCall_includesMultipartSdpOffer() throws Exception {
    HttpResponse<String> response = mock(HttpResponse.class);
    when(response.statusCode()).thenReturn(200);
    when(response.body()).thenReturn("ok");
    ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
    when(openAiRealtimeHttpInvoker.invoke(captor.capture())).thenReturn(response);

    service.createRealtimeCall("v=0\r\nCUSTOM_OFFER_MARK", null);

    String raw = utf8Drain(captor.getValue());
    assertThat(raw).contains("CUSTOM_OFFER_MARK");
    assertThat(raw).contains("name=\"sdp\"");
    assertThat(raw).contains("name=\"session\"");
  }

  @Test
  void createRealtimeCall_multipartUsesCrlfBeforeBoundary_afterLfOnlySdp() throws Exception {
    HttpResponse<String> response = mock(HttpResponse.class);
    when(response.statusCode()).thenReturn(200);
    when(response.body()).thenReturn("ok");
    ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
    when(openAiRealtimeHttpInvoker.invoke(captor.capture())).thenReturn(response);

    String sdpLfOnly = "v=0\no=LF_ONLY_TERMINATOR\n";
    service.createRealtimeCall(sdpLfOnly, null);

    String raw = utf8Drain(captor.getValue());
    assertThat(raw).contains("o=LF_ONLY_TERMINATOR\r\n");
    int marker = raw.indexOf("o=LF_ONLY_TERMINATOR");
    assertThat(marker).isGreaterThan(0);
    String afterOfferLine =
        raw.substring(marker + "o=LF_ONLY_TERMINATOR".length());
    assertThat(afterOfferLine).startsWith("\r\n\r\n--");
  }

  @Test
  void createRealtimeCall_usesNorwegianInstructionsForNb() throws Exception {
    HttpResponse<String> response = mock(HttpResponse.class);
    when(response.statusCode()).thenReturn(200);
    when(response.body()).thenReturn("ok");
    ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
    when(openAiRealtimeHttpInvoker.invoke(captor.capture())).thenReturn(response);

    service.createRealtimeCall("v=0", " NB ");

    JsonNode json = extractSessionJson(captor.getValue());

    assertThat(json.get("instructions").asText()).contains("Du er en hjelpsom");
    assertThat(json.get("model").asText()).isEqualTo("gpt-realtime-2");
    assertThat(json.get("reasoning_effort").asText()).isEqualTo("low");
    assertThat(json.get("max_response_output_tokens").asInt()).isEqualTo(512);
    assertThat(json.at("/audio/output/voice").asText()).isEqualTo("marin");
    assertThat(json.at("/input_audio_transcription/model").asText()).isEqualTo("whisper-1");
    assertThat(json.at("/modalities").toString()).contains("text").contains("audio");
  }

  @Test
  void createRealtimeCall_defaultsToEnglishForUnsupportedLanguageHeaders() throws Exception {
    HttpResponse<String> response = mock(HttpResponse.class);
    when(response.statusCode()).thenReturn(200);
    when(response.body()).thenReturn("ok");
    ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
    when(openAiRealtimeHttpInvoker.invoke(captor.capture())).thenReturn(response);

    service.createRealtimeCall("v=0", "fr");

    JsonNode json = extractSessionJson(captor.getValue());
    assertThat(json.get("instructions").asText()).contains("portfolio website").contains("third person");

    HttpRequest req = captor.getValue();
    assertThat(req.uri()).isEqualTo(URI.create("https://api.openai.com/v1/realtime/calls"));
    assertThat(req.headers().firstValue("Authorization")).hasValue("Bearer sk-test-openai-key");

    verify(aiCircuitBreaker).assertClosed();
    verify(aiBudgetService).assertWithinBudget(anyString(), anyBoolean());
  }

  @Test
  void createRealtimeCall_truncatesOpenAiSafetyHeaderForLongAuthenticatedUser() throws Exception {
    String username = "u".repeat(200);
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(username, "pw", List.of(new SimpleGrantedAuthority("ROLE_USER"))));

    HttpResponse<String> response = mock(HttpResponse.class);
    when(response.statusCode()).thenReturn(200);
    when(response.body()).thenReturn("ok");
    ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
    when(openAiRealtimeHttpInvoker.invoke(captor.capture())).thenReturn(response);

    service.createRealtimeCall("v=0\r\noffer", null);

    String safety = captor.getValue().headers().firstValue("OpenAI-Safety-Identifier").orElseThrow();
    assertThat(safety).hasSize(64);
    // "user:" (5 chars) plus 59 "u"s = first 64 characters of identifier
    assertThat(safety).isEqualTo("user:" + "u".repeat(59));
    verify(aiBudgetService).assertWithinBudget(eq("user:" + username), eq(false));
  }

  static String utf8Drain(HttpRequest request) throws Exception {
    Flow.Publisher<ByteBuffer> publisher = request.bodyPublisher().orElseThrow();

    CompletableFuture<ByteArrayOutputStream> done = new CompletableFuture<>();
    publisher.subscribe(
        new Flow.Subscriber<>() {
          private Subscription subscription;
          final ByteArrayOutputStream out = new ByteArrayOutputStream();

          @Override
          public void onSubscribe(final Subscription subscription) {
            this.subscription = subscription;
            subscription.request(Long.MAX_VALUE);
          }

          @Override
          public void onNext(final ByteBuffer b) {
            while (b.hasRemaining()) {
              out.write(b.get());
            }
          }

          @Override
          public void onError(final Throwable throwable) {
            done.completeExceptionally(throwable);
          }

          @Override
          public void onComplete() {
            done.complete(out);
          }
        });

    ByteArrayOutputStream bos = done.get(5, TimeUnit.SECONDS);
    return bos.toString(StandardCharsets.UTF_8);
  }

  private JsonNode extractSessionJson(HttpRequest request) throws IOException {
    try {
      String raw = utf8Drain(request);
      int sessionField = raw.indexOf("name=\"session\"");
      assertThat(sessionField).isGreaterThanOrEqualTo(0);
      int jsonStart = raw.indexOf('{', sessionField);
      assertThat(jsonStart).isGreaterThanOrEqualTo(0);
      int jsonEndExclusive = raw.indexOf("\r\n--", jsonStart);
      assertThat(jsonEndExclusive).isGreaterThan(jsonStart);
      String jsonSlice = raw.substring(jsonStart, jsonEndExclusive).trim();
      return mapper.readTree(jsonSlice);
    } catch (Exception e) {
      throw new IOException(e);
    }
  }
}
