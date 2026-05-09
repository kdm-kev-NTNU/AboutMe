package com.kevinmazali.portfolio.service;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;
import com.kevinmazali.portfolio.config.RealtimeVoiceProperties;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * Proxies WebRTC SDP offers to {@code POST https://api.openai.com/v1/realtime/calls} so the browser never sees the API key.
 */
@Slf4j
@Service
public class RealtimeWebRtcService {

    private final RealtimeVoiceProperties realtimeVoiceProperties;
    private final RestClient openAiRestClient;
    private final ObjectMapper objectMapper;
    private final String openAiApiKey;

    public RealtimeWebRtcService(
            RealtimeVoiceProperties realtimeVoiceProperties,
            ObjectMapper objectMapper,
            @Value("${spring.ai.openai.api-key:}") String openAiApiKey) {
        this.realtimeVoiceProperties = realtimeVoiceProperties;
        this.objectMapper = objectMapper;
        this.openAiApiKey = openAiApiKey != null ? openAiApiKey.trim() : "";
        JdkClientHttpRequestFactory rf = new JdkClientHttpRequestFactory();
        rf.setReadTimeout(Duration.ofSeconds(90));
        this.openAiRestClient = RestClient.builder().requestFactory(rf).baseUrl("https://api.openai.com").build();
    }

    /**
     * @param languageIso6391 optional {@code en} / {@code no} for input transcription hint
     */
    public String exchangeSdp(String sdpOffer, String languageIso6391) {
        if (!StringUtils.hasText(openAiApiKey)) {
            throw new IllegalStateException("OpenAI API key is not configured.");
        }
        if (!StringUtils.hasText(sdpOffer)) {
            throw new IllegalArgumentException("SDP offer is required.");
        }

        String sessionJson = buildSessionJson(languageIso6391);

        MultipartBodyBuilder multipart = new MultipartBodyBuilder();
        multipart.part("sdp", sdpOffer).contentType(MediaType.parseMediaType("application/sdp"));
        multipart.part("session", sessionJson).contentType(MediaType.APPLICATION_JSON);

        try {
            byte[] body =
                    openAiRestClient
                            .post()
                            .uri("/v1/realtime/calls")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + openAiApiKey)
                            .body(multipart.build())
                            .retrieve()
                            .body(byte[].class);
            if (body == null || body.length == 0) {
                throw new IllegalStateException("OpenAI returned an empty SDP answer.");
            }
            return new String(body, StandardCharsets.UTF_8);
        } catch (RestClientResponseException e) {
            log.warn(
                    "OpenAI /v1/realtime/calls failed: status={} body={}",
                    e.getStatusCode(),
                    truncate(e.getResponseBodyAsString(StandardCharsets.UTF_8), 800));
            throw e;
        }
    }

    private String buildSessionJson(String languageIso6391) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("type", "realtime");
        root.put("model", realtimeVoiceProperties.getModel());
        root.put("instructions", realtimeVoiceProperties.getInstructions());

        ArrayNode modalities = objectMapper.createArrayNode();
        modalities.add("audio");
        root.set("output_modalities", modalities);

        ObjectNode audio = objectMapper.createObjectNode();
        ObjectNode input = objectMapper.createObjectNode();

        ObjectNode transcription = objectMapper.createObjectNode();
        transcription.put("model", "whisper-1");
        if (StringUtils.hasText(languageIso6391)) {
            String lang = languageIso6391.trim().toLowerCase().replace('\n', ' ');
            if (lang.length() <= 8) {
                transcription.put("language", lang);
            }
        }
        input.set("transcription", transcription);

        ObjectNode turnDetection = objectMapper.createObjectNode();
        turnDetection.put("type", "server_vad");
        input.set("turn_detection", turnDetection);

        audio.set("input", input);
        root.set("audio", audio);

        try {
            return objectMapper.writeValueAsString(root);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize realtime session JSON", e);
        }
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max) + "…";
    }
}
