package com.kevinmazali.portfolio.model.chat;

import java.util.Arrays;
import java.util.Optional;

/**
 * Allow-listed chat models exposed to clients. Unknown ids are rejected server-side.
 */
public enum SupportedChatModel {

  GPT_5_4_MINI("gpt-5.4-mini", ChatProvider.OPENAI, "GPT-5.4 mini"),
  GPT_5_4("gpt-5.4", ChatProvider.OPENAI, "GPT-5.4"),

  CLAUDE_HAIKU_4_5("claude-haiku-4-5-20251001", ChatProvider.ANTHROPIC, "Claude Haiku 4.5"),
  CLAUDE_SONNET_4_6("claude-sonnet-4-6", ChatProvider.ANTHROPIC, "Claude Sonnet 4.6");

  private final String modelId;
  private final ChatProvider provider;
  private final String label;

  SupportedChatModel(String modelId, ChatProvider provider, String label) {
    this.modelId = modelId;
    this.provider = provider;
    this.label = label;
  }

  public String modelId() {
    return modelId;
  }

  public ChatProvider provider() {
    return provider;
  }

  public String label() {
    return label;
  }

  public static Optional<SupportedChatModel> fromModelId(String id) {
    if (id == null || id.isBlank()) {
      return Optional.empty();
    }
    return Arrays.stream(values())
        .filter(m -> m.modelId.equals(id.trim()))
        .findFirst();
  }

  /**
   * Premium models that anonymous public {@code /ask} callers must not use (cheaper models stay public).
   */
  public boolean requiresAuthenticationForPublicChat() {
    return this == GPT_5_4 || this == CLAUDE_SONNET_4_6;
  }
}
