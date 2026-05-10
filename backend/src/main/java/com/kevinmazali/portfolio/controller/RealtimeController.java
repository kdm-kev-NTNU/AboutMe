package com.kevinmazali.portfolio.controller;

import com.kevinmazali.portfolio.config.AiBudgetProperties;
import com.kevinmazali.portfolio.config.RealtimeProperties;
import com.kevinmazali.portfolio.exception.RealtimeSessionException;
import com.kevinmazali.portfolio.model.ApiError;
import com.kevinmazali.portfolio.model.ElevenLabsTokenRequest;
import com.kevinmazali.portfolio.model.ElevenLabsTokenResponse;
import com.kevinmazali.portfolio.model.RealtimeLookupRequest;
import com.kevinmazali.portfolio.model.RealtimeModelOption;
import com.kevinmazali.portfolio.model.RealtimeStatusResponse;
import com.kevinmazali.portfolio.model.VoiceTraceCompleteRequest;
import com.kevinmazali.portfolio.model.analytics.RealtimeVoiceAnalyticsContext;
import com.kevinmazali.portfolio.service.ElevenLabsRealtimeTokenService;
import com.kevinmazali.portfolio.service.PostHogLlmService;
import com.kevinmazali.portfolio.service.RealtimeLookupService;
import com.kevinmazali.portfolio.service.RealtimeModelCatalog;
import com.kevinmazali.portfolio.service.RealtimeSessionService;
import com.kevinmazali.portfolio.service.RequestLogService;
import com.kevinmazali.portfolio.util.AiRequestContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
/**
 * OpenAI Realtime voice: browser SDP exchange and availability flag.
 */
@Slf4j
@RestController
@Tag(name = "Chat", description = "RAG-backed question answering")
public class RealtimeController {

  private final RealtimeProperties realtimeProperties;
  private final RealtimeSessionService realtimeSessionService;
  private final RealtimeLookupService realtimeLookupService;
  private final RealtimeModelCatalog realtimeModelCatalog;
  private final ElevenLabsRealtimeTokenService elevenLabsRealtimeTokenService;
  private final RequestLogService requestLogService;
  private final AiBudgetProperties budgetProperties;
  @Nullable private final PostHogLlmService postHogLlmService;

  public RealtimeController(
      RealtimeProperties realtimeProperties,
      RealtimeSessionService realtimeSessionService,
      RealtimeLookupService realtimeLookupService,
      RealtimeModelCatalog realtimeModelCatalog,
      ElevenLabsRealtimeTokenService elevenLabsRealtimeTokenService,
      RequestLogService requestLogService,
      AiBudgetProperties budgetProperties,
      @Autowired(required = false) @Nullable PostHogLlmService postHogLlmService,
      @Value("${spring.ai.openai.api-key:}") String ignoredOpenAiApiKey) {
    this.realtimeProperties = realtimeProperties;
    this.realtimeSessionService = realtimeSessionService;
    this.realtimeLookupService = realtimeLookupService;
    this.realtimeModelCatalog = realtimeModelCatalog;
    this.elevenLabsRealtimeTokenService = elevenLabsRealtimeTokenService;
    this.requestLogService = requestLogService;
    this.budgetProperties = budgetProperties;
    this.postHogLlmService = postHogLlmService;
  }

  @Operation(summary = "Realtime voice available", description = "True when at least one realtime voice provider is configured.")
  @GetMapping("/realtime/status")
  public ResponseEntity<RealtimeStatusResponse> status() {
    boolean ok = realtimeModelCatalog.hasAvailableModels();
    return ResponseEntity.ok(new RealtimeStatusResponse(
        ok,
        RealtimeProperties.ALLOWED_VOICES,
        RealtimeProperties.ALLOWED_REASONING_EFFORTS,
        realtimeProperties.defaultVoice(),
        realtimeProperties.defaultReasoningEffort()));
  }

