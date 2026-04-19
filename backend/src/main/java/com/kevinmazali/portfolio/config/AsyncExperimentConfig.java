package com.kevinmazali.portfolio.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncExperimentConfig {

  @Bean(name = "experimentTaskExecutor")
  public Executor experimentTaskExecutor() {
    ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
    ex.setCorePoolSize(1);
    ex.setMaxPoolSize(2);
    ex.setQueueCapacity(20);
    ex.setThreadNamePrefix("experiment-");
    ex.initialize();
    return ex;
  }
}
