package com.kevinmazali.portfolio.repository;

import com.kevinmazali.portfolio.model.interview.InterviewTranscriptTurnEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InterviewTranscriptTurnRepository extends JpaRepository<InterviewTranscriptTurnEntity, Long> {

  List<InterviewTranscriptTurnEntity> findBySessionIdOrderBySequenceNoAsc(String sessionId);

  void deleteBySessionId(String sessionId);
}
