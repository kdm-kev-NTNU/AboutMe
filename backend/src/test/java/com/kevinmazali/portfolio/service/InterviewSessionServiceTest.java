package com.kevinmazali.portfolio.service;

import com.kevinmazali.portfolio.config.RealtimeProperties;
import com.kevinmazali.portfolio.model.IngestionResult;
import com.kevinmazali.portfolio.model.interview.CreateInterviewSessionRequest;
import com.kevinmazali.portfolio.model.interview.InterviewSessionEntity;
import com.kevinmazali.portfolio.model.interview.InterviewSessionResponse;
import com.kevinmazali.portfolio.model.interview.InterviewTranscriptEntity;
import com.kevinmazali.portfolio.model.interview.InterviewTranscriptTurnEntity;
import com.kevinmazali.portfolio.model.interview.InterviewTurnBatchRequest;
import com.kevinmazali.portfolio.model.interview.InterviewTurnDto;
import com.kevinmazali.portfolio.repository.InterviewDocumentRepository;
import com.kevinmazali.portfolio.repository.InterviewSessionRepository;
import com.kevinmazali.portfolio.repository.InterviewTranscriptRepository;
import com.kevinmazali.portfolio.repository.InterviewTranscriptTurnRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InterviewSessionServiceTest {

  @Mock private InterviewSessionRepository sessionRepository;
  @Mock private InterviewDocumentRepository documentRepository;
  @Mock private InterviewTranscriptTurnRepository turnRepository;
  @Mock private InterviewTranscriptRepository transcriptRepository;
  @Mock private InterviewTranscriptCleanerService transcriptCleanerService;
  @Mock private DocumentIngestionService documentIngestionService;

  private RealtimeProperties realtimeProperties;
  private InterviewSessionService service;

  @BeforeEach
  void setUp() {
    realtimeProperties = new RealtimeProperties();
    realtimeProperties.setVoice("marin");
    service =
        new InterviewSessionService(
            sessionRepository,
            documentRepository,
            turnRepository,
            transcriptRepository,
            transcriptCleanerService,
            documentIngestionService,
            realtimeProperties);
  }

  @Test
  void createSession_requiresDocumentId() {
    assertThatThrownBy(() -> service.createSession(null))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("documentId");
  }

  @Test
  void createSession_requiresExistingDocument() {
    when(documentRepository.existsById("missing")).thenReturn(false);

    assertThatThrownBy(() -> service.createSession(new CreateInterviewSessionRequest("missing", "en", null)))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("Document not found");
  }

  @Test
  void createSession_normalizesLanguageAndSaves() {
    when(documentRepository.existsById("doc1")).thenReturn(true);
    when(sessionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    InterviewSessionResponse response =
        service.createSession(new CreateInterviewSessionRequest("doc1", "nb", "cedar"));

    assertThat(response.documentId()).isEqualTo("doc1");
    assertThat(response.language()).isEqualTo("no");
    assertThat(response.status()).isEqualTo(InterviewSessionService.STATUS_ACTIVE);
    assertThat(response.voice()).isEqualTo("cedar");

    ArgumentCaptor<InterviewSessionEntity> captor = ArgumentCaptor.forClass(InterviewSessionEntity.class);
    verify(sessionRepository).save(captor.capture());
    assertThat(captor.getValue().getLanguage()).isEqualTo("no");
  }

  @Test
  void appendTurns_skipsEmptyBatch() {
    stubActiveSession("sess1");

    service.appendTurns("sess1", new InterviewTurnBatchRequest(List.of()));

    verify(turnRepository, never()).saveAll(any());
  }

  @Test
  void appendTurns_persistsValidTurnsWithAutoSequence() {
    stubActiveSession("sess1");
    when(turnRepository.findBySessionIdOrderBySequenceNoAsc("sess1")).thenReturn(List.of());

    service.appendTurns(
        "sess1",
        new InterviewTurnBatchRequest(
            List.of(
                new InterviewTurnDto("interviewer", "  Hello?  ", -1),
                new InterviewTurnDto("", "ignored", 1),
                new InterviewTurnDto("user", "Hi there", -1))));

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<InterviewTranscriptTurnEntity>> captor = ArgumentCaptor.forClass(List.class);
    verify(turnRepository).saveAll(captor.capture());
    assertThat(captor.getValue()).hasSize(2);
    assertThat(captor.getValue().get(0).getRole()).isEqualTo("interviewer");
    assertThat(captor.getValue().get(0).getText()).isEqualTo("Hello?");
    assertThat(captor.getValue().get(1).getRole()).isEqualTo("user");
  }

  @Test
  void finalizeSession_createsTranscriptAndMarksFinalized() {
    stubActiveSession("sess1");
    when(turnRepository.findBySessionIdOrderBySequenceNoAsc("sess1"))
        .thenReturn(
            List.of(
                InterviewTranscriptTurnEntity.builder()
                    .sessionId("sess1")
                    .role("user")
                    .text("Answer")
                    .sequenceNo(0)
                    .build()));
    when(transcriptCleanerService.structureRawTranscript(any())).thenReturn("raw transcript");
    when(transcriptRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(sessionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    var response = service.finalizeSession("sess1");

    assertThat(response.cleanStatus()).isEqualTo(InterviewSessionService.CLEAN_PENDING);
    assertThat(response.rawText()).isEqualTo("raw transcript");

    ArgumentCaptor<InterviewSessionEntity> sessionCaptor = ArgumentCaptor.forClass(InterviewSessionEntity.class);
    verify(sessionRepository).save(sessionCaptor.capture());
    assertThat(sessionCaptor.getValue().getStatus()).isEqualTo(InterviewSessionService.STATUS_FINALIZED);
    assertThat(sessionCaptor.getValue().getEndedAt()).isNotNull();
  }

  @Test
  void cleanTranscript_setsCleanedTextOnSuccess() {
    InterviewTranscriptEntity transcript =
        InterviewTranscriptEntity.builder()
            .id("tr1")
            .sessionId("sess1")
            .rawText("raw")
            .cleanStatus(InterviewSessionService.CLEAN_PENDING)
            .createdAt(Instant.now())
            .build();
    when(transcriptRepository.findById("tr1")).thenReturn(Optional.of(transcript));
    when(sessionRepository.findById("sess1"))
        .thenReturn(
            Optional.of(
                InterviewSessionEntity.builder()
                    .id("sess1")
                    .documentId("doc1")
                    .language("en")
                    .status(InterviewSessionService.STATUS_FINALIZED)
                    .startedAt(Instant.now())
                    .build()));
    when(transcriptCleanerService.cleanForIngest("raw", "en")).thenReturn("cleaned");
    when(transcriptRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    var response = service.cleanTranscript("tr1");

    assertThat(response.cleanedText()).isEqualTo("cleaned");
    assertThat(response.cleanStatus()).isEqualTo(InterviewSessionService.CLEAN_CLEANED);
  }

  @Test
  void ingestTranscript_requiresCleanedTranscript() {
    InterviewTranscriptEntity transcript =
        InterviewTranscriptEntity.builder()
            .id("tr1")
            .sessionId("sess1")
            .cleanStatus(InterviewSessionService.CLEAN_PENDING)
            .createdAt(Instant.now())
            .build();
    when(transcriptRepository.findById("tr1")).thenReturn(Optional.of(transcript));

    assertThatThrownBy(() -> service.ingestTranscript("tr1", false))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("must be cleaned");
  }

  @Test
  void ingestTranscript_returnsExistingWhenAlreadyIngested() throws Exception {
    InterviewTranscriptEntity transcript =
        InterviewTranscriptEntity.builder()
            .id("tr1")
            .sessionId("sess1")
            .cleanStatus(InterviewSessionService.CLEAN_CLEANED)
            .cleanedText("cleaned")
            .ingestedDocumentId("existing-doc")
            .createdAt(Instant.now())
            .build();
    when(transcriptRepository.findById("tr1")).thenReturn(Optional.of(transcript));

    IngestionResult result = service.ingestTranscript("tr1", false);

    assertThat(result.documentId()).isEqualTo("existing-doc");
    assertThat(result.message()).contains("Already ingested");
    verify(documentIngestionService, never()).ingestTextContent(anyString(), anyString(), anyBoolean());
  }

  @Test
  void ingestTranscript_ingestsCleanedText() throws Exception {
    InterviewTranscriptEntity transcript =
        InterviewTranscriptEntity.builder()
            .id("tr1")
            .sessionId("sess1")
            .cleanStatus(InterviewSessionService.CLEAN_CLEANED)
            .cleanedText("cleaned markdown")
            .createdAt(Instant.now())
            .build();
    when(transcriptRepository.findById("tr1")).thenReturn(Optional.of(transcript));
    when(documentIngestionService.ingestTextContent(eq("cleaned markdown"), anyString(), eq(false)))
        .thenReturn(new IngestionResult("ing1", "interview-transcript", 3, false, "ok"));
    when(transcriptRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    IngestionResult result = service.ingestTranscript("tr1", false);

    assertThat(result.documentId()).isEqualTo("ing1");
    assertThat(transcript.getIngestedDocumentId()).isEqualTo("ing1");
  }

  @Test
  void listSessions_andGetSession_mapTranscriptMetadata() {
    InterviewSessionEntity session =
        InterviewSessionEntity.builder()
            .id("sess1")
            .documentId("doc1")
            .language("en")
            .status(InterviewSessionService.STATUS_FINALIZED)
            .startedAt(Instant.now())
            .build();
    InterviewTranscriptEntity transcript =
        InterviewTranscriptEntity.builder()
            .id("tr1")
            .sessionId("sess1")
            .cleanStatus(InterviewSessionService.CLEAN_CLEANED)
            .createdAt(Instant.now())
            .build();
    when(sessionRepository.findByDeletedAtIsNullOrderByStartedAtDesc()).thenReturn(List.of(session));
    when(transcriptRepository.findBySessionId("sess1")).thenReturn(Optional.of(transcript));
    when(sessionRepository.findById("sess1")).thenReturn(Optional.of(session));
    when(turnRepository.findBySessionIdOrderBySequenceNoAsc("sess1")).thenReturn(List.of());

    assertThat(service.listSessions()).hasSize(1);
    assertThat(service.getSession("sess1").transcriptId()).isEqualTo("tr1");
    assertThat(service.getTranscript("tr1").id()).isEqualTo("tr1");
  }

  @Test
  void cleanTranscript_marksFailedOnCleanerError() {
    InterviewTranscriptEntity transcript =
        InterviewTranscriptEntity.builder()
            .id("tr1")
            .sessionId("sess1")
            .rawText("raw")
            .cleanStatus(InterviewSessionService.CLEAN_PENDING)
            .createdAt(Instant.now())
            .build();
    when(transcriptRepository.findById("tr1")).thenReturn(Optional.of(transcript));
    when(sessionRepository.findById("sess1"))
        .thenReturn(
            Optional.of(
                InterviewSessionEntity.builder()
                    .id("sess1")
                    .documentId("doc1")
                    .language("en")
                    .status(InterviewSessionService.STATUS_FINALIZED)
                    .startedAt(Instant.now())
                    .build()));
    when(transcriptCleanerService.cleanForIngest("raw", "en")).thenThrow(new RuntimeException("boom"));
    when(transcriptRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    assertThatThrownBy(() -> service.cleanTranscript("tr1"))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("Transcript clean failed");
    assertThat(transcript.getCleanStatus()).isEqualTo(InterviewSessionService.CLEAN_FAILED);
  }

  @Test
  void requireActiveSession_rejectsDeleted() {
    when(sessionRepository.findById("sess1"))
        .thenReturn(
            Optional.of(
                InterviewSessionEntity.builder()
                    .id("sess1")
                    .documentId("doc1")
                    .language("en")
                    .status(InterviewSessionService.STATUS_DELETED)
                    .startedAt(Instant.now())
                    .deletedAt(Instant.now())
                    .build()));

    assertThatThrownBy(() -> service.getSession("sess1"))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("Session not found");
  }

  @Test
  void deleteSession_softDeletes() {
    InterviewSessionEntity session =
        InterviewSessionEntity.builder()
            .id("sess1")
            .documentId("doc1")
            .language("en")
            .status(InterviewSessionService.STATUS_ACTIVE)
            .startedAt(Instant.now())
            .build();
    when(sessionRepository.findById("sess1")).thenReturn(Optional.of(session));
    when(sessionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    service.deleteSession("sess1");

    assertThat(session.getStatus()).isEqualTo(InterviewSessionService.STATUS_DELETED);
    assertThat(session.getDeletedAt()).isNotNull();
  }

  private void stubActiveSession(String sessionId) {
    when(sessionRepository.findById(sessionId))
        .thenReturn(
            Optional.of(
                InterviewSessionEntity.builder()
                    .id(sessionId)
                    .documentId("doc1")
                    .language("en")
                    .status(InterviewSessionService.STATUS_ACTIVE)
                    .startedAt(Instant.now())
                    .build()));
  }
}
