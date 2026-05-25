package com.kevinmazali.portfolio.service;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import com.kevinmazali.portfolio.config.DocumentIngestProperties;
import com.kevinmazali.portfolio.config.SanitizerProperties;
import com.kevinmazali.portfolio.config.VectorStoreProperties;
import com.kevinmazali.portfolio.model.ChunkExportResponse;
import com.kevinmazali.portfolio.model.ChunkItem;
import com.kevinmazali.portfolio.model.ChunkListResponse;
import com.kevinmazali.portfolio.model.DocumentListEntry;
import com.kevinmazali.portfolio.model.IngestionResult;
import com.kevinmazali.portfolio.model.SanitizeResult;
import com.kevinmazali.portfolio.model.VectorStoreCollectionEntry;
import com.kevinmazali.portfolio.model.VectorStoreInfoResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.ai.vectorstore.pgvector.autoconfigure.PgVectorStoreProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Phased document pipeline (read → split → pgvector), inspired by Piscada-style ingestion,
 * adapted for Spring AI {@link VectorStore}.
 */
@Slf4j
@Service
@Order(Ordered.LOWEST_PRECEDENCE)
public class DocumentIngestionService implements ApplicationRunner {

  private static final int MAX_PATH_INGEST_BATCH = 100;

  private static final List<String> SUPPORTED_EXTENSIONS = List.of(
      "pdf", "docx", "doc", "txt", "md", "png", "jpg", "jpeg", "gif", "bmp", "tiff", "webp", "svg");

  private final VectorStore vectorStore;
  private final JdbcTemplate jdbcTemplate;
  private final ObjectMapper objectMapper;
  private final PgVectorStoreProperties pgVectorStoreProperties;
  private final VectorStoreProperties vectorStoreProperties;
  private final NoiseCleaner noiseCleaner;
  private final PiiSanitizerService piiSanitizerService;
  private final boolean sanitizerEnabled;
  private final DocumentIngestProperties documentIngestProperties;
  private final ExecutorService parseExecutor = Executors.newVirtualThreadPerTaskExecutor();

  public DocumentIngestionService(
      @Lazy VectorStore vectorStore,
      JdbcTemplate jdbcTemplate,
      ObjectMapper objectMapper,
      PgVectorStoreProperties pgVectorStoreProperties,
      VectorStoreProperties vectorStoreProperties,
      NoiseCleaner noiseCleaner,
      ObjectProvider<PiiSanitizerService> piiSanitizerProvider,
      SanitizerProperties sanitizerProperties,
      DocumentIngestProperties documentIngestProperties) {
    this.vectorStore = vectorStore;
    this.jdbcTemplate = jdbcTemplate;
    this.objectMapper = objectMapper;
    this.pgVectorStoreProperties = pgVectorStoreProperties;
    this.vectorStoreProperties = vectorStoreProperties;
    this.noiseCleaner = noiseCleaner;
    this.piiSanitizerService = piiSanitizerProvider.getIfAvailable();
    this.sanitizerEnabled = sanitizerProperties.isEnabled();
    this.documentIngestProperties = documentIngestProperties;
  }

  private String qualifiedVectorTable() {
    return pgVectorStoreProperties.getSchemaName() + "." + pgVectorStoreProperties.getTableName();
  }

