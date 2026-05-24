package com.kevinmazali.portfolio;

import com.kevinmazali.portfolio.config.AiBudgetProperties;
import com.kevinmazali.portfolio.config.AiKillSwitchProperties;
import com.kevinmazali.portfolio.config.AiLimitsProperties;
import com.kevinmazali.portfolio.config.AskRateLimitProperties;
import com.kevinmazali.portfolio.config.DatasetGenerateRateLimitProperties;
import com.kevinmazali.portfolio.config.ExperimentRunRateLimitProperties;
import com.kevinmazali.portfolio.config.PostHogProperties;
import com.kevinmazali.portfolio.config.RelevanceGateProperties;
import com.kevinmazali.portfolio.config.RetrievalProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Spring Boot entry point for the portfolio backend application.
 * Boots the web context and exposes REST APIs.
 */
@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties({
    AiLimitsProperties.class,
    AiBudgetProperties.class,
    AiKillSwitchProperties.class,
    AskRateLimitProperties.class,
    ExperimentRunRateLimitProperties.class,
    DatasetGenerateRateLimitProperties.class,
    PostHogProperties.class,
    RetrievalProperties.class,
    RelevanceGateProperties.class
})
public class PortfolioApplication {

	/**
	 * Application bootstrap.
	 *
	 * @param args command-line arguments
	 */
	public static void main(String[] args) {
		SpringApplication.run(PortfolioApplication.class, args);
	}

}
