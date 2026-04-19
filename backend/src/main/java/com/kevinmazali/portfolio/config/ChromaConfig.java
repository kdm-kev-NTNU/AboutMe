package com.kevinmazali.portfolio.config;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.micrometer.observation.ObservationRegistry;

import org.springframework.ai.chroma.vectorstore.ChromaApi;
import org.springframework.ai.chroma.vectorstore.ChromaVectorStore;
import org.springframework.ai.embedding.BatchingStrategy;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.TokenCountBatchingStrategy;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.chroma.autoconfigure.ChromaApiProperties;
import org.springframework.ai.vectorstore.chroma.autoconfigure.ChromaConnectionDetails;
import org.springframework.ai.vectorstore.chroma.autoconfigure.ChromaVectorStoreProperties;
import org.springframework.ai.vectorstore.observation.VectorStoreObservationConvention;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

/**
 * Replaces {@link org.springframework.ai.vectorstore.chroma.autoconfigure.ChromaVectorStoreAutoConfiguration}
 * so the {@link VectorStore} bean is created lazily: Chroma is not contacted during context refresh, which
 * avoids crash loops when ChromaDB is temporarily unreachable (e.g. Railway startup ordering).
 */
@Configuration
@EnableConfigurationProperties({ ChromaApiProperties.class, ChromaVectorStoreProperties.class })
public class ChromaConfig {

  /** Host/port/token wiring from {@code spring.ai.vectorstore.chroma.*} when no custom connection bean exists. */
  @Bean
  @ConditionalOnMissingBean(ChromaConnectionDetails.class)
  PropertiesChromaConnectionDetails chromaConnectionDetails(ChromaApiProperties properties) {
    return new PropertiesChromaConnectionDetails(properties);
  }

  /** Low-level REST client to Chroma; supports API key or basic auth from properties. */
  @Bean
  @ConditionalOnMissingBean
  public ChromaApi chromaApi(ChromaApiProperties apiProperties,
      ObjectProvider<RestClient.Builder> restClientBuilderProvider,
      ChromaConnectionDetails connectionDetails,
      ObjectMapper objectMapper) {

    String chromaUrl = String.format("%s:%s", connectionDetails.getHost(), connectionDetails.getPort());

    ChromaApi chromaApi = ChromaApi.builder()
        .baseUrl(chromaUrl)
        .restClientBuilder(restClientBuilderProvider.getIfAvailable(RestClient::builder))
        .objectMapper(objectMapper)
        .build();

    if (StringUtils.hasText(connectionDetails.getKeyToken())) {
      chromaApi = chromaApi.withKeyToken(connectionDetails.getKeyToken());
    }
    else if (StringUtils.hasText(apiProperties.getUsername()) && StringUtils.hasText(apiProperties.getPassword())) {
      chromaApi = chromaApi.withBasicAuthCredentials(apiProperties.getUsername(), apiProperties.getPassword());
    }

    return chromaApi;
  }

  /** Caps embedding batch size by token count before writes to Chroma. */
  @Bean
  @ConditionalOnMissingBean(BatchingStrategy.class)
  BatchingStrategy chromaBatchingStrategy() {
    return new TokenCountBatchingStrategy();
  }

  /**
   * Lazy so {@link org.springframework.beans.factory.InitializingBean#afterPropertiesSet} on
   * {@link ChromaVectorStore} (Chroma collection probe) runs only on first RAG / ingest use, not at startup.
   */
  @Bean
  @Lazy
  public VectorStore vectorStore(EmbeddingModel embeddingModel,
      ChromaApi chromaApi,
      ChromaVectorStoreProperties storeProperties,
      ObjectProvider<ObservationRegistry> observationRegistry,
      ObjectProvider<VectorStoreObservationConvention> customObservationConvention,
      BatchingStrategy chromaBatchingStrategy) {
    return ChromaVectorStore.builder(chromaApi, embeddingModel)
        .collectionName(storeProperties.getCollectionName())
        .databaseName(storeProperties.getDatabaseName())
        .tenantName(storeProperties.getTenantName())
        .initializeSchema(storeProperties.isInitializeSchema())
        .observationRegistry(observationRegistry.getIfUnique(() -> ObservationRegistry.NOOP))
        .customObservationConvention(customObservationConvention.getIfAvailable(() -> null))
        .batchingStrategy(chromaBatchingStrategy)
        .build();
  }

  static final class PropertiesChromaConnectionDetails implements ChromaConnectionDetails {

    private final ChromaApiProperties properties;

    PropertiesChromaConnectionDetails(ChromaApiProperties properties) {
      this.properties = properties;
    }

    @Override
    public String getHost() {
      return this.properties.getHost();
    }

    @Override
    public int getPort() {
      return this.properties.getPort();
    }

    @Override
    public String getKeyToken() {
      return this.properties.getKeyToken();
    }
  }
}
