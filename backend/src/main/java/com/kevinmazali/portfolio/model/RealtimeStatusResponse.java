package com.kevinmazali.portfolio.model;

import java.util.List;

/**
 * Whether browser clients may show the voice chat entry point.
 */
public record RealtimeStatusResponse(
    boolean enabled,
    boolean liveEnabled,
    List<String> voices,
    List<String> reasoningEfforts,
    List<String> vadEagernessOptions,
    String defaultVoice,
    String defaultReasoningEffort,
    String defaultVadEagerness) {}
