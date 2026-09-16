package com.kevinmazali.portfolio.service;

import java.io.IOException;
import java.net.URI;
import java.util.Map;

/**
 * Transport seam for OpenAI Realtime signaling calls (SDP offer in, SDP answer out).
 *
 * <p>The contract is deliberately transport-neutral -- a URI, headers and a byte body -- so callers
 * describe <em>what</em> to send while the implementation owns connection policy: timeouts, TLS and
 * address selection. An implementation MUST try every address resolved for the host; see
 * {@link com.kevinmazali.portfolio.config.OutboundHttp} for why that is a hard requirement and not a
 * quality-of-implementation detail.
 */
@FunctionalInterface
public interface OpenAiRealtimeHttpInvoker {

  /**
   * Performs a POST and returns the upstream outcome.
   *
   * <p>Non-2xx responses are returned rather than thrown: the status is upstream's answer, and
   * callers map it to a domain {@link com.kevinmazali.portfolio.exception.RealtimeErrorCode}. Only
   * the absence of any response is exceptional.
   *
   * @param headers request headers, applied verbatim
   * @param body request body, sent as-is
   * @throws IOException when no response was produced at all (connect, TLS or read failure)
   */
  Response post(URI uri, Map<String, String> headers, byte[] body) throws IOException;

  /**
   * Upstream response reduced to what realtime signaling needs.
   *
   * @param status HTTP status code
   * @param body response body, or {@code null} when upstream sent none
   */
  record Response(int status, String body) {}
}
