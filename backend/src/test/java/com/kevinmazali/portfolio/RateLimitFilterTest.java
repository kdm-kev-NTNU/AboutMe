package com.kevinmazali.portfolio;

import com.kevinmazali.portfolio.config.WebConfig;
import com.kevinmazali.portfolio.controller.QuestionController;
import com.kevinmazali.portfolio.model.Answer;
import com.kevinmazali.portfolio.model.Question;
import com.kevinmazali.portfolio.service.OpenAIService;
import com.kevinmazali.portfolio.service.RequestLogService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.mockito.Mockito;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = QuestionController.class)
@Import({WebConfig.class, MockConfig.class})
class RateLimitFilterTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OpenAIService openAIService;

    @Autowired
    private RequestLogService requestLogService;

    @Test
    void rateLimiterShouldReturn429AfterFiveRequestsInWindow() throws Exception {
        when(openAIService.getAnswer(any(Question.class))).thenReturn(new Answer("ok"));

        String body = "{\"question\":\"hi\"}";

        // First five requests should pass (HTTP 200)
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/ask")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                .andExpect(status().isOk());
        }

        // Sixth should be rate limited (HTTP 429)
        mockMvc.perform(post("/ask")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isTooManyRequests());
    }
}

    // Test beans are provided by MockConfig


