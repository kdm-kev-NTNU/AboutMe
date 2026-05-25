package com.kevinmazali.portfolio.service;

import com.kevinmazali.portfolio.model.DocumentEntity;
import com.kevinmazali.portfolio.repository.DocumentRepository;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DocumentRegistryService {

  private final DocumentRepository documentRepository;

  @Transactional
  public void upsertFromIngest(String documentId, String filename, String sourceUri, Instant ingestedAt) {
    DocumentEntity existing = documentRepository.findById(documentId).orElse(null);
    if (existing == null) {
      documentRepository.save(
          DocumentEntity.builder()
              .id(documentId)
              .filename(filename)
              .sourceUri(sourceUri)
              .contentHash(documentId)
              .ingestedAt(ingestedAt)
              .build());
      return;
    }
    existing.setFilename(filename);
    if (sourceUri != null) {
      existing.setSourceUri(sourceUri);
    }
    if (ingestedAt != null && (existing.getIngestedAt() == null || ingestedAt.isAfter(existing.getIngestedAt()))) {
      existing.setIngestedAt(ingestedAt);
    }
    documentRepository.save(existing);
  }

  @Transactional(readOnly = true)
  public List<DocumentEntry> listEntries() {
    return documentRepository.findAllByOrderByFilenameAsc().stream()
        .map(d -> new DocumentEntry(d.getId(), d.getFilename(), d.getIngestedAt()))
        .toList();
  }

  public record DocumentEntry(String id, String filename, Instant ingestedAt) {}

  @Transactional
  public void markReplaced(String documentId, Instant replacedAt) {
    documentRepository.findById(documentId).ifPresent(doc -> {
      doc.setReplacedAt(replacedAt);
      documentRepository.save(doc);
    });
  }
}
