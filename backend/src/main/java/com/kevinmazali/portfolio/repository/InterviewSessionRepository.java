package com.kevinmazali.portfolio.repository;

import com.kevinmazali.portfolio.model.interview.InterviewSessionEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InterviewSessionRepository extends JpaRepository<InterviewSessionEntity, String> {

  List<InterviewSessionEntity> findByDeletedAtIsNullOrderByStartedAtDesc();
}
