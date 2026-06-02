package com.kevinmazali.portfolio.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.kevinmazali.portfolio.config.RealtimeProperties;
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
  void hidesOpenAiWithoutKey() {
    RealtimeProperties props = new RealtimeProperties();
    props.setEnabled(true);

    RealtimeModelCatalog catalog = new RealtimeModelCatalog(props, "");

    assertThat(catalog.listAvailableModels()).isEmpty();
    assertThat(catalog.hasAvailableModels()).isFalse();
  }
}