  @Operation(summary = "List realtime voice models", description = "Configured voice provider/model options.")
  @GetMapping("/realtime/models")
  public ResponseEntity<List<RealtimeModelOption>> models() {
    return ResponseEntity.ok(realtimeModelCatalog.listAvailableModels());
  }

  @Operation(summary = "Create Realtime WebRTC session", description = "POST SDP offer as raw body; returns SDP answer for RTCPeerConnection.")
  // Do not constrain `produces` to application/sdp: error paths return JSON `ApiError` with the correct Content-Type set by Spring.
  @PostMapping(value = "/realtime/session", consumes = { "application/sdp", "text/plain" })
  public ResponseEntity<?> createSession(
      @RequestBody String sdp,
      @RequestHeader(value = "X-Chat-Language", required = false) String chatLanguage,
      @RequestHeader(value = "X-Realtime-Model", required = false) String model,
      @RequestHeader(value = "X-Realtime-Voice", required = false) String voice,
      @RequestHeader(value = "X-Realtime-Reasoning-Effort", required = false) String reasoningEffort,
      @RequestHeader(value = RealtimeVoiceAnalyticsContext.HEADER_AI_TRACE_ID, required = false)
          String aiTraceId,
      @RequestHeader(value = RealtimeVoiceAnalyticsContext.HEADER_POSTHOG_SESSION_ID, required = false)
          String posthogSessionId) {
    if (!realtimeProperties.isEnabled()) {
      return ResponseEntity.status(503)
          .body(new ApiError("Voice chat is disabled.", "REALTIME_DISABLED"));
    }
    if (!realtimeProperties.isAllowedVoice(voice)) {
      return ResponseEntity.badRequest().body(new ApiError("Unsupported realtime voice.", "BAD_REQUEST"));
    }
    if (!realtimeProperties.isAllowedReasoningEffort(reasoningEffort)) {
      return ResponseEntity.badRequest().body(new ApiError("Unsupported realtime reasoning effort.", "BAD_REQUEST"));
    }
    requestLogService.save("/realtime/session", "POST", "sdp-bytes", null);
    RealtimeVoiceAnalyticsContext voiceAnalytics =
        RealtimeVoiceAnalyticsContext.fromHeaders(aiTraceId, posthogSessionId);
    try {
      String answer =
          realtimeSessionService.createRealtimeCall(
              sdp, chatLanguage, model, voice, reasoningEffort, voiceAnalytics);
      return ResponseEntity.ok().contentType(MediaType.parseMediaType("application/sdp")).body(answer);
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().body(new ApiError(e.getMessage(), "BAD_REQUEST"));
    } catch (RealtimeSessionException e) {
      log.warn(
          "realtime session: code={} status={} message={}",
          e.getErrorCode(),
          e.getHttpStatus().value(),
          e.getMessage());
      return ResponseEntity.status(e.getHttpStatus())
          .body(new ApiError(e.getMessage(), e.getErrorCode().name()));
    } catch (IllegalStateException e) {
      log.warn("realtime session (legacy): {}", e.getMessage());
      return ResponseEntity.status(503).body(new ApiError(e.getMessage()));
    }
  }

