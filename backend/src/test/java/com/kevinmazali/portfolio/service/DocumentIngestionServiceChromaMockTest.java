package com.kevinmazali.portfolio.service;

import com.kevinmazali.portfolio.config.PortfolioChromaProperties;
import com.kevinmazali.portfolio.config.SanitizerProperties;
import com.kevinmazali.portfolio.config.VectorStoreProperties;
import com.kevinmazali.portfolio.model.ChromaCollectionsResponse;
import com.kevinmazali.portfolio.model.IngestionResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chroma.vectorstore.ChromaApi;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.chroma.autoconfigure.ChromaVectorStoreProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.client.RestClientException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link DocumentIngestionService} exercised with mocked {@link ChromaApi} and {@link VectorStore}
 * so JaCoCo can include this class in the bundle without a real Chroma server.
 */
@ExtendWith(MockitoExtension.class)
class DocumentIngestionServiceChromaMockTest {

  private static final String TENANT = "default_tenant";
  private static final String DATABASE = "default_database";
  private static final String COLL_NAME = "portfolio-docs";
  private static final String COLLECTION_ID = "col-id-1";

  @Mock
  private VectorStore vectorStore;
  @Mock
  private ObjectProvider<ChromaApi> chromaApiProvider;
  @Mock
  private org.springframework.core.env.Environment environment;
  @Mock
  private ChromaVectorStoreProperties chromaStoreProperties;
  @Mock
  private VectorStoreProperties vectorStoreProperties;
  @Mock
  private PortfolioChromaProperties portfolioChromaProperties;
  @Mock
  private ObjectProvider<PiiSanitizerService> piiSanitizerProvider;
  @Mock
  private ChromaApi chromaApi;

  private DocumentIngestionService service;

  @BeforeEach
  void setUp() {
    reset(vectorStore, chromaApiProvider, environment, chromaStoreProperties, vectorStoreProperties,
        portfolioChromaProperties, chromaApi);
    SanitizerProperties sanitizerProperties = new SanitizerProperties();
    sanitizerProperties.setEnabled(false);
    service = new DocumentIngestionService(
        vectorStore,
        chromaApiProvider,
        environment,
        chromaStoreProperties,
        vectorStoreProperties,
        portfolioChromaProperties,
        new NoiseCleaner(),
        piiSanitizerProvider,
        sanitizerProperties);
  }

  private void stubChromaEnabledAndCollection() {
    when(portfolioChromaProperties.isEnabled()).thenReturn(true);
    when(chromaApiProvider.getIfAvailable()).thenReturn(chromaApi);
    when(environment.getProperty("spring.ai.vectorstore.chroma.client.host", "")).thenReturn("localhost");
    when(environment.getProperty("spring.ai.vectorstore.chroma.client.port", Integer.class, 8100)).thenReturn(8000);
    when(chromaStoreProperties.getTenantName()).thenReturn(TENANT);
    when(chromaStoreProperties.getDatabaseName()).thenReturn(DATABASE);
    when(chromaStoreProperties.getCollectionName()).thenReturn(COLL_NAME);
    when(chromaApi.getCollection(TENANT, DATABASE, COLL_NAME))
        .thenReturn(new ChromaApi.Collection(COLLECTION_ID, COLL_NAME, Map.of()));
  }

  private static ChromaApi.GetEmbeddingResponse emptyEmbeddingPage() {
    return new ChromaApi.GetEmbeddingResponse(List.of(), null, null, List.of());
  }

  @Test
  void ingestMultipartIndexesTextAndCallsVectorStore() throws Exception {
    stubChromaEnabledAndCollection();
    AtomicBoolean chunksAlreadyExist = new AtomicBoolean(false);
    when(chromaApi.getEmbeddings(eq(TENANT), eq(DATABASE), eq(COLLECTION_ID), any()))
        .thenAnswer(inv -> {
          ChromaApi.GetEmbeddingsRequest r = inv.getArgument(3);
          if (r.where() != null && Objects.equals(1, r.limit())) {
            return chunksAlreadyExist.get()
                ? new ChromaApi.GetEmbeddingResponse(List.of("hit"), null, null, null)
                : emptyEmbeddingPage();
          }
          return emptyEmbeddingPage();
        });

    var file = new MockMultipartFile(
        "file",
        "note.txt",
        "text/plain",
        "Hello from portfolio ingestion test. ".repeat(20).getBytes(StandardCharsets.UTF_8));
    var result = service.ingestMultipart(file, null, false);

    assertTrue(result.chunksIngested() > 0, "expected at least one chunk");
    assertEquals("OK", result.message());
    ArgumentCaptor<List<Document>> captor = ArgumentCaptor.forClass(List.class);
    verify(vectorStore).add(captor.capture());
    assertFalse(captor.getValue().isEmpty());
  }

