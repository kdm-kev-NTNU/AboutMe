package com.kevinmazali.portfolio.model;

import java.util.List;
import org.springframework.lang.Nullable;

/**
 * Whether browser clients may show the public voice chat entry point.
 *
 * <p>{@code enabled} is realtime <em>capability</em> (config + API key). {@code liveEnabled} is
 * public availability ({@code enabled} and kill switch not engaged). Admin interview uses
 * capability; the public SPA uses {@code liveEnabled}.
 */
public record RealtimeStatusResponse(
    boolean enabled,
    boolean liveEnabled,
    @Nullable String liveDisabledReason,
    List<String> voices,
    List<String> reasoningEfforts,
    List<String> vadEagernessOptions,
    String defaultVoice,
    String defaultReasoningEffort,
    String defaultVadEagerness) {}