  @Operation(summary = "Create ElevenLabs WebRTC token", description = "Returns a browser-safe token for a configured ElevenLabs agent.")
  @PostMapping(value = "/realtime/elevenlabs/token", consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<?> createElevenLabsToken(
      @RequestBody(required = false) ElevenLabsTokenRequest request,
      @RequestHeader(value = RealtimeVoiceAnalyticsContext.HEADER_AI_TRACE_ID, required = false)
          String aiTraceId,
      @RequestHeader(value = RealtimeVoiceAnalyticsContext.HEADER_POSTHOG_SESSION_ID, required = false)
          String posthogSessionId) {
    if (!realtimeProperties.isEnabled()) {
      return ResponseEntity.status(503)
          .body(new ApiError("Voice chat is disabled.", "REALTIME_DISABLED"));
    }
    RealtimeVoiceAnalyticsContext voiceAnalytics =
        RealtimeVoiceAnalyticsContext.fromHeaders(aiTraceId, posthogSessionId);
    try {
      String token =
          elevenLabsRealtimeTokenService.createConversationToken(
              request == null ? null : request.modelId(), voiceAnalytics);
      requestLogService.save("/realtime/elevenlabs/token", "POST", "token", null);
      return ResponseEntity.ok(new ElevenLabsTokenResponse(token));
    } catch (RealtimeSessionException e) {
      log.warn(
          "elevenlabs realtime token: code={} status={} message={}",
          e.getErrorCode(),
          e.getHttpStatus().value(),
          e.getMessage());
      return ResponseEntity.status(e.getHttpStatus())
          .body(new ApiError(e.getMessage(), e.getErrorCode().name()));
    }
  }

  @Operation(
      summary = "Lookup public voice facts",
      description = "Returns short snippets for the Realtime voice assistant; not a full RAG answer.")
  @PostMapping(value = "/realtime/lookup", consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<?> lookup(
      @RequestBody(required = false) RealtimeLookupRequest request,
      @RequestHeader(value = RealtimeVoiceAnalyticsContext.HEADER_AI_TRACE_ID, required = false)
          String aiTraceId,
      @RequestHeader(value = RealtimeVoiceAnalyticsContext.HEADER_POSTHOG_SESSION_ID, required = false)
          String posthogSessionId) {
    if (!realtimeProperties.isEnabled()) {
      return ResponseEntity.status(503)
          .body(new ApiError("Voice chat is disabled.", "REALTIME_DISABLED"));
    }
    RealtimeVoiceAnalyticsContext voiceAnalytics =
        RealtimeVoiceAnalyticsContext.fromHeaders(aiTraceId, posthogSessionId);
    try {
      String query = request == null ? null : request.query();
      String language = request == null ? null : request.language();
      return ResponseEntity.ok(realtimeLookupService.lookup(query, language, voiceAnalytics));
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().body(new ApiError(e.getMessage(), "BAD_REQUEST"));
    }
  }

  @Operation(
      summary = "Complete voice PostHog trace",
      description = "Browser beacon to emit {@code $ai_trace} when a voice session ends.")
  @PostMapping(value = "/realtime/analytics/voice-trace", consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<?> completeVoiceTrace(@RequestBody(required = false) VoiceTraceCompleteRequest body) {
    if (!realtimeProperties.isEnabled()) {
      return ResponseEntity.status(503)
          .body(new ApiError("Voice chat is disabled.", "REALTIME_DISABLED"));
    }
    PostHogLlmService ph = postHogLlmService;
    if (ph == null || !ph.isEnabled()) {
      return ResponseEntity.noContent().build();
    }
    if (body == null || body.traceId() == null || body.traceId().isBlank()) {
      return ResponseEntity.badRequest().body(new ApiError("traceId is required.", "BAD_REQUEST"));
    }
    String traceId = RealtimeVoiceAnalyticsContext.parseTraceId(body.traceId());
    if (traceId == null) {
      return ResponseEntity.badRequest().body(new ApiError("traceId must be a valid UUID.", "BAD_REQUEST"));
    }
    String sessionId = RealtimeVoiceAnalyticsContext.sanitizePosthogSessionId(body.sessionId());
    double duration = body.durationSeconds() != null ? body.durationSeconds() : 0;
    if (Double.isNaN(duration) || duration < 0) {
      duration = 0;
    }
    boolean failed = body.error() != null && body.error();
    String errMsg = body.errorMessage();
    if (errMsg != null && errMsg.length() > 4000) {
      errMsg = errMsg.substring(0, 4000);
    }
    String distinctId = AiRequestContext.budgetUserIdentifier(budgetProperties);
    boolean anonymous = AiRequestContext.isAnonymousInteractiveUser();
    ph.captureTraceAsync(
        distinctId,
        traceId,
        sessionId,
        "voice_session",
        duration,
        failed,
        errMsg,
        anonymous);
    return ResponseEntity.accepted().build();
  }
}
