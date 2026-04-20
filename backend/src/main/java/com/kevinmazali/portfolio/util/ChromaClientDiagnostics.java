package com.kevinmazali.portfolio.util;

import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;

/**
 * Builds the Chroma HTTP client base URL and human-readable errors when the REST client fails with
 * empty or generic messages (common for {@link java.net.ConnectException} on Railway private networking).
 */
public final class ChromaClientDiagnostics {

  private ChromaClientDiagnostics() {
  }

  /** Same format as {@code ChromaApi} base URL: {@code host:port}. */
  public static String baseUrl(@Nullable String host, int port) {
    if (!StringUtils.hasText(host)) {
      return "<unset-host>:" + port;
    }
    return host + ":" + port;
  }

  /**
   * Message for health checks and wrapped service exceptions when Chroma cannot be reached.
   */
  public static String describeChromaFailure(Throwable throwable, String chromaBaseUrl) {
    String detail = summarizeThrowable(throwable);
    return "Cannot reach Chroma at " + chromaBaseUrl + ". " + detail + railwayPrivateNetworkingHint();
  }

  /**
   * Body text for {@link RestClientException} on admin routes: avoid raw {@code "...": null} from Spring's
   * RestClient and add an ops hint.
   */
  public static String apiErrorBodyMessage(RestClientException ex, String chromaBaseUrl) {
    String raw = ex.getMessage();
    if (!StringUtils.hasText(raw) || raw.endsWith(": null")) {
      return describeChromaFailure(ex, chromaBaseUrl);
    }
    return raw + railwayPrivateNetworkingHint();
  }

  /**
   * For {@code /health/chroma}: keep normal exception messages; only enrich Chroma client / I/O style failures.
   */
  public static String healthFailureMessage(Throwable e, String chromaBaseUrl) {
    if (e instanceof RestClientException rce) {
      return apiErrorBodyMessage(rce, chromaBaseUrl);
    }
    String raw = e.getMessage();
    if (!StringUtils.hasText(raw) || raw.endsWith(": null")) {
      return describeChromaFailure(e, chromaBaseUrl);
    }
    return raw;
  }

  private static String railwayPrivateNetworkingHint() {
    return " Hint: on Railway private networking, bind Chroma to IPv6 (e.g. CHROMA_HOST_ADDR=::) or set CHROMA_ENABLED=false if Chroma is not deployed.";
  }

  private static String summarizeThrowable(Throwable throwable) {
    if (throwable == null) {
      return "Unknown error.";
    }
    if (StringUtils.hasText(throwable.getMessage())) {
      return throwable.getMessage();
    }
    Throwable cursor = throwable.getCause();
    int depth = 0;
    while (cursor != null && depth++ < 8) {
      if (StringUtils.hasText(cursor.getMessage())) {
        return cursor.getClass().getSimpleName() + ": " + cursor.getMessage();
      }
      cursor = cursor.getCause();
    }
    return throwable.getClass().getSimpleName()
        + " (no detail message; often connection refused, timeout, or DNS failure).";
  }
}
