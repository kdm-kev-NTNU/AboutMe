package com.kevinmazali.portfolio.service;

import com.kevinmazali.portfolio.model.PromptVersion;
import com.kevinmazali.portfolio.model.prompt.PromptDiffResponse;
import com.kevinmazali.portfolio.model.prompt.PromptNameEntry;
import com.kevinmazali.portfolio.model.prompt.PromptVersionResponse;
import com.kevinmazali.portfolio.repository.PromptVersionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class PromptVersionServiceTest {

	@Mock
	private PromptVersionRepository repo;

	@InjectMocks
	private PromptVersionService service;

	@Test
	void loadPromptContentUsesDbAndCachesSecondLookup() {
		PromptVersion row = PromptVersion.builder()
			.id(1L)
			.name("custom")
			.version(1)
			.language(null)
			.provider("openai")
			.content("from-db")
			.contentHash("h")
			.isActive(true)
			.build();
		when(repo.findActiveVariant("custom", null, "openai")).thenReturn(Optional.of(row));

		assertEquals("from-db", service.loadPromptContent("custom", null, "openai"));
		assertEquals("from-db", service.loadPromptContent("custom", null, "openai"));

		verify(repo, times(1)).findActiveVariant("custom", null, "openai");
	}

	@Test
	void loadPromptContentFallsBackToClasspathForKnownRagVariant() {
		when(repo.findActiveVariant("rag_portfolio", null, "openai")).thenReturn(Optional.empty());

		String content = service.loadPromptContent("rag_portfolio", null, "openai");

		assertThat(content).isNotBlank();
		assertThat(content).contains("portfolio");
	}

	@Test
	void loadPromptContentThrowsWhenNoDbAndNoFallback() {
		when(repo.findActiveVariant("unknown_prompt", null, null)).thenReturn(Optional.empty());

		assertThrows(IllegalStateException.class,
			() -> service.loadPromptContent("unknown_prompt", null, null));
	}

	@Test
	void loadRagPromptDelegatesToRagPortfolioName() {
		when(repo.findActiveVariant("rag_portfolio", null, "anthropic")).thenReturn(Optional.empty());

		String content = service.loadRagPrompt("anthropic");

		assertThat(content).isNotBlank();
	}

	@Test
	void listActiveNamesMapsRepositoryRows() {
		PromptVersion a = PromptVersion.builder()
			.id(1L).name("n1").version(2).language(null).provider(null)
			.content("c").contentHash("h").isActive(true)
			.createdAt(OffsetDateTime.parse("2024-06-01T12:00:00Z"))
			.build();
		when(repo.findAllActive()).thenReturn(List.of(a));

		List<PromptNameEntry> names = service.listActiveNames();

		assertThat(names).hasSize(1);
		assertThat(names.getFirst().name()).isEqualTo("n1");
		assertThat(names.getFirst().activeVersion()).isEqualTo(2);
		assertThat(names.getFirst().activeId()).isEqualTo(1L);
	}

	@Test
	void createVersionSavesNextVersion() {
		when(repo.findMaxVersion("p", null, "openai")).thenReturn(Optional.of(3));
		when(repo.save(any(PromptVersion.class))).thenAnswer(invocation -> {
			PromptVersion pv = invocation.getArgument(0);
			pv.setId(100L);
			return pv;
		});

		PromptVersionResponse res = service.createVersion("p", "body", null, "openai", "note");

		assertThat(res.version()).isEqualTo(4);
		assertThat(res.content()).isEqualTo("body");
		ArgumentCaptor<PromptVersion> cap = ArgumentCaptor.forClass(PromptVersion.class);
		verify(repo).save(cap.capture());
		assertThat(cap.getValue().getVersion()).isEqualTo(4);
		assertThat(cap.getValue().getIsActive()).isFalse();
	}

	@Test
	void activateVersionMarksTargetActiveAndSavesSiblings() {
		PromptVersion v1 = PromptVersion.builder()
			.id(1L).name("x").version(1).language(null).provider(null)
			.content("a").contentHash("h1").isActive(true).build();
		PromptVersion v2 = PromptVersion.builder()
			.id(2L).name("x").version(2).language(null).provider(null)
			.content("b").contentHash("h2").isActive(false).build();
		when(repo.findById(2L)).thenReturn(Optional.of(v2));
		when(repo.findAllForVariant("x", null, null)).thenReturn(List.of(v1, v2));

		PromptVersionResponse res = service.activateVersion(2L);

		assertThat(res.id()).isEqualTo(2L);
		assertThat(res.isActive()).isTrue();
		assertThat(v1.getIsActive()).isFalse();
		assertThat(v2.getIsActive()).isTrue();
		verify(repo).saveAll(List.of(v1, v2));
	}

	@Test
	void activateVersionThrowsWhenIdMissing() {
		when(repo.findById(99L)).thenReturn(Optional.empty());

		assertThrows(IllegalArgumentException.class, () -> service.activateVersion(99L));
	}

	@Test
	void deleteVariantRemovesRows() {
		PromptVersion row = PromptVersion.builder()
			.id(1L).name("d").version(1).language(null).provider("openai")
			.content("c").contentHash("h").isActive(true).build();
		when(repo.findAllForVariant("d", null, "openai")).thenReturn(List.of(row));

		int deleted = service.deleteVariant("d", null, "openai");

		assertThat(deleted).isEqualTo(1);
		verify(repo).deleteAll(List.of(row));
	}

	@Test
	void diffCombinesDbAndClasspath() {
		when(repo.findActiveVariant("rag_portfolio", null, "openai"))
			.thenReturn(Optional.of(PromptVersion.builder()
				.id(1L).name("rag_portfolio").version(1).language(null).provider("openai")
				.content("db-only").contentHash("x").isActive(true).build()));

		PromptDiffResponse d = service.diff("rag_portfolio", null, "openai");

		assertThat(d.hasDbActive()).isTrue();
		assertThat(d.hasCodeFallback()).isTrue();
		assertThat(d.dbContent()).isEqualTo("db-only");
		assertThat(d.fallbackContent()).isNotNull();
	}

	@Test
	void diffUnknownVariantHasNeitherDbNorClasspathFallback() {
		when(repo.findActiveVariant("custom_xyz", null, null)).thenReturn(Optional.empty());

		PromptDiffResponse d = service.diff("custom_xyz", null, null);

		assertThat(d.hasDbActive()).isFalse();
		assertThat(d.hasCodeFallback()).isFalse();
		assertThat(d.isEqual()).isFalse();
		assertThat(d.dbContent()).isNull();
		assertThat(d.fallbackContent()).isNull();
	}

	@Test
	void invalidateCacheForNameForcesReloadFromRepository() {
		PromptVersion rowA =
			PromptVersion.builder()
				.id(1L)
				.name("cache-me")
				.version(1)
				.language(null)
				.provider("openai")
				.content("v1")
				.contentHash("h")
				.isActive(true)
				.build();
		when(repo.findActiveVariant("cache-me", null, "openai")).thenReturn(Optional.of(rowA));

		assertEquals("v1", service.loadPromptContent("cache-me", null, "openai"));
		assertEquals("v1", service.loadPromptContent("cache-me", null, "openai"));
		verify(repo, times(1)).findActiveVariant("cache-me", null, "openai");

		PromptVersion rowB =
			PromptVersion.builder()
				.id(2L)
				.name("cache-me")
				.version(2)
				.language(null)
				.provider("openai")
				.content("v2")
				.contentHash("h2")
				.isActive(true)
				.build();
		when(repo.findActiveVariant("cache-me", null, "openai")).thenReturn(Optional.of(rowB));
		service.invalidateCache("cache-me");

		assertEquals("v2", service.loadPromptContent("cache-me", null, "openai"));
		verify(repo, times(2)).findActiveVariant("cache-me", null, "openai");
	}

	@Test
	void seedFromClasspathSkipsWhenVariantAlreadyExists() {
		when(repo.countVariant("rag_portfolio", null, "openai")).thenReturn(1L);
		when(repo.countVariant("rag_portfolio", null, "anthropic")).thenReturn(1L);

		Map<String, Object> result = service.seedFromClasspath();

		assertThat((Integer) result.get("skipped")).isGreaterThan(0);
		verify(repo, never()).save(any(PromptVersion.class));
	}

	@Test
	void createVersionStartsAtOneWhenNoPriorVersions() {
		when(repo.findMaxVersion("fresh", null, null)).thenReturn(Optional.empty());
		when(repo.save(any(PromptVersion.class)))
			.thenAnswer(
				invocation -> {
					PromptVersion pv = invocation.getArgument(0);
					pv.setId(50L);
					return pv;
				});

		PromptVersionResponse res = service.createVersion("fresh", "body", null, null, "first");

		assertThat(res.version()).isEqualTo(1);
	}

	@Test
	void blankLanguageAndProviderAreNormalizedForLookup() {
		when(repo.findActiveVariant("rag_portfolio", null, "openai")).thenReturn(Optional.empty());

		service.loadPromptContent("rag_portfolio", "   ", "openai");

		verify(repo).findActiveVariant("rag_portfolio", null, "openai");
	}
}
