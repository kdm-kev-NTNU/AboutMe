package com.kevinmazali.portfolio.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.kevinmazali.portfolio.config.RealtimeProperties;
import java.util.List;
import org.junit.jupiter.api.Test;

class RealtimeModelCatalogTest {

  @Test
  void hidesAllModelsWhenGlobalRealtimeDisabled() {
    RealtimeProperties props = new RealtimeProperties();
    props.setEnabled(false);

    RealtimeModelCatalog catalog = new RealtimeModelCatalog(props, "sk-test");

    assertThat(catalog.listAvailableModels()).isEmpty();
    assertThat(catalog.hasAvailableModels()).isFalse();
  }

  @Test
  void exposesOpenAiFallbackWhenKeyAndProviderConfigured() {
    RealtimeProperties props = new RealtimeProperties();
    props.setEnabled(true);

    RealtimeModelCatalog catalog = new RealtimeModelCatalog(props, "sk-test");

    assertThat(catalog.listAvailableModels())
        .singleElement()
        .satisfies(option -> {
          assertThat(option.provider()).isEqualTo("OPENAI");
          assertThat(option.id()).isEqualTo("gpt-realtime-2");
          assertThat(option.defaultOption()).isTrue();
        });
  }

  @Test
  void hidesOpenAiWithoutKeyAndExposesConfiguredElevenLabsAgents() {
    RealtimeProperties props = new RealtimeProperties();
    props.setEnabled(true);
    props.getProviders().getElevenlabs().setEnabled(true);
    props.getProviders().getElevenlabs().setApiKey("xi-test");
    RealtimeProperties.ElevenLabsAgent agent = new RealtimeProperties.ElevenLabsAgent();
    agent.setAgentId("agent_123");
    agent.setLabel("Kevin ElevenLabs");
    agent.setDefaultAgent(true);
    props.getProviders().getElevenlabs().setAgents(List.of(agent));

    RealtimeModelCatalog catalog = new RealtimeModelCatalog(props, "");

    assertThat(catalog.listAvailableModels())
        .singleElement()
        .satisfies(option -> {
          assertThat(option.provider()).isEqualTo("ELEVENLABS");
          assertThat(option.id()).isEqualTo("agent_123");
          assertThat(option.label()).isEqualTo("Kevin ElevenLabs");
          assertThat(option.defaultOption()).isTrue();
        });
    assertThat(catalog.findElevenLabsAgent("agent_123")).isSameAs(agent);
    assertThat(catalog.findElevenLabsAgent("missing")).isNull();
  }
}
