package com.kevinmazali.portfolio.model;

import java.util.List;

/**
 * Whether browser clients may show the voice chat entry point.
 */
public record RealtimeStatusResponse(
    boolean enabled,
    boolean standardEnabled,
    boolean liveEnabled,
    List<String> voices,
    List<String> reasoningEfforts,
    String defaultVoice,
    String defaultReasoningEffort) {}
