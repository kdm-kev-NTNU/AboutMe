package com.kevinmazali.portfolio.controller;

import com.kevinmazali.portfolio.config.RealtimeProperties;
import com.kevinmazali.portfolio.exception.RealtimeSessionException;
import com.kevinmazali.portfolio.model.ApiError;
import com.kevinmazali.portfolio.model.RealtimeStatusResponse;
import com.kevinmazali.portfolio.service.RealtimeSessionService;
import com.kevinmazali.portfolio.service.RequestLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
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
  private final RequestLogService requestLogService;
  private final String openAiApiKey;

  public RealtimeController(
      RealtimeProperties realtimeProperties,
      RealtimeSessionService realtimeSessionService,
      RequestLogService requestLogService,
      @Value("${spring.ai.openai.api-key:}") String openAiApiKey) {
    this.realtimeProperties = realtimeProperties;
    this.realtimeSessionService = realtimeSessionService;
    this.requestLogService = requestLogService;
    this.openAiApiKey = openAiApiKey;
  }

  @Operation(summary = "Realtime voice available", description = "True when realtime voice is enabled and OpenAI is configured.")
  @GetMapping("/realtime/status")
  public ResponseEntity<RealtimeStatusResponse> status() {
    boolean ok = realtimeProperties.isEnabled() && StringUtils.hasText(openAiApiKey);
    return ResponseEntity.ok(new RealtimeStatusResponse(ok));
  }

  @Operation(summary = "Create Realtime WebRTC session", description = "POST SDP offer as raw body; returns SDP answer for RTCPeerConnection.")
  // Do not constrain `produces` to application/sdp: error paths return JSON `ApiError` with the correct Content-Type set by Spring.
  @PostMapping(value = "/realtime/session", consumes = { "application/sdp", "text/plain" })
  public ResponseEntity<?> createSession(
      @RequestBody String sdp,
      @RequestHeader(value = "X-Chat-Language", required = false) String chatLanguage) {
    if (!realtimeProperties.isEnabled()) {
      return ResponseEntity.status(503)
          .body(new ApiError("Voice chat is disabled.", "REALTIME_DISABLED"));
    }
    if (!StringUtils.hasText(openAiApiKey)) {
      return ResponseEntity.status(503)
          .body(new ApiError("Voice chat is not configured.", "API_KEY_MISSING"));
    }
    requestLogService.save("/realtime/session", "POST", "sdp-bytes", null);
    try {
      String answer = realtimeSessionService.createRealtimeCall(sdp, chatLanguage);
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
}
