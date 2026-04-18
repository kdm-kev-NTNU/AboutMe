package com.kevinmazali.portfolio;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(
	properties = {
		"spring.datasource.url=jdbc:h2:mem:aboutmetest;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
		"spring.datasource.driver-class-name=org.h2.Driver",
		"spring.datasource.username=sa",
		"spring.datasource.password=",
		"spring.jpa.hibernate.ddl-auto=create-drop",
		"spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
		"spring.ai.openai.api-key=test-placeholder-key-for-context-tests-only",
		"spring.ai.model.chat=none",
		"portfolio.chat.default-model-id=gpt-4o-mini",
		"server.port=0"
	}
)
class PortfolioApplicationTests {

	@Test
	void contextLoads() {
	}

}
