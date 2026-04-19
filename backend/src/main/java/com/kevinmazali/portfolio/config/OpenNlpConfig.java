package com.kevinmazali.portfolio.config;

import lombok.extern.slf4j.Slf4j;
import opennlp.tools.namefind.TokenNameFinderModel;
import opennlp.tools.tokenize.TokenizerModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.io.InputStream;

/**
 * Loads OpenNLP models for PII name detection when the sanitizer is enabled.
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "portfolio.sanitizer.enabled", havingValue = "true", matchIfMissing = true)
public class OpenNlpConfig {

    @Bean
    public TokenNameFinderModel tokenNameFinderModel() throws IOException {
        try (InputStream is = getClass().getResourceAsStream("/models/en-ner-person.bin")) {
            if (is == null) {
                throw new IOException("OpenNLP model not found on classpath: /models/en-ner-person.bin");
            }
            log.info("Loaded OpenNLP person NER model from classpath");
            return new TokenNameFinderModel(is);
        }
    }

    @Bean
    public TokenizerModel tokenizerModel() throws IOException {
        try (InputStream is = getClass().getResourceAsStream("/models/en-token.bin")) {
            if (is == null) {
                throw new IOException("OpenNLP model not found on classpath: /models/en-token.bin");
            }
            log.info("Loaded OpenNLP tokenizer model from classpath");
            return new TokenizerModel(is);
        }
    }
}
