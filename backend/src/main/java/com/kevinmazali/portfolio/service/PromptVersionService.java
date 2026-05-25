package com.kevinmazali.portfolio.service;

import com.kevinmazali.portfolio.model.PromptTemplate;
import com.kevinmazali.portfolio.model.PromptVersion;
import com.kevinmazali.portfolio.model.prompt.PromptDiffResponse;
import com.kevinmazali.portfolio.model.prompt.PromptNameEntry;
import com.kevinmazali.portfolio.model.prompt.PromptVersionResponse;
import com.kevinmazali.portfolio.repository.PromptTemplateRepository;
import com.kevinmazali.portfolio.repository.PromptVersionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Prompt version management mirroring Piscada's prompt_version_repo + prompt_loader pattern.
 *
 * <p>DB-first resolution with classpath .st fallback and in-memory cache. Immutable versions
 * per variant {@code (name, language, provider)}, at most one active per variant.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PromptVersionService {

    /**
     * Known classpath templates that can be seeded into the DB.
     * Key = {@code (name, language, provider)}, value = classpath path.
     */
    private static final Map<VariantKey, String> CLASSPATH_FALLBACKS = Map.of(
        new VariantKey("rag_portfolio", null, "openai"), "templates/rag-prompt-template-openai.st",
        new VariantKey("rag_portfolio", null, "anthropic"), "templates/rag-prompt-template-anthropic.st"
    );

    private final PromptVersionRepository repo;
    private final PromptTemplateRepository templateRepository;

    private final ConcurrentHashMap<VariantKey, String> cache = new ConcurrentHashMap<>();

    // ── Resolution (DB → fallback → cache) ──────────────────────────────

    /**
     * Load prompt content for a variant. Checks cache, then DB (active), then classpath.
     *
     * @throws IllegalStateException if no content found anywhere
     */
    public String loadPromptContent(String name, String language, String provider) {
        var key = new VariantKey(name, blankToNull(language), blankToNull(provider));

        String cached = cache.get(key);
        if (cached != null) return cached;

        Optional<PromptVersion> dbRow = repo.findActiveVariant(key.name, key.language, key.provider);
        if (dbRow.isPresent()) {
            cache.put(key, dbRow.get().getContent());
            return dbRow.get().getContent();
        }

        String classpathPath = CLASSPATH_FALLBACKS.get(key);
        if (classpathPath != null) {
            String fallback = readClasspath(classpathPath);
            if (fallback != null) {
                cache.put(key, fallback);
                return fallback;
            }
        }

        throw new IllegalStateException(
            "No prompt found for name=%s, language=%s, provider=%s".formatted(name, language, provider));
    }

    /**
     * Resolve the RAG prompt template for a given provider name (convenience wrapper).
     */
    public String loadRagPrompt(String providerName) {
        return loadPromptContent("rag_portfolio", null, providerName);
    }

    // ── Cache management ────────────────────────────────────────────────

    public void invalidateCache() {
        cache.clear();
    }

    public void invalidateCache(String name) {
        cache.entrySet().removeIf(e -> e.getKey().name.equals(name));
    }

    // ── List / history ──────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<PromptNameEntry> listActiveNames() {
        return repo.findAllActive().stream()
            .map(PromptVersionService::toNameEntry)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<PromptVersionResponse> listHistory(String name, String language, String provider) {
        return repo.findVariantHistory(name, blankToNull(language), blankToNull(provider))
            .stream()
            .map(PromptVersionService::toResponse)
            .toList();
    }

    // ── Create ──────────────────────────────────────────────────────────

    @Transactional
    public PromptVersionResponse createVersion(String name, String content,
                                                String language, String provider,
                                                String description) {
        String lang = blankToNull(language);
        String prov = blankToNull(provider);
        int nextVersion = repo.findMaxVersion(name, lang, prov).orElse(0) + 1;

        PromptTemplate template = ensureTemplate(name, lang, prov);
        PromptVersion row = PromptVersion.builder()
            .template(template)
            .name(name)
            .version(nextVersion)
            .language(lang)
            .provider(prov)
            .content(content)
            .contentHash(sha256(content))
            .isActive(false)
            .description(description)
            .metadataJson(Map.of("source", "api"))
            .build();
        repo.save(row);
        return toResponse(row);
    }

    // ── Activate ────────────────────────────────────────────────────────

    @Transactional
    public PromptVersionResponse activateVersion(long versionId) {
        PromptVersion target = repo.findById(versionId)
            .orElseThrow(() -> new IllegalArgumentException("Prompt version id=" + versionId + " not found"));

        // Exactly one active row per (name, language, provider): flip flags for the whole variant group.
        List<PromptVersion> siblings = repo.findAllForVariant(
            target.getName(), target.getLanguage(), target.getProvider());
        for (PromptVersion sib : siblings) {
            sib.setIsActive(sib.getId().equals(target.getId()));
        }
        repo.saveAll(siblings);
        invalidateCache(target.getName());
        return toResponse(target);
    }

    // ── Delete variant ──────────────────────────────────────────────────

    @Transactional
    public int deleteVariant(String name, String language, String provider) {
        List<PromptVersion> rows = repo.findAllForVariant(name, blankToNull(language), blankToNull(provider));
        repo.deleteAll(rows);
        invalidateCache(name);
        return rows.size();
    }

    // ── Seed from classpath ─────────────────────────────────────────────

    @Transactional
    public Map<String, Object> seedFromClasspath() {
        int created = 0;
        int skipped = 0;

        for (var entry : CLASSPATH_FALLBACKS.entrySet()) {
            VariantKey key = entry.getKey();
            String classpathPath = entry.getValue();
            String content = readClasspath(classpathPath);
            if (content == null || content.isBlank()) {
                skipped++;
                continue;
            }

            long existing = repo.countVariant(key.name, key.language, key.provider);
            if (existing > 0) {
                skipped++;
                continue;
            }

            PromptTemplate template = ensureTemplate(key.name, key.language, key.provider);
            PromptVersion row = PromptVersion.builder()
                .template(template)
                .name(key.name)
                .version(1)
                .language(key.language)
                .provider(key.provider)
                .content(content)
                .contentHash(sha256(content))
                .isActive(true)
                .description("Seeded from classpath: " + classpathPath)
                .metadataJson(Map.of("source", "classpath_seed"))
                .build();
            repo.save(row);
            created++;
        }

        invalidateCache();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("created", created);
        result.put("skipped", skipped);
        result.put("total_fallbacks", CLASSPATH_FALLBACKS.size());
        return result;
    }

    // ── Diff ────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public PromptDiffResponse diff(String name, String language, String provider) {
        String lang = blankToNull(language);
        String prov = blankToNull(provider);

        Optional<PromptVersion> active = repo.findActiveVariant(name, lang, prov);
        String dbContent = active.map(PromptVersion::getContent).orElse(null);

        VariantKey key = new VariantKey(name, lang, prov);
        String classpathPath = CLASSPATH_FALLBACKS.get(key);
        String fallbackContent = classpathPath != null ? readClasspath(classpathPath) : null;

        boolean isEqual = dbContent != null && fallbackContent != null && dbContent.equals(fallbackContent);

        return new PromptDiffResponse(
            name, lang, prov,
            dbContent != null,
            fallbackContent != null,
            isEqual,
            dbContent,
            fallbackContent
        );
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    private PromptTemplate ensureTemplate(String name, String language, String provider) {
        return templateRepository
            .findVariant(name, language, provider)
            .orElseGet(
                () ->
                    templateRepository.save(
                        PromptTemplate.builder().name(name).language(language).provider(provider).build()));
    }

    private static String sha256(String content) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(content.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 unavailable", e);
        }
    }

    private static String readClasspath(String path) {
        try {
            ClassPathResource res = new ClassPathResource(path);
            try (InputStream in = res.getInputStream()) {
                return new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            log.debug("Could not read classpath resource: {}", path, e);
            return null;
        }
    }

    private static PromptNameEntry toNameEntry(PromptVersion pv) {
        return new PromptNameEntry(
            pv.getName(),
            pv.getLanguage(),
            pv.getProvider(),
            pv.getVersion(),
            pv.getId(),
            pv.getCreatedAt() != null ? pv.getCreatedAt().toString() : null
        );
    }

    static PromptVersionResponse toResponse(PromptVersion pv) {
        return new PromptVersionResponse(
            pv.getId(),
            pv.getName(),
            pv.getVersion(),
            pv.getLanguage(),
            pv.getProvider(),
            pv.getContent(),
            pv.getContentHash(),
            Boolean.TRUE.equals(pv.getIsActive()),
            pv.getDescription(),
            pv.getCreatedAt() != null ? pv.getCreatedAt().toString() : null
        );
    }

    private record VariantKey(String name, String language, String provider) {}
}