  @Test
  void ingestFromResourceSkipsWhenSameContentAlreadyIndexed() throws Exception {
    stubChromaEnabledAndCollection();
    when(chromaApi.getEmbeddings(eq(TENANT), eq(DATABASE), eq(COLLECTION_ID), any()))
        .thenAnswer(inv -> {
          ChromaApi.GetEmbeddingsRequest r = inv.getArgument(3);
          if (r.where() != null && Objects.equals(1, r.limit())) {
            return new ChromaApi.GetEmbeddingResponse(List.of("existing"), null, null, null);
          }
          return emptyEmbeddingPage();
        });

    byte[] bytes = "stable-content-for-hash".getBytes(StandardCharsets.UTF_8);
    var result = service.ingestFromResource(new ByteArrayResource(bytes) {
      @Override
      public String getFilename() {
        return "same.txt";
      }
    }, false);

    assertTrue(result.skipped());
    assertEquals(0, result.chunksIngested());
    verify(vectorStore, never()).add(any(List.class));
  }

  @Test
  void ingestFromResourceForceReplaceDeletesExistingChunks() throws Exception {
    stubChromaEnabledAndCollection();
    AtomicInteger existenceCalls = new AtomicInteger(0);
    when(chromaApi.getEmbeddings(eq(TENANT), eq(DATABASE), eq(COLLECTION_ID), any()))
        .thenAnswer(inv -> {
          ChromaApi.GetEmbeddingsRequest r = inv.getArgument(3);
          if (r.where() != null && Objects.equals(1, r.limit())) {
            existenceCalls.incrementAndGet();
            return new ChromaApi.GetEmbeddingResponse(List.of("hit"), null, null, null);
          }
          return emptyEmbeddingPage();
        });

    byte[] bytes = "force-replace-content ".repeat(15).getBytes(StandardCharsets.UTF_8);
    var result = service.ingestFromResource(new ByteArrayResource(bytes) {
      @Override
      public String getFilename() {
        return "forced.txt";
      }
    }, true);

    assertTrue(result.chunksIngested() > 0);
    verify(vectorStore).delete(any(Filter.Expression.class));
    verify(vectorStore).add(any(List.class));
    assertEquals(1, existenceCalls.get());
  }

  @Test
  void seedFromClasspathSkipsWhenCollectionAlreadyHasEmbeddings() throws Exception {
    stubChromaEnabledAndCollection();
    when(chromaApi.countEmbeddings(TENANT, DATABASE, COLLECTION_ID)).thenReturn(12L);
    when(vectorStoreProperties.isForceReindex()).thenReturn(false);

    service.seedFromClasspathIfCollectionEmpty();

    verify(chromaApi, never()).getEmbeddings(anyString(), anyString(), anyString(), any());
  }

  @Test
  void seedFromClasspathDoesNothingWhenCountZeroAndNoResources() throws Exception {
    stubChromaEnabledAndCollection();
    when(chromaApi.countEmbeddings(TENANT, DATABASE, COLLECTION_ID)).thenReturn(0L);
    when(vectorStoreProperties.isForceReindex()).thenReturn(false);
    when(vectorStoreProperties.getDocumentsToLoad()).thenReturn(null);
    when(vectorStoreProperties.getDocumentsToLoadDir()).thenReturn(null);

    service.seedFromClasspathIfCollectionEmpty();

    verify(vectorStore, never()).add(any(List.class));
  }

  @Test
  void reseedClasspathDocumentsReturnsEmptyWhenNoResources() throws Exception {
    stubChromaEnabledAndCollection();
    when(vectorStoreProperties.getDocumentsToLoad()).thenReturn(null);
    when(vectorStoreProperties.getDocumentsToLoadDir()).thenReturn(null);

    assertTrue(service.reseedClasspathDocuments().isEmpty());
  }

  @Test
  void reseedClasspathDocumentsRecordsFailurePerResource() throws Exception {
    stubChromaEnabledAndCollection();
    Resource bad = mock(Resource.class);
    when(bad.getFilename()).thenReturn("bad.txt");
    when(bad.getInputStream()).thenThrow(new IOException("read failed"));
    when(vectorStoreProperties.getDocumentsToLoad()).thenReturn(List.of(bad));

    List<IngestionResult> results = service.reseedClasspathDocuments();
    assertEquals(1, results.size());
    assertFalse(results.get(0).message().isBlank());
    assertEquals(0, results.get(0).chunksIngested());
  }

