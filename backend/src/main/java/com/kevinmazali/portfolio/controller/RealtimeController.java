package com.kevinmazali.portfolio.controller;

import com.kevinmazali.portfolio.config.RealtimeProperties;
import com.kevinmazali.portfolio.exception.RealtimeSessionException;
import com.kevinmazali.portfolio.model.ApiError;
import com.kevinmazali.portfolio.model.ElevenLabsTokenRequest;
import com.kevinmazali.portfolio.model.ElevenLabsTokenResponse;
import com.kevinmazali.portfolio.model.RealtimeLookupRequest;
import com.kevinmazali.portfolio.model.RealtimeModelOption;
import com.kevinmazali.portfolio.model.RealtimeStatusResponse;
import com.kevinmazali.portfolio.service.ElevenLabsRealtimeTokenService;
import com.kevinmazali.portfolio.service.RealtimeLookupService;
import com.kevinmazali.portfolio.service.RealtimeModelCatalog;
import com.kevinmazali.portfolio.service.RealtimeSessionService;
import com.kevinmazali.portfolio.service.RequestLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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

  public RealtimeController(
      RealtimeProperties realtimeProperties,
      RealtimeSessionService realtimeSessionService,
      RealtimeLookupService realtimeLookupService,
      RealtimeModelCatalog realtimeModelCatalog,
      ElevenLabsRealtimeTokenService elevenLabsRealtimeTokenService,
      RequestLogService requestLogService) {
    this.realtimeProperties = realtimeProperties;
    this.realtimeSessionService = realtimeSessionService;
    this.realtimeLookupService = realtimeLookupService;
    this.realtimeModelCatalog = realtimeModelCatalog;
    this.elevenLabsRealtimeTokenService = elevenLabsRealtimeTokenService;
    this.requestLogService = requestLogService;
  }

  @Operation(summary = "Realtime voice available", description = "True when at least one realtime voice provider is configured.")
  @GetMapping("/realtime/status")
  public ResponseEntity<RealtimeStatusResponse> status() {
    boolean liveEnabled = realtimeModelCatalog.hasAvailableModels();
    return ResponseEntity.ok(new RealtimeStatusResponse(
        liveEnabled,
        liveEnabled,
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
      @RequestHeader(value = "X-Realtime-Reasoning-Effort", required = false) String reasoningEffort) {
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
    try {
      String answer = realtimeSessionService.createRealtimeCall(sdp, chatLanguage, model, voice, reasoningEffort);
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
  public ResponseEntity<?> createElevenLabsToken(@RequestBody(required = false) ElevenLabsTokenRequest request) {
    if (!realtimeProperties.isEnabled()) {
      return ResponseEntity.status(503)
          .body(new ApiError("Voice chat is disabled.", "REALTIME_DISABLED"));
    }
    requestLogService.save("/realtime/elevenlabs/token", "POST", "token", null);
    try {
      String token = elevenLabsRealtimeTokenService.createConversationToken(request == null ? null : request.modelId());
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
  public ResponseEntity<?> lookup(@RequestBody(required = false) RealtimeLookupRequest request) {
    try {
      String query = request == null ? null : request.query();
      String language = request == null ? null : request.language();
      return ResponseEntity.ok(realtimeLookupService.lookup(query, language));
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().body(new ApiError(e.getMessage(), "BAD_REQUEST"));
    }
  }
}
