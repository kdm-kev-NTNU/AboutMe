package com.kevinmazali.portfolio.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * OpenAI Realtime (WebRTC) — SDP offer relayed through {@code POST /realtime/webrtc}.
 */
@ConfigurationProperties(prefix = "portfolio.realtime")
public class RealtimeVoiceProperties {

    private boolean enabled = true;

    private String model = "gpt-realtime";

    private int sessionMaxMinutes = 3;

    private String instructions =
            "You are a helpful voice assistant for Kevin's portfolio website. "
                    + "You are AI — not Kevin speaking in real time. "
                    + "Be concise and friendly. For long document or portfolio knowledge questions, suggest the text chat on the site.";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public int getSessionMaxMinutes() {
        return sessionMaxMinutes;
    }

    public void setSessionMaxMinutes(int sessionMaxMinutes) {
        this.sessionMaxMinutes = sessionMaxMinutes;
    }

    public String getInstructions() {
        return instructions;
    }

    public void setInstructions(String instructions) {
        this.instructions = instructions;
    }
}
