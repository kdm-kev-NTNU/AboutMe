package com.kevinmazali.portfolio;

import com.kevinmazali.portfolio.service.OpenAIService;
import com.kevinmazali.portfolio.service.RequestLogService;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration(proxyBeanMethods = false)
public class MockConfig {

    @Bean
    OpenAIService openAIService() {
        return Mockito.mock(OpenAIService.class);
    }

    @Bean
    RequestLogService requestLogService() {
        return Mockito.mock(RequestLogService.class);
    }
}


