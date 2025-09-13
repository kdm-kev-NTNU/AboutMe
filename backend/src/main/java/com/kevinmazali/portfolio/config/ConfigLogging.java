package com.kevinmazali.portfolio.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;

@Configuration
public class ConfigLogging implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ConfigLogging.class);

    private final Environment environment;

    public ConfigLogging(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("Config: user.dir={} (working directory)", System.getProperty("user.dir"));
        java.io.File envHere = new java.io.File(".env");
        java.io.File envBackend = new java.io.File("backend/.env");
        log.info("Config: exists ./.env = {}", envHere.exists());
        log.info("Config: exists backend/.env = {}", envBackend.exists());
        // Report resolved values (mask secrets)
        log.info("Config: server.port={}", environment.getProperty("server.port"));
        log.info("Config: spring.datasource.url={}", environment.getProperty("spring.datasource.url"));
        log.info("Config: spring.datasource.username={}", environment.getProperty("spring.datasource.username"));
        String pwd = environment.getProperty("spring.datasource.password");
        log.info("Config: spring.datasource.password={} (masked)", pwd == null || pwd.isBlank() ? "<empty>" : "***");

        // Indicate whether .env was loaded
        if (environment instanceof ConfigurableEnvironment ce) {
            boolean hasDotEnv = false;
            for (PropertySource<?> ps : ce.getPropertySources()) {
                String name = ps.getName();
                if (name != null && name.toLowerCase().contains(".env")) {
                    hasDotEnv = true;
                    break;
                }
            }
            log.info("Config: .env property source loaded = {}", hasDotEnv);
        }
    }
}


