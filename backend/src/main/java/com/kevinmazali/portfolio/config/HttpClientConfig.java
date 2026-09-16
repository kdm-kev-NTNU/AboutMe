package com.kevinmazali.portfolio.config;

import com.kevinmazali.portfolio.service.OpenAiRealtimeHttpInvoker;
import java.io.IOException;
import java.time.Duration;
import org.springframework.boot.restclient.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** Outbound HTTP wiring for calls to OpenAI. */
@Configuration
public class HttpClientConfig {

    /**
     * Prevents Apache HttpClient 5 from advertising Brotli ({@code br}) encoding to upstream APIs.
     * OpenAI sometimes returns Brotli-compressed responses whose decompression truncates the JSON,
     * causing {@link tools.jackson.core.io.JsonEOFException} inside Spring AI's chat model
     * deserialization.
     *
     * @see <a href="https://github.com/spring-projects/spring-ai/issues/2345">spring-ai#2345</a>
     */
    private static final String SAFE_ACCEPT_ENCODING = "gzip, deflate";

    private static final Duration REALTIME_CONNECT_TIMEOUT = Duration.ofSeconds(15);
    private static final Duration REALTIME_READ_TIMEOUT = Duration.ofSeconds(60);

    /**
     * Realtime signaling transport.
     *
     * <p>Built on {@link OutboundHttp} so it shares the address-selection behaviour of the chat path.
     * It previously used {@code java.net.http.HttpClient}, which cannot reach a dual-stack host when
     * the JVM prefers an address family the container cannot route -- see {@link OutboundHttp}.
     */
    @Bean
    OpenAiRealtimeHttpInvoker openAiRealtimeHttpInvoker() {
        RestClient client = RestClient.builder()
            .requestFactory(
                OutboundHttp.requestFactory(REALTIME_CONNECT_TIMEOUT, REALTIME_READ_TIMEOUT))
            .defaultHeader(HttpHeaders.ACCEPT_ENCODING, SAFE_ACCEPT_ENCODING)
            .build();

        return (uri, headers, body) -> {
            try {
                ResponseEntity<String> response = client.post()
                    .uri(uri)
                    .headers(target -> headers.forEach(target::set))
                    .body(body)
                    .retrieve()
                    // Hand every status back to the caller; it owns the status-to-error-code mapping.
                    .onStatus(status -> true, (request, upstream) -> { })
                    .toEntity(String.class);
                return new OpenAiRealtimeHttpInvoker.Response(
                    response.getStatusCode().value(), response.getBody());
            } catch (RestClientException e) {
                // No response was produced. Surfaced as IOException so callers map it to
                // OPENAI_UNREACHABLE rather than leaking a Spring type across the seam.
                throw new IOException(e.getMessage(), e);
            }
        };
    }

    @Bean
    RestClientCustomizer disableBrotliEncoding() {
        return builder -> builder.defaultHeaders(headers ->
                headers.set(HttpHeaders.ACCEPT_ENCODING, SAFE_ACCEPT_ENCODING));
    }
}
