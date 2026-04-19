package com.kevinmazali.portfolio.service;

import com.kevinmazali.portfolio.config.PortfolioChromaProperties;
import com.kevinmazali.portfolio.config.VectorStoreProperties;
import com.kevinmazali.portfolio.exception.ChromaFeatureDisabledException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chroma.vectorstore.ChromaApi;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.chroma.autoconfigure.ChromaVectorStoreProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.ApplicationArguments;
import org.springframework.core.env.Environment;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentIngestionServiceTest {

  @Mock
  private VectorStore vectorStore;
  @Mock
  private ObjectProvider<ChromaApi> chromaApiProvider;
  @Mock
  private Environment environment;
  @Mock
  private ChromaVectorStoreProperties chromaStoreProperties;
  @Mock
  private VectorStoreProperties vectorStoreProperties;
  @Mock
  private PortfolioChromaProperties portfolioChromaProperties;
  @Mock
  private ApplicationArguments applicationArguments;

  private DocumentIngestionService service;

  @BeforeEach
  void setUp() {
    service = new DocumentIngestionService(
        vectorStore,
        chromaApiProvider,
        environment,
        chromaStoreProperties,
        vectorStoreProperties,
        portfolioChromaProperties);
  }

  @Test
  void runSkipsWhenChromaDisabled() {
    when(portfolioChromaProperties.isEnabled()).thenReturn(false);
    service.run(applicationArguments);
    verify(chromaApiProvider, never()).getIfAvailable();
  }

  @Test
  void describeCollectionsThrowsWhenChromaDisabled() {
    when(portfolioChromaProperties.isEnabled()).thenReturn(false);
    assertThrows(ChromaFeatureDisabledException.class, () -> service.describeCollections());
  }

  @Test
  void describeCollectionsThrowsWhenChromaApiBeanMissing() {
    when(portfolioChromaProperties.isEnabled()).thenReturn(true);
    when(chromaApiProvider.getIfAvailable()).thenReturn(null);
    when(environment.getProperty("spring.ai.vectorstore.chroma.client.host", "")).thenReturn("localhost");
    when(environment.getProperty("spring.ai.vectorstore.chroma.client.port", Integer.class, 8100)).thenReturn(8100);

    IllegalStateException ex = assertThrows(IllegalStateException.class, () -> service.describeCollections());
    assertTrue(ex.getMessage().contains("ChromaApi bean is missing"));
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
    assertTrue(results.get(0).message().contains("Too many paths"));
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
    assertTrue(results.get(0).message().contains("Invalid path"));
  }

  @Test
  void ingestFromPathsRejectsUnsupportedExtension() throws Exception {
    when(vectorStoreProperties.getDocumentsToLoadDir()).thenReturn("file:./data/");
    var results = service.ingestFromPaths(List.of("x.exe"), false);
    assertEquals(1, results.size());
    assertTrue(results.get(0).message().contains("Unsupported file type"));
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
    assertTrue(service.listAvailableFiles().isEmpty());
  }

  @Test
  void listAvailableFilesReturnsEmptyForNonFileBaseUrl() throws Exception {
    when(vectorStoreProperties.getDocumentsToLoadDir()).thenReturn("http://example.invalid/docs/");
    assertTrue(service.listAvailableFiles().isEmpty());
  }

  @Test
  void ingestMultipartRejectsNullFile() throws Exception {
    var r = service.ingestMultipart(null, null, false);
    assertEquals("Empty file", r.message());
  }

  @Test
  void ingestMultipartRejectsEmptyUpload() throws Exception {
    var file = new MockMultipartFile("file", "a.txt", "text/plain", new byte[0]);
    var r = service.ingestMultipart(file, null, false);
    assertEquals("Empty file", r.message());
  }
}
