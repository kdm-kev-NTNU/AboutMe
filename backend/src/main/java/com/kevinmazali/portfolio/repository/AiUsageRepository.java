package com.kevinmazali.portfolio.repository;

import com.kevinmazali.portfolio.model.AiUsageRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public interface AiUsageRepository extends JpaRepository<AiUsageRecord, Long> {

  @Query("SELECT COALESCE(SUM(r.estimatedCostUsd), 0) FROM AiUsageRecord r WHERE r.userIdentifier = :uid AND r.createdAt >= :since")
  BigDecimal sumCostSince(@Param("uid") String uid, @Param("since") Instant since);

  @Query("SELECT COALESCE(SUM(r.estimatedCostUsd), 0) FROM AiUsageRecord r WHERE r.createdAt >= :since")
  BigDecimal sumGlobalCostSince(@Param("since") Instant since);

  @Query("SELECT COALESCE(SUM(r.estimatedCostUsd), 0) FROM AiUsageRecord r WHERE r.userIdentifier = :uid AND r.createdAt >= :from AND r.createdAt < :to")
  BigDecimal sumCostBetween(
      @Param("uid") String uid,
      @Param("from") Instant from,
      @Param("to") Instant to);

  @Query(
      value = """
          SELECT CAST(r.created_at AS DATE) AS day_bucket, r.model AS model_id, COALESCE(SUM(r.estimated_cost_usd), 0) AS total_usd
          FROM ai_usage_record r
          WHERE r.created_at >= :since
          GROUP BY CAST(r.created_at AS DATE), r.model
          ORDER BY day_bucket DESC, model_id
          """,
      nativeQuery = true)
  List<Object[]> aggregateByDayAndModel(@Param("since") Instant since);
}
