package com.kevinmazali.portfolio.controller;

import com.kevinmazali.portfolio.model.ApiError;
import com.kevinmazali.portfolio.service.PostHogFeatureFlagManagementClient;
import com.kevinmazali.portfolio.service.RealtimeModelCatalog;
import com.kevinmazali.portfolio.service.VoiceKillSwitch;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;

/**
 * Admin APIs for feature kill switches backed by PostHog feature flags.
 */
@Slf4j
@RestController
@RequestMapping("/admin/tools/features")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Feature switches", description = "PostHog-backed kill switches for public features")
public class FeatureSwitchAdminController {

  private final VoiceKillSwitch voiceKillSwitch;
  private final PostHogFeatureFlagManagementClient managementClient;
  private final RealtimeModelCatalog realtimeModelCatalog;

  public record KillSwitchBody(Boolean engaged) {}

  @Operation(summary = "Public voice kill-switch status")
  @GetMapping("/voice")
  public Map<String, Object> voiceStatus() {
    return voiceStatusBody();
  }

  @Operation(summary = "Toggle public voice kill switch via PostHog")
  @PostMapping("/voice/kill-switch")
  public ResponseEntity<?> setVoiceKillSwitch(@RequestBody KillSwitchBody body) {
    if (body == null || body.engaged() == null) {
      return ResponseEntity.badRequest()
          .body(new ApiError("Body must include \"engaged\": true|false", "BAD_REQUEST"));
    }
    if (!managementClient.isConfigured()) {
      return ResponseEntity.status(503)
          .body(
              new ApiError(
                  "PostHog feature-flag management is not configured (POSTHOG_PERSONAL_API_KEY / POSTHOG_PROJECT_ID / POSTHOG_API_HOST).",
                  "POSTHOG_NOT_CONFIGURED"));
    }
    try {
      managementClient.setKillSwitchEngaged(Boolean.TRUE.equals(body.engaged()));
      voiceKillSwitch.applyKnownState(
          Boolean.TRUE.equals(body.engaged()), VoiceKillSwitch.Source.MANAGEMENT_WRITE);
      return ResponseEntity.ok(voiceStatusBody());
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().body(new ApiError(e.getMessage(), "BAD_REQUEST"));
    } catch (RestClientException | IllegalStateException e) {
      log.warn("voice_kill_switch_write_failed: {}", e.getMessage());
      return ResponseEntity.status(502)
          .body(new ApiError("Could not update PostHog feature flag: " + e.getMessage(), "POSTHOG_WRITE_FAILED"));
    }
  }

  private Map<String, Object> voiceStatusBody() {
    VoiceKillSwitch.Snapshot snap = voiceKillSwitch.snapshot();
    boolean capability = realtimeModelCatalog.hasAvailableModels();
    boolean liveEnabled = capability && !snap.engaged();
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("killSwitchEngaged", snap.engaged());
    m.put("killSwitchState", snap.state().name());
    m.put("liveEnabled", liveEnabled);
    m.put("capability", capability);
    m.put("source", snap.source().name());
    m.put("lastSyncedAt", snap.lastSyncedAt() != null ? snap.lastSyncedAt().toString() : null);
    m.put("posthogConfigured", managementClient.isConfigured());
    m.put("posthogReadConfigured", voiceKillSwitch.isPosthogReadConfigured());
    m.put("flagKey", snap.flagKey());
    m.put("posthogUrl", managementClient.posthogFlagUrl());
    return m;
  }
}
