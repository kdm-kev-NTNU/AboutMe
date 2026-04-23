package com.kevinmazali.portfolio.config;

import com.kevinmazali.portfolio.service.DocumentReranker;
import com.kevinmazali.portfolio.service.OnnxCrossEncoderReranker;
import com.kevinmazali.portfolio.service.PassThroughDocumentReranker;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.Nullable;

/**
 * Wires retrieval helpers: ONNX rerank when enabled and configured, otherwise pass-through.
 */
@Slf4j
@Configuration
public class RetrievalConfiguration {

  @Bean
  @ConditionalOnMissingBean
  public DocumentReranker documentReranker(
      RetrievalProperties props, @Nullable MeterRegistry meterRegistry) {
    if (!props.isRerankEnabled()) {
      return new PassThroughDocumentReranker();
    }
    String path = props.getOnnxModelPath();
    if (path == null || path.isBlank()) {
      log.warn(
          "portfolio.retrieval.rerank-enabled is true but portfolio.retrieval.onnx-model-path is empty; "
              + "using pass-through reranker");
      return new PassThroughDocumentReranker();
    }
    return new OnnxCrossEncoderReranker(props, meterRegistry);
  }
}
