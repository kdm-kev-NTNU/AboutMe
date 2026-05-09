package com.kevinmazali.portfolio.config;

import com.kevinmazali.portfolio.controller.advice.ApiErrorBodyAdvice;
import com.kevinmazali.portfolio.controller.advice.ApiErrorCorrelation;
import io.micrometer.tracing.Tracer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApiErrorConfiguration {

  @Bean
  ApiErrorCorrelation apiErrorCorrelation(ObjectProvider<Tracer> tracer) {
    return new ApiErrorCorrelation(tracer);
  }

  @Bean
  ApiErrorBodyAdvice apiErrorBodyAdvice(ApiErrorCorrelation correlation) {
    return new ApiErrorBodyAdvice(correlation);
  }
}
