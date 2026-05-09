package com.kevinmazali.portfolio.model.realtime;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Browser WebRTC SDP offer for OpenAI Realtime.")
public record RealtimeWebRtcRequest(
        @NotBlank
                @Schema(description = "SDP offer text from RTCPeerConnection.createOffer()", requiredMode = Schema.RequiredMode.REQUIRED)
                String sdp) {}
