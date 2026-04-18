package com.kevinmazali.portfolio.controller;

import com.kevinmazali.portfolio.MockConfig;
import com.kevinmazali.portfolio.MvcTestUserDetailsConfig;
import com.kevinmazali.portfolio.config.SecurityConfig;
import com.kevinmazali.portfolio.config.WebConfig;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = QuestionController.class)
@Import({ WebConfig.class, SecurityConfig.class, MvcTestUserDetailsConfig.class, MockConfig.class })
class QuestionControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private OpenAIService openAIService;

	@Autowired
	private RequestLogService requestLogService;

	@Test
	void askRejectsEmptyQuestion() throws Exception {
		mockMvc.perform(post("/ask")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"question\":\"\"}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.error").value("Question cannot be empty"));

		verify(openAIService, never()).getAnswer(any());
	}

	@Test
	void askRejectsInvalidQuestionFormat() throws Exception {
		mockMvc.perform(post("/ask")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"question\":\"<script>x</script>\"}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.error").value("Invalid question format"));

		verify(openAIService, never()).getAnswer(any());
	}

	@Test
	void askReturnsAnswerAndLogs() throws Exception {
		when(openAIService.getAnswer(any(Question.class))).thenReturn(new Answer("ok"));

		mockMvc.perform(post("/ask")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"question\":\"What is your name?\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.answer").value("ok"));

		verify(requestLogService).save(eq("/ask"), eq("POST"), eq("What is your name?"), isNull());
		verify(requestLogService).save(eq("/ask:response"), eq("POST"), eq("ok"), isNull());
		verify(openAIService, times(1)).getAnswer(any(Question.class));
	}

	@Test
	void askReturns503WhenServiceFails() throws Exception {
		when(openAIService.getAnswer(any(Question.class))).thenThrow(new RuntimeException("down"));

		mockMvc.perform(post("/ask")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"question\":\"Hello?\"}"))
			.andExpect(status().isServiceUnavailable())
			.andExpect(jsonPath("$.error").exists());
	}
}
