package com.kevinmazali.portfolio.service;

import com.kevinmazali.portfolio.config.AiBudgetProperties;
import com.kevinmazali.portfolio.exception.BudgetExceededException;
import com.kevinmazali.portfolio.model.analytics.AiGenerationAnalytics;
import com.kevinmazali.portfolio.model.AiUsageEvent;
import com.kevinmazali.portfolio.repository.AiUsageRepository;
import com.kevinmazali.portfolio.repository.UserRepository;
import com.kevinmazali.portfolio.security.AnalyticsIdentityService;
import com.kevinmazali.portfolio.util.AiUsageIdentity;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;

/**
 * Per-user / anonymous spend limits, cost estimation, usage persistence, and spend spike logging (Layers 3 and 5).
 */
@Slf4j
@Service
public class AiBudgetService {

  private static final String SYSTEM_EVALUATOR = "system:evaluator";

  private final AiBudgetProperties properties;
  private final AiUsageRepository usageRepository;
  private final UserRepository userRepository;
  private final MeterRegistry meterRegistry;
  @Nullable
  private final PostHogLlmService postHogLlmService;
  private final AnalyticsIdentityService analyticsIdentityService;

  public AiBudgetService(
      AiBudgetProperties properties,
      AiUsageRepository usageRepository,
      UserRepository userRepository,
      MeterRegistry meterRegistry,
      AnalyticsIdentityService analyticsIdentityService,
      @Autowired(required = false) @Nullable PostHogLlmService postHogLlmService) {
    this.properties = properties;
    this.usageRepository = usageRepository;
    this.userRepository = userRepository;
    this.meterRegistry = meterRegistry;
    this.analyticsIdentityService = analyticsIdentityService;
    this.postHogLlmService = postHogLlmService;
  }

  /**
   * Skips enforcement for internal service accounts (e.g. evaluator); still records usage when {@link #recordUsage} is called.
   */
  public void assertWithinBudget(String userIdentifier, boolean anonymous) {
    if (!properties.isEnabled()) {
      return;
    }
    if (userIdentifier != null && userIdentifier.startsWith("system:")) {
      return;
    }
    Instant startOfDay = Instant.now().atZone(ZoneOffset.UTC).toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant();
    Instant startOfMonth = Instant.now()
        .atZone(ZoneOffset.UTC)
        .with(TemporalAdjusters.firstDayOfMonth())
        .truncatedTo(ChronoUnit.DAYS)
        .toInstant();

    BigDecimal daily = usageRepository.sumCostSince(userIdentifier, startOfDay);
    BigDecimal monthly = usageRepository.sumCostSince(userIdentifier, startOfMonth);

    BigDecimal dailyLimit = anonymous ? properties.getAnonymousDailyLimitUsd() : properties.getDailyLimitUsd();
    BigDecimal monthlyLimit = anonymous ? properties.getAnonymousMonthlyLimitUsd() : properties.getMonthlyLimitUsd();

    if (daily.compareTo(dailyLimit) > 0) {
      throw new BudgetExceededException("Daily AI budget exceeded for this account.");
    }
    if (monthly.compareTo(monthlyLimit) > 0) {
      throw new BudgetExceededException("Monthly AI budget exceeded for this account.");
    }
  }

  @Transactional
  public void recordUsage(
      String userIdentifier,
      String modelId,
      int promptTokens,
      int completionTokens,
      boolean anonymous) {
    recordUsage(
        userIdentifier,
        modelId,
        promptTokens,
        completionTokens,
        anonymous,
        null,
        null,
        AiGenerationAnalytics.empty());
  }

  /**
   * Records usage and optionally forwards LLM analytics to PostHog after successful commit.
   *
   * @param latencySeconds wall-clock LLM call duration, or null if not measured
   * @param generationSpanName logical step name for PostHog (e.g. {@code query_expansion}, {@code rag_completion})
   */
  @Transactional
  public void recordUsage(
      String userIdentifier,
      String modelId,
      int promptTokens,
      int completionTokens,
      boolean anonymous,
      @Nullable Double latencySeconds,
      @Nullable String generationSpanName) {
    recordUsage(
        userIdentifier,
        modelId,
        promptTokens,
        completionTokens,
        anonymous,
        latencySeconds,
        generationSpanName,
        AiGenerationAnalytics.empty());
  }

