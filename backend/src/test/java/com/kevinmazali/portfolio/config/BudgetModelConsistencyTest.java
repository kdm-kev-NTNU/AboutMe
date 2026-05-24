package com.kevinmazali.portfolio.config;

import com.kevinmazali.portfolio.model.chat.SupportedChatModel;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Set.of;

import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Ensures {@link SupportedChatModel} and the {@code portfolio.ai.budget.models}
 * YAML block stay in sync. Pure unit test: no Spring context, runs in the fast CI gate.
 */
class BudgetModelConsistencyTest {

  /**
   * Budget YAML entries for non-chat models (e.g. OpenAI Whisper) must not require a {@link SupportedChatModel} enum value.
   */
  private static final Set<String> BUDGET_KEYS_EXEMPT_FROM_CHAT_ENUM =
      of("whisper-1", "gpt-4o-mini-transcribe", "gpt-4o-transcribe", "tts-1", "gpt-realtime-2");

  private static Set<String> budgetModelKeys;

  @BeforeAll
  static void loadBudgetKeysFromYaml() throws Exception {
    YamlPropertySourceLoader loader = new YamlPropertySourceLoader();
    var sources = loader.load("application", new ClassPathResource("application.yaml"));
    assertThat(sources).isNotEmpty();

    PropertySource<?> ps = sources.get(0);

    budgetModelKeys = Arrays.stream(SupportedChatModel.values())
        .map(SupportedChatModel::modelId)
        .filter(id -> ps.getProperty("portfolio.ai.budget.models." + id + ".input-per-million-usd") != null)
        .collect(Collectors.toSet());

    Set<String> allEnumIds = Arrays.stream(SupportedChatModel.values())
        .map(SupportedChatModel::modelId)
        .collect(Collectors.toSet());

    Set<String> yamlBudgetKeys = allEnumIds.stream()
        .filter(id -> ps.getProperty("portfolio.ai.budget.models." + id + ".input-per-million-usd") != null)
        .collect(Collectors.toSet());

    budgetModelKeys = yamlBudgetKeys;
  }

  @Test
  void everyEnumModelHasBudgetPricing() {
    for (SupportedChatModel model : SupportedChatModel.values()) {
      assertThat(budgetModelKeys)
          .as("Budget pricing missing for model '%s' in application.yaml", model.modelId())
          .contains(model.modelId());
    }
  }

  @Test
  void enumCoversAllBudgetEntries() {
    Set<String> enumIds = Arrays.stream(SupportedChatModel.values())
        .map(SupportedChatModel::modelId)
        .collect(Collectors.toSet());

    for (String budgetKey : budgetModelKeys) {
      if (BUDGET_KEYS_EXEMPT_FROM_CHAT_ENUM.contains(budgetKey)) {
        continue;
      }
      assertThat(enumIds)
          .as("Budget entry '%s' has no matching SupportedChatModel enum value", budgetKey)
          .contains(budgetKey);
    }
  }

  @Test
  void defaultModelIdIsInEnum() throws Exception {
    YamlPropertySourceLoader loader = new YamlPropertySourceLoader();
    var sources = loader.load("application", new ClassPathResource("application.yaml"));
    PropertySource<?> ps = sources.get(0);

    Object defaultId = ps.getProperty("portfolio.chat.default-model-id");
    assertThat(defaultId).as("portfolio.chat.default-model-id must be set").isNotNull();

    assertThat(SupportedChatModel.fromModelId(defaultId.toString()))
        .as("default-model-id '%s' must be a valid SupportedChatModel", defaultId)
        .isPresent();
  }

  @Test
  void openAiDefaultModelIsInEnum() throws Exception {
    YamlPropertySourceLoader loader = new YamlPropertySourceLoader();
    var sources = loader.load("application", new ClassPathResource("application.yaml"));
    PropertySource<?> ps = sources.get(0);

    Object openaiDefault = ps.getProperty("spring.ai.openai.chat.options.model");
    assertThat(openaiDefault).as("spring.ai.openai.chat.options.model must be set").isNotNull();
    assertThat(SupportedChatModel.fromModelId(openaiDefault.toString()))
        .as("OpenAI default model '%s' must be a valid SupportedChatModel", openaiDefault)
        .isPresent();
  }

  @Test
  void anthropicDefaultModelIsInEnum() throws Exception {
    YamlPropertySourceLoader loader = new YamlPropertySourceLoader();
    var sources = loader.load("application", new ClassPathResource("application.yaml"));
    PropertySource<?> ps = sources.get(0);

    Object anthropicDefault = ps.getProperty("spring.ai.anthropic.chat.options.model");
    assertThat(anthropicDefault).as("spring.ai.anthropic.chat.options.model must be set").isNotNull();
    assertThat(SupportedChatModel.fromModelId(anthropicDefault.toString()))
        .as("Anthropic default model '%s' must be a valid SupportedChatModel", anthropicDefault)
        .isPresent();
  }
}
