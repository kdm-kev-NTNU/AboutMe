package com.kevinmazali.portfolio.service;

import com.kevinmazali.portfolio.config.OutboundHttp;
import com.kevinmazali.portfolio.config.VoiceKillSwitchProperties;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Writes the public voice kill switch via PostHog's private feature-flag management API.
 *
 * <p>Only the allow-listed key in {@link VoiceKillSwitchProperties#getFlagKey()} may be toggled.
 * Uses {@code POST .../enable} and {@code POST .../disable} on {@code eu.posthog.com} (not the
 * ingest host).
 */
@Slf4j
@Service
public class PostHogFeatureFlagManagementClient {

  /** Only this flag may be mutated through the admin kill-switch API. */
  static final String ALLOWED_FLAG_KEY = "aboutme_voice_kill_switch";

  private final VoiceKillSwitchProperties properties;
  private final ObjectMapper objectMapper;
  @Nullable private final RestClient restClient;
  private final AtomicReference<Long> cachedFlagId = new AtomicReference<>(null);

  @Autowired
  public PostHogFeatureFlagManagementClient(
      VoiceKillSwitchProperties properties, ObjectMapper objectMapper) {
    this(
        properties,
        objectMapper,
        properties.isManagementConfigured()
            ? RestClient.builder()
                .requestFactory(
                    OutboundHttp.requestFactory(Duration.ofSeconds(5), Duration.ofSeconds(15)))
                .baseUrl(normalizeApiHost(properties.getApiHost()))
                .defaultHeader("Authorization", "Bearer " + properties.getPersonalApiKey().trim())
                .build()
            : null);
  }

  /** Test-visible constructor for {@link org.springframework.test.web.client.MockRestServiceServer}. */
  PostHogFeatureFlagManagementClient(
      VoiceKillSwitchProperties properties,
      ObjectMapper objectMapper,
      @Nullable RestClient restClient) {
    this.properties = properties;
    this.objectMapper = objectMapper;
    this.restClient = restClient;
  }

  public boolean isConfigured() {
    return restClient != null;
  }

  /**
   * Enables or disables the allow-listed kill-switch flag.
   *
   * @param engaged {@code true} → enable flag (voice off); {@code false} → disable flag (voice on)
   * @throws IllegalStateException when management is not configured
   * @throws IllegalArgumentException when the configured key is not allow-listed
   * @throws RestClientException on transport or non-2xx responses
   */
  public void setKillSwitchEngaged(boolean engaged) {
    if (restClient == null) {
      throw new IllegalStateException("PostHog feature-flag management is not configured.");
    }
    String key = requireAllowListedKey();
    long id = resolveFlagId(key);
    String action = engaged ? "enable" : "disable";
    String path =
        "/api/projects/"
            + properties.getProjectId().trim()
            + "/feature_flags/"
            + id
            + "/"
            + action
            + "/";
    restClient.post().uri(path).retrieve().toBodilessEntity();
    log.info("posthog_feature_flag_{} flagKey={} flagId={}", action, key, id);
  }

  public String posthogFlagUrl() {
    Long id = cachedFlagId.get();
    String base =
        normalizeApiHost(properties.getApiHost())
            + "/project/"
            + properties.getProjectId().trim()
            + "/feature_flags";
    return id == null ? base : base + "/" + id;
  }

  private String requireAllowListedKey() {
    String key = properties.getFlagKey() == null ? "" : properties.getFlagKey().trim();
    if (!ALLOWED_FLAG_KEY.equals(key)) {
      throw new IllegalArgumentException(
          "Refusing to manage flag '" + key + "'; only " + ALLOWED_FLAG_KEY + " is allowed.");
    }
    return key;
  }

  long resolveFlagId(String key) {
    Long cached = cachedFlagId.get();
    if (cached != null) {
      return cached;
    }
    if (restClient == null) {
      throw new IllegalStateException("PostHog feature-flag management is not configured.");
    }
    String path =
        "/api/projects/"
            + properties.getProjectId().trim()
            + "/feature_flags/?search="
            + key;
    try {
      String json = restClient.get().uri(path).accept(MediaType.APPLICATION_JSON).retrieve().body(String.class);
      long id = extractFlagId(json, key);
      cachedFlagId.compareAndSet(null, id);
      return id;
    } catch (RestClientException e) {
      throw e;
    }
  }

  long extractFlagId(@Nullable String json, String key) {
    if (!StringUtils.hasText(json)) {
      throw new IllegalStateException("PostHog feature flag list returned empty body.");
    }
    try {
      JsonNode root = objectMapper.readTree(json);
      JsonNode results = root.get("results");
      if (results == null || !results.isArray()) {
        throw new IllegalStateException("PostHog feature flag list missing results array.");
      }
      for (JsonNode row : results) {
        if (key.equals(row.path("key").asText())) {
          long id = row.path("id").asLong(0);
          if (id > 0) {
            return id;
          }
        }
      }
      throw new IllegalStateException("PostHog feature flag not found for key: " + key);
    } catch (IllegalStateException e) {
      throw e;
    } catch (Exception e) {
      throw new IllegalStateException("Could not parse PostHog feature flag list: " + e.getMessage(), e);
    }
  }

  static String normalizeApiHost(String host) {
    if (host == null || host.isBlank()) {
      return "https://eu.posthog.com";
    }
    String h = host.trim();
    if (h.endsWith("/")) {
      h = h.substring(0, h.length() - 1);
    }
    return h;
  }
}
