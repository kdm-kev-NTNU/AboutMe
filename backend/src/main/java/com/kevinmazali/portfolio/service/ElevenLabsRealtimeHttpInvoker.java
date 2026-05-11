package com.kevinmazali.portfolio.service;

import java.io.IOException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * Executes synchronous JDK {@link java.net.http.HttpClient} calls for ElevenLabs realtime voice setup.
 */
@FunctionalInterface
public interface ElevenLabsRealtimeHttpInvoker {

  HttpResponse<String> invoke(HttpRequest request) throws IOException, InterruptedException;
}
