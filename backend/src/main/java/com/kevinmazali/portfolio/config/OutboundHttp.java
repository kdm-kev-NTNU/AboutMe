package com.kevinmazali.portfolio.config;

import java.time.Duration;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.http.client.ClientHttpRequestFactory;

/**
 * Single owner of outbound HTTP transport policy for this service.
 *
 * <p>Every outbound call must go through a client that attempts <em>all</em> addresses resolved for
 * the target host. Apache HttpComponents does; {@code java.net.http.HttpClient} on Java 21 does not
 * -- it commits to one resolved address and reports failure without trying the rest.
 *
 * <p>That difference is not academic here. {@code backend/Dockerfile} starts the JVM with
 * {@code -Djava.net.preferIPv6Addresses=true}, which Railway's IPv6-only private networking needs.
 * The flag also reorders public dual-stack hosts, so a single-address client resolves
 * {@code api.openai.com} to its AAAA record and dies with {@code ENETUNREACH} because the container
 * has no public IPv6 route. Live voice and PostHog feature flags were both silently down in
 * production for exactly this reason while every health endpoint reported UP, because each had
 * picked {@code java.net.http.HttpClient} independently.
 *
 * <p>Routing all outbound HTTP through here makes reachability a property of the transport rather
 * than a coincidence of which client a given feature happened to choose, and keeps it independent of
 * JVM-wide address-family preference.
 */
public final class OutboundHttp {

  private OutboundHttp() {}

  /**
   * Builds an Apache HttpComponents request factory with explicit timeouts.
   *
   * <p>{@code httpComponents()} is named rather than detected so that losing the {@code httpclient5}
   * dependency breaks the build instead of silently falling back to a single-address client.
   *
   * @param connectTimeout budget for establishing a connection to one address
   * @param readTimeout budget for waiting on a response once connected
   */
  public static ClientHttpRequestFactory requestFactory(
      Duration connectTimeout, Duration readTimeout) {
    return ClientHttpRequestFactoryBuilder.httpComponents()
        .build(HttpClientSettings.defaults().withTimeouts(connectTimeout, readTimeout));
  }
}
