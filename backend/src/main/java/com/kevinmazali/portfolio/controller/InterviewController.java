package com.kevinmazali.portfolio.controller;

import com.kevinmazali.portfolio.config.OpenApiConfig;
import com.kevinmazali.portfolio.exception.RealtimeSessionException;
import com.kevinmazali.portfolio.model.ApiError;
import com.kevinmazali.portfolio.model.IngestionResult;
import com.kevinmazali.portfolio.model.interview.CreateInterviewSessionRequest;
import com.kevinmazali.portfolio.model.interview.InterviewDocumentResponse;
import com.kevinmazali.portfolio.model.interview.InterviewSessionResponse;
import com.kevinmazali.portfolio.model.interview.InterviewTextDocumentRequest;
import com.kevinmazali.portfolio.model.interview.InterviewTranscriptResponse;
import com.kevinmazali.portfolio.model.interview.InterviewTurnBatchRequest;
import com.kevinmazali.portfolio.service.InterviewDocumentService;
import com.kevinmazali.portfolio.service.InterviewRealtimeSessionService;
import com.kevinmazali.portfolio.service.InterviewSessionService;
import com.kevinmazali.portfolio.service.RequestLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import java.security.Principal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/admin/tools/interview")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Admin interview", description = "Voice interview practice with document context (ADMIN)")
@SecurityRequirement(name = OpenApiConfig.BASIC_AUTH_SCHEME)
public class InterviewController {

  private final InterviewDocumentService interviewDocumentService;
  private final InterviewSessionService interviewSessionService;
  private final InterviewRealtimeSessionService interviewRealtimeSessionService;
  private final RequestLogService requestLogService;

  @Operation(summary = "Upload interview source document")
  @PostMapping(value = "/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<InterviewDocumentResponse> uploadDocument(
      @RequestParam("file") MultipartFile file, Principal principal) throws IOException {
    String createdBy = principal != null ? principal.getName() : null;
    return ResponseEntity.ok(interviewDocumentService.storeMultipart(file, createdBy));
  }

  @Operation(summary = "Create interview source document from pasted text")
  @PostMapping(value = "/documents/text", consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<InterviewDocumentResponse> createTextDocument(
      @RequestBody InterviewTextDocumentRequest request, Principal principal) {
    String createdBy = principal != null ? principal.getName() : null;
    return ResponseEntity.ok(
        interviewDocumentService.storeText(request.text(), request.filename(), createdBy));
  }

  @Operation(summary = "Get interview source document metadata")
  @GetMapping("/documents/{id}")
  public ResponseEntity<InterviewDocumentResponse> getDocument(@PathVariable String id) {
    return ResponseEntity.ok(interviewDocumentService.getDocument(id));
  }

  @Operation(summary = "Create interview session")
  @PostMapping("/sessions")
  public ResponseEntity<InterviewSessionResponse> createSession(
      @RequestBody CreateInterviewSessionRequest request) {
    return ResponseEntity.ok(interviewSessionService.createSession(request));
  }

  @Operation(summary = "List interview sessions")
  @GetMapping("/sessions")
  public ResponseEntity<List<InterviewSessionResponse>> listSessions() {
    return ResponseEntity.ok(interviewSessionService.listSessions());
  }

  @Operation(summary = "Get interview session with turns")
  @GetMapping("/sessions/{id}")
  public ResponseEntity<InterviewSessionResponse> getSession(@PathVariable String id) {
    return ResponseEntity.ok(interviewSessionService.getSession(id));
  }

  @Operation(summary = "Append transcript turns")
  @PostMapping("/sessions/{id}/turns")
  public ResponseEntity<Void> appendTurns(
      @PathVariable String id, @RequestBody InterviewTurnBatchRequest request) {
    interviewSessionService.appendTurns(id, request);
    return ResponseEntity.ok().build();
  }

  @Operation(summary = "Finalize interview session and save raw transcript")
  @PostMapping("/sessions/{id}/finalize")
  public ResponseEntity<InterviewTranscriptResponse> finalizeSession(@PathVariable String id) {
    return ResponseEntity.ok(interviewSessionService.finalizeSession(id));
  }

  @Operation(summary = "Create admin interview Realtime WebRTC session")
  @PostMapping(value = "/sessions/{id}/realtime/session", consumes = {"application/sdp", "text/plain"})
  public ResponseEntity<?> createRealtimeSession(
      @PathVariable String id,
      @RequestBody String sdp,
      @RequestHeader(value = "X-Chat-Language", required = false) String chatLanguage,
      @RequestHeader(value = "X-Realtime-Model", required = false) String model,
      @RequestHeader(value = "X-Realtime-Voice", required = false) String voice,
      @RequestHeader(value = "X-Realtime-Reasoning-Effort", required = false) String reasoningEffort) {
    requestLogService.save("/admin/tools/interview/sessions/" + id + "/realtime/session", "POST", "sdp-bytes", null);
    try {
      String answer =
          interviewRealtimeSessionService.createInterviewCall(
              id, sdp, chatLanguage, model, voice, reasoningEffort);
      return ResponseEntity.ok().contentType(MediaType.parseMediaType("application/sdp")).body(answer);
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().body(new ApiError(e.getMessage(), "BAD_REQUEST"));
    } catch (RealtimeSessionException e) {
      log.warn(
          "interview realtime session: code={} status={} message={}",
          e.getErrorCode(),
          e.getHttpStatus().value(),
          e.getMessage());
      return ResponseEntity.status(e.getHttpStatus())
          .body(new ApiError(e.getMessage(), e.getErrorCode().name()));
    }
  }

  @Operation(summary = "Get transcript")
  @GetMapping("/transcripts/{id}")
  public ResponseEntity<InterviewTranscriptResponse> getTranscript(@PathVariable String id) {
    return ResponseEntity.ok(interviewSessionService.getTranscript(id));
  }

  @Operation(summary = "Clean transcript for knowledge-base ingest")
  @PostMapping("/transcripts/{id}/clean")
  public ResponseEntity<InterviewTranscriptResponse> cleanTranscript(@PathVariable String id) {
    return ResponseEntity.ok(interviewSessionService.cleanTranscript(id));
  }

  @Operation(summary = "Ingest cleaned transcript into vector store")
  @PostMapping("/transcripts/{id}/ingest")
  public ResponseEntity<IngestionResult> ingestTranscript(
      @PathVariable String id, @RequestParam(value = "force", defaultValue = "false") boolean force) {
    return ResponseEntity.ok(interviewSessionService.ingestTranscript(id, force));
  }

  @Operation(summary = "Soft-delete interview session")
  @DeleteMapping("/sessions/{id}")
  public ResponseEntity<Void> deleteSession(@PathVariable String id) {
    interviewSessionService.deleteSession(id);
    return ResponseEntity.noContent().build();
  }
}
