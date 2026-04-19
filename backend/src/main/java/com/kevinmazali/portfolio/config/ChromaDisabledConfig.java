package com.kevinmazali.portfolio.config;

import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "portfolio.chroma.enabled", havingValue = "false")
public class ChromaDisabledConfig {

  @Bean
  public VectorStore vectorStore() {
    return new NoOpVectorStore();
  }
}
