package com.kevinmazali.portfolio.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Feature flag for Chroma-backed RAG and admin document pipeline.
 * When disabled, {@link NoOpVectorStore} is used and document/Chroma admin APIs return 501.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "portfolio.chroma")
public class PortfolioChromaProperties {

  /**
   * When {@code false}, skips Chroma client beans, uses a no-op {@link org.springframework.ai.vectorstore.VectorStore},
   * and admin ingestion endpoints respond with HTTP 501. Set {@code CHROMA_ENABLED=false} on Railway if Chroma is not deployed.
   */
  private boolean enabled = true;
}
