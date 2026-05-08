package com.kevinmazali.portfolio.controller;

import com.kevinmazali.portfolio.model.ApiError;
import com.kevinmazali.portfolio.model.TranscribeResponse;
import com.kevinmazali.portfolio.service.RequestLogService;
import com.kevinmazali.portfolio.service.TranscriptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Locale;
import java.util.Set;

/**
 * Speech-to-text for the chat UI using OpenAI transcription (Spring AI).
 */
@Slf4j
@RestController
@Tag(name = "Chat", description = "RAG-backed question answering")
public class TranscriptionController {

  private static final Set<String> ALLOWED_AUDIO_TYPES = Set.of(
      "audio/webm",
      "audio/ogg",
      "audio/mp4",
      "audio/m4a",
      "audio/wav",
      "audio/wave",
      "audio/x-wav",
      "audio/flac",
      "audio/mpeg",
      "audio/mp3",
      "application/ogg");

  private final TranscriptionService transcriptionService;
  private final RequestLogService requestLogService;

  public TranscriptionController(TranscriptionService transcriptionService, RequestLogService requestLogService) {
    this.transcriptionService = transcriptionService;
    this.requestLogService = requestLogService;
  }

  @Operation(summary = "Transcribe audio", description = "Upload a short audio clip (multipart field `file`) for speech-to-text. Rate limits match POST /ask.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Transcript text",
          content = @Content(schema = @Schema(implementation = TranscribeResponse.class))),
      @ApiResponse(responseCode = "400", description = "Missing file, invalid type, or too large",
          content = @Content(schema = @Schema(implementation = ApiError.class))),
      @ApiResponse(responseCode = "429", description = "Budget or rate limit exceeded",
          content = @Content(schema = @Schema(implementation = ApiError.class))),
      @ApiResponse(responseCode = "503", description = "Transcription unavailable or AI circuit open",
          content = @Content(schema = @Schema(implementation = ApiError.class)))
  })
  @PostMapping(value = "/transcribe", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<?> transcribe(
      @RequestParam("file") MultipartFile file,
      @RequestHeader(value = "X-Chat-Language", required = false) String chatLanguage) {
    if (file == null || file.isEmpty()) {
      return ResponseEntity.badRequest().body(new ApiError("Audio file is required (multipart field 'file')."));
    }

    String contentType = file.getContentType();
    if (contentType == null || !isAllowedAudio(contentType)) {
      return ResponseEntity.badRequest().body(new ApiError("Unsupported or missing audio Content-Type."));
    }

    long size = file.getSize();
    if (size > TranscriptionService.MAX_AUDIO_BYTES) {
      return ResponseEntity.badRequest().body(new ApiError("Audio file exceeds maximum size of 25 MB."));
    }

    String langOverride = normalizeLanguageHeader(chatLanguage);

    String logHint = file.getOriginalFilename() != null ? file.getOriginalFilename() : contentType;
    requestLogService.save("/transcribe", "POST", logHint, null);

    try {
      byte[] bytes = file.getBytes();
      var resource = new ByteArrayResource(bytes) {
        @Override
        public String getFilename() {
          String n = file.getOriginalFilename();
          return n != null ? n : "audio.bin";
        }
      };
      String text = transcriptionService.transcribe(resource, bytes.length, langOverride);
      requestLogService.save("/transcribe:response", "POST", text, null);
      return ResponseEntity.ok(new TranscribeResponse(text));
    } catch (IOException e) {
      log.warn("/transcribe failed reading upload: {}", e.getMessage());
      return ResponseEntity.badRequest().body(new ApiError("Could not read uploaded audio."));
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().body(new ApiError(e.getMessage()));
    } catch (IllegalStateException e) {
      log.warn("/transcribe unavailable: {}", e.getMessage());
      return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
          .body(new ApiError("Speech-to-text is temporarily unavailable."));
    }
    // Other runtime exceptions (Spring AI / OpenAI transports such as RestClientException,
    // HttpServerErrorException, etc.) bubble up to GlobalApiExceptionHandler so the SPA gets
    // a structured ApiError JSON instead of Spring Boot's default 500 HTML body.
  }

  private static boolean isAllowedAudio(String contentType) {
    String base = contentType.split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
    return ALLOWED_AUDIO_TYPES.contains(base);
  }

  /**
   * Optional hint for Whisper: {@code en} or {@code no}. Other values are ignored so clients cannot drive arbitrary API params.
   */
  private static String normalizeLanguageHeader(String raw) {
    if (raw == null || raw.isBlank()) {
      return null;
    }
    String v = raw.trim().toLowerCase(Locale.ROOT);
    if ("en".equals(v) || "no".equals(v)) {
      return v;
    }
    return null;
  }
}
