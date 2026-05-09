package com.kevinmazali.portfolio.exception;

import org.springframework.http.HttpStatus;

/**
 * Failure while creating an OpenAI Realtime WebRTC session; carries the HTTP status and
 * {@link RealtimeErrorCode} to return to the client.
 */
public final class RealtimeSessionException extends RuntimeException {

  private final HttpStatus httpStatus;
  private final RealtimeErrorCode errorCode;

  public RealtimeSessionException(HttpStatus httpStatus, RealtimeErrorCode errorCode, String message) {
    super(message);
    this.httpStatus = httpStatus;
    this.errorCode = errorCode;
  }

  public RealtimeSessionException(
      HttpStatus httpStatus, RealtimeErrorCode errorCode, String message, Throwable cause) {
    super(message, cause);
    this.httpStatus = httpStatus;
    this.errorCode = errorCode;
  }

  public HttpStatus getHttpStatus() {
    return httpStatus;
  }

  public RealtimeErrorCode getErrorCode() {
    return errorCode;
  }
}
