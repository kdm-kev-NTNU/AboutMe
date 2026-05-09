package com.kevinmazali.portfolio.controller;

import com.kevinmazali.portfolio.config.RealtimeVoiceProperties;
import com.kevinmazali.portfolio.model.ApiError;
import com.kevinmazali.portfolio.model.realtime.RealtimeWebRtcRequest;
import com.kevinmazali.portfolio.model.realtime.RealtimeWebRtcResponse;
import com.kevinmazali.portfolio.service.RealtimeWebRtcService;
import com.kevinmazali.portfolio.util.LlmClientDiagnostics;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientResponseException;

/** Browser WebRTC SDP exchange with OpenAI Realtime (API key stays on the server). */
@Slf4j
@RestController
@RequestMapping("/realtime")
@Tag(name = "Realtime voice", description = "OpenAI Realtime WebRTC SDP relay")
public class RealtimeWebRtcController {

    private final RealtimeVoiceProperties realtimeVoiceProperties;
    private final RealtimeWebRtcService realtimeWebRtcService;

    public RealtimeWebRtcController(
            RealtimeVoiceProperties realtimeVoiceProperties, RealtimeWebRtcService realtimeWebRtcService) {
        this.realtimeVoiceProperties = realtimeVoiceProperties;
        this.realtimeWebRtcService = realtimeWebRtcService;
    }

    public record RealtimeStatusResponse(boolean enabled, int sessionMaxMinutes, String model) {}

    @GetMapping("/status")
    @Operation(summary = "Realtime voice feature flags for the SPA")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Status payload")})
    public ResponseEntity<RealtimeStatusResponse> status() {
        return ResponseEntity.ok(new RealtimeStatusResponse(
                realtimeVoiceProperties.isEnabled(),
                realtimeVoiceProperties.getSessionMaxMinutes(),
                realtimeVoiceProperties.getModel()));
    }

    @PostMapping(value = "/webrtc", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Exchange WebRTC SDP offer for OpenAI Realtime SDP answer")
    @ApiResponses(
            value = {
                @ApiResponse(responseCode = "200", description = "SDP answer"),
                @ApiResponse(
                        responseCode = "400",
                        description = "Bad request",
                        content = @Content(schema = @Schema(implementation = ApiError.class))),
                @ApiResponse(
                        responseCode = "502",
                        description = "OpenAI error",
                        content = @Content(schema = @Schema(implementation = ApiError.class))),
                @ApiResponse(
                        responseCode = "503",
                        description = "Disabled or not configured",
                        content = @Content(schema = @Schema(implementation = ApiError.class)))
            })
    public ResponseEntity<?> webrtc(
            @Valid @RequestBody RealtimeWebRtcRequest request,
            @RequestHeader(value = "X-Chat-Language", required = false) String chatLanguage) {
        if (!realtimeVoiceProperties.isEnabled()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(new ApiError("Realtime voice is disabled."));
        }
        try {
            String answer = realtimeWebRtcService.exchangeSdp(request.sdp(), chatLanguage);
            return ResponseEntity.ok(new RealtimeWebRtcResponse(answer));
        } catch (IllegalStateException e) {
            log.warn("/realtime/webrtc unavailable: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(new ApiError(e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ApiError(e.getMessage()));
        } catch (RestClientResponseException e) {
            log.warn("/realtime/webrtc upstream: {}", LlmClientDiagnostics.describeAskFailure(e));
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(new ApiError("OpenAI Realtime error. Try again in a moment."));
        }
    }
}
