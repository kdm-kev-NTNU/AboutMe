package com.kevinmazali.portfolio.service;

import com.kevinmazali.portfolio.config.VectorStoreProperties;
import com.kevinmazali.portfolio.model.ChromaCollectionSummary;
import com.kevinmazali.portfolio.model.ChromaCollectionsResponse;
import com.kevinmazali.portfolio.model.DocumentListEntry;
import com.kevinmazali.portfolio.model.IngestionResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chroma.vectorstore.ChromaApi;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.ai.vectorstore.chroma.autoconfigure.ChromaVectorStoreProperties;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Phased document pipeline (read → split → ChromaDB), inspired by
 * Piscada-style ingestion, adapted for Spring AI {@link VectorStore}.
 */
@Slf4j
@Service
@Order(Ordered.LOWEST_PRECEDENCE)
public class DocumentIngestionService implements ApplicationRunner {

  private static final List<String> SUPPORTED_EXTENSIONS = List.of(
      "pdf", "docx", "doc", "txt", "md", "png", "jpg", "jpeg", "gif", "bmp", "tiff", "webp", "svg");

  private final VectorStore vectorStore;
  private final ChromaApi chromaApi;
  private final ChromaVectorStoreProperties chromaStoreProperties;
  private final VectorStoreProperties vectorStoreProperties;

  public DocumentIngestionService(
      @Lazy VectorStore vectorStore,
      ChromaApi chromaApi,
      ChromaVectorStoreProperties chromaStoreProperties,
      VectorStoreProperties vectorStoreProperties) {
    this.vectorStore = vectorStore;
    this.chromaApi = chromaApi;
    this.chromaStoreProperties = chromaStoreProperties;
    this.vectorStoreProperties = vectorStoreProperties;
  }

  @Override
  public void run(ApplicationArguments args) {
    try {
      seedFromClasspathIfCollectionEmpty();
    } catch (Exception e) {
      log.warn("Startup vector seed skipped or failed: {}", e.getMessage(), e);
    }
  }

  /**
   * When the configured Chroma collection has no embeddings yet, ingest documents from
   * {@link VectorStoreProperties#getDocumentsToLoad()} or {@code documentsToLoadDir}.
   */
  public void seedFromClasspathIfCollectionEmpty() throws IOException {
    String collectionId = requireCollectionId();
    Long count = chromaApi.countEmbeddings(
        chromaStoreProperties.getTenantName(),
        chromaStoreProperties.getDatabaseName(),
        collectionId);
    boolean forceReindex = vectorStoreProperties.isForceReindex();
    if (count != null && count > 0 && !forceReindex) {
      log.info("Chroma collection '{}' already has {} embeddings; skipping classpath seed.",
          chromaStoreProperties.getCollectionName(), count);
      return;
    }
    if (forceReindex && count != null && count > 0) {
      log.warn(
          "forceReindex=true: re-ingesting classpath seed documents (existing {} embeddings in '{}').",
          count, chromaStoreProperties.getCollectionName());
    }
    List<Resource> resources = resolveClasspathResources();
    if (resources.isEmpty()) {
      log.info("No classpath/file seed documents found for initial Chroma ingest.");
      return;
    }
    log.info("Seeding Chroma from {} resource(s) (force replace={}).", resources.size(), forceReindex);
    for (Resource res : resources) {
      try {
        ingestFromResource(res, forceReindex);
      } catch (Exception e) {
        log.error("Seed ingest failed for {}: {}", safeName(res), e.getMessage(), e);
      }
    }
  }

