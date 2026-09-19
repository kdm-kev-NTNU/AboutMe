package com.kevinmazali.portfolio.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kevinmazali.portfolio.config.VoiceKillSwitchProperties;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class PostHogFeatureFlagManagementClientTest {

  @Test
  void extractFlagId_findsMatchingKey() {
    VoiceKillSwitchProperties props = new VoiceKillSwitchProperties();
    props.setPersonalApiKey("phx_test");
    props.setProjectId("162788");
    props.setApiHost("https://eu.posthog.com");
    PostHogFeatureFlagManagementClient client =
        new PostHogFeatureFlagManagementClient(props, new ObjectMapper());
    String json =
        "{\"results\":[{\"id\":281659,\"key\":\"aboutme_voice_kill_switch\"},{\"id\":1,\"key\":\"other\"}]}";
    assertThat(client.extractFlagId(json, "aboutme_voice_kill_switch")).isEqualTo(281659L);
  }

  @Test
  void setKillSwitch_rejectsNonAllowListedKey() {
    VoiceKillSwitchProperties props = new VoiceKillSwitchProperties();
    props.setFlagKey("some_other_flag");
    props.setPersonalApiKey("phx_test");
    props.setProjectId("162788");
    props.setApiHost("https://eu.posthog.com");
    PostHogFeatureFlagManagementClient client =
        new PostHogFeatureFlagManagementClient(props, new ObjectMapper());
    assertThat(client.isConfigured()).isTrue();
    assertThatThrownBy(() -> client.setKillSwitchEngaged(true))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("aboutme_voice_kill_switch");
  }

  @Test
  void isConfigured_falseWithoutPersonalKey() {
    VoiceKillSwitchProperties props = new VoiceKillSwitchProperties();
    props.setProjectId("162788");
    PostHogFeatureFlagManagementClient client =
        new PostHogFeatureFlagManagementClient(props, new ObjectMapper());
    assertThat(client.isConfigured()).isFalse();
  }
}
