package com.kevinmazali.portfolio.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * HTTP base URL and API key for Phoenix dataset REST calls (separate from OTLP tracing endpoint).
 */
@ConfigurationProperties(prefix = "portfolio.phoenix")
public class PhoenixProperties {

  /**
   * Phoenix UI / REST root, e.g. {@code http://localhost:6006} or Railway URL.
   */
  private String baseUrl = "";

  /**
   * Optional bearer token (same env as OTLP when using Phoenix Cloud / secured instance).
   */
  private String apiKey = "";

  public String getBaseUrl() {
    return baseUrl;
  }

  public void setBaseUrl(String baseUrl) {
    this.baseUrl = baseUrl;
  }

  public String getApiKey() {
    return apiKey;
  }

  public void setApiKey(String apiKey) {
    this.apiKey = apiKey;
  }

  public boolean isConfigured() {
    return baseUrl != null && !baseUrl.isBlank();
  }
}
