package com.kevinmazali.portfolio.repository;

import com.kevinmazali.portfolio.model.experiment.DatasetGeneration;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DatasetGenerationRepository extends JpaRepository<DatasetGeneration, Long> {}
