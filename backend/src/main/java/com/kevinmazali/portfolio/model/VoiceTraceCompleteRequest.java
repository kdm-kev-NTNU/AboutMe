package com.kevinmazali.portfolio.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.lang.Nullable;

/**
 * Client beacon to close a PostHog {@code $ai_trace} for a voice session (browser knows end time).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record VoiceTraceCompleteRequest(
    String traceId,
    @Nullable String sessionId,
    @Nullable Double durationSeconds,
    @Nullable Boolean error,
    @Nullable String errorMessage) {}
