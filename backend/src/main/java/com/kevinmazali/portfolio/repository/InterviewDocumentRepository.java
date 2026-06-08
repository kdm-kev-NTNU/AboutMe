package com.kevinmazali.portfolio.repository;

import com.kevinmazali.portfolio.model.interview.InterviewDocumentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InterviewDocumentRepository extends JpaRepository<InterviewDocumentEntity, String> {}
