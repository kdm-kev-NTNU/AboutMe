package com.kevinmazali.portfolio.model;

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
@Table(name = "documents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentEntity {

  @Id
  @Column(length = 64)
  private String id;

  @Column(nullable = false, length = 512)
  private String filename;

  @Column(name = "source_uri")
  private String sourceUri;

  @Column(name = "content_hash", nullable = false, length = 64)
  private String contentHash;

  @Column(name = "ingested_at", nullable = false)
  private Instant ingestedAt;

  @Column(name = "replaced_at")
  private Instant replacedAt;
}
