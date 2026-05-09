package com.kevinmazali.portfolio.service;

import java.io.IOException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * Executes a synchronous JDK {@link HttpClient} call for OpenAI realtime voice signaling.
 */
@FunctionalInterface
public interface OpenAiRealtimeHttpInvoker {

  HttpResponse<String> invoke(HttpRequest request) throws IOException, InterruptedException;
}
