package com.kevinmazali.portfolio.service;

import tools.jackson.databind.ObjectMapper;
import com.kevinmazali.portfolio.config.SanitizerProperties;
import com.kevinmazali.portfolio.config.VectorStoreProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.autoconfigure.PgVectorStoreProperties;
import org.springframework.boot.ApplicationArguments;
import org.springframework.jdbc.core.JdbcTemplate;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentIngestionServiceTest {

  @Mock
  private VectorStore vectorStore;
  @Mock
  private JdbcTemplate jdbcTemplate;
  @Mock
  private VectorStoreProperties vectorStoreProperties;
  @Mock
  private ApplicationArguments applicationArguments;
  @Mock
  private org.springframework.beans.factory.ObjectProvider<PiiSanitizerService> piiSanitizerProvider;

  private DocumentIngestionService service;
  private final PgVectorStoreProperties pgVectorStoreProperties = new PgVectorStoreProperties();

  @BeforeEach
  void setUp() {
    SanitizerProperties sanitizerProperties = new SanitizerProperties();
    sanitizerProperties.setEnabled(false);
    pgVectorStoreProperties.setSchemaName("public");
    pgVectorStoreProperties.setTableName("vector_store");
    service = new DocumentIngestionService(
        vectorStore,
        jdbcTemplate,
        new ObjectMapper(),
        pgVectorStoreProperties,
        vectorStoreProperties,
        new NoiseCleaner(),
        piiSanitizerProvider,
        sanitizerProperties);
  }

  @Test
  void runTouchesVectorStore() {
    when(jdbcTemplate.queryForObject(contains("COUNT(*)"), eq(Long.class))).thenReturn(0L);
    service.run(applicationArguments);
    verify(vectorStore).add(Collections.emptyList());
  }

  @Test
  void ingestFromPathsNullYieldsMessage() throws Exception {
    var results = service.ingestFromPaths(null, false);
    assertEquals(1, results.size());
    assertEquals("No paths provided", results.get(0).message());
  }

  @Test
  void ingestFromPathsEmptyYieldsMessage() throws Exception {
    var results = service.ingestFromPaths(Collections.emptyList(), false);
    assertEquals(1, results.size());
    assertEquals("No paths provided", results.get(0).message());
  }

  @Test
  void ingestFromPathsRejectsBatchOverLimit() throws Exception {
    List<String> many = IntStream.range(0, 101).mapToObj(i -> "a.txt").toList();
    var results = service.ingestFromPaths(many, false);
    assertEquals(1, results.size());
    assertEquals(true, results.get(0).message().contains("Too many paths"));
  }

  @Test
  void ingestFromPathsRequiresDocumentsDir() throws Exception {
    when(vectorStoreProperties.getDocumentsToLoadDir()).thenReturn(null);
    var results = service.ingestFromPaths(List.of("a.txt"), false);
    assertEquals(1, results.size());
    assertEquals("documentsToLoadDir is not configured", results.get(0).message());
  }

  @Test
  void ingestFromPathsRejectsInvalidRelativePath() throws Exception {
    when(vectorStoreProperties.getDocumentsToLoadDir()).thenReturn("file:./data/");
    var results = service.ingestFromPaths(List.of("../secret.txt"), false);
    assertEquals(1, results.size());
    assertEquals(true, results.get(0).message().contains("Invalid path"));
  }

  @Test
  void ingestFromPathsRejectsUnsupportedExtension() throws Exception {
    when(vectorStoreProperties.getDocumentsToLoadDir()).thenReturn("file:./data/");
    var results = service.ingestFromPaths(List.of("x.exe"), false);
    assertEquals(1, results.size());
    assertEquals(true, results.get(0).message().contains("Unsupported file type"));
  }

  @Test
  void ingestFromPathsMarksMissingFile(@TempDir Path tmp) throws Exception {
    String base = tmp.toUri().toString();
    if (!base.endsWith("/")) {
      base = base + "/";
    }
    when(vectorStoreProperties.getDocumentsToLoadDir()).thenReturn(base);
    var results = service.ingestFromPaths(List.of("does-not-exist-xyz.txt"), false);
    assertEquals(1, results.size());
    assertEquals("File not found or not readable", results.get(0).message());
  }

  @Test
  void listAvailableFilesReturnsEmptyWhenDirBlank() throws Exception {
    when(vectorStoreProperties.getDocumentsToLoadDir()).thenReturn("  ");
    assertEquals(true, service.listAvailableFiles().isEmpty());
  }

  @Test
  void listAvailableFilesReturnsEmptyForNonFileBaseUrl() throws Exception {
    when(vectorStoreProperties.getDocumentsToLoadDir()).thenReturn("http://example.invalid/docs/");
    assertEquals(true, service.listAvailableFiles().isEmpty());
  }

  @Test
  void ingestMultipartRejectsNullFile() throws Exception {
    var r = service.ingestMultipart(null, null, false);
    assertEquals("Empty file", r.message());
  }

  @Test
  void ingestMultipartRejectsEmptyUpload() throws Exception {
    var file = new org.springframework.mock.web.MockMultipartFile("file", "a.txt", "text/plain", new byte[0]);
    var r = service.ingestMultipart(file, null, false);
    assertEquals("Empty file", r.message());
  }
}
