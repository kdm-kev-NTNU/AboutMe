package com.kevinmazali.portfolio.service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.kevinmazali.portfolio.config.PostHogProperties;
import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Resolves PostHog feature flags for a distinct id via {@code /decide}, for attaching {@code $feature/*}
 * properties to {@code $ai_generation} analytics.
 */
@Slf4j
@Service
public class PostHogFeatureFlagService {

  private final PostHogProperties properties;
  private final ObjectMapper objectMapper;
  @Nullable
  private final RestClient restClient;

  public PostHogFeatureFlagService(PostHogProperties properties, ObjectMapper objectMapper) {
    this.properties = properties;
    this.objectMapper = objectMapper;
    if (properties.isFeatureFlagDecideConfigured()) {
      String base = normalizeBaseUrl(properties.getHost());
      JdkClientHttpRequestFactory rf = new JdkClientHttpRequestFactory();
      rf.setReadTimeout(Duration.ofMillis(Math.max(200, properties.getFeatureFlagsTimeoutMs())));
      this.restClient = RestClient.builder().requestFactory(rf).baseUrl(base).build();
    } else {
      this.restClient = null;
    }
  }

  public boolean isEnabled() {
    return restClient != null;
  }

  /**
   * Map of {@code $feature/<flagKey>} → variant (boolean, string, or number) for keys listed in config.
   * Returns an empty map when disabled, on transport errors, or when decide has no matching flags.
   */
  public Map<String, Object> resolveForDistinctId(String distinctId) {
    if (restClient == null) {
      return Map.of();
    }
    String id = distinctId != null && !distinctId.isBlank() ? distinctId : "unknown";
    List<String> wanted = properties.getFeatureFlagKeys();
    if (wanted == null || wanted.isEmpty()) {
      return Map.of();
    }
    try {
      Map<String, Object> body = Map.of(
          "api_key", properties.getApiKey().trim(),
          "distinct_id", id);
      String json =
          restClient
              .post()
              .uri("/decide?v=3")
              .contentType(MediaType.APPLICATION_JSON)
              .body(objectMapper.writeValueAsString(body))
              .retrieve()
              .body(String.class);
      if (json == null || json.isBlank()) {
        return Map.of();
      }
      JsonNode root = objectMapper.readTree(json);
      JsonNode flags = root.get("featureFlags");
      if (flags == null || !flags.isObject()) {
        return Map.of();
      }
      Map<String, Object> out = new LinkedHashMap<>();
      for (String key : wanted) {
        if (key == null || key.isBlank()) {
          continue;
        }
        String k = key.trim();
        if (!flags.has(k)) {
          continue;
        }
        JsonNode v = flags.get(k);
        Object java = jsonNodeToJava(v);
        if (java != null) {
          out.put("$feature/" + k, java);
        }
      }
      return out.isEmpty() ? Map.of() : Collections.unmodifiableMap(out);
    } catch (RestClientException e) {
      log.warn("PostHog /decide failed: {}", e.getMessage());
      return Map.of();
    } catch (Exception e) {
      log.warn("PostHog /decide parse failed: {}", e.getMessage());
      return Map.of();
    }
  }

  @Nullable
  private static Object jsonNodeToJava(JsonNode v) {
    if (v == null || v.isNull()) {
      return null;
    }
    if (v.isBoolean()) {
      return v.booleanValue();
    }
    if (v.isTextual()) {
      return v.asText();
    }
    if (v.isNumber()) {
      return v.numberValue();
    }
    return v.toString();
  }

  static String normalizeBaseUrl(String host) {
    if (host == null) {
      return "https://eu.i.posthog.com";
    }
    String h = host.trim();
    if (h.endsWith("/")) {
      h = h.substring(0, h.length() - 1);
    }
    return h;
  }
}
