package com.kevinmazali.portfolio.model.chat;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Live integration test that calls the real OpenAI / Anthropic APIs to verify
 * every {@link SupportedChatModel} returns non-empty assistant text for the same
 * default English questions as {@code HomeView.vue} ({@code questionsByLang.en}).
 *
 * <p>Excluded from the normal CI gate — run manually or via the
 * {@code model-validation} GitHub Actions workflow:
 * <pre>mvn test -DexcludedGroups= -Dtest=LiveModelValidationIT</pre>
 *
 * <p>Requires {@code OPENAI_API_KEY} and/or {@code ANTHROPIC_API_KEY} env vars.
 * For full matrix coverage in CI, configure both as repository secrets.
 */
@Tag("live")
class LiveModelValidationIT {

  /**
   * Mirrors {@code questionsByLang.en} in {@code frontend/homepage/src/views/HomeView.vue}
   * (order preserved).
   */
  private static final List<String> DEFAULT_QUESTIONS = List.of(
      "Why did Kevin create this website?",
      "Which courses has Kevin taken?",
      "Which projects has Kevin worked on?",
      "Who is Kevin?"
  );

  private static final ObjectMapper JSON = new ObjectMapper();

  private static final HttpClient HTTP = HttpClient.newBuilder()
      .connectTimeout(Duration.ofSeconds(15))
      .build();

  @BeforeAll
  static void requireAtLeastOneProviderKey() {
    boolean openai = isNonBlank(System.getenv("OPENAI_API_KEY"));
    boolean anthropic = isNonBlank(System.getenv("ANTHROPIC_API_KEY"));
    if (!openai && !anthropic) {
      fail(
          "Live model validation requires at least one of OPENAI_API_KEY or ANTHROPIC_API_KEY. "
              + "Set them in the environment or as GitHub Actions secrets so this job does not pass "
              + "with no provider coverage.");
    }
  }

  static Stream<Arguments> openAiCases() {
    return Arrays.stream(SupportedChatModel.values())
        .filter(m -> m.provider() == ChatProvider.OPENAI)
        .flatMap(m -> DEFAULT_QUESTIONS.stream().map(q -> Arguments.of(m, q)));
  }

  static Stream<Arguments> anthropicCases() {
    return Arrays.stream(SupportedChatModel.values())
        .filter(m -> m.provider() == ChatProvider.ANTHROPIC)
        .flatMap(m -> DEFAULT_QUESTIONS.stream().map(q -> Arguments.of(m, q)));
  }

  @ParameterizedTest(name = "OpenAI {0} answers: {1}")
  @MethodSource("openAiCases")
  @EnabledIfEnvironmentVariable(named = "OPENAI_API_KEY", matches = ".+")
  void openAiModelAnswersDefaultQuestion(SupportedChatModel model, String question) throws Exception {
    String apiKey = System.getenv("OPENAI_API_KEY");
    assertNotNull(apiKey, "OPENAI_API_KEY must be set");

    String body = JSON.writeValueAsString(
        Map.of(
            "model", model.modelId(),
            "messages", List.of(Map.of("role", "user", "content", question)),
            "max_completion_tokens", 50));

    HttpRequest req = HttpRequest.newBuilder()
        .uri(URI.create("https://api.openai.com/v1/chat/completions"))
        .header("Authorization", "Bearer " + apiKey)
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(body))
        .timeout(Duration.ofSeconds(60))
        .build();

    HttpResponse<String> res = HTTP.send(req, HttpResponse.BodyHandlers.ofString());

    if (res.statusCode() == 404 || res.body().contains("model_not_found")) {
      fail("Model ID '" + model.modelId() + "' is not recognized by OpenAI. "
          + "HTTP " + res.statusCode() + ": " + res.body());
    }
    assertEquals(200, res.statusCode(),
        "Model '" + model.modelId() + "' returned HTTP " + res.statusCode()
            + ": " + truncate(res.body(), 800));

    JsonNode root = JSON.readTree(res.body());
    JsonNode contentNode = root.path("choices").path(0).path("message").path("content");
    String content = contentNode.isMissingNode() || contentNode.isNull() ? "" : contentNode.asString();
    assertFalse(content.isBlank(),
        "Expected non-empty assistant content for model " + model.modelId()
            + "; body: " + truncate(res.body(), 800));
  }

  @ParameterizedTest(name = "Anthropic {0} answers: {1}")
  @MethodSource("anthropicCases")
  @EnabledIfEnvironmentVariable(named = "ANTHROPIC_API_KEY", matches = ".+")
  void anthropicModelAnswersDefaultQuestion(SupportedChatModel model, String question) throws Exception {
    String apiKey = System.getenv("ANTHROPIC_API_KEY");
    assertNotNull(apiKey, "ANTHROPIC_API_KEY must be set");

    String body = JSON.writeValueAsString(
        Map.of(
            "model", model.modelId(),
            "messages", List.of(Map.of("role", "user", "content", question)),
            "max_tokens", 50));

    HttpRequest req = HttpRequest.newBuilder()
        .uri(URI.create("https://api.anthropic.com/v1/messages"))
        .header("x-api-key", apiKey)
        .header("anthropic-version", "2023-06-01")
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(body))
        .timeout(Duration.ofSeconds(60))
        .build();

    HttpResponse<String> res = HTTP.send(req, HttpResponse.BodyHandlers.ofString());

    if (res.statusCode() == 404 || res.body().contains("model_not_found")
        || res.body().contains("not_found_error")) {
      fail("Model ID '" + model.modelId() + "' is not recognized by Anthropic. "
          + "HTTP " + res.statusCode() + ": " + res.body());
    }
    assertEquals(200, res.statusCode(),
        "Model '" + model.modelId() + "' returned HTTP " + res.statusCode()
            + ": " + truncate(res.body(), 800));

    JsonNode root = JSON.readTree(res.body());
    String assistantText = extractAnthropicText(root);
    assertFalse(assistantText.isBlank(),
        "Expected non-empty assistant text for model " + model.modelId()
            + "; body: " + truncate(res.body(), 800));
  }

  private static String extractAnthropicText(JsonNode root) {
    StringBuilder sb = new StringBuilder();
    JsonNode content = root.path("content");
    if (content.isArray()) {
      for (JsonNode block : content) {
        JsonNode type = block.path("type");
        JsonNode text = block.path("text");
        if ("text".equals(type.isMissingNode() || type.isNull() ? "" : type.asString())) {
          sb.append(text.isMissingNode() || text.isNull() ? "" : text.asString());
        }
      }
    }
    return sb.toString();
  }

  private static boolean isNonBlank(String s) {
    return s != null && !s.isBlank();
  }

  private static String truncate(String s, int max) {
    if (s == null) {
      return "";
    }
    return s.length() <= max ? s : s.substring(0, max) + "…";
  }
}