  /**
   * Re-ingests all classpath seed documents ({@code documentsToLoad} or {@code documentsToLoadDir}),
   * replacing existing chunks per {@code document_id} (content hash). Admin-only at the HTTP layer.
   */
  public List<IngestionResult> reseedClasspathDocuments() throws IOException {
    requireCollectionId();
    List<Resource> resources = resolveClasspathResources();
    if (resources.isEmpty()) {
      log.warn("reseedClasspathDocuments: no seed resources resolved.");
      return List.of();
    }
    log.info("Admin reseed: re-ingesting {} classpath resource(s) with force replace.", resources.size());
    List<IngestionResult> results = new ArrayList<>();
    for (Resource res : resources) {
      try {
        results.add(ingestFromResource(res, true));
      } catch (Exception e) {
        log.error("Reseed ingest failed for {}: {}", safeName(res), e.getMessage(), e);
        String name = Optional.ofNullable(res.getFilename()).orElse(safeName(res));
        results.add(new IngestionResult("", name, 0, false, e.getMessage()));
      }
    }
    return results;
  }

  public IngestionResult ingestMultipart(MultipartFile file, String titleOverride, boolean force) throws IOException {
    if (file == null || file.isEmpty()) {
      return new IngestionResult("", "", 0, false, "Empty file");
    }
    String originalName = Optional.ofNullable(file.getOriginalFilename()).orElse("upload.bin");
    byte[] bytes = file.getBytes();
    String contentHash = sha256Hex(bytes);
    String logicalName = (titleOverride != null && !titleOverride.isBlank()) ? titleOverride.trim() : originalName;

    ByteArrayResource resource = new ByteArrayResource(bytes) {
      @Override
      public String getFilename() {
        return logicalName;
      }
    };

    return ingestFromResource(resource, contentHash, logicalName, force);
  }

  public IngestionResult ingestFromResource(Resource resource, boolean force) throws IOException {
    byte[] bytes;
    try (var in = resource.getInputStream()) {
      bytes = in.readAllBytes();
    }
    String contentHash = sha256Hex(bytes);
    String name = Optional.ofNullable(resource.getFilename()).orElse("resource");
    return ingestFromResource(resource, contentHash, name, force);
  }

  private IngestionResult ingestFromResource(Resource resource, String contentHash, String displayFilename, boolean force)
      throws IOException {
    requireCollectionId();

    if (!force && documentChunksExist(contentHash)) {
      return new IngestionResult(contentHash, displayFilename, 0, true,
          "Same content already indexed (document_id = content hash). Use force to replace.");
    }
    if (force && documentChunksExist(contentHash)) {
      deleteByDocumentId(contentHash);
    }

    TikaDocumentReader reader = new TikaDocumentReader(resource);
    List<Document> docs = reader.get();
    if (docs == null || docs.isEmpty()) {
      return new IngestionResult(contentHash, displayFilename, 0, false, "No text extracted from file");
    }

    List<Document> processed = processMultimodalDocuments(docs, resource, displayFilename);
    TextSplitter splitter = new TokenTextSplitter();
    List<Document> splitDocs = splitter.apply(processed);
    if (splitDocs == null || splitDocs.isEmpty()) {
      return new IngestionResult(contentHash, displayFilename, 0, false, "No chunks after splitting");
    }

    String ingestedAt = Instant.now().toString();
    List<Document> toAdd = new ArrayList<>();
    for (int i = 0; i < splitDocs.size(); i++) {
      Document d = splitDocs.get(i);
      Map<String, Object> meta = new HashMap<>(d.getMetadata());
      meta.put("document_id", contentHash);
      meta.put("content_hash", contentHash);
      meta.put("chunk_index", i);
      meta.put("filename", displayFilename);
      meta.put("source", safeName(resource));
      meta.put("ingested_at", ingestedAt);

      String text = d.getText();

      String chunkId = contentHash + "_" + i;
      toAdd.add(Document.builder()
          .id(chunkId)
          .text(text)
          .metadata(meta)
          .build());
    }

    vectorStore.add(toAdd);
    log.info("Ingested {} chunks for document_id={} ({})", toAdd.size(), contentHash, displayFilename);
    return new IngestionResult(contentHash, displayFilename, toAdd.size(), false, "OK");
  }

