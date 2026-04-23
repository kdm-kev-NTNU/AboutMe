package com.kevinmazali.portfolio.testsupport;

import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.autoconfigure.PgVectorStoreProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
@EnableConfigurationProperties(PgVectorStoreProperties.class)
public class VectorStoreTestConfiguration {

  @Bean
  @Primary
  VectorStore testVectorStore() {
    return new TestNoOpVectorStore();
  }
}
