package com.kevinmazali.portfolio.repository;

import com.kevinmazali.portfolio.model.interview.InterviewTranscriptEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InterviewTranscriptRepository extends JpaRepository<InterviewTranscriptEntity, String> {

  Optional<InterviewTranscriptEntity> findBySessionId(String sessionId);
}
