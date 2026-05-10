package com.kevinmazali.portfolio.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.kevinmazali.portfolio.config.RealtimeProperties;
import com.kevinmazali.portfolio.model.RealtimeModelOption;
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

  @Test
  void promotesFirstListedModelWhenNoneMarkedDefault() {
    RealtimeProperties props = new RealtimeProperties();
    props.setEnabled(true);
    props.setModel("custom-realtime");
    RealtimeProperties.OpenAiModel a = new RealtimeProperties.OpenAiModel();
    a.setId("alpha");
    a.setLabel("Alpha");
    a.setDefaultModel(false);
    RealtimeProperties.OpenAiModel b = new RealtimeProperties.OpenAiModel();
    b.setId("beta");
    b.setLabel("Beta");
    b.setDefaultModel(false);
    props.getProviders().getOpenai().setModels(List.of(a, b));

    RealtimeModelCatalog catalog = new RealtimeModelCatalog(props, "sk-test");

    assertThat(catalog.listAvailableModels())
        .hasSize(2)
        .extracting(RealtimeModelOption::defaultOption)
        .containsExactly(true, false);
    assertThat(catalog.listAvailableModels().getFirst().id()).isEqualTo("alpha");
  }

  @Test
  void usesModelIdAsLabelWhenOpenAiLabelBlank() {
    RealtimeProperties props = new RealtimeProperties();
    props.setEnabled(true);
    RealtimeProperties.OpenAiModel m = new RealtimeProperties.OpenAiModel();
    m.setId("m-id");
    m.setLabel("");
    m.setDefaultModel(true);
    props.getProviders().getOpenai().setModels(List.of(m));

    RealtimeModelCatalog catalog = new RealtimeModelCatalog(props, "sk-key");

    assertThat(catalog.listAvailableModels())
        .singleElement()
        .satisfies(o -> {
          assertThat(o.id()).isEqualTo("m-id");
          assertThat(o.label()).isEqualTo("m-id");
        });
  }

  @Test
  void skipsElevenLabsAgentsWithBlankId() {
    RealtimeProperties props = new RealtimeProperties();
    props.setEnabled(true);
    props.getProviders().getElevenlabs().setEnabled(true);
    props.getProviders().getElevenlabs().setApiKey("xi-test");
    RealtimeProperties.ElevenLabsAgent blank = new RealtimeProperties.ElevenLabsAgent();
    blank.setAgentId("  ");
    RealtimeProperties.ElevenLabsAgent ok = new RealtimeProperties.ElevenLabsAgent();
    ok.setAgentId("good_agent");
    ok.setLabel("OK");
    ok.setDefaultAgent(true);
    props.getProviders().getElevenlabs().setAgents(List.of(blank, ok));

    RealtimeModelCatalog catalog = new RealtimeModelCatalog(props, "");

    assertThat(catalog.listAvailableModels())
        .singleElement()
        .satisfies(o -> assertThat(o.id()).isEqualTo("good_agent"));
  }

  @Test
  void findElevenLabsAgentNullWhenProviderDisabled() {
    RealtimeProperties props = new RealtimeProperties();
    props.setEnabled(true);
    props.getProviders().getElevenlabs().setEnabled(false);
    props.getProviders().getElevenlabs().setApiKey("xi-test");
    RealtimeProperties.ElevenLabsAgent agent = new RealtimeProperties.ElevenLabsAgent();
    agent.setAgentId("a1");
    props.getProviders().getElevenlabs().setAgents(List.of(agent));

    RealtimeModelCatalog catalog = new RealtimeModelCatalog(props, "");

    assertThat(catalog.findElevenLabsAgent("a1")).isNull();
  }

  @Test
  void resolvesElevenLabsAgentViaProviderDefaultAgentId() {
    RealtimeProperties props = new RealtimeProperties();
    props.setEnabled(true);
    props.getProviders().getElevenlabs().setEnabled(true);
    props.getProviders().getElevenlabs().setApiKey("xi-test");
    props.getProviders().getElevenlabs().setDefaultAgentId("preferred");
    RealtimeProperties.ElevenLabsAgent agent = new RealtimeProperties.ElevenLabsAgent();
    agent.setAgentId("preferred");
    agent.setDefaultAgent(false);
    props.getProviders().getElevenlabs().setAgents(List.of(agent));

    RealtimeModelCatalog catalog = new RealtimeModelCatalog(props, "");

    assertThat(catalog.findElevenLabsAgent(null)).isSameAs(agent);
    assertThat(catalog.findElevenLabsAgent("")).isSameAs(agent);
  }

  @Test
  void resolvesElevenLabsAgentToFirstAgentWhenNoExplicitDefault() {
    RealtimeProperties props = new RealtimeProperties();
    props.setEnabled(true);
    props.getProviders().getElevenlabs().setEnabled(true);
    props.getProviders().getElevenlabs().setApiKey("xi-test");
    RealtimeProperties.ElevenLabsAgent first = new RealtimeProperties.ElevenLabsAgent();
    first.setAgentId("first_id");
    first.setDefaultAgent(false);
    RealtimeProperties.ElevenLabsAgent second = new RealtimeProperties.ElevenLabsAgent();
    second.setAgentId("second_id");
    second.setDefaultAgent(false);
    props.getProviders().getElevenlabs().setAgents(List.of(first, second));

    RealtimeModelCatalog catalog = new RealtimeModelCatalog(props, "");

    assertThat(catalog.findElevenLabsAgent(null)).isSameAs(first);
  }

  @Test
  void isOpenAiModelConfiguredMatchesCatalogIds() {
    RealtimeProperties props = new RealtimeProperties();
    props.setEnabled(true);
    props.setModel("fallback-m");
    RealtimeProperties.OpenAiModel m = new RealtimeProperties.OpenAiModel();
    m.setId("configured-id");
    m.setLabel("L");
    m.setDefaultModel(true);
    props.getProviders().getOpenai().setModels(List.of(m));

    RealtimeModelCatalog catalog = new RealtimeModelCatalog(props, "sk");

    assertThat(catalog.isOpenAiModelConfigured("configured-id")).isTrue();
    assertThat(catalog.isOpenAiModelConfigured("other")).isFalse();
  }

  @Test
  void isOpenAiModelConfiguredFalseWhenRealtimeDisabled() {
    RealtimeProperties props = new RealtimeProperties();
    props.setEnabled(false);
    props.getProviders().getOpenai().setModels(List.of());

    RealtimeModelCatalog catalog = new RealtimeModelCatalog(props, "sk");

    assertThat(catalog.isOpenAiModelConfigured("gpt-realtime-2")).isFalse();
  }
}
