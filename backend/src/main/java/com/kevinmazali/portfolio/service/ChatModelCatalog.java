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
 * Resolves allow-listed models and filters the catalog by configured API keys.
 */
@Service
public class ChatModelCatalog {

  private final Environment environment;

  public ChatModelCatalog(Environment environment) {
    this.environment = environment;
  }

  public List<ChatModelOption> listAvailableModels() {
    boolean openai = hasApiKey("spring.ai.openai.api-key");
    boolean anthropic = hasApiKey("spring.ai.anthropic.api-key");
    return Arrays.stream(SupportedChatModel.values())
        .filter(m -> m.provider() == ChatProvider.OPENAI ? openai : anthropic)
        .map(m -> new ChatModelOption(m.modelId(), m.provider(), m.label()))
        .toList();
  }

  public boolean isModelConfigured(SupportedChatModel model) {
    return switch (model.provider()) {
      case OPENAI -> hasApiKey("spring.ai.openai.api-key");
      case ANTHROPIC -> hasApiKey("spring.ai.anthropic.api-key");
    };
  }

  private boolean hasApiKey(String propertyName) {
    String v = environment.getProperty(propertyName);
    return StringUtils.hasText(v);
  }
}
