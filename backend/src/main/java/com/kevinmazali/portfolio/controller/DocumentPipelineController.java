package com.kevinmazali.portfolio.controller;

import com.kevinmazali.portfolio.model.ChromaCollectionsResponse;
import com.kevinmazali.portfolio.model.DocumentListEntry;
import com.kevinmazali.portfolio.model.IngestionResult;
import com.kevinmazali.portfolio.service.DocumentIngestionService;
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
public class DocumentPipelineController {

  private static final Set<String> ALLOWED_EXT = Set.of(
      "pdf", "docx", "doc", "txt", "md", "png", "jpg", "jpeg", "gif", "bmp", "tiff", "webp", "svg");

  private final DocumentIngestionService documentIngestionService;

  @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<IngestionResult> upload(
      @RequestParam("file") MultipartFile file,
      @RequestParam(value = "title", required = false) String title,
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

  @GetMapping
  public List<DocumentListEntry> list() {
    return documentIngestionService.listDocuments();
  }

  @DeleteMapping("/{documentId}")
  public ResponseEntity<Void> delete(@PathVariable("documentId") String documentId) {
    if (documentId == null || documentId.isBlank()) {
      return ResponseEntity.badRequest().build();
    }
    documentIngestionService.deleteByDocumentId(documentId);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/collections")
  public ChromaCollectionsResponse collections() {
    return documentIngestionService.describeCollections();
  }

  private static String extension(String filename) {
    int i = filename.lastIndexOf('.');
    if (i < 0 || i == filename.length() - 1) {
      return "";
    }
    return filename.substring(i + 1).toLowerCase(Locale.ROOT);
  }
}
