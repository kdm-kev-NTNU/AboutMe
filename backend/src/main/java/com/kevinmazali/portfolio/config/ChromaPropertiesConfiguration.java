package com.kevinmazali.portfolio.config;

import org.springframework.ai.vectorstore.chroma.autoconfigure.ChromaApiProperties;
import org.springframework.ai.vectorstore.chroma.autoconfigure.ChromaVectorStoreProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Binds Chroma-related properties even when {@link ChromaConfig} is not active
 * ({@code portfolio.chroma.enabled=false}).
 */
@Configuration
@EnableConfigurationProperties({
    ChromaApiProperties.class,
    ChromaVectorStoreProperties.class,
    PortfolioChromaProperties.class
})
public class ChromaPropertiesConfiguration {
}
