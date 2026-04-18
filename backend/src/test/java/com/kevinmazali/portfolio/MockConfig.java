package com.kevinmazali.portfolio;

import com.kevinmazali.portfolio.model.ChatModelOption;
import com.kevinmazali.portfolio.model.chat.ChatProvider;
import com.kevinmazali.portfolio.model.chat.SupportedChatModel;
import com.kevinmazali.portfolio.service.ChatModelCatalog;
import com.kevinmazali.portfolio.service.OpenAIService;
import com.kevinmazali.portfolio.service.RequestLogService;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

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

    @Bean
    ChatModelCatalog chatModelCatalog() {
        ChatModelCatalog catalog = Mockito.mock(ChatModelCatalog.class);
        lenient().when(catalog.isModelConfigured(any(SupportedChatModel.class))).thenReturn(true);
        lenient().when(catalog.listAvailableModels()).thenReturn(List.of(
            new ChatModelOption("gpt-4o-mini", ChatProvider.OPENAI, "GPT-4o mini")
        ));
        return catalog;
    }
}


