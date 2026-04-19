package com.kevinmazali.portfolio.repository;

import com.kevinmazali.portfolio.model.RequestLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;

/** Read-side helpers for chat/request audit timelines (ordered by {@code createdAt}). */
public interface RequestLogRepository extends JpaRepository<RequestLog, Long> {

    List<RequestLog> findAllByOrderByCreatedAtAsc();

    List<RequestLog> findAllByOrderByCreatedAtDesc();

    List<RequestLog> findByCreatedAtBetweenOrderByCreatedAtAsc(OffsetDateTime start, OffsetDateTime end);

    List<RequestLog> findByRequesterIdOrderByCreatedAtAsc(String requesterId);

    List<RequestLog> findByRequesterIdAndCreatedAtBetweenOrderByCreatedAtAsc(String requesterId, OffsetDateTime start, OffsetDateTime end);
}


