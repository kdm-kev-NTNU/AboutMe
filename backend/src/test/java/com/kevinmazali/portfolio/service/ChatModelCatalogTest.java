package com.kevinmazali.portfolio.service;

import com.kevinmazali.portfolio.model.chat.ChatProvider;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

class ChatModelCatalogTest {

  @Test
  void listAvailableModels_includesOpenAiWhenKeyPresent() {
    MockEnvironment env = new MockEnvironment()
        .withProperty("spring.ai.openai.api-key", "sk-openai-test")
        .withProperty("spring.ai.anthropic.api-key", "");
    ChatModelCatalog catalog = new ChatModelCatalog(env);

    assertThat(catalog.listAvailableModels())
        .extracting("provider")
        .containsOnly(ChatProvider.OPENAI);
  }

  @Test
  void listAvailableModels_includesBothWhenKeysPresent() {
    MockEnvironment env = new MockEnvironment()
        .withProperty("spring.ai.openai.api-key", "sk-openai-test")
        .withProperty("spring.ai.anthropic.api-key", "sk-ant-test");
    ChatModelCatalog catalog = new ChatModelCatalog(env);

    assertThat(catalog.listAvailableModels()).hasSize(4);
  }
}
