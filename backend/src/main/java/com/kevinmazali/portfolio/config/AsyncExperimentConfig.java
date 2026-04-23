package com.kevinmazali.portfolio.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncExperimentConfig {

  /**
   * Copies {@link SecurityContextHolder} so async experiment runs use the admin caller for AI budgets
   * (instead of treating every job as anonymous).
   */
  @Bean
  public TaskDecorator securityContextCopyingTaskDecorator() {
    return runnable -> {
      Authentication auth = SecurityContextHolder.getContext().getAuthentication();
      return () -> {
        try {
          var ctx = SecurityContextHolder.createEmptyContext();
          ctx.setAuthentication(auth);
          SecurityContextHolder.setContext(ctx);
          runnable.run();
        } finally {
          SecurityContextHolder.clearContext();
        }
      };
    };
  }

  @Bean(name = "experimentTaskExecutor")
  public Executor experimentTaskExecutor(TaskDecorator securityContextCopyingTaskDecorator) {
    ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
    ex.setCorePoolSize(1);
    ex.setMaxPoolSize(2);
    ex.setQueueCapacity(20);
    ex.setThreadNamePrefix("experiment-");
    ex.setTaskDecorator(securityContextCopyingTaskDecorator);
    ex.initialize();
    return ex;
  }
}
