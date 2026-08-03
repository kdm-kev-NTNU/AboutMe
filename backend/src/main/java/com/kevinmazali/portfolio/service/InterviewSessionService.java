package com.kevinmazali.portfolio.service;

import com.kevinmazali.portfolio.config.RealtimeProperties;
import com.kevinmazali.portfolio.model.IngestionResult;
import com.kevinmazali.portfolio.model.interview.CreateInterviewSessionRequest;
import com.kevinmazali.portfolio.model.interview.InterviewSessionEntity;
import com.kevinmazali.portfolio.model.interview.InterviewSessionResponse;
import com.kevinmazali.portfolio.model.interview.InterviewTranscriptEntity;
import com.kevinmazali.portfolio.model.interview.InterviewTranscriptResponse;
import com.kevinmazali.portfolio.model.interview.InterviewTranscriptTurnEntity;
import com.kevinmazali.portfolio.model.interview.InterviewTurnBatchRequest;
import com.kevinmazali.portfolio.model.interview.InterviewTurnDto;
import com.kevinmazali.portfolio.repository.InterviewDocumentRepository;
import com.kevinmazali.portfolio.repository.InterviewSessionRepository;
import com.kevinmazali.portfolio.repository.InterviewTranscriptRepository;
import com.kevinmazali.portfolio.repository.InterviewTranscriptTurnRepository;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewSessionService {

  public static final String STATUS_ACTIVE = "ACTIVE";
  public static final String STATUS_FINALIZED = "FINALIZED";
  public static final String STATUS_DELETED = "DELETED";
  public static final String CLEAN_PENDING = "PENDING";
  public static final String CLEAN_CLEANED = "CLEANED";
  public static final String CLEAN_FAILED = "FAILED";

  private final InterviewSessionRepository sessionRepository;
  private final InterviewDocumentRepository documentRepository;
  private final InterviewTranscriptTurnRepository turnRepository;
  private final InterviewTranscriptRepository transcriptRepository;
  private final InterviewTranscriptCleanerService transcriptCleanerService;
  private final DocumentIngestionService documentIngestionService;
  private final RealtimeProperties realtimeProperties;

  @Transactional
  public InterviewSessionResponse createSession(CreateInterviewSessionRequest request) {
    if (request == null || !StringUtils.hasText(request.documentId())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "documentId is required");
    }
    if (!documentRepository.existsById(request.documentId())) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found");
    }
    String lang = normalizeLang(request.language());
    String voice = realtimeProperties.resolveVoice(request.voice());
    String id = UUID.randomUUID().toString().replace("-", "");
    InterviewSessionEntity session =
        InterviewSessionEntity.builder()
            .id(id)
            .documentId(request.documentId())
            .language(lang)
            .status(STATUS_ACTIVE)
            .voice(voice)
            .startedAt(Instant.now())
            .build();
    sessionRepository.save(session);
    return toResponse(session, List.of(), null);
  }

  public List<InterviewSessionResponse> listSessions() {
    return sessionRepository.findByDeletedAtIsNullOrderByStartedAtDesc().stream()
        .map(s -> toResponse(s, List.of(), transcriptRepository.findBySessionId(s.getId()).orElse(null)))
        .toList();
  }

  public InterviewSessionResponse getSession(String sessionId) {
    InterviewSessionEntity session = requireExistingSession(sessionId);
    List<InterviewTurnDto> turns = loadTurns(sessionId);
    InterviewTranscriptEntity transcript = transcriptRepository.findBySessionId(sessionId).orElse(null);
    return toResponse(session, turns, transcript);
  }

  @Transactional
  public void appendTurns(String sessionId, InterviewTurnBatchRequest request) {
    requireLiveSession(sessionId);
    if (request == null || request.turns() == null || request.turns().isEmpty()) {
      return;
    }
    List<InterviewTranscriptTurnEntity> existing = turnRepository.findBySessionIdOrderBySequenceNoAsc(sessionId);
    int maxSeq = existing.stream().mapToInt(InterviewTranscriptTurnEntity::getSequenceNo).max().orElse(-1);
    Instant now = Instant.now();
    List<InterviewTranscriptTurnEntity> toSave = new ArrayList<>();
    for (InterviewTurnDto turn : request.turns()) {
      if (!StringUtils.hasText(turn.text()) || !StringUtils.hasText(turn.role())) {
        continue;
      }
      int seq = turn.sequenceNo() >= 0 ? turn.sequenceNo() : ++maxSeq;
      maxSeq = Math.max(maxSeq, seq);
      toSave.add(
          InterviewTranscriptTurnEntity.builder()
              .sessionId(sessionId)
              .role(normalizeRole(turn.role()))
              .text(turn.text().trim())
              .sequenceNo(seq)
              .createdAt(now)
              .build());
    }
    if (!toSave.isEmpty()) {
      turnRepository.saveAll(toSave);
    }
  }

  @Transactional
  public InterviewTranscriptResponse finalizeSession(String sessionId) {
    InterviewSessionEntity session = requireExistingSession(sessionId);
    if (!STATUS_ACTIVE.equals(session.getStatus()) && !STATUS_FINALIZED.equals(session.getStatus())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Session cannot be finalized");
    }
    List<InterviewTurnDto> turns = loadTurns(sessionId);
    String raw = transcriptCleanerService.structureRawTranscript(turns);
    InterviewTranscriptEntity transcript =
        transcriptRepository
            .findBySessionId(sessionId)
            .orElseGet(
                () ->
                    InterviewTranscriptEntity.builder()
                        .id(UUID.randomUUID().toString().replace("-", ""))
                        .sessionId(sessionId)
                        .createdAt(Instant.now())
                        .build());
    transcript.setRawText(raw);
    transcript.setCleanStatus(CLEAN_PENDING);
    transcript.setCleanedText(null);
    transcript.setCleanedAt(null);
    transcript.setIngestedDocumentId(null);
    transcriptRepository.save(transcript);
    session.setStatus(STATUS_FINALIZED);
    session.setEndedAt(Instant.now());
    sessionRepository.save(session);
    return toTranscriptResponse(transcript);
  }

  @Transactional
  public InterviewSessionResponse reopenSession(String sessionId) {
    InterviewSessionEntity session = requireExistingSession(sessionId);
    if (!STATUS_FINALIZED.equals(session.getStatus())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only finalized sessions can be reopened");
    }
    session.setStatus(STATUS_ACTIVE);
    session.setEndedAt(null);
    sessionRepository.save(session);
    List<InterviewTurnDto> turns = loadTurns(sessionId);
    InterviewTranscriptEntity transcript = transcriptRepository.findBySessionId(sessionId).orElse(null);
    return toResponse(session, turns, transcript);
  }

  @Transactional
  public InterviewTranscriptResponse cleanTranscript(String transcriptId) {
    InterviewTranscriptEntity transcript = requireTranscript(transcriptId);
    InterviewSessionEntity session =
        sessionRepository
            .findById(transcript.getSessionId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));
    try {
      String cleaned = transcriptCleanerService.cleanForIngest(transcript.getRawText(), session.getLanguage());
      transcript.setCleanedText(cleaned);
      transcript.setCleanStatus(CLEAN_CLEANED);
      transcript.setCleanedAt(Instant.now());
      transcriptRepository.save(transcript);
      return toTranscriptResponse(transcript);
    } catch (Exception e) {
      log.warn("Transcript clean failed id={}: {}", transcriptId, e.getMessage());
      transcript.setCleanStatus(CLEAN_FAILED);
      transcriptRepository.save(transcript);
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Transcript clean failed");
    }
  }

  public InterviewTranscriptResponse getTranscript(String transcriptId) {
    return toTranscriptResponse(requireTranscript(transcriptId));
  }

  @Transactional
  public IngestionResult ingestTranscript(String transcriptId, boolean force) {
    InterviewTranscriptEntity transcript = requireTranscript(transcriptId);
    if (!CLEAN_CLEANED.equals(transcript.getCleanStatus()) || !StringUtils.hasText(transcript.getCleanedText())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Transcript must be cleaned before ingest");
    }
    if (StringUtils.hasText(transcript.getIngestedDocumentId()) && !force) {
      return new IngestionResult(
          transcript.getIngestedDocumentId(),
          "interview-transcript",
          0,
          true,
          "Already ingested. Use force to replace.");
    }
    String date = DateTimeFormatter.ISO_LOCAL_DATE.format(Instant.now().atZone(java.time.ZoneOffset.UTC));
    String filename = "interview-" + date + "-cleaned.md";
    try {
      IngestionResult result =
          documentIngestionService.ingestTextContent(transcript.getCleanedText(), filename, force);
      transcript.setIngestedDocumentId(result.documentId());
      transcriptRepository.save(transcript);
      return result;
    } catch (Exception e) {
      log.warn("Interview transcript ingest failed id={}: {}", transcriptId, e.getMessage());
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Ingest failed: " + e.getMessage());
    }
  }

  @Transactional
  public void deleteSession(String sessionId) {
    InterviewSessionEntity session =
        sessionRepository
            .findById(sessionId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));
    session.setDeletedAt(Instant.now());
    session.setStatus(STATUS_DELETED);
    sessionRepository.save(session);
  }

  /**
   * Returns a non-deleted session (ACTIVE or FINALIZED). Soft-deleted sessions are not found.
   */
  InterviewSessionEntity requireExistingSession(String sessionId) {
    InterviewSessionEntity session =
        sessionRepository
            .findById(sessionId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));
    if (session.getDeletedAt() != null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found");
    }
    return session;
  }

  /** Live voice and turn append require an ACTIVE, non-deleted session. */
  InterviewSessionEntity requireLiveSession(String sessionId) {
    InterviewSessionEntity session = requireExistingSession(sessionId);
    if (!STATUS_ACTIVE.equals(session.getStatus())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Interview session is not active");
    }
    return session;
  }

  /** @deprecated Prefer {@link #requireExistingSession(String)} or {@link #requireLiveSession(String)}. */
  InterviewSessionEntity requireActiveSession(String sessionId) {
    return requireExistingSession(sessionId);
  }

  private InterviewTranscriptEntity requireTranscript(String transcriptId) {
    return transcriptRepository
        .findById(transcriptId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transcript not found"));
  }

  private List<InterviewTurnDto> loadTurns(String sessionId) {
    return turnRepository.findBySessionIdOrderBySequenceNoAsc(sessionId).stream()
        .map(t -> new InterviewTurnDto(t.getRole(), t.getText(), t.getSequenceNo()))
        .toList();
  }

  private InterviewSessionResponse toResponse(
      InterviewSessionEntity session, List<InterviewTurnDto> turns, InterviewTranscriptEntity transcript) {
    return new InterviewSessionResponse(
        session.getId(),
        session.getDocumentId(),
        session.getLanguage(),
        session.getStatus(),
        session.getVoice(),
        session.getStartedAt(),
        session.getEndedAt(),
        transcript != null ? transcript.getId() : null,
        transcript != null ? transcript.getCleanStatus() : null,
        transcript != null ? transcript.getIngestedDocumentId() : null,
        turns);
  }

  private static InterviewTranscriptResponse toTranscriptResponse(InterviewTranscriptEntity transcript) {
    return new InterviewTranscriptResponse(
        transcript.getId(),
        transcript.getSessionId(),
        transcript.getRawText(),
        transcript.getCleanedText(),
        transcript.getCleanStatus(),
        transcript.getIngestedDocumentId(),
        transcript.getCreatedAt(),
        transcript.getCleanedAt());
  }

  private static String normalizeLang(String raw) {
    if (!StringUtils.hasText(raw)) {
      return "en";
    }
    String v = raw.trim().toLowerCase(Locale.ROOT);
    if ("no".equals(v) || "nb".equals(v) || "nn".equals(v)) {
      return "no";
    }
    return "en";
  }

  private static String normalizeRole(String role) {
    String r = role.trim().toLowerCase(Locale.ROOT);
    if ("assistant".equals(r) || "interviewer".equals(r)) {
      return "interviewer";
    }
    return "user";
  }
}
