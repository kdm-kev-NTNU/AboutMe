package com.kevinmazali.portfolio.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

import java.time.OffsetDateTime;

@Getter
@Entity
@Table(name = "request_log")
public class RequestLog {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String path;

  @Column(nullable = false)
  private String method;

  @Column(name = "user_id")
  private Long userId;

  @Column(name = "status_code")
  private Short statusCode;

  @Column(name = "duration_ms")
  private Integer durationMs;

  @Column(nullable = true, length = 128)
  private String requesterId;

  @Column(nullable = false)
  private OffsetDateTime createdAt = OffsetDateTime.now();

  public void setPath(String path) {
    this.path = path;
  }

  public void setMethod(String method) {
    this.method = method;
  }

  public void setUserId(Long userId) {
    this.userId = userId;
  }

  public void setStatusCode(Short statusCode) {
    this.statusCode = statusCode;
  }

  public void setDurationMs(Integer durationMs) {
    this.durationMs = durationMs;
  }

  public void setRequesterId(String requesterId) {
    this.requesterId = requesterId;
  }

  public void setCreatedAt(OffsetDateTime createdAt) {
    this.createdAt = createdAt;
  }
}
