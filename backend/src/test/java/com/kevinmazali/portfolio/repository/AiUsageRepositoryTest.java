package com.kevinmazali.portfolio.repository;

import com.kevinmazali.portfolio.model.AiUsageEvent;
import com.kevinmazali.portfolio.model.AiUsageEvent.IdentityType;
import com.kevinmazali.portfolio.service.DocumentIngestionService;
import com.kevinmazali.portfolio.testsupport.VectorStoreTestConfiguration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Import(VectorStoreTestConfiguration.class)
@TestPropertySource(
    properties = {
        "spring.autoconfigure.exclude=org.springframework.ai.vectorstore.pgvector.autoconfigure.PgVectorStoreAutoConfiguration",
        "spring.datasource.url=jdbc:h2:mem:aiusagerepo;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.ai.openai.api-key=test-placeholder-key-for-context-tests-only",
        "spring.ai.openai.chat.enabled=true",
        "spring.ai.anthropic.api-key=test-anthropic-api-key-not-real",
        "portfolio.chat.default-model-id=gpt-5.4-mini",
        "server.port=0",
    })
class AiUsageRepositoryTest {

  @MockitoBean
  private DocumentIngestionService documentIngestionService;

  @Autowired
  private AiUsageRepository repository;

  @Test
  void sumCostSince_filtersByUserAndTime() {
    Instant base = Instant.parse("2026-01-15T12:00:00Z");
    repository.save(row("alice", "gpt-1", base.minus(1, ChronoUnit.DAYS), "0.01"));
    repository.save(row("alice", "gpt-1", base.minus(1, ChronoUnit.HOURS), "0.02"));
    repository.save(row("bob", "gpt-1", base.minus(1, ChronoUnit.HOURS), "9.99"));

    BigDecimal alice = repository.sumCostSince("alice", base.minus(2, ChronoUnit.DAYS));
    assertThat(alice).isEqualByComparingTo("0.03");

    BigDecimal global = repository.sumGlobalCostSince(base.minus(2, ChronoUnit.DAYS));
    assertThat(global).isEqualByComparingTo("10.02");
  }

  @Test
  void sumCostBetween_respectsWindow() {
    Instant from = Instant.parse("2026-02-01T00:00:00Z");
    Instant to = Instant.parse("2026-02-02T00:00:00Z");
    repository.save(row("u", "gpt-1", from.plus(1, ChronoUnit.HOURS), "1.00"));
    repository.save(row("u", "gpt-1", to.minus(1, ChronoUnit.SECONDS), "2.00"));
    repository.save(row("u", "gpt-1", to, "99.00"));

    BigDecimal sum = repository.sumCostBetween("u", from, to);
    assertThat(sum).isEqualByComparingTo("3.00");
  }

  @Test
  void aggregateByDayAndModel_groupsRows() {
    Instant day = Instant.parse("2026-03-10T10:00:00Z");
    repository.save(row("u", "m1", day, "0.5"));
    repository.save(row("u", "m1", day.plus(1, ChronoUnit.HOURS), "0.5"));
    repository.save(row("u", "m2", day.plus(2, ChronoUnit.HOURS), "1.0"));

    List<Object[]> rows = repository.aggregateByDayAndModel(day.minus(1, ChronoUnit.DAYS));
    assertThat(rows).isNotEmpty();
    BigDecimal m1Total =
        rows.stream()
            .filter(r -> "m1".equals(r[1]))
            .map(r -> (BigDecimal) r[2])
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    assertThat(m1Total).isEqualByComparingTo("1.0");
  }

  private static AiUsageEvent row(String user, String model, Instant created, String costUsd) {
    AiUsageEvent r = new AiUsageEvent();
    r.setIdentityType(IdentityType.anonymous);
    r.setIdentityKey(user);
    r.setModel(model);
    r.setPromptTokens(1);
    r.setCompletionTokens(1);
    r.setEstimatedCostUsd(new BigDecimal(costUsd));
    r.setCreatedAt(created);
    return r;
  }
}
