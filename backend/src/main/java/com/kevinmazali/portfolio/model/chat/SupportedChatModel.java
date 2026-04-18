package com.kevinmazali.portfolio.model.chat;

import java.util.Arrays;
import java.util.Optional;

/**
 * Allow-listed chat models exposed to clients. Unknown ids are rejected server-side.
 */
public enum SupportedChatModel {

  GPT_4O_MINI("gpt-4o-mini", ChatProvider.OPENAI, "GPT-4o mini"),
  GPT_4O("gpt-4o", ChatProvider.OPENAI, "GPT-4o"),

  CLAUDE_SONNET_4("claude-sonnet-4-20250514", ChatProvider.ANTHROPIC, "Claude Sonnet 4"),
  CLAUDE_3_5_HAIKU("claude-3-5-haiku-20241022", ChatProvider.ANTHROPIC, "Claude 3.5 Haiku");

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
}
