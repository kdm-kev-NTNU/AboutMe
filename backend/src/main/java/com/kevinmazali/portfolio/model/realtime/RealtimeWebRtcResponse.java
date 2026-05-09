package com.kevinmazali.portfolio.model.realtime;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "SDP answer from OpenAI to complete the WebRTC handshake.")
public record RealtimeWebRtcResponse(
        @Schema(description = "SDP answer text for RTCPeerConnection.setRemoteDescription()") String sdp) {}
