package com.kevinmazali.portfolio.model.interview;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "interview_documents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterviewDocumentEntity {

  @Id
  @Column(length = 64)
  private String id;

  @Column(name = "original_filename", nullable = false, length = 512)
  private String originalFilename;

  @Column(name = "mime_type", length = 128)
  private String mimeType;

  @Column(name = "parsed_text", nullable = false, columnDefinition = "TEXT")
  private String parsedText;

  @Column(name = "char_count", nullable = false)
  private int charCount;

  @Column(name = "created_by", length = 128)
  private String createdBy;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;
}
