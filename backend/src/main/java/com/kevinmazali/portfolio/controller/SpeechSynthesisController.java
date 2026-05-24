package com.kevinmazali.portfolio.controller;

import com.kevinmazali.portfolio.model.ApiError;
import com.kevinmazali.portfolio.model.SynthesizeRequest;
import com.kevinmazali.portfolio.service.RequestLogService;
import com.kevinmazali.portfolio.service.SpeechSynthesisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/**
 * Text-to-speech endpoint for turn-based standard voice mode.
 */
@Slf4j
@RestController
@Tag(name = "Chat", description = "RAG-backed question answering")
public class SpeechSynthesisController {

  private final SpeechSynthesisService speechSynthesisService;
  private final RequestLogService requestLogService;

  public SpeechSynthesisController(
      SpeechSynthesisService speechSynthesisService,
      RequestLogService requestLogService) {
    this.speechSynthesisService = speechSynthesisService;
    this.requestLogService = requestLogService;
  }

  @Operation(summary = "Synthesize speech", description = "Convert short text to MP3 audio for standard voice mode.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "MP3 audio bytes"),
      @ApiResponse(responseCode = "400", description = "Invalid text payload",
          content = @Content(schema = @Schema(implementation = ApiError.class))),
      @ApiResponse(responseCode = "429", description = "Budget or rate limit exceeded",
          content = @Content(schema = @Schema(implementation = ApiError.class))),
      @ApiResponse(responseCode = "503", description = "Speech synthesis unavailable",
          content = @Content(schema = @Schema(implementation = ApiError.class)))
  })
  @PostMapping(value = "/synthesize", consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<?> synthesize(
      @RequestBody(required = false) SynthesizeRequest request,
      @RequestHeader(value = "X-Chat-Language", required = false) String chatLanguage) {
    String text = request == null ? null : request.text();
    requestLogService.save("/synthesize", "POST", text == null ? "null" : "chars:" + text.length(), null);
    try {
      byte[] audio = speechSynthesisService.synthesize(text, chatLanguage);
      return ResponseEntity.ok()
          .contentType(MediaType.parseMediaType("audio/mpeg"))
          .header(HttpHeaders.CACHE_CONTROL, "no-store")
          .body(audio);
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().body(new ApiError(e.getMessage()));
    } catch (IllegalStateException e) {
      log.warn("/synthesize unavailable: {}", e.getMessage());
      return ResponseEntity.status(503).body(new ApiError("Speech synthesis is temporarily unavailable."));
    }
  }
}
