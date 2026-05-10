package com.kevinmazali.portfolio.model.analytics;

import java.util.regex.Pattern;
import org.springframework.lang.Nullable;

/**
 * Optional PostHog LLM correlation for realtime voice: browser sends trace id (UUID) and optionally
 * PostHog session id on realtime API calls.
 */
public record RealtimeVoiceAnalyticsContext(String traceId, @Nullable String sessionId) {

  public static final String HEADER_AI_TRACE_ID = "X-AI-Trace-Id";
  public static final String HEADER_POSTHOG_SESSION_ID = "X-PostHog-Session-Id";

  private static final Pattern UUID_PATTERN =
      Pattern.compile(
          "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-8][0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$");

  /**
   * Parses optional analytics headers. Returns {@code null} when trace id is missing or not a valid UUID
   * (invalid values are ignored — no server-generated fallback).
   */
  @Nullable
  public static RealtimeVoiceAnalyticsContext fromHeaders(
      @Nullable String traceHeader, @Nullable String sessionHeader) {
    String traceId = parseTraceId(traceHeader);
    if (traceId == null) {
      return null;
    }
    return new RealtimeVoiceAnalyticsContext(traceId, sanitizePosthogSessionId(sessionHeader));
  }

  @Nullable
  public static String parseTraceId(@Nullable String raw) {
    if (raw == null) {
      return null;
    }
    String t = raw.trim();
    if (t.isEmpty() || !UUID_PATTERN.matcher(t).matches()) {
      return null;
    }
    return t;
  }

  @Nullable
  public static String sanitizePosthogSessionId(@Nullable String raw) {
    if (raw == null) {
      return null;
    }
    String s = raw.trim();
    if (s.isEmpty() || s.length() > 200) {
      return null;
    }
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      if (c < 0x20 || c == 0x7f) {
        return null;
      }
    }
    return s;
  }
}
