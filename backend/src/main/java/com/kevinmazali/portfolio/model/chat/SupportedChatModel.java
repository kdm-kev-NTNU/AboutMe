package com.kevinmazali.portfolio.model.chat;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

/**
 * Allow-listed chat models exposed to clients. Unknown ids are rejected server-side.
 */
public enum SupportedChatModel {

  GPT_5_4_MINI("gpt-5.4-mini", ChatProvider.OPENAI, "GPT-5.4 mini", EnumSet.of(ModelTag.FAST)),

  CLAUDE_HAIKU_4_5("claude-haiku-4-5-20251001", ChatProvider.ANTHROPIC, "Claude Haiku 4.5", EnumSet.of(ModelTag.FAST));

  private final String modelId;
  private final ChatProvider provider;
  private final String label;
  private final Set<ModelTag> tags;

  SupportedChatModel(String modelId, ChatProvider provider, String label, Set<ModelTag> tags) {
    this.modelId = modelId;
    this.provider = provider;
    this.label = label;
    this.tags = Collections.unmodifiableSet(tags);
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

  public Set<ModelTag> tags() {
    return tags;
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
   * Models that anonymous public {@code /ask} callers must not use (fast models stay public).
   */
  public boolean requiresAuthenticationForPublicChat() {
    return tags.contains(ModelTag.REASONING);
  }
}
