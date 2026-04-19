package com.kevinmazali.portfolio.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Request body for ingesting documents by path relative to {@code sfg.aiapp.documentsToLoadDir}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PathIngestRequest(
    List<String> paths,
    Boolean force
) {

  public boolean forceOrDefault() {
    return Boolean.TRUE.equals(force);
  }
}
