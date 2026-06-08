package com.kevinmazali.portfolio.service;

import com.kevinmazali.portfolio.config.DocumentIngestProperties;
import com.kevinmazali.portfolio.model.interview.InterviewDocumentEntity;
import com.kevinmazali.portfolio.model.interview.InterviewDocumentResponse;
import com.kevinmazali.portfolio.repository.InterviewDocumentRepository;
import com.kevinmazali.portfolio.util.InputValidator;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewDocumentService {

  static final Set<String> ALLOWED_EXT =
      Set.of("pdf", "docx", "doc", "txt", "md", "png", "jpg", "jpeg", "gif", "bmp", "tiff", "webp", "svg");

  static final int MAX_CONTEXT_CHARS = 14_000;

  private final InterviewDocumentRepository documentRepository;
  private final DocumentIngestProperties documentIngestProperties;

  public InterviewDocumentResponse storeMultipart(MultipartFile file, String createdBy) throws IOException {
    if (file == null || file.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Empty file");
    }
    String originalName = Optional.ofNullable(file.getOriginalFilename()).orElse("upload.bin");
    String ext = extensionFromFilename(originalName);
    if (!ALLOWED_EXT.contains(ext)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported file type: " + ext);
    }
    byte[] bytes = file.getBytes();
    if (bytes.length > documentIngestProperties.getMaxParseBytes()) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "File exceeds maximum parse size of " + documentIngestProperties.getMaxParseBytes() + " bytes");
    }
    ByteArrayResource resource =
        new ByteArrayResource(bytes) {
          @Override
          public String getFilename() {
            return originalName;
          }
        };
    String parsed = parseResource(resource);
    return saveDocument(originalName, file.getContentType(), parsed, createdBy);
  }

  public InterviewDocumentResponse storeText(String text, String filename, String createdBy) {
    if (!StringUtils.hasText(text)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Text is required");
    }
    String safeName =
        StringUtils.hasText(filename) ? InputValidator.sanitizeString(filename.trim()) : "pasted-text.md";
    if (safeName.length() > 512) {
      safeName = safeName.substring(0, 512);
    }
    return saveDocument(safeName, "text/plain", text.trim(), createdBy);
  }

  public InterviewDocumentResponse getDocument(String id) {
    InterviewDocumentEntity entity =
        documentRepository
            .findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));
    return toResponse(entity);
  }

  public String contextForSession(String documentId) {
    InterviewDocumentEntity entity =
        documentRepository
            .findById(documentId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));
    return truncateContext(entity.getParsedText());
  }

  static String truncateContext(String text) {
    if (text == null) {
      return "";
    }
    if (text.length() <= MAX_CONTEXT_CHARS) {
      return text;
    }
    return text.substring(0, MAX_CONTEXT_CHARS) + "\n\n[truncated]";
  }

  private InterviewDocumentResponse saveDocument(
      String originalFilename, String mimeType, String parsedText, String createdBy) {
    if (!StringUtils.hasText(parsedText)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No text extracted from input");
    }
    String id = UUID.randomUUID().toString().replace("-", "");
    InterviewDocumentEntity entity =
        InterviewDocumentEntity.builder()
            .id(id)
            .originalFilename(originalFilename)
            .mimeType(mimeType)
            .parsedText(parsedText)
            .charCount(parsedText.length())
            .createdBy(createdBy)
            .createdAt(Instant.now())
            .build();
    documentRepository.save(entity);
    log.info("Stored interview document id={} filename={} chars={}", id, originalFilename, parsedText.length());
    return toResponse(entity);
  }

  private String parseResource(Resource resource) throws IOException {
    TikaDocumentReader reader = new TikaDocumentReader(resource);
    List<Document> docs = reader.get();
    if (docs == null || docs.isEmpty()) {
      return "";
    }
    StringBuilder sb = new StringBuilder();
    for (Document doc : docs) {
      if (doc.getText() != null && !doc.getText().isBlank()) {
        if (sb.length() > 0) {
          sb.append("\n\n");
        }
        sb.append(doc.getText().trim());
      }
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

  private static InterviewDocumentResponse toResponse(InterviewDocumentEntity entity) {
    return new InterviewDocumentResponse(
        entity.getId(),
        entity.getOriginalFilename(),
        entity.getMimeType(),
        entity.getCharCount(),
        entity.getCreatedBy(),
        entity.getCreatedAt());
  }
}
