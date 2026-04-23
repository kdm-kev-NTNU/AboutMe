package com.kevinmazali.portfolio.service;

import com.kevinmazali.portfolio.model.ChatModelOption;
import com.kevinmazali.portfolio.model.chat.ChatProvider;
import com.kevinmazali.portfolio.model.chat.SupportedChatModel;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;

/**
 * Exposes {@link com.kevinmazali.portfolio.model.chat.SupportedChatModel} entries to the SPA,
 * hiding providers whose API keys are missing so the UI never offers unusable models.
 */
@Service
public class ChatModelCatalog {

  private final Environment environment;

  public ChatModelCatalog(Environment environment) {
    this.environment = environment;
  }

  /** One row per configured provider/model pair (OpenAI and/or Anthropic). */
  public List<ChatModelOption> listAvailableModels() {
    boolean openai = hasApiKey("spring.ai.openai.api-key") && isOpenAiChatEnabled();
    boolean anthropic = hasApiKey("spring.ai.anthropic.api-key");
    return Arrays.stream(SupportedChatModel.values())
        .filter(m -> m.provider() == ChatProvider.OPENAI ? openai : anthropic)
        .map(m -> new ChatModelOption(m.modelId(), m.provider(), m.label()))
        .toList();
  }

  /** Used by {@link com.kevinmazali.portfolio.controller.QuestionController} before calling the LLM. */
  public boolean isModelConfigured(SupportedChatModel model) {
    return switch (model.provider()) {
      case OPENAI -> hasApiKey("spring.ai.openai.api-key") && isOpenAiChatEnabled();
      case ANTHROPIC -> hasApiKey("spring.ai.anthropic.api-key");
    };
  }

  private boolean isOpenAiChatEnabled() {
    return Boolean.parseBoolean(environment.getProperty("spring.ai.openai.chat.enabled", "false"));
  }

  /** Non-blank Spring property means the corresponding SDK can be constructed. */
  private boolean hasApiKey(String propertyName) {
    String v = environment.getProperty(propertyName);
    return StringUtils.hasText(v);
  }
}
