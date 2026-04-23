package com.kevinmazali.portfolio.service;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.UUID;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

/**
 * Thread-local trace hierarchy for PostHog {@code $ai_generation} (parent/child spans per request or eval job).
 */
@Component
public class PostHogTraceContext {

  private static final ThreadLocal<TraceState> TLS = new ThreadLocal<>();

  /** Start a new root trace (e.g. one RAG ask or one experiment example). Clears any prior state on this thread. */
  public void beginTrace(String traceName, @Nullable String conversationId) {
    TraceState s = new TraceState();
    s.rootTraceId = UUID.randomUUID().toString();
    s.traceName = traceName != null ? traceName : "trace";
    s.conversationId = conversationId;
    s.spanStack.clear();
    TLS.set(s);
  }

  /** Optional: attach experiment run metadata for all captures on this thread until {@link #clear()}. */
  public void setExperimentRunId(@Nullable Long runId) {
    TraceState s = TLS.get();
    if (s != null) {
      s.experimentRunId = runId;
    }
  }

  @Nullable
  public Long getExperimentRunId() {
    TraceState s = TLS.get();
    return s != null ? s.experimentRunId : null;
  }

  @Nullable
  public String rootTraceId() {
    TraceState s = TLS.get();
    return s != null ? s.rootTraceId : null;
  }

  @Nullable
  public String traceName() {
    TraceState s = TLS.get();
    return s != null ? s.traceName : null;
  }

  @Nullable
  public String conversationId() {
    TraceState s = TLS.get();
    return s != null ? s.conversationId : null;
  }

  /**
   * Call before an LLM invocation; returns span id and parent for PostHog. {@link #endSpan()} must run after
   * capture (e.g. in finally).
   */
  public ActiveSpan startSpan() {
    TraceState s = TLS.get();
    if (s == null) {
      String tid = UUID.randomUUID().toString();
      return new ActiveSpan(tid, null, tid, null, null);
    }
    String spanId = UUID.randomUUID().toString();
    String parent = s.spanStack.isEmpty() ? null : s.spanStack.peek();
    s.spanStack.push(spanId);
    return new ActiveSpan(s.rootTraceId, parent, spanId, s.traceName, s.conversationId);
  }

  public void endSpan() {
    TraceState s = TLS.get();
    if (s != null && !s.spanStack.isEmpty()) {
      s.spanStack.pop();
    }
  }

  public void clear() {
    TLS.remove();
  }

  public record ActiveSpan(
      String rootTraceId,
      @Nullable String parentSpanId,
      String spanId,
      @Nullable String traceName,
      @Nullable String conversationId) {}

  private static final class TraceState {
    String rootTraceId = "";
    String traceName = "";
    @Nullable String conversationId;
    @Nullable Long experimentRunId;
    final Deque<String> spanStack = new ArrayDeque<>();
  }
}
