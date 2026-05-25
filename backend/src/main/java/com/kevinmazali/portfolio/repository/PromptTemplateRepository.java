package com.kevinmazali.portfolio.repository;

import com.kevinmazali.portfolio.model.PromptTemplate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PromptTemplateRepository extends JpaRepository<PromptTemplate, Long> {

  @Query("""
      SELECT pt FROM PromptTemplate pt
      WHERE pt.name = :name
        AND (:language IS NULL AND pt.language IS NULL OR pt.language = :language)
        AND (:provider IS NULL AND pt.provider IS NULL OR pt.provider = :provider)
      """)
  Optional<PromptTemplate> findVariant(
      @Param("name") String name,
      @Param("language") String language,
      @Param("provider") String provider);
}
