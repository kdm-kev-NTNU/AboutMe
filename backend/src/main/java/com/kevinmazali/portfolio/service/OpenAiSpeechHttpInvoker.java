package com.kevinmazali.portfolio.service;

import java.io.IOException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * Executes synchronous JDK {@link java.net.http.HttpClient} calls for OpenAI speech synthesis.
 */
@FunctionalInterface
public interface OpenAiSpeechHttpInvoker {

  HttpResponse<byte[]> invoke(HttpRequest request) throws IOException, InterruptedException;
}
