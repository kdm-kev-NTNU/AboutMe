package com.kevinmazali.portfolio;


import com.kevinmazali.portfolio.MvcTestSessionAuthConfig;import com.kevinmazali.portfolio.config.AskRateLimitProperties;
import com.kevinmazali.portfolio.config.DatasetGenerateRateLimitProperties;
import com.kevinmazali.portfolio.config.ExperimentRunRateLimitProperties;
import com.kevinmazali.portfolio.config.RealtimeRateLimitProperties;
import com.kevinmazali.portfolio.config.SecurityConfig;
import com.kevinmazali.portfolio.config.WebConfig;
import com.kevinmazali.portfolio.controller.QuestionController;
import com.kevinmazali.portfolio.model.Answer;
import com.kevinmazali.portfolio.model.Question;
import com.kevinmazali.portfolio.service.OpenAIService;
import com.kevinmazali.portfolio.service.RequestLogService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = QuestionController.class, properties = "portfolio.chat.default-model-id=gpt-5.4-mini")
@TestPropertySource(properties = "portfolio.ask-rate-limit.enabled=true")
@EnableConfigurationProperties({
  AskRateLimitProperties.class,
  ExperimentRunRateLimitProperties.class,
  DatasetGenerateRateLimitProperties.class,
  RealtimeRateLimitProperties.class
})
@Import({ WebConfig.class, SecurityConfig.class, MvcTestSessionAuthConfig.class, MvcTestUserDetailsConfig.class, MockConfig.class })
class RateLimitFilterTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OpenAIService openAIService;

    @Autowired
    private RequestLogService requestLogService;

    @Test
    void rateLimiterShouldReturn429AfterAnonymousWindowExceeded() throws Exception {
        when(openAIService.getAnswer(any(Question.class))).thenReturn(new Answer("ok"));

        String body = "{\"question\":\"hi\"}";

        // Anonymous tier: 3 requests / 10s (see portfolio.ask-rate-limit.anonymous-capacity)
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/ask")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                .andExpect(status().isOk());
        }

        mockMvc.perform(post("/ask")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isTooManyRequests());
    }
}
