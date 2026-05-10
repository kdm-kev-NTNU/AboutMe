package com.kevinmazali.portfolio.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kevinmazali.portfolio.config.AiBudgetProperties;
import com.kevinmazali.portfolio.model.RealtimeLookupResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

@ExtendWith(MockitoExtension.class)
class RealtimeLookupServiceTest {

  @Mock private VectorStore vectorStore;

  private RealtimeLookupService service;

  @BeforeEach
  void setUp() {
    service =
        new RealtimeLookupService(
            new RealtimeProfileService(), vectorStore, new AiBudgetProperties(), null);
  }

  @Test
  void lookupReturnsProfileFactsFirstWithoutVectorWhenEnoughProfileMatches() {
    RealtimeLookupResponse response = service.lookup("NTNU", "en");

    assertThat(response.found()).isTrue();
    assertThat(response.snippets()).hasSizeGreaterThanOrEqualTo(2);
    assertThat(response.snippets()).allMatch(s -> "profile".equals(s.sourceType()));
    verify(vectorStore, never()).similaritySearch(any(SearchRequest.class));
  }

  @Test
  void lookupUsesSmallVectorFallbackWhenProfileMatchesAreSparse() {
    when(vectorStore.similaritySearch(any(SearchRequest.class)))
        .thenReturn(List.of(Document.builder()
            .text("Kevin has a concise public document snippet about a rare topic.")
            .metadata(new HashMap<String, Object>(Map.of("filename", "rare.md", "content_type", "text")))
            .build()));

    RealtimeLookupResponse response = service.lookup("rare distributed systems topic", "en");

    assertThat(response.found()).isTrue();
    assertThat(response.snippets()).anySatisfy(snippet -> {
      assertThat(snippet.sourceType()).isEqualTo("rag");
      assertThat(snippet.title()).isEqualTo("rare.md");
      assertThat(snippet.text()).contains("rare topic");
    });

    ArgumentCaptor<SearchRequest> captor = ArgumentCaptor.forClass(SearchRequest.class);
    verify(vectorStore).similaritySearch(captor.capture());
    assertThat(captor.getValue().getTopK()).isEqualTo(5);
    assertThat(captor.getValue().getQuery()).isEqualTo("rare distributed systems topic");
  }

  @Test
  void lookupToleratesVectorFailureAndReturnsEmptyWhenNothingElseMatches() {
    when(vectorStore.similaritySearch(any(SearchRequest.class)))
        .thenThrow(new IllegalStateException("vector unavailable"));

    RealtimeLookupResponse response = service.lookup("unmatched zzzzzzz topic", "en");

    assertThat(response.found()).isFalse();
    assertThat(response.snippets()).isEmpty();
  }

  @Test
  void lookupValidatesQuery() {
    assertThatThrownBy(() -> service.lookup(" ", "en"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("required");

    assertThatThrownBy(() -> service.lookup("x".repeat(301), "en"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("300");
  }

  @Test
  void lookupCachesForSameLanguageAndQuery() {
    when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());

    service.lookup("cache-only-miss", "en");
    service.lookup("cache-only-miss", "en");

    verify(vectorStore).similaritySearch(any(SearchRequest.class));
  }
}
