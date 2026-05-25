package com.kevinmazali.portfolio.service;

import com.kevinmazali.portfolio.model.ChatModelOption;
import com.kevinmazali.portfolio.model.chat.ChatProvider;
import com.kevinmazali.portfolio.model.chat.ModelTag;
import com.kevinmazali.portfolio.model.chat.SupportedChatModel;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import java.util.EnumSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatModelCatalogTest {

  @Test
  void listAvailableModels_includesOpenAiWhenKeyPresent() {
    MockEnvironment env = new MockEnvironment()
        .withProperty("spring.ai.openai.api-key", "sk-openai-test")
        .withProperty("spring.ai.openai.chat.enabled", "true")
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
        .withProperty("spring.ai.openai.chat.enabled", "true")
        .withProperty("spring.ai.anthropic.api-key", "test-anthropic-api-key");
    ChatModelCatalog catalog = new ChatModelCatalog(env);

    assertThat(catalog.listAvailableModels()).hasSize(2);
  }

  @Test
  void listAvailableModels_includesTagsOnEachOption() {
    MockEnvironment env = new MockEnvironment()
        .withProperty("spring.ai.openai.api-key", "sk-openai-test")
        .withProperty("spring.ai.openai.chat.enabled", "true")
        .withProperty("spring.ai.anthropic.api-key", "test-anthropic-api-key");
    ChatModelCatalog catalog = new ChatModelCatalog(env);

    ChatModelOption mini = catalog.listAvailableModels().stream()
        .filter(o -> "gpt-5.4-mini".equals(o.id()))
        .findFirst()
        .orElseThrow();
    assertThat(mini.tags()).isEqualTo(EnumSet.of(ModelTag.FAST));

    ChatModelOption haiku = catalog.listAvailableModels().stream()
        .filter(o -> "claude-haiku-4-5-20251001".equals(o.id()))
        .findFirst()
        .orElseThrow();
    assertThat(haiku.tags()).isEqualTo(EnumSet.of(ModelTag.FAST));
  }

  @Test
  void listAvailableModels_anthropicOnlyWhenOpenAiKeyMissing() {
    MockEnvironment env = new MockEnvironment()
        .withProperty("spring.ai.openai.api-key", "")
        .withProperty("spring.ai.anthropic.api-key", "test-anthropic-api-key");
    ChatModelCatalog catalog = new ChatModelCatalog(env);

    assertThat(catalog.listAvailableModels())
        .extracting("provider")
        .containsOnly(ChatProvider.ANTHROPIC);
  }

  @Test
  void isModelConfigured_requiresMatchingProviderKey() {
    MockEnvironment env = new MockEnvironment()
        .withProperty("spring.ai.openai.api-key", "sk-openai-test")
        .withProperty("spring.ai.openai.chat.enabled", "true")
        .withProperty("spring.ai.anthropic.api-key", "");
    ChatModelCatalog catalog = new ChatModelCatalog(env);

    assertTrue(catalog.isModelConfigured(SupportedChatModel.GPT_5_4_MINI));
    assertFalse(catalog.isModelConfigured(SupportedChatModel.CLAUDE_HAIKU_4_5));
  }
}
