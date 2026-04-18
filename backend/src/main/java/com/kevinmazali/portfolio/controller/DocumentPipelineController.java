package com.kevinmazali.portfolio.controller;

import com.kevinmazali.portfolio.config.OpenApiConfig;
import com.kevinmazali.portfolio.model.ChunkListResponse;
import com.kevinmazali.portfolio.model.ChromaCollectionsResponse;
import com.kevinmazali.portfolio.model.DocumentListEntry;
import com.kevinmazali.portfolio.model.IngestionResult;
import com.kevinmazali.portfolio.service.DocumentIngestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Admin-only HTTP API for document ingestion and ChromaDB inspection.
 */
@RestController
@RequestMapping("/admin/tools/documents")
@RequiredArgsConstructor
@Tag(name = "Admin documents", description = "Ingest and manage indexed documents (ADMIN + HTTP Basic)")
@SecurityRequirement(name = OpenApiConfig.BASIC_AUTH_SCHEME)
public class DocumentPipelineController {

  private static final Set<String> ALLOWED_EXT = Set.of(
      "pdf", "docx", "doc", "txt", "md", "png", "jpg", "jpeg", "gif", "bmp", "tiff", "webp", "svg");

  private final DocumentIngestionService documentIngestionService;

  @Operation(summary = "Upload and ingest a document")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Ingestion finished (check success flag in body)",
          content = @Content(schema = @Schema(implementation = IngestionResult.class))),
      @ApiResponse(responseCode = "400", description = "Missing filename or unsupported type",
          content = @Content(schema = @Schema(implementation = IngestionResult.class)))
  })
  @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<IngestionResult> upload(
      @Parameter(description = "Document file", required = true)
      @RequestParam("file") MultipartFile file,
      @Parameter(description = "Optional display title")
      @RequestParam(value = "title", required = false) String title,
      @Parameter(description = "When true, re-ingest even if content hash exists")
      @RequestParam(value = "force", defaultValue = "false") boolean force
  ) throws IOException {
    String original = file.getOriginalFilename();
    if (original == null || original.isBlank()) {
      return ResponseEntity.badRequest().body(
          new IngestionResult("", "", 0, false, "Missing filename"));
    }
    String ext = extension(original);
    if (ext.isEmpty() || !ALLOWED_EXT.contains(ext)) {
      return ResponseEntity.badRequest().body(
          new IngestionResult("", original, 0, false, "Unsupported file type: " + ext));
    }
    IngestionResult result = documentIngestionService.ingestMultipart(file, title, force);
    return ResponseEntity.ok(result);
  }

  @Operation(summary = "List ingested documents")
  @ApiResponse(responseCode = "200",
      content = @Content(array = @ArraySchema(schema = @Schema(implementation = DocumentListEntry.class))))
  @GetMapping
  public List<DocumentListEntry> list() {
    return documentIngestionService.listDocuments();
  }

  /**
   * Paginated chunk listing with optional {@code documentId} filter (content hash / document_id in Chroma metadata).
   */
  @GetMapping("/chunks")
  public ChunkListResponse chunks(
      @RequestParam(value = "documentId", required = false) String documentId,
      @RequestParam(value = "limit", defaultValue = "25") int limit,
      @RequestParam(value = "offset", defaultValue = "0") int offset
  ) {
    return documentIngestionService.getChunks(documentId, limit, offset);
  }

  @Operation(summary = "Delete document by id")
  @ApiResponses({
      @ApiResponse(responseCode = "204", description = "Deleted"),
      @ApiResponse(responseCode = "400", description = "Blank document id")
  })
  @DeleteMapping("/{documentId}")
  public ResponseEntity<Void> delete(
      @Parameter(description = "Document identifier", required = true)
      @PathVariable("documentId") String documentId) {
    if (documentId == null || documentId.isBlank()) {
      return ResponseEntity.badRequest().build();
    }
    documentIngestionService.deleteByDocumentId(documentId);
    return ResponseEntity.noContent().build();
  }

  @Operation(summary = "Describe Chroma collections")
  @ApiResponse(responseCode = "200",
      content = @Content(schema = @Schema(implementation = ChromaCollectionsResponse.class)))
  @GetMapping("/collections")
  public ChromaCollectionsResponse collections() {
    return documentIngestionService.describeCollections();
  }

  /**
   * Re-ingests classpath seed documents (same sources as startup seed), replacing existing chunks
   * per content hash. Requires admin credentials.
   */
  @Operation(summary = "Re-seed from classpath", description = "Re-ingests built-in seed documents; replaces chunks per content hash.")
  @ApiResponse(responseCode = "200",
      content = @Content(array = @ArraySchema(schema = @Schema(implementation = IngestionResult.class))))
  @PostMapping("/reseed")
  public ResponseEntity<List<IngestionResult>> reseedClasspath() throws IOException {
    List<IngestionResult> results = documentIngestionService.reseedClasspathDocuments();
    return ResponseEntity.ok(results);
  }

  private static String extension(String filename) {
    int i = filename.lastIndexOf('.');
    if (i < 0 || i == filename.length() - 1) {
      return "";
    }
    return filename.substring(i + 1).toLowerCase(Locale.ROOT);
  }
}