  public List<DocumentListEntry> listDocuments() {
    String collectionId = requireCollectionId();
    Map<String, Agg> grouped = new LinkedHashMap<>();
    int offset = 0;
    final int page = 500;
    while (true) {
      ChromaApi.GetEmbeddingResponse pageResp = chromaApi.getEmbeddings(
          chromaStoreProperties.getTenantName(),
          chromaStoreProperties.getDatabaseName(),
          collectionId,
          new ChromaApi.GetEmbeddingsRequest(
              null,
              null,
              page,
              offset,
              List.of(ChromaApi.QueryRequest.Include.METADATAS)));
      if (pageResp == null || pageResp.metadata() == null || pageResp.metadata().isEmpty()) {
        break;
      }
      List<Map<String, Object>> metas = flattenMetadata(pageResp.metadata());
      for (Map<String, Object> meta : metas) {
        if (meta == null) {
          continue;
        }
        Object docIdObj = meta.get("document_id");
        if (docIdObj == null) {
          continue;
        }
        String docId = String.valueOf(docIdObj);
        Agg agg = grouped.computeIfAbsent(docId, Agg::new);
        agg.chunkCount++;
        Object fn = meta.get("filename");
        if (fn != null && (agg.filename == null || agg.filename.isBlank())) {
          agg.filename = String.valueOf(fn);
        }
        Object ing = meta.get("ingested_at");
        if (ing != null) {
          String ingStr = String.valueOf(ing);
          if (agg.lastIngestedAt == null || ingStr.compareTo(agg.lastIngestedAt) > 0) {
            agg.lastIngestedAt = ingStr;
          }
        }
      }
      if (metas.size() < page) {
        break;
      }
      offset += page;
    }

    return grouped.values().stream()
        .map(a -> new DocumentListEntry(
            a.documentId,
            Optional.ofNullable(a.filename).orElse("(unknown)"),
            a.chunkCount,
            Optional.ofNullable(a.lastIngestedAt).orElse("")))
        .sorted(Comparator.comparing(DocumentListEntry::filename))
        .collect(Collectors.toList());
  }

  public void deleteByDocumentId(String documentId) {
    FilterExpressionBuilder b = new FilterExpressionBuilder();
    vectorStore.delete(b.eq("document_id", documentId).build());
    log.info("Deleted chunks for document_id={}", documentId);
  }

  public ChromaCollectionsResponse describeCollections() {
    String tenant = chromaStoreProperties.getTenantName();
    String database = chromaStoreProperties.getDatabaseName();
    List<?> raw = chromaApi.listCollections(tenant, database);
    List<ChromaCollectionSummary> summaries = new ArrayList<>();
    if (raw != null) {
      for (Object o : raw) {
        if (o instanceof ChromaApi.Collection c) {
          summaries.add(new ChromaCollectionSummary(c.id(), c.name()));
        }
      }
    }
    String collectionId = requireCollectionId();
    Long count = chromaApi.countEmbeddings(tenant, database, collectionId);
    return new ChromaCollectionsResponse(
        chromaStoreProperties.getCollectionName(),
        count == null ? 0L : count,
        summaries);
  }

  private boolean documentChunksExist(String documentId) {
    String collectionId = requireCollectionId();
    Map<String, Object> where = Map.of("document_id", documentId);
    ChromaApi.GetEmbeddingResponse resp = chromaApi.getEmbeddings(
        chromaStoreProperties.getTenantName(),
        chromaStoreProperties.getDatabaseName(),
        collectionId,
        new ChromaApi.GetEmbeddingsRequest(null, where, 1, 0, List.of(ChromaApi.QueryRequest.Include.METADATAS)));
    return resp != null && resp.ids() != null && !resp.ids().isEmpty();
  }

  private String requireCollectionId() {
    ChromaApi.Collection col = chromaApi.getCollection(
        chromaStoreProperties.getTenantName(),
        chromaStoreProperties.getDatabaseName(),
        chromaStoreProperties.getCollectionName());
    if (col == null) {
      throw new IllegalStateException("Chroma collection not found: " + chromaStoreProperties.getCollectionName());
    }
    return col.id();
  }

