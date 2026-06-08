package com.kevinmazali.portfolio.service;

import com.kevinmazali.portfolio.config.DocumentIngestProperties;
import com.kevinmazali.portfolio.model.interview.InterviewDocumentEntity;
import com.kevinmazali.portfolio.repository.InterviewDocumentRepository;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InterviewDocumentServiceTest {

  @Mock private InterviewDocumentRepository documentRepository;

  private DocumentIngestProperties documentIngestProperties;
  private InterviewDocumentService service;

  @BeforeEach
  void setUp() {
    documentIngestProperties = new DocumentIngestProperties();
    documentIngestProperties.setMaxParseBytes(1024 * 1024);
    service = new InterviewDocumentService(documentRepository, documentIngestProperties);
  }

  @Test
  void storeText_savesPlainTextDocument() {
    when(documentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    var response = service.storeText("  Hello NTNU  ", "notes.md", "admin");

    assertThat(response.originalFilename()).isEqualTo("notes.md");
    assertThat(response.charCount()).isEqualTo("Hello NTNU".length());

    ArgumentCaptor<InterviewDocumentEntity> captor = ArgumentCaptor.forClass(InterviewDocumentEntity.class);
    verify(documentRepository).save(captor.capture());
    assertThat(captor.getValue().getParsedText()).isEqualTo("Hello NTNU");
    assertThat(captor.getValue().getCreatedBy()).isEqualTo("admin");
  }

  @Test
  void storeText_rejectsBlankInput() {
    assertThatThrownBy(() -> service.storeText("   ", null, "admin"))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("Text is required");
  }

  @Test
  void storeMultipart_parsesTxtFile() throws Exception {
    when(documentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    MockMultipartFile file =
        new MockMultipartFile("file", "resume.txt", "text/plain", "Kevin studies at NTNU.".getBytes(StandardCharsets.UTF_8));

    var response = service.storeMultipart(file, "admin");

    assertThat(response.originalFilename()).isEqualTo("resume.txt");
    assertThat(response.charCount()).isGreaterThan(0);
  }

  @Test
  void storeMultipart_rejectsUnsupportedExtension() {
    MockMultipartFile file = new MockMultipartFile("file", "virus.exe", "application/octet-stream", new byte[] {1});

    assertThatThrownBy(() -> service.storeMultipart(file, "admin"))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("Unsupported file type");
  }

  @Test
  void contextForSession_returnsTruncatedText() {
    String longText = "a".repeat(InterviewDocumentService.MAX_CONTEXT_CHARS + 100);
    InterviewDocumentEntity entity =
        InterviewDocumentEntity.builder()
            .id("doc1")
            .originalFilename("big.txt")
            .parsedText(longText)
            .charCount(longText.length())
            .build();
    when(documentRepository.findById("doc1")).thenReturn(Optional.of(entity));

    String context = service.contextForSession("doc1");

    assertThat(context).hasSize(InterviewDocumentService.MAX_CONTEXT_CHARS + "\n\n[truncated]".length());
    assertThat(context).endsWith("[truncated]");
  }

  @Test
  void truncateContext_handlesNullAndShortText() {
    assertThat(InterviewDocumentService.truncateContext(null)).isEmpty();
    assertThat(InterviewDocumentService.truncateContext("short")).isEqualTo("short");
  }

  @Test
  void storeMultipart_rejectsEmptyFile() {
    MockMultipartFile file = new MockMultipartFile("file", "empty.txt", "text/plain", new byte[0]);

    assertThatThrownBy(() -> service.storeMultipart(file, "admin"))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("Empty file");
  }

  @Test
  void storeMultipart_rejectsOversizedFile() {
    documentIngestProperties.setMaxParseBytes(4);
    MockMultipartFile file = new MockMultipartFile("file", "big.txt", "text/plain", "12345".getBytes(StandardCharsets.UTF_8));

    assertThatThrownBy(() -> service.storeMultipart(file, "admin"))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("maximum parse size");
  }

  @Test
  void getDocument_throwsWhenMissing() {
    when(documentRepository.findById("missing")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.getDocument("missing"))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("Document not found");
  }

  @Test
  void getDocument_mapsEntity() {
    InterviewDocumentEntity entity =
        InterviewDocumentEntity.builder()
            .id("doc1")
            .originalFilename("cv.pdf")
            .mimeType("application/pdf")
            .parsedText("text")
            .charCount(4)
            .build();
    when(documentRepository.findById("doc1")).thenReturn(Optional.of(entity));

    assertThat(service.getDocument("doc1").id()).isEqualTo("doc1");
  }
}
