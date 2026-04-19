package com.kevinmazali.portfolio.util;

import org.springframework.core.NestedExceptionUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientResponseException;

import java.nio.charset.StandardCharsets;

/**
 * Human-readable summaries for LLM HTTP client failures on {@code POST /ask}.
 * OpenAI often returns {@code application/json} error bodies that are not a {@code ChatCompletion},
 * which surfaces as generic "Error while extracting response..." messages unless the status/body are logged.
 */
public final class LlmClientDiagnostics {

  private static final int MAX_BODY_LOG_CHARS = 900;

  private LlmClientDiagnostics() {
  }

  /**
   * Best-effort description: HTTP status + truncated response body when a {@link RestClientResponseException}
   * appears in the cause chain; otherwise the most specific cause message.
   */
  public static String describeAskFailure(@Nullable Throwable throwable) {
    if (throwable == null) {
      return "unknown";
    }
    for (Throwable t = throwable; t != null; t = t.getCause()) {
      if (t instanceof RestClientResponseException r) {
        String body = truncateWhitespace(safeResponseBody(r), MAX_BODY_LOG_CHARS);
        return "upstream HTTP " + r.getStatusCode().value() + " " + r.getStatusText()
            + " | body: " + (StringUtils.hasText(body) ? body : "<empty>");
      }
    }
    Throwable root = NestedExceptionUtils.getMostSpecificCause(throwable);
    String msg = root.getMessage();
    if (StringUtils.hasText(msg)) {
      return root.getClass().getSimpleName() + ": " + msg;
    }
    return throwable.getClass().getSimpleName();
  }

  private static String safeResponseBody(RestClientResponseException r) {
    try {
      return r.getResponseBodyAsString(StandardCharsets.UTF_8);
    } catch (Exception ignored) {
      return "";
    }
  }

  private static String truncateWhitespace(String s, int maxChars) {
    if (!StringUtils.hasText(s)) {
      return "";
    }
    String oneLine = s.replaceAll("\\s+", " ").trim();
    if (oneLine.length() <= maxChars) {
      return oneLine;
    }
    return oneLine.substring(0, maxChars) + "…";
  }
}
