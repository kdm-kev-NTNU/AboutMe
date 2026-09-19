package com.kevinmazali.portfolio.service;

import com.kevinmazali.portfolio.config.OutboundHttp;
import com.kevinmazali.portfolio.config.PostHogProperties;
import com.kevinmazali.portfolio.config.VoiceKillSwitchProperties;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Cached PostHog kill switch for <em>public</em> live voice.
 *
 * <p>Tri-state contract:
 *
 * <ul>
 *   <li>{@link State#ENGAGED} — flag evaluates true → hide public voice
 *   <li>{@link State#NOT_ENGAGED} — flag inactive/false/absent → public voice allowed (subject to
 *       realtime capability)
 *   <li>{@link State#UNKNOWN} — transport/parse failure; never overwrites a known value
 * </ul>
 *
 * <p>Cold start with no successful poll yet treats the switch as not engaged so {@code
 * PORTFOLIO_REALTIME_ENABLED} remains the gate.
 */
@Slf4j
@Service
public class VoiceKillSwitch {

  public enum State {
    ENGAGED,
    NOT_ENGAGED,
    UNKNOWN
  }

  public enum Source {
    POSTHOG,
    COLD_START,
    MANAGEMENT_WRITE,
    DISABLED
  }

  public record Snapshot(
      State state, boolean engaged, Source source, @Nullable Instant lastSyncedAt, String flagKey) {}

  private final VoiceKillSwitchProperties properties;
  private final PostHogProperties postHogProperties;
  private final ObjectMapper objectMapper;
  @Nullable private final RestClient flagsClient;

  private final AtomicReference<State> knownState = new AtomicReference<>(State.UNKNOWN);
  private final AtomicReference<Source> source = new AtomicReference<>(Source.COLD_START);
  private final AtomicReference<Instant> lastSyncedAt = new AtomicReference<>(null);

  public VoiceKillSwitch(
      VoiceKillSwitchProperties properties,
      PostHogProperties postHogProperties,
      ObjectMapper objectMapper) {
    this.properties = properties;
    this.postHogProperties = postHogProperties;
    this.objectMapper = objectMapper;
    if (properties.isFlagsReadConfigured() && postHogProperties.isCaptureConfigured()) {
      this.flagsClient =
          RestClient.builder()
              .requestFactory(
                  OutboundHttp.requestFactory(
                      Duration.ofSeconds(2),
                      Duration.ofMillis(
                          Math.max(200, postHogProperties.getFeatureFlagsTimeoutMs()))))
              .baseUrl(normalizeHost(postHogProperties.getHost()))
              .build();
    } else {
      this.flagsClient = null;
      if (!properties.isEnabled()) {
        source.set(Source.DISABLED);
        knownState.set(State.NOT_ENGAGED);
      }
    }
  }

  /** True when public voice should be hidden because the kill switch is engaged. */
  public boolean isEngaged() {
    return knownState.get() == State.ENGAGED;
  }

  public Snapshot snapshot() {
    State state = knownState.get();
    boolean engaged = state == State.ENGAGED;
    return new Snapshot(
        state, engaged, source.get(), lastSyncedAt.get(), properties.getFlagKey());
  }

  public boolean isPosthogReadConfigured() {
    return flagsClient != null;
  }

  /**
   * Applies a known state after a successful management write so the admin UI does not wait for the
   * next poll.
   */
  public void applyKnownState(boolean engaged, Source writeSource) {
    knownState.set(engaged ? State.ENGAGED : State.NOT_ENGAGED);
    source.set(writeSource);
    lastSyncedAt.set(Instant.now());
  }

  @Scheduled(fixedDelayString = "${portfolio.voice-kill-switch.poll-interval-ms:30000}")
  public void refresh() {
    if (!properties.isEnabled() || flagsClient == null) {
      return;
    }
    State evaluated = evaluateFromPostHog();
    if (evaluated == State.UNKNOWN) {
      // Never overwrite a known value with UNKNOWN (last-known-good).
      return;
    }
    State previous = knownState.getAndSet(evaluated);
    source.set(Source.POSTHOG);
    lastSyncedAt.set(Instant.now());
    if (previous != evaluated) {
      log.info(
          "voice_kill_switch_state_changed previous={} next={} flagKey={}",
          previous,
          evaluated,
          properties.getFlagKey());
    }
  }

  State evaluateFromPostHog() {
    if (flagsClient == null) {
      return State.UNKNOWN;
    }
    try {
      Map<String, Object> body =
          Map.of(
              "api_key",
              postHogProperties.getApiKey().trim(),
              "distinct_id",
              properties.getEvaluateDistinctId());
      String json =
          flagsClient
              .post()
              .uri("/flags?v=2")
              .contentType(MediaType.APPLICATION_JSON)
              .body(objectMapper.writeValueAsString(body))
              .retrieve()
              .body(String.class);
      if (!StringUtils.hasText(json)) {
        return State.UNKNOWN;
      }
      return parseFlagsResponse(json, properties.getFlagKey());
    } catch (RestClientException e) {
      log.warn("PostHog /flags voice kill switch failed: {}", e.getMessage());
      return State.UNKNOWN;
    } catch (Exception e) {
      log.warn("PostHog /flags voice kill switch parse failed: {}", e.getMessage());
      return State.UNKNOWN;
    }
  }

  /**
   * Maps a {@code /flags?v=2} body to our tri-state. An inactive flag is omitted → NOT_ENGAGED. An
   * explicit {@code true} → ENGAGED. An explicit {@code false} → NOT_ENGAGED.
   */
  static State parseFlagsResponse(String json, String flagKey) throws Exception {
    ObjectMapper mapper = new ObjectMapper();
    JsonNode root = mapper.readTree(json);
    JsonNode flags = root.get("flags");
    if (flags == null || !flags.isObject()) {
      // Older shape used featureFlags; accept both.
      flags = root.get("featureFlags");
    }
    if (flags == null || !flags.isObject()) {
      return State.NOT_ENGAGED;
    }
    if (!flags.has(flagKey)) {
      return State.NOT_ENGAGED;
    }
    JsonNode value = flags.get(flagKey);
    if (value == null || value.isNull()) {
      return State.NOT_ENGAGED;
    }
    // /flags?v=2 may nest as { "aboutme_voice_kill_switch": { "enabled": true, ... } }
    if (value.isObject()) {
      JsonNode enabled = value.get("enabled");
      if (enabled != null && enabled.isBoolean()) {
        return enabled.booleanValue() ? State.ENGAGED : State.NOT_ENGAGED;
      }
      JsonNode keyVal = value.get("key") != null ? value.get("key") : value.get("variant");
      if (keyVal != null && keyVal.isBoolean()) {
        return keyVal.booleanValue() ? State.ENGAGED : State.NOT_ENGAGED;
      }
    }
    if (value.isBoolean()) {
      return value.booleanValue() ? State.ENGAGED : State.NOT_ENGAGED;
    }
    if (value.isTextual()) {
      String text = value.asText();
      if ("true".equalsIgnoreCase(text)) {
        return State.ENGAGED;
      }
      return State.NOT_ENGAGED;
    }
    return State.NOT_ENGAGED;
  }

  static String normalizeHost(String host) {
    if (host == null || host.isBlank()) {
      return "https://eu.i.posthog.com";
    }
    String h = host.trim();
    if (h.endsWith("/")) {
      h = h.substring(0, h.length() - 1);
    }
    return h;
  }
}
