package com.kevinmazali.portfolio.model.chat;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SupportedChatModelTest {

  @Test
  void fromModelId_resolvesAllConfiguredIds() {
    for (SupportedChatModel m : SupportedChatModel.values()) {
      assertThat(SupportedChatModel.fromModelId(m.modelId())).contains(m);
    }
  }

  @Test
  void fromModelId_trimsWhitespace() {
    assertThat(SupportedChatModel.fromModelId("  gpt-5.4-mini  ")).contains(SupportedChatModel.GPT_5_4_MINI);
  }

  @Test
  void fromModelId_emptyOrUnknown() {
    assertThat(SupportedChatModel.fromModelId(null)).isEmpty();
    assertThat(SupportedChatModel.fromModelId("")).isEmpty();
    assertThat(SupportedChatModel.fromModelId("gpt-4o")).isEmpty();
  }

  @Test
  void fastModelsDoNotRequireAuthForPublicChat() {
    assertThat(SupportedChatModel.GPT_5_4_NANO.tags()).isEqualTo(EnumSet.of(ModelTag.FAST));
    assertThat(SupportedChatModel.GPT_5_4_MINI.tags()).isEqualTo(EnumSet.of(ModelTag.FAST));
    assertThat(SupportedChatModel.CLAUDE_HAIKU_4_5.tags()).isEqualTo(EnumSet.of(ModelTag.FAST));

    assertFalse(SupportedChatModel.GPT_5_4_NANO.requiresAuthenticationForPublicChat());
    assertFalse(SupportedChatModel.GPT_5_4_MINI.requiresAuthenticationForPublicChat());
    assertFalse(SupportedChatModel.CLAUDE_HAIKU_4_5.requiresAuthenticationForPublicChat());
  }

  @Test
  void reasoningModelsRequireAuthForPublicChat() {
    assertThat(SupportedChatModel.GPT_5_4.tags()).isEqualTo(EnumSet.of(ModelTag.REASONING));
    assertThat(SupportedChatModel.GPT_5_5.tags()).isEqualTo(EnumSet.of(ModelTag.REASONING));
    assertThat(SupportedChatModel.CLAUDE_SONNET_4_6.tags()).isEqualTo(EnumSet.of(ModelTag.REASONING));
    assertThat(SupportedChatModel.CLAUDE_OPUS_4_7.tags()).isEqualTo(EnumSet.of(ModelTag.REASONING));

    assertTrue(SupportedChatModel.GPT_5_4.requiresAuthenticationForPublicChat());
    assertTrue(SupportedChatModel.GPT_5_5.requiresAuthenticationForPublicChat());
    assertTrue(SupportedChatModel.CLAUDE_SONNET_4_6.requiresAuthenticationForPublicChat());
    assertTrue(SupportedChatModel.CLAUDE_OPUS_4_7.requiresAuthenticationForPublicChat());
  }
}
