package com.kevinmazali.portfolio.controller;


import com.kevinmazali.portfolio.MvcTestSessionAuthConfig;import com.kevinmazali.portfolio.MvcTestUserDetailsConfig;
import com.kevinmazali.portfolio.config.AiLimitsProperties;
import com.kevinmazali.portfolio.config.AskRateLimitProperties;
import com.kevinmazali.portfolio.config.DatasetGenerateRateLimitProperties;
import com.kevinmazali.portfolio.config.ExperimentRunRateLimitProperties;
import com.kevinmazali.portfolio.config.RealtimeRateLimitProperties;
import com.kevinmazali.portfolio.config.SecurityConfig;
import com.kevinmazali.portfolio.config.WebConfig;
import com.kevinmazali.portfolio.model.FeedbackSubmission;
import com.kevinmazali.portfolio.repository.FeedbackRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = FeedbackController.class)
@EnableConfigurationProperties({
    AskRateLimitProperties.class,
    ExperimentRunRateLimitProperties.class,
    DatasetGenerateRateLimitProperties.class,
    RealtimeRateLimitProperties.class,
    AiLimitsProperties.class
})
@Import({ WebConfig.class, SecurityConfig.class, MvcTestSessionAuthConfig.class, MvcTestUserDetailsConfig.class })
class FeedbackControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FeedbackRepository feedbackRepository;

    @Test
    void submitFeedbackReturns204ForValidMessage() throws Exception {
        mockMvc.perform(post("/feedback")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"message\":\"Great site!\"}"))
            .andExpect(status().isNoContent());

        verify(feedbackRepository).save(any(FeedbackSubmission.class));
    }

    @Test
    void submitFeedbackRejectsEmptyMessage() throws Exception {
        mockMvc.perform(post("/feedback")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"message\":\"\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("Invalid or empty feedback message"));

        verify(feedbackRepository, never()).save(any());
    }

    @Test
    void submitFeedbackRejectsNullMessage() throws Exception {
        mockMvc.perform(post("/feedback")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"replyEmail\":\"test@example.com\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("Invalid or empty feedback message"));

        verify(feedbackRepository, never()).save(any());
    }

    @Test
    void submitFeedbackRejectsScriptInjection() throws Exception {
        mockMvc.perform(post("/feedback")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"message\":\"<script>alert(1)</script>\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("Invalid or empty feedback message"));

        verify(feedbackRepository, never()).save(any());
    }

    @Test
    void submitFeedbackRejectsTooLongMessage() throws Exception {
        String longMessage = "a".repeat(4001);
        mockMvc.perform(post("/feedback")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"message\":\"" + longMessage + "\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("Invalid or empty feedback message"));

        verify(feedbackRepository, never()).save(any());
    }
}
