package com.kevinmazali.portfolio.repository;

import com.kevinmazali.portfolio.model.FeedbackSubmission;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence layer for visitor feedback submissions. */
public interface FeedbackRepository extends JpaRepository<FeedbackSubmission, Long> {
}