  @Test
  void listDocumentsAggregatesChunksFromMetadataPages() {
    stubChromaEnabledAndCollection();
    AtomicInteger page = new AtomicInteger(0);
    when(chromaApi.getEmbeddings(eq(TENANT), eq(DATABASE), eq(COLLECTION_ID), any()))
        .thenAnswer(inv -> {
          ChromaApi.GetEmbeddingsRequest r = inv.getArgument(3);
          if (r.where() != null) {
            return emptyEmbeddingPage();
          }
          if (r.include() != null && r.include().size() == 1 && Objects.equals(500, r.limit())) {
            int p = page.getAndIncrement();
            if (p == 0) {
              Map<String, String> m1 = Map.of(
                  "document_id", "doc-a",
                  "filename", "alpha.txt",
                  "ingested_at", "2026-02-01T00:00:00Z",
                  "chunk_index", "0");
              Map<String, String> m2 = Map.of("document_id", "doc-a", "chunk_index", "1");
              Map<String, String> m3 = Map.of("chunk_index", "0");
              return new ChromaApi.GetEmbeddingResponse(null, null, null, List.of(m1, m2, m3));
            }
            return emptyEmbeddingPage();
          }
          return emptyEmbeddingPage();
        });

    var entries = service.listDocuments();
    assertEquals(1, entries.size());
    assertEquals("doc-a", entries.get(0).documentId());
    assertEquals(2, entries.get(0).chunkCount());
    assertEquals("alpha.txt", entries.get(0).filename());
  }

  @Test
  void getChunksWithoutDocumentIdUsesPagedEmbeddings() {
    stubChromaEnabledAndCollection();
    when(chromaApi.countEmbeddings(TENANT, DATABASE, COLLECTION_ID)).thenReturn(100L);
    when(chromaApi.getEmbeddings(eq(TENANT), eq(DATABASE), eq(COLLECTION_ID), any()))
        .thenAnswer(inv -> {
          ChromaApi.GetEmbeddingsRequest r = inv.getArgument(3);
          if (r.where() == null && r.include() != null && r.include().size() == 2) {
            Map<String, String> meta = Map.of("filename", "page.txt", "chunk_index", "0");
            return new ChromaApi.GetEmbeddingResponse(
                List.of("e1"), null, List.of("chunk body"), List.of(meta));
          }
          return emptyEmbeddingPage();
        });

    var resp = service.getChunks(null, 10, 0);
    assertEquals(COLL_NAME, resp.collectionName());
    assertEquals(1, resp.chunks().size());
    assertEquals("chunk body", resp.chunks().get(0).text());
  }

  @Test
  void getChunksWithDocumentIdFetchesAllMatchingThenWindows() {
    stubChromaEnabledAndCollection();
    when(chromaApi.countEmbeddings(TENANT, DATABASE, COLLECTION_ID)).thenReturn(50L);
    AtomicInteger fetchPage = new AtomicInteger(0);
    when(chromaApi.getEmbeddings(eq(TENANT), eq(DATABASE), eq(COLLECTION_ID), any()))
        .thenAnswer(inv -> {
          ChromaApi.GetEmbeddingsRequest r = inv.getArgument(3);
          if (r.where() != null && r.include() != null && r.include().size() == 2 && Objects.equals(500, r.limit())) {
            int p = fetchPage.getAndIncrement();
            if (p == 0) {
              Map<String, String> m0 = Map.of("document_id", "target", "chunk_index", "1", "filename", "t.txt");
              Map<String, String> m1 = Map.of("document_id", "target", "chunk_index", "0", "filename", "t.txt");
              return new ChromaApi.GetEmbeddingResponse(
                  List.of("a", "b"),
                  null,
                  List.of("second", "first"),
                  List.of(m0, m1));
            }
            return emptyEmbeddingPage();
          }
          return emptyEmbeddingPage();
        });

    var resp = service.getChunks("target", 1, 1);
    assertEquals(2, resp.totalMatching());
    assertEquals(1, resp.chunks().size());
    assertEquals(1, resp.chunks().get(0).chunkIndex());
  }

  @Test
  void describeCollectionsSummarizesCollectionsAndCount() {
    stubChromaEnabledAndCollection();
    when(chromaApi.listCollections(TENANT, DATABASE))
        .thenReturn(List.of(
            new ChromaApi.Collection("c1", "n1", Map.of()),
            new ChromaApi.Collection("c2", "n2", Map.of())));
    when(chromaApi.countEmbeddings(TENANT, DATABASE, COLLECTION_ID)).thenReturn(42L);

    ChromaCollectionsResponse resp = service.describeCollections();
    assertEquals(COLL_NAME, resp.activeCollectionName());
    assertEquals(42L, resp.activeCollectionEmbeddingCount());
    assertEquals(2, resp.collections().size());
    assertEquals("c1", resp.collections().get(0).id());
  }

  @Test
  void deleteByDocumentIdUsesVectorStoreFilter() {
    service.deleteByDocumentId("doc-xyz");
    verify(vectorStore).delete(any(Filter.Expression.class));
  }

  @Test
  void requireCollectionIdFailureWrapsRestClientException() {
    stubChromaEnabledAndCollection();
    when(chromaApi.getCollection(TENANT, DATABASE, COLL_NAME))
        .thenThrow(new RestClientException("connection refused"));

    IllegalStateException ex = assertThrows(IllegalStateException.class, () -> service.listDocuments());
    assertNotNull(ex.getMessage());
  }
}
