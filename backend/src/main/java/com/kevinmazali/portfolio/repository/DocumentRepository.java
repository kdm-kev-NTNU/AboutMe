package com.kevinmazali.portfolio.repository;

import com.kevinmazali.portfolio.model.DocumentEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentRepository extends JpaRepository<DocumentEntity, String> {

  List<DocumentEntity> findAllByOrderByFilenameAsc();
}
