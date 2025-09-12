package com.kevinmazali.portfolio.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Getter;

import java.time.OffsetDateTime;

/**
 * JPA entity capturing a minimal audit log for API requests and responses.
 */
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

    @Lob
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String payload;

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

  public void setPayload(String payload) {
        this.payload = payload;
    }

  public void setRequesterId(String requesterId) {
        this.requesterId = requesterId;
    }

  public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}


