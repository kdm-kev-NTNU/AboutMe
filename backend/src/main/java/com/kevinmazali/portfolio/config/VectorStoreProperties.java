package com.kevinmazali.portfolio.config;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.util.List;

/**
 * Configuration properties for document seeding sources and optional content encryption.
 */
@Getter
@Configuration
@ConfigurationProperties(prefix = "sfg.aiapp")
public class VectorStoreProperties {

  /**
   * Document sources to ingest on first startup. Supports file:, classpath:, http:, https:.
   */
  private List<Resource> documentsToLoad;

  /**
   * Enables encryption of document content stored in the vector store. Default: true.
   */
  private boolean encryptContent = true;

  /**
   * Base64-encoded AES-256 key (32 bytes) used for encryption/decryption.
   * Can alternatively be provided via the VECTORSTORE_ENC_KEY environment variable.
   * When both are present, this property takes precedence.
   */
  private String encryptionKeyBase64;

  /**
   * Base directory to scan for documents to ingest. Supports classpath:, file:, etc.
   * Example: classpath:/tmp/docs/
   */
  private String documentsToLoadDir;

  /**
   * When {@code true}, startup classpath seeding re-ingests seed files even if the Chroma collection
   * already has embeddings, replacing chunks per {@code content_hash} / {@code document_id}.
   */
  private boolean forceReindex = false;

  public void setDocumentsToLoad(List<Resource> documentsToLoad) {
    this.documentsToLoad = documentsToLoad;
  }

  public void setEncryptContent(boolean encryptContent) {
    this.encryptContent = encryptContent;
  }

  public void setEncryptionKeyBase64(String encryptionKeyBase64) {
    this.encryptionKeyBase64 = encryptionKeyBase64;
  }

  public void setDocumentsToLoadDir(String documentsToLoadDir) {
    this.documentsToLoadDir = documentsToLoadDir;
  }

  public void setForceReindex(boolean forceReindex) {
    this.forceReindex = forceReindex;
  }
}