  private long countRows() {
    Long c = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + qualifiedVectorTable(), Long.class);
    return c == null ? 0L : c;
  }

  private void touchVectorStoreBean() {
    vectorStore.add(Collections.emptyList());
  }

  @Override
  public void run(ApplicationArguments args) {
    try {
      touchVectorStoreBean();
      seedFromClasspathIfCollectionEmpty();
    } catch (Exception e) {
      log.warn("Startup vector seed skipped or failed: {}", e.getMessage(), e);
    }
  }

  /**
   * When the vector table has no rows yet, ingest documents from
   * {@link VectorStoreProperties#getDocumentsToLoad()} or {@code documentsToLoadDir}.
   */
  public void seedFromClasspathIfCollectionEmpty() throws IOException {
    ensureVectorTableAccessible();
    long count = countRows();
    boolean forceReindex = vectorStoreProperties.isForceReindex();
    if (count > 0 && !forceReindex) {
      log.info("Vector table '{}' already has {} rows; skipping classpath seed.",
          qualifiedVectorTable(), count);
      return;
    }
    if (forceReindex && count > 0) {
      log.warn(
          "forceReindex=true: re-ingesting classpath seed documents (existing {} rows in '{}').",
          count, qualifiedVectorTable());
    }
    List<Resource> resources = resolveClasspathResources();
    if (resources.isEmpty()) {
      log.info("No classpath/file seed documents found for initial vector ingest.");
      return;
    }
    log.info("Seeding pgvector from {} resource(s) (force replace={}).", resources.size(), forceReindex);
    for (Resource res : resources) {
      try {
        ingestFromResource(res, forceReindex);
      } catch (Exception e) {
        log.error("Seed ingest failed for {}: {}", safeName(res), e.getMessage(), e);
      }
    }
  }

  public List<IngestionResult> reseedClasspathDocuments() throws IOException {
    ensureVectorTableAccessible();
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

  public List<IngestionResult> ingestFromPaths(List<String> relativePaths, boolean force) throws IOException {
    List<IngestionResult> results = new ArrayList<>();
    if (relativePaths == null || relativePaths.isEmpty()) {
      results.add(new IngestionResult("", "", 0, false, "No paths provided"));
      return results;
    }
    if (relativePaths.size() > MAX_PATH_INGEST_BATCH) {
      results.add(new IngestionResult("", "", 0, false,
          "Too many paths (max " + MAX_PATH_INGEST_BATCH + ")"));
      return results;
    }
    String baseDir = vectorStoreProperties.getDocumentsToLoadDir();
    if (baseDir == null || baseDir.isBlank()) {
      results.add(new IngestionResult("", "", 0, false, "documentsToLoadDir is not configured"));
      return results;
    }
    if (!baseDir.endsWith("/")) {
      baseDir = baseDir + "/";
    }
    PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
    for (String raw : relativePaths) {
      String sanitized = sanitizeRelativePath(raw);
      if (sanitized == null) {
        results.add(new IngestionResult("", raw == null ? "" : raw.trim(), 0, false,
            "Invalid path (empty or contains '..' or absolute segments)"));
        continue;
      }
      if (sanitized.isEmpty()) {
        results.add(new IngestionResult("", "", 0, false, "Empty path after trim"));
        continue;
      }
      String ext = extensionFromFilename(sanitized);
      if (ext.isEmpty() || !SUPPORTED_EXTENSIONS.contains(ext)) {
        results.add(new IngestionResult("", sanitized, 0, false, "Unsupported file type: " + ext));
        continue;
      }
      Resource resource = resolver.getResource(baseDir + sanitized);
      try {
        if (!resource.exists() || !resource.isReadable()) {
          results.add(new IngestionResult("", sanitized, 0, false, "File not found or not readable"));
          continue;
        }
        results.add(ingestFromResource(resource, force));
      } catch (Exception e) {
        log.warn("Path ingest failed for {}: {}", sanitized, e.getMessage());
        results.add(new IngestionResult("", sanitized, 0, false,
            e.getMessage() != null ? e.getMessage() : "Ingest failed"));
      }
    }
    return results;
  }

  public List<String> listAvailableFiles() throws IOException {
    String baseDir = vectorStoreProperties.getDocumentsToLoadDir();
    if (baseDir == null || baseDir.isBlank()) {
      return List.of();
    }
    if (!baseDir.toLowerCase(Locale.ROOT).startsWith("file:")) {
      log.debug("listAvailableFiles: skipping non-file base: {}", baseDir);
      return List.of();
    }
    PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
    Resource base = resolver.getResource(baseDir);
    if (!base.exists() || !base.isReadable()) {
      return List.of();
    }
    File root;
    try {
      root = base.getFile();
    } catch (IOException e) {
      log.warn("listAvailableFiles: cannot resolve directory for {}: {}", baseDir, e.getMessage());
      return List.of();
    }
    if (!root.isDirectory()) {
      return List.of();
    }
    Path rootPath = root.toPath();
    List<String> out = new ArrayList<>();
    try (Stream<Path> walk = Files.walk(rootPath)) {
      walk.filter(Files::isRegularFile).forEach(p -> {
        String name = p.getFileName().toString();
        String ext = extensionFromFilename(name);
        if (!ext.isEmpty() && SUPPORTED_EXTENSIONS.contains(ext)) {
          out.add(rootPath.relativize(p).toString().replace('\\', '/'));
        }
      });
    }
    out.sort(String.CASE_INSENSITIVE_ORDER);
    return out;
  }

  @Nullable
  static String sanitizeRelativePath(@Nullable String raw) {
    if (raw == null) {
      return null;
    }
    String p = raw.trim().replace('\\', '/');
    if (p.isEmpty()) {
      return null;
    }
    if (p.startsWith("/")) {
      return null;
    }
    String[] parts = p.split("/");
    StringBuilder sb = new StringBuilder();
    for (String part : parts) {
      if (part.isEmpty()) {
        continue;
      }
      if (".".equals(part) || "..".equals(part)) {
        return null;
      }
      if (sb.length() > 0) {
        sb.append('/');
      }
      sb.append(part);
    }
    if (sb.isEmpty()) {
      return null;
    }
    return sb.toString();
  }

  private static String extensionFromFilename(String filename) {
    int i = filename.lastIndexOf('.');
    if (i < 0 || i == filename.length() - 1) {
      return "";
    }
    return filename.substring(i + 1).toLowerCase(Locale.ROOT);
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
      bytes = in.readNBytes(Math.toIntExact(documentIngestProperties.getMaxParseBytes()) + 1);
    }
    if (bytes.length > documentIngestProperties.getMaxParseBytes()) {
      throw new IOException("File exceeds maximum parse size of " + documentIngestProperties.getMaxParseBytes() + " bytes");
    }
    String contentHash = sha256Hex(bytes);
    String name = Optional.ofNullable(resource.getFilename()).orElse("resource");
    return ingestFromResource(resource, contentHash, name, force);
  }

  private IngestionResult ingestFromResource(Resource resource, String contentHash, String displayFilename, boolean force)
      throws IOException {
    ensureVectorTableAccessible();

    if (!force && documentChunksExist(contentHash)) {
      return new IngestionResult(contentHash, displayFilename, 0, true,
          "Same content already indexed (document_id = content hash). Use force to replace.");
    }
    if (force && documentChunksExist(contentHash)) {
      deleteByDocumentId(contentHash);
    }

    TikaDocumentReader reader = new TikaDocumentReader(resource);
    List<Document> docs = parseDocumentsWithTimeout(reader);
    if (docs == null || docs.isEmpty()) {
      return new IngestionResult(contentHash, displayFilename, 0, false, "No text extracted from file");
    }

    List<Document> processed = processMultimodalDocuments(docs, resource, displayFilename);
    processed = sanitizeDocuments(processed);
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

  private List<Document> sanitizeDocuments(List<Document> documents) {
    if (!sanitizerEnabled) {
      return documents;
    }
    List<Document> result = new ArrayList<>(documents.size());
    for (Document doc : documents) {
      String text = doc.getText();
      if (text == null || text.isBlank()) {
        result.add(doc);
        continue;
      }

      NoiseCleaner.NoiseCleaningResult noiseResult = noiseCleaner.cleanNoise(text);
      String cleaned = noiseResult.cleanedText();

      Map<String, Object> meta = new HashMap<>(doc.getMetadata());
      meta.put("noise_chars_removed", noiseResult.originalLength() - noiseResult.cleanedLength());

      if (piiSanitizerService != null) {
        SanitizeResult piiResult = piiSanitizerService.sanitize(cleaned);
        cleaned = piiResult.sanitizedText();
        meta.put("pii_status", piiResult.complianceStatus());
        meta.put("pii_detected_count", piiResult.piiDetectedCount());
      }

      result.add(Document.builder()
          .text(cleaned)
          .metadata(meta)
          .build());
    }
    return result;
  }

  public List<DocumentListEntry> listDocuments() {
    ensureVectorTableAccessible();
    String sql = """
        SELECT metadata->>'document_id' AS doc_id,
               MAX(metadata->>'filename') AS filename,
               COUNT(*)::int AS cnt,
               MAX(metadata->>'ingested_at') AS last_ingested
        FROM %s
        WHERE COALESCE(metadata->>'document_id', '') <> ''
        GROUP BY metadata->>'document_id'
        ORDER BY MAX(metadata->>'filename')
        """.formatted(qualifiedVectorTable());
    return jdbcTemplate.query(sql, (rs, rowNum) -> new DocumentListEntry(
        rs.getString("doc_id"),
        Optional.ofNullable(rs.getString("filename")).filter(s -> !s.isBlank()).orElse("(unknown)"),
        rs.getInt("cnt"),
        Optional.ofNullable(rs.getString("last_ingested")).orElse("")));
  }

  private static final int CHUNK_LIST_MAX_LIMIT = 200;

  /** Paginates through {@link #getChunks(String, int, int)} and returns every chunk row for export. */
  public ChunkExportResponse exportChunks(@Nullable String documentId) {
    ensureVectorTableAccessible();
    List<ChunkItem> all = new ArrayList<>();
    int offset = 0;
    while (true) {
      ChunkListResponse page = getChunks(documentId, CHUNK_LIST_MAX_LIMIT, offset);
      if (page.chunks().isEmpty()) {
        break;
      }
      all.addAll(page.chunks());
      if (page.chunks().size() < CHUNK_LIST_MAX_LIMIT) {
        break;
      }
      offset += CHUNK_LIST_MAX_LIMIT;
    }
    String tableLabel = pgVectorStoreProperties.getTableName();
    String trimmedDoc = documentId == null ? "" : documentId.trim();
    String docField = trimmedDoc.isEmpty() ? null : trimmedDoc;
    return new ChunkExportResponse(Instant.now(), tableLabel, docField, all.size(), all);
  }

  public ChunkListResponse getChunks(@Nullable String documentId, int limit, int offset) {
    ensureVectorTableAccessible();
    String table = qualifiedVectorTable();
    String tableLabel = pgVectorStoreProperties.getTableName();
    int lim = Math.min(Math.max(limit, 1), CHUNK_LIST_MAX_LIMIT);
    int off = Math.max(offset, 0);

    long total = countRows();

    String trimmedDocId = documentId == null ? "" : documentId.trim();
    if (!trimmedDocId.isEmpty()) {
      Long matching = jdbcTemplate.queryForObject(
          "SELECT COUNT(*) FROM " + table + " WHERE metadata->>'document_id' = ?",
          Long.class,
          trimmedDocId);
      long totalMatching = matching == null ? 0L : matching;
      String listSql = """
          SELECT id, content, metadata::text AS metadata
          FROM %s
          WHERE metadata->>'document_id' = ?
          ORDER BY (NULLIF(trim(metadata->>'chunk_index'), ''))::int NULLS LAST, id
          LIMIT ? OFFSET ?
          """.formatted(table);
      List<ChunkItem> page = jdbcTemplate.query(listSql, (rs, rowNum) -> mapRowToChunkItem(rs), trimmedDocId, lim, off);
      return new ChunkListResponse(tableLabel, total, totalMatching, lim, off, page);
    }

    String pageSql = """
        SELECT id, content, metadata::text AS metadata
        FROM %s
        ORDER BY id
        LIMIT ? OFFSET ?
        """.formatted(table);
    List<ChunkItem> page = jdbcTemplate.query(pageSql, (rs, rowNum) -> mapRowToChunkItem(rs), lim, off);
    return new ChunkListResponse(tableLabel, total, total, lim, off, page);
  }

  private ChunkItem mapRowToChunkItem(java.sql.ResultSet rs) throws java.sql.SQLException {
    String id = rs.getString("id");
    String text = Optional.ofNullable(rs.getString("content")).orElse("");
    Map<String, Object> meta = parseMetadataJson(rs.getString("metadata"));
    String title = Optional.ofNullable(meta.get("filename"))
        .map(String::valueOf)
        .filter(s -> !s.isBlank())
        .orElse("");
    Integer chunkIndex = parseChunkIndex(meta.get("chunk_index"));
    return new ChunkItem(id, title, chunkIndex, text, meta);
  }

  private Map<String, Object> parseMetadataJson(@Nullable String json) {
    if (json == null || json.isBlank()) {
      return new HashMap<>();
    }
    try {
      Map<String, Object> m = objectMapper.readValue(json, new TypeReference<>() {});
      return m == null ? new HashMap<>() : new HashMap<>(m);
    } catch (Exception e) {
      log.debug("Failed to parse metadata json: {}", e.getMessage());
      return new HashMap<>();
    }
  }

  private static Integer parseChunkIndex(@Nullable Object o) {
    if (o == null) {
      return null;
    }
    if (o instanceof Number n) {
      return n.intValue();
    }
    try {
      return Integer.parseInt(String.valueOf(o));
    } catch (NumberFormatException e) {
      return null;
    }
  }

  public void deleteByDocumentId(String documentId) {
    FilterExpressionBuilder b = new FilterExpressionBuilder();
    vectorStore.delete(b.eq("document_id", documentId).build());
    log.info("Deleted chunks for document_id={}", documentId);
  }

  public VectorStoreInfoResponse describeCollections() {
    ensureVectorTableAccessible();
    String name = pgVectorStoreProperties.getTableName();
    long count = countRows();
    return new VectorStoreInfoResponse(
        name,
        count,
        List.of(new VectorStoreCollectionEntry("", name)));
  }

  private boolean documentChunksExist(String documentId) {
    Long n = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM " + qualifiedVectorTable() + " WHERE metadata->>'document_id' = ?",
        Long.class,
        documentId);
    return n != null && n > 0;
  }

  private void ensureVectorTableAccessible() {
    try {
      jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + qualifiedVectorTable(), Long.class);
    } catch (DataAccessException e) {
      throw new IllegalStateException(
          "Cannot access vector table " + qualifiedVectorTable() + ": " + e.getMessage(), e);
    }
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

  private List<Document> parseDocumentsWithTimeout(TikaDocumentReader reader) throws IOException {
    Callable<List<Document>> task = reader::get;
    try {
      List<Document> docs = parseExecutor
          .submit(task)
          .get(documentIngestProperties.getParseTimeoutSeconds(), TimeUnit.SECONDS);
      return docs == null ? List.of() : docs;
    } catch (TimeoutException e) {
      throw new IOException("Document parsing timed out after "
          + documentIngestProperties.getParseTimeoutSeconds() + " seconds");
    } catch (ExecutionException e) {
      Throwable cause = e.getCause() != null ? e.getCause() : e;
      if (cause instanceof IOException io) {
        throw io;
      }
      throw new IOException("Document parsing failed: " + cause.getMessage(), cause);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IOException("Document parsing interrupted", e);
    }
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
}
