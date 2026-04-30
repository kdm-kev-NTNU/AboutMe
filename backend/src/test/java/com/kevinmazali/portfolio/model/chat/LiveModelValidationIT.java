package com.kevinmazali.portfolio.model.chat;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Live integration test that calls the real OpenAI / Anthropic APIs to verify
 * every {@link SupportedChatModel} ID is accepted by the provider.
 *
 * <p>Excluded from the normal CI gate — run manually or via the
 * {@code model-validation} GitHub Actions workflow:
 * <pre>mvn test -Dgroups=live</pre>
 *
 * <p>Requires {@code OPENAI_API_KEY} and/or {@code ANTHROPIC_API_KEY} env vars.
 */
@Tag("live")
class LiveModelValidationIT {

  private static final HttpClient HTTP = HttpClient.newBuilder()
      .connectTimeout(Duration.ofSeconds(15))
      .build();

  @ParameterizedTest(name = "OpenAI model {0} is callable")
  @EnumSource(value = SupportedChatModel.class, mode = EnumSource.Mode.MATCH_ANY,
      names = {"GPT_.*"})
  @EnabledIfEnvironmentVariable(named = "OPENAI_API_KEY", matches = ".+")
  void openAiModelAccepted(SupportedChatModel model) throws Exception {
    String apiKey = System.getenv("OPENAI_API_KEY");
    assertNotNull(apiKey, "OPENAI_API_KEY must be set");

    String body = """
        {"model":"%s","messages":[{"role":"user","content":"Say hi"}],"max_completion_tokens":5}
        """.formatted(model.modelId()).strip();

    HttpRequest req = HttpRequest.newBuilder()
        .uri(URI.create("https://api.openai.com/v1/chat/completions"))
        .header("Authorization", "Bearer " + apiKey)
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(body))
        .timeout(Duration.ofSeconds(30))
        .build();

    HttpResponse<String> res = HTTP.send(req, HttpResponse.BodyHandlers.ofString());

    if (res.statusCode() == 404 || res.body().contains("model_not_found")) {
      fail("Model ID '" + model.modelId() + "' is not recognized by OpenAI. "
          + "HTTP " + res.statusCode() + ": " + res.body());
    }
    assertEquals(200, res.statusCode(),
        "Model '" + model.modelId() + "' returned HTTP " + res.statusCode()
            + ": " + truncate(res.body(), 500));
  }

  @ParameterizedTest(name = "Anthropic model {0} is callable")
  @EnumSource(value = SupportedChatModel.class, mode = EnumSource.Mode.MATCH_ANY,
      names = {"CLAUDE_.*"})
  @EnabledIfEnvironmentVariable(named = "ANTHROPIC_API_KEY", matches = ".+")
  void anthropicModelAccepted(SupportedChatModel model) throws Exception {
    String apiKey = System.getenv("ANTHROPIC_API_KEY");
    assertNotNull(apiKey, "ANTHROPIC_API_KEY must be set");

    String body = """
        {"model":"%s","messages":[{"role":"user","content":"Say hi"}],"max_tokens":5}
        """.formatted(model.modelId()).strip();

    HttpRequest req = HttpRequest.newBuilder()
        .uri(URI.create("https://api.anthropic.com/v1/messages"))
        .header("x-api-key", apiKey)
        .header("anthropic-version", "2023-06-01")
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(body))
        .timeout(Duration.ofSeconds(30))
        .build();

    HttpResponse<String> res = HTTP.send(req, HttpResponse.BodyHandlers.ofString());

    if (res.statusCode() == 404 || res.body().contains("model_not_found")
        || res.body().contains("not_found_error")) {
      fail("Model ID '" + model.modelId() + "' is not recognized by Anthropic. "
          + "HTTP " + res.statusCode() + ": " + res.body());
    }
    assertEquals(200, res.statusCode(),
        "Model '" + model.modelId() + "' returned HTTP " + res.statusCode()
            + ": " + truncate(res.body(), 500));
  }

  private static String truncate(String s, int max) {
    if (s == null) return "";
    return s.length() <= max ? s : s.substring(0, max) + "…";
  }
}
