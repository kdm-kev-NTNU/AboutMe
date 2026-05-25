package com.kevinmazali.portfolio.controller;

import com.kevinmazali.portfolio.config.AiBudgetProperties;
import com.kevinmazali.portfolio.config.AiKillSwitchProperties;
import com.kevinmazali.portfolio.model.ApiError;
import com.kevinmazali.portfolio.repository.AiUsageRepository;
import com.kevinmazali.portfolio.service.AiCircuitBreaker;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Admin APIs for AI cost controls, kill switch, and usage aggregates (Layers 4 and 5).
 */
@RestController
@RequestMapping("/admin/tools/ai")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Tag(name = "AI cost controls", description = "Kill switch, status, and usage aggregates")
public class AiAdminController {

  private final AiCircuitBreaker circuitBreaker;
  private final AiUsageRepository usageRepository;
  private final AiBudgetProperties budgetProperties;
  private final AiKillSwitchProperties killSwitchProperties;

  @Operation(summary = "AI circuit status and configured limits")
  @GetMapping("/status")
  public Map<String, Object> status() {
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("circuitOpen", circuitBreaker.isOpen());
    m.put("adminKillOpen", circuitBreaker.isAdminKillOpen());
    m.put("autoKillOpen", circuitBreaker.isAutoKillOpen());
    m.put("globalMonthSpendUsd", circuitBreaker.getLastKnownMonthSpendUsd());
    m.put("killSwitchMonthlyLimitUsd", killSwitchProperties.getMonthlyLimitUsd());
    m.put("killSwitchEnabled", killSwitchProperties.isEnabled());
    m.put("budgetEnabled", budgetProperties.isEnabled());
    m.put("budgetDailyLimitUsd", budgetProperties.getDailyLimitUsd());
    m.put("budgetMonthlyLimitUsd", budgetProperties.getMonthlyLimitUsd());
    m.put("budgetAnonymousDailyLimitUsd", budgetProperties.getAnonymousDailyLimitUsd());
    m.put("budgetAnonymousMonthlyLimitUsd", budgetProperties.getAnonymousMonthlyLimitUsd());
    return m;
  }

  public record KillSwitchBody(Boolean open, Boolean releaseAuto) {
  }

  @Operation(summary = "Toggle admin kill switch; optionally clear auto trip")
  @PostMapping("/kill-switch")
  public ResponseEntity<?> killSwitch(@RequestBody KillSwitchBody body) {
    if (body == null || body.open() == null) {
      return ResponseEntity.badRequest().body(new ApiError("Body must include \"open\": true|false"));
    }
    circuitBreaker.setAdminKillOpen(Boolean.TRUE.equals(body.open()));
    if (Boolean.TRUE.equals(body.releaseAuto())) {
      circuitBreaker.clearAutoTrip();
    }
    return ResponseEntity.accepted().build();
  }

  @Operation(summary = "Aggregated AI spend by UTC day and model")
  @GetMapping("/usage")
  public List<Map<String, Object>> usage(@RequestParam(name = "days", defaultValue = "30") int days) {
    int safeDays = Math.min(Math.max(days, 1), 365);
    Instant since = Instant.now().minus(safeDays, ChronoUnit.DAYS);
    List<Object[]> rows = usageRepository.aggregateByDayAndModel(since);
    List<Map<String, Object>> out = new ArrayList<>();
    for (Object[] row : rows) {
      Map<String, Object> item = new LinkedHashMap<>();
      item.put("day", row[0] != null ? row[0].toString() : null);
      item.put("model", row[1]);
      item.put("totalUsd", row[2] instanceof BigDecimal bd ? bd : new BigDecimal(row[2].toString()));
      out.add(item);
    }
    return out;
  }
}
