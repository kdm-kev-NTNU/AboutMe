package com.kevinmazali.portfolio.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.kevinmazali.portfolio.config.VoiceKillSwitchProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

class PostHogFeatureFlagManagementClientHttpTest {

  private VoiceKillSwitchProperties props;
  private RestClient.Builder builder;
  private MockRestServiceServer server;
  private PostHogFeatureFlagManagementClient client;

  @BeforeEach
  void setUp() {
    props = new VoiceKillSwitchProperties();
    props.setPersonalApiKey("phx_test");
    props.setProjectId("162788");
    props.setApiHost("https://eu.posthog.com");
    props.setFlagKey("aboutme_voice_kill_switch");
    builder = RestClient.builder().baseUrl("https://eu.posthog.com");
    server = MockRestServiceServer.bindTo(builder).build();
    client =
        new PostHogFeatureFlagManagementClient(
            props,
            new ObjectMapper(),
            builder
                .defaultHeader("Authorization", "Bearer phx_test")
                .build());
  }

  @Test
  void setKillSwitchEngaged_postsEnableAfterResolvingFlagId() {
    server
        .expect(requestTo("https://eu.posthog.com/api/projects/162788/feature_flags/?search=aboutme_voice_kill_switch"))
        .andExpect(method(HttpMethod.GET))
        .andExpect(header("Authorization", "Bearer phx_test"))
        .andRespond(
            withSuccess(
                "{\"results\":[{\"id\":281659,\"key\":\"aboutme_voice_kill_switch\"}]}",
                MediaType.APPLICATION_JSON));
    server
        .expect(requestTo("https://eu.posthog.com/api/projects/162788/feature_flags/281659/enable/"))
        .andExpect(method(HttpMethod.POST))
        .andRespond(withSuccess());

    client.setKillSwitchEngaged(true);
    server.verify();
  }

  @Test
  void setKillSwitchEngaged_postsDisable() {
    server
        .expect(requestTo("https://eu.posthog.com/api/projects/162788/feature_flags/?search=aboutme_voice_kill_switch"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(
            withSuccess(
                "{\"results\":[{\"id\":281659,\"key\":\"aboutme_voice_kill_switch\"}]}",
                MediaType.APPLICATION_JSON));
    server
        .expect(requestTo("https://eu.posthog.com/api/projects/162788/feature_flags/281659/disable/"))
        .andExpect(method(HttpMethod.POST))
        .andRespond(withSuccess());

    client.setKillSwitchEngaged(false);
    server.verify();
  }

  @Test
  void setKillSwitchEngaged_failsWhenFlagMissing() {
    server
        .expect(requestTo("https://eu.posthog.com/api/projects/162788/feature_flags/?search=aboutme_voice_kill_switch"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess("{\"results\":[]}", MediaType.APPLICATION_JSON));

    assertThatThrownBy(() -> client.setKillSwitchEngaged(true))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("not found");
    server.verify();
  }

  @Test
  void posthogFlagUrl_includesCachedIdAfterResolve() {
    server
        .expect(requestTo("https://eu.posthog.com/api/projects/162788/feature_flags/?search=aboutme_voice_kill_switch"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(
            withSuccess(
                "{\"results\":[{\"id\":281659,\"key\":\"aboutme_voice_kill_switch\"}]}",
                MediaType.APPLICATION_JSON));
    server
        .expect(requestTo("https://eu.posthog.com/api/projects/162788/feature_flags/281659/disable/"))
        .andExpect(method(HttpMethod.POST))
        .andRespond(withSuccess());

    client.setKillSwitchEngaged(false);
    assertThat(client.posthogFlagUrl()).contains("/281659");
    server.verify();
  }
}