  private List<Resource> resolveClasspathResources() throws IOException {
    List<Resource> result = new ArrayList<>();
    if (vectorStoreProperties.getDocumentsToLoad() != null && !vectorStoreProperties.getDocumentsToLoad().isEmpty()) {
      result.addAll(vectorStoreProperties.getDocumentsToLoad());
      return result;
    }
    String baseDir = vectorStoreProperties.getDocumentsToLoadDir();
    if (baseDir == null || baseDir.isBlank()) {
      log.warn("No documentsToLoadDir configured; nothing to seed.");
      return result;
    }
    if (!baseDir.endsWith("/")) {
      baseDir = baseDir + "/";
    }
    PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
    if (baseDir.startsWith("classpath:")) {
      Resource base = resolver.getResource(baseDir);
      if (!base.exists()) {
        log.warn("Seed base directory does not exist: {}", baseDir);
        return result;
      }
    }
    for (String ext : SUPPORTED_EXTENSIONS) {
      String pattern = baseDir + "**/*." + ext;
      try {
        Resource[] found = resolver.getResources(pattern);
        result.addAll(Arrays.asList(found));
      } catch (Exception e) {
        log.debug("Skipped search for .{} in '{}': {}", ext, baseDir, e.getMessage());
      }
    }
    if (!result.isEmpty() && log.isInfoEnabled()) {
      log.info("Seed files from '{}':", baseDir);
      result.forEach(r -> log.info(" - {}", safeName(r)));
    }
    return result;
  }

  private static String safeName(Resource r) {
    try {
      return r.getURL().toString();
    } catch (Exception e) {
      return r.getFilename();
    }
  }

  private List<Document> processMultimodalDocuments(List<Document> documents, Resource resource, String displayFilename) {
    List<Document> processedDocs = new ArrayList<>();
    for (Document doc : documents) {
      try {
        String filename = displayFilename != null ? displayFilename : resource.getFilename();
        if (filename == null) {
          processedDocs.add(doc);
          continue;
        }
        String lowerFilename = filename.toLowerCase();
        Document copy = new Document(doc.getText(), new HashMap<>(doc.getMetadata()));
        if (lowerFilename.endsWith(".png") || lowerFilename.endsWith(".jpg")
            || lowerFilename.endsWith(".jpeg") || lowerFilename.endsWith(".gif")
            || lowerFilename.endsWith(".bmp") || lowerFilename.endsWith(".tiff")
            || lowerFilename.endsWith(".webp") || lowerFilename.endsWith(".svg")) {
          copy.getMetadata().put("content_type", "image");
        } else {
          copy.getMetadata().put("content_type", "text");
        }
        copy.getMetadata().put("filename", filename);
        copy.getMetadata().put("source", safeName(resource));
        processedDocs.add(copy);
      } catch (Exception e) {
        log.warn("Error while processing document from '{}': {}", safeName(resource), e.getMessage());
        processedDocs.add(doc);
      }
    }
    return processedDocs;
  }

  @SuppressWarnings("unchecked")
  private static List<Map<String, Object>> flattenMetadata(List<?> raw) {
    List<Map<String, Object>> out = new ArrayList<>();
    if (raw == null) {
      return out;
    }
    for (Object row : raw) {
      if (row instanceof List<?> inner) {
        for (Object m : inner) {
          if (m instanceof Map<?, ?> map) {
            out.add((Map<String, Object>) map);
          }
        }
      } else if (row instanceof Map<?, ?> map) {
        out.add((Map<String, Object>) map);
      }
    }
    return out;
  }

  private static String sha256Hex(byte[] data) {
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      byte[] digest = md.digest(data);
      StringBuilder sb = new StringBuilder(digest.length * 2);
      for (byte b : digest) {
        sb.append(String.format("%02x", b));
      }
      return sb.toString();
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }

  private static final class Agg {
    final String documentId;
    int chunkCount;
    String filename;
    String lastIngestedAt;

    Agg(String documentId) {
      this.documentId = documentId;
    }
  }
}
