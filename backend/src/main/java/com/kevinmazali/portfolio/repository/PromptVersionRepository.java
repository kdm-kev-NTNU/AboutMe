package com.kevinmazali.portfolio.repository;

import com.kevinmazali.portfolio.model.PromptVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * JPQL queries for versioned RAG prompts; NULL language/provider are matched with explicit {@code IS NULL}
 * semantics (portable across SQL databases).
 */
public interface PromptVersionRepository extends JpaRepository<PromptVersion, Long> {

    /**
     * Find the active version for a variant. Handles NULL language/provider explicitly
     * because MySQL treats {@code NULL = NULL} as false.
     */
    @Query("""
        SELECT pv FROM PromptVersion pv
        WHERE pv.name = :name
          AND pv.isActive = true
          AND (:language IS NULL AND pv.language IS NULL OR pv.language = :language)
          AND (:provider IS NULL AND pv.provider IS NULL OR pv.provider = :provider)
        """)
    Optional<PromptVersion> findActiveVariant(
        @Param("name") String name,
        @Param("language") String language,
        @Param("provider") String provider
    );

    /**
     * All versions for a variant (newest first), used for history listing.
     */
    @Query("""
        SELECT pv FROM PromptVersion pv
        WHERE pv.name = :name
          AND (:language IS NULL AND pv.language IS NULL OR pv.language = :language)
          AND (:provider IS NULL AND pv.provider IS NULL OR pv.provider = :provider)
        ORDER BY pv.version DESC, pv.createdAt DESC
        """)
    List<PromptVersion> findVariantHistory(
        @Param("name") String name,
        @Param("language") String language,
        @Param("provider") String provider
    );

    /**
     * All active prompt versions across all names, for the "names" listing.
     */
    @Query("SELECT pv FROM PromptVersion pv WHERE pv.isActive = true ORDER BY pv.name, pv.language, pv.provider")
    List<PromptVersion> findAllActive();

    /**
     * All rows for a variant (for bulk deletion or activation toggle).
     */
    @Query("""
        SELECT pv FROM PromptVersion pv
        WHERE pv.name = :name
          AND (:language IS NULL AND pv.language IS NULL OR pv.language = :language)
          AND (:provider IS NULL AND pv.provider IS NULL OR pv.provider = :provider)
        """)
    List<PromptVersion> findAllForVariant(
        @Param("name") String name,
        @Param("language") String language,
        @Param("provider") String provider
    );

    /**
     * Count versions for a variant, used to compute the next version number.
     */
    @Query("""
        SELECT COUNT(pv) FROM PromptVersion pv
        WHERE pv.name = :name
          AND (:language IS NULL AND pv.language IS NULL OR pv.language = :language)
          AND (:provider IS NULL AND pv.provider IS NULL OR pv.provider = :provider)
        """)
    long countVariant(
        @Param("name") String name,
        @Param("language") String language,
        @Param("provider") String provider
    );

    /**
     * Highest version number for a variant.
     */
    @Query("""
        SELECT MAX(pv.version) FROM PromptVersion pv
        WHERE pv.name = :name
          AND (:language IS NULL AND pv.language IS NULL OR pv.language = :language)
          AND (:provider IS NULL AND pv.provider IS NULL OR pv.provider = :provider)
        """)
    Optional<Integer> findMaxVersion(
        @Param("name") String name,
        @Param("language") String language,
        @Param("provider") String provider
    );
}
