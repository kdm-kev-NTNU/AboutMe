package com.kevinmazali.portfolio;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot entry point for the portfolio backend application.
 * Boots the web context and exposes REST APIs.
 */
@SpringBootApplication
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