  /**
   * Same as {@link #recordUsage(String, String, int, int, boolean, Double, String)} with rich PostHog payload
   * (input/output/context, trace ids).
   */
  @Transactional
  public void recordUsage(
      String userIdentifier,
      String modelId,
      int promptTokens,
      int completionTokens,
      boolean anonymous,
      @Nullable Double latencySeconds,
      @Nullable String generationSpanName,
      @Nullable AiGenerationAnalytics analytics) {
    BigDecimal cost = estimateCostUsd(modelId, promptTokens, completionTokens);
    String budgetKey = userIdentifier != null ? userIdentifier : "unknown";
    Long userId = resolveUserId(budgetKey);
    AiUsageIdentity identity = AiUsageIdentity.fromBudgetUserId(budgetKey, userId);
    AiUsageEvent row = new AiUsageEvent();
    row.setUserId(userId);
    row.setIdentityType(identity.identityType());
    row.setIdentityKey(identity.identityKey());
    row.setModel(modelId);
    row.setPromptTokens(Math.max(0, promptTokens));
    row.setCompletionTokens(Math.max(0, completionTokens));
    row.setEstimatedCostUsd(cost);
    row.setCreatedAt(Instant.now());
    usageRepository.save(row);

    String userType = anonymous ? "anonymous" : "authenticated";
    Tags tags = Tags.of("model", modelId, "user_type", userType);
    Counter.builder("ai.tokens.prompt")
        .tags(tags)
        .register(meterRegistry)
        .increment(Math.max(0, promptTokens));
    Counter.builder("ai.tokens.completion")
        .tags(tags)
        .register(meterRegistry)
        .increment(Math.max(0, completionTokens));
    Counter.builder("ai.cost.usd")
        .tags(tags)
        .register(meterRegistry)
        .increment(cost.doubleValue());

    schedulePostHogGenerationCapture(
        userIdentifier,
        modelId,
        promptTokens,
        completionTokens,
        anonymous,
        cost,
        latencySeconds,
        generationSpanName,
        analytics != null ? analytics : AiGenerationAnalytics.empty());

    if (userIdentifier == null || !userIdentifier.startsWith("system:")) {
      detectSpendSpike(userIdentifier != null ? userIdentifier : "unknown", anonymous);
    }
  }

  private void schedulePostHogGenerationCapture(
      String userIdentifier,
      String modelId,
      int promptTokens,
      int completionTokens,
      boolean anonymous,
      BigDecimal cost,
      @Nullable Double latencySeconds,
      @Nullable String generationSpanName,
      AiGenerationAnalytics analytics) {
    PostHogLlmService sink = postHogLlmService;
    if (sink == null || !sink.isEnabled()) {
      return;
    }
    String uid = userIdentifier != null ? userIdentifier : "unknown";
    AnalyticsIdentityService.PostHogCaptureIdentity phIdentity =
        analyticsIdentityService.captureIdentity(uid, anonymous);
    Runnable task =
        () ->
            sink.captureGenerationAsync(
                phIdentity.distinctId(),
                modelId,
                promptTokens,
                completionTokens,
                cost,
                phIdentity.anonymous(),
                latencySeconds,
                generationSpanName,
                analytics);
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.registerSynchronization(
          new TransactionSynchronization() {
            @Override
            public void afterCommit() {
              task.run();
            }
          });
    } else {
      task.run();
    }
  }

  /** Identifier used for LLM-as-judge traffic (tracked globally, no per-user budget). */
  public static String systemEvaluatorUserId() {
    return SYSTEM_EVALUATOR;
  }

  private Long resolveUserId(String budgetKey) {
    if (!budgetKey.startsWith("user:")) {
      return null;
    }
    String username = budgetKey.substring(5);
    return userRepository.findByUsername(username).map(u -> u.getId()).orElse(null);
  }

  private void detectSpendSpike(String userIdentifier, boolean anonymous) {
    if (!properties.isEnabled()) {
      return;
    }
    Instant now = Instant.now();
    Instant hourAgo = now.minus(1, ChronoUnit.HOURS);
    BigDecimal lastHour = usageRepository.sumCostBetween(userIdentifier, hourAgo, now);
    if (lastHour == null) {
      lastHour = BigDecimal.ZERO;
    }

    BigDecimal dailyLimit = anonymous ? properties.getAnonymousDailyLimitUsd() : properties.getDailyLimitUsd();
    BigDecimal expectedHourly = dailyLimit.divide(BigDecimal.valueOf(24), 8, RoundingMode.HALF_UP);
    if (expectedHourly.signum() <= 0) {
      return;
    }
    BigDecimal threshold = expectedHourly.multiply(BigDecimal.valueOf(properties.getSpikeHourlyMultiplier()));
    if (lastHour.compareTo(threshold) > 0) {
      log.warn(
          "AI spend spike detected for userIdentifier={}: lastHourUsd={} thresholdUsd={} (dailyLimitUsd={}, multiplier={})",
          userIdentifier, lastHour, threshold, dailyLimit, properties.getSpikeHourlyMultiplier());
    }
  }

  public BigDecimal estimateCostUsd(String modelId, int promptTokens, int completionTokens) {
    AiBudgetProperties.ModelPricing p = properties.getModels().get(modelId);
    if (p == null) {
      return BigDecimal.ZERO;
    }
    BigDecimal in = p.getInputPerMillionUsd() != null ? p.getInputPerMillionUsd() : BigDecimal.ZERO;
    BigDecimal out = p.getOutputPerMillionUsd() != null ? p.getOutputPerMillionUsd() : BigDecimal.ZERO;
    BigDecimal promptCost = in.multiply(BigDecimal.valueOf(promptTokens)).divide(BigDecimal.valueOf(1_000_000), 8, RoundingMode.HALF_UP);
    BigDecimal completionCost = out.multiply(BigDecimal.valueOf(completionTokens)).divide(BigDecimal.valueOf(1_000_000), 8, RoundingMode.HALF_UP);
    return promptCost.add(completionCost);
  }
}
