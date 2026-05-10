package com.kevinmazali.portfolio.config;

import com.kevinmazali.portfolio.service.OpenAiRealtimeHttpInvoker;
import com.kevinmazali.portfolio.service.ElevenLabsRealtimeHttpInvoker;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.springframework.boot.restclient.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;

/**
 * Prevents Apache HttpClient 5 from advertising Brotli ({@code br}) encoding to upstream APIs.
 * OpenAI sometimes returns Brotli-compressed responses whose decompression truncates the JSON,
 * causing {@link tools.jackson.core.io.JsonEOFException} inside Spring AI's chat model
 * deserialization.
 *
 * @see <a href="https://github.com/spring-projects/spring-ai/issues/2345">spring-ai#2345</a>
 */
@Configuration
public class HttpClientConfig {

    @Bean
    OpenAiRealtimeHttpInvoker openAiRealtimeHttpInvoker() {
        HttpClient client =
            HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();
        return (request) ->
            client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    @Bean
    ElevenLabsRealtimeHttpInvoker elevenLabsRealtimeHttpInvoker() {
        HttpClient client =
            HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();
        return (request) ->
            client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    @Bean
    RestClientCustomizer disableBrotliEncoding() {
        return builder -> builder.defaultHeaders(headers ->
                headers.set(HttpHeaders.ACCEPT_ENCODING, "gzip, deflate"));
    }
}
