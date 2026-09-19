package com.kevinmazali.portfolio.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.kevinmazali.portfolio.config.PostHogProperties;
import com.kevinmazali.portfolio.config.VoiceKillSwitchProperties;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class VoiceKillSwitchTest {

  @Test
  void parseFlags_trueBooleanIsEngaged() throws Exception {
    String json = "{\"flags\":{\"aboutme_voice_kill_switch\":true}}";
    assertThat(VoiceKillSwitch.parseFlagsResponse(json, "aboutme_voice_kill_switch"))
        .isEqualTo(VoiceKillSwitch.State.ENGAGED);
  }

  @Test
  void parseFlags_absentKeyIsNotEngaged() throws Exception {
    String json = "{\"flags\":{}}";
    assertThat(VoiceKillSwitch.parseFlagsResponse(json, "aboutme_voice_kill_switch"))
        .isEqualTo(VoiceKillSwitch.State.NOT_ENGAGED);
  }

  @Test
  void parseFlags_nestedEnabledObject() throws Exception {
    String json =
        "{\"flags\":{\"aboutme_voice_kill_switch\":{\"enabled\":true,\"variant\":null}}}";
    assertThat(VoiceKillSwitch.parseFlagsResponse(json, "aboutme_voice_kill_switch"))
        .isEqualTo(VoiceKillSwitch.State.ENGAGED);
  }

  @Test
  void parseFlags_legacyFeatureFlagsMap() throws Exception {
    String json = "{\"featureFlags\":{\"aboutme_voice_kill_switch\":false}}";
    assertThat(VoiceKillSwitch.parseFlagsResponse(json, "aboutme_voice_kill_switch"))
        .isEqualTo(VoiceKillSwitch.State.NOT_ENGAGED);
  }

  @Test
  void coldStartIsNotEngagedUntilPostHogSaysOtherwise() {
    VoiceKillSwitchProperties props = new VoiceKillSwitchProperties();
    props.setEnabled(true);
    PostHogProperties posthog = new PostHogProperties();
    posthog.setEnabled(false);
    VoiceKillSwitch switcher = new VoiceKillSwitch(props, posthog, new ObjectMapper());
    assertThat(switcher.isEngaged()).isFalse();
    assertThat(switcher.snapshot().state()).isEqualTo(VoiceKillSwitch.State.UNKNOWN);
  }

  @Test
  void applyKnownStateEngagesImmediately() {
    VoiceKillSwitchProperties props = new VoiceKillSwitchProperties();
    PostHogProperties posthog = new PostHogProperties();
    VoiceKillSwitch switcher = new VoiceKillSwitch(props, posthog, new ObjectMapper());
    switcher.applyKnownState(true, VoiceKillSwitch.Source.MANAGEMENT_WRITE);
    assertThat(switcher.isEngaged()).isTrue();
    assertThat(switcher.snapshot().source()).isEqualTo(VoiceKillSwitch.Source.MANAGEMENT_WRITE);
  }

  @Test
  void refreshUnknownDoesNotOverwriteKnownState() {
    VoiceKillSwitchProperties props = new VoiceKillSwitchProperties();
    props.setEnabled(true);
    PostHogProperties posthog = new PostHogProperties();
    posthog.setEnabled(false); // no flags client → refresh is a no-op
    VoiceKillSwitch switcher = new VoiceKillSwitch(props, posthog, new ObjectMapper());
    switcher.applyKnownState(true, VoiceKillSwitch.Source.MANAGEMENT_WRITE);
    switcher.refresh();
    assertThat(switcher.isEngaged()).isTrue();
  }

  @Test
  void parseFlags_textualTrueIsEngaged() throws Exception {
    String json = "{\"flags\":{\"aboutme_voice_kill_switch\":\"true\"}}";
    assertThat(VoiceKillSwitch.parseFlagsResponse(json, "aboutme_voice_kill_switch"))
        .isEqualTo(VoiceKillSwitch.State.ENGAGED);
  }

  @Test
  void parseFlags_nestedEnabledFalse() throws Exception {
    String json =
        "{\"flags\":{\"aboutme_voice_kill_switch\":{\"enabled\":false}}}";
    assertThat(VoiceKillSwitch.parseFlagsResponse(json, "aboutme_voice_kill_switch"))
        .isEqualTo(VoiceKillSwitch.State.NOT_ENGAGED);
  }

  @Test
  void parseFlags_emptyRootIsNotEngaged() throws Exception {
    assertThat(VoiceKillSwitch.parseFlagsResponse("{}", "aboutme_voice_kill_switch"))
        .isEqualTo(VoiceKillSwitch.State.NOT_ENGAGED);
  }

  @Test
  void disabledKillSwitchMarksSourceDisabled() {
    VoiceKillSwitchProperties props = new VoiceKillSwitchProperties();
    props.setEnabled(false);
    PostHogProperties posthog = new PostHogProperties();
    posthog.setEnabled(false);
    VoiceKillSwitch switcher = new VoiceKillSwitch(props, posthog, new ObjectMapper());
    assertThat(switcher.snapshot().source()).isEqualTo(VoiceKillSwitch.Source.DISABLED);
    assertThat(switcher.isEngaged()).isFalse();
    assertThat(switcher.isPosthogReadConfigured()).isFalse();
  }

  @Test
  void normalizeHost_stripsTrailingSlashAndDefaultsBlank() {
    assertThat(VoiceKillSwitch.normalizeHost("https://eu.i.posthog.com/"))
        .isEqualTo("https://eu.i.posthog.com");
    assertThat(VoiceKillSwitch.normalizeHost("  ")).isEqualTo("https://eu.i.posthog.com");
    assertThat(VoiceKillSwitch.normalizeHost(null)).isEqualTo("https://eu.i.posthog.com");
  }

  @Test
  void applyKnownStateReleaseClearsEngagement() {
    VoiceKillSwitchProperties props = new VoiceKillSwitchProperties();
    PostHogProperties posthog = new PostHogProperties();
    VoiceKillSwitch switcher = new VoiceKillSwitch(props, posthog, new ObjectMapper());
    switcher.applyKnownState(true, VoiceKillSwitch.Source.MANAGEMENT_WRITE);
    switcher.applyKnownState(false, VoiceKillSwitch.Source.MANAGEMENT_WRITE);
    assertThat(switcher.isEngaged()).isFalse();
    assertThat(switcher.snapshot().state()).isEqualTo(VoiceKillSwitch.State.NOT_ENGAGED);
  }
}
