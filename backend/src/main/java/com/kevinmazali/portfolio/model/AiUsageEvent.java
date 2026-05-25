package com.kevinmazali.portfolio.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
    name = "ai_usage_events",
    indexes = {
        @Index(name = "idx_ai_usage_ev_identity_created", columnList = "identity_type,identity_key,created_at"),
        @Index(name = "idx_ai_usage_ev_user_created", columnList = "user_id,created_at"),
        @Index(name = "idx_ai_usage_ev_created", columnList = "created_at")
    })
@Getter
@Setter
public class AiUsageEvent {

  public enum IdentityType {
    authenticated,
    anonymous,
    system
  }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "user_id")
  private Long userId;

  @Enumerated(EnumType.STRING)
  @Column(name = "identity_type", nullable = false, length = 16)
  private IdentityType identityType;

  @Column(name = "identity_key", nullable = false, length = 256)
  private String identityKey;

  @Column(nullable = false, length = 128)
  private String model;

  @Column(name = "prompt_tokens", nullable = false)
  private int promptTokens;

  @Column(name = "completion_tokens", nullable = false)
  private int completionTokens;

  @Column(name = "estimated_cost_usd", nullable = false, precision = 19, scale = 8)
  private BigDecimal estimatedCostUsd;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();
}
