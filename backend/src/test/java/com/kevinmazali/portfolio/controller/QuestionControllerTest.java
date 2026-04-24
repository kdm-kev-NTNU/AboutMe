package com.kevinmazali.portfolio.controller;

import com.kevinmazali.portfolio.MockConfig;
import com.kevinmazali.portfolio.MvcTestUserDetailsConfig;
import com.kevinmazali.portfolio.controller.advice.GlobalApiExceptionHandler;
import com.kevinmazali.portfolio.config.SecurityConfig;
import com.kevinmazali.portfolio.config.WebConfig;
import com.kevinmazali.portfolio.model.Answer;
import com.kevinmazali.portfolio.model.Question;
import com.kevinmazali.portfolio.model.chat.SupportedChatModel;
import com.kevinmazali.portfolio.service.ChatModelCatalog;
import com.kevinmazali.portfolio.config.AskRateLimitProperties;
import com.kevinmazali.portfolio.config.DatasetGenerateRateLimitProperties;
import com.kevinmazali.portfolio.config.ExperimentRunRateLimitProperties;
import com.kevinmazali.portfolio.exception.AiCircuitOpenException;
import com.kevinmazali.portfolio.exception.BudgetExceededException;
import com.kevinmazali.portfolio.service.OpenAIService;
import com.kevinmazali.portfolio.service.RequestLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = QuestionController.class, properties = "portfolio.chat.default-model-id=gpt-5.4-mini")
@EnableConfigurationProperties({
  AskRateLimitProperties.class,
  ExperimentRunRateLimitProperties.class,
  DatasetGenerateRateLimitProperties.class
})
@Import({ WebConfig.class, SecurityConfig.class, MvcTestUserDetailsConfig.class, MockConfig.class, GlobalApiExceptionHandler.class })
class QuestionControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private OpenAIService openAIService;

	@Autowired
	private RequestLogService requestLogService;

	@Autowired
	private ChatModelCatalog chatModelCatalog;

	@BeforeEach
	void resetCatalogStub() {
		reset(openAIService, chatModelCatalog);
		when(chatModelCatalog.isModelConfigured(any(SupportedChatModel.class))).thenReturn(true);
	}

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
		verify(openAIService, times(1)).getAnswer(argThat(q ->
			"What is your name?".equals(q.question()) && "gpt-5.4-mini".equals(q.model())));
	}

	@Test
	void askRejectsUnknownModel() throws Exception {
		mockMvc.perform(post("/ask")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"question\":\"What is your name?\",\"model\":\"not-a-real-model\"}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.error").value("Unknown chat model."));

		verify(openAIService, never()).getAnswer(any());
	}

	@Test
	void askRejectsModelWhenProviderNotConfigured() throws Exception {
		when(chatModelCatalog.isModelConfigured(any(SupportedChatModel.class))).thenReturn(false);

		mockMvc.perform(post("/ask")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"question\":\"What is your name?\",\"model\":\"gpt-5.4-mini\"}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.error").exists());

		verify(openAIService, never()).getAnswer(any());
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

	@Test
	void askReturns429WhenBudgetExceeded() throws Exception {
		when(openAIService.getAnswer(any(Question.class))).thenThrow(new BudgetExceededException("over limit"));

		mockMvc.perform(post("/ask")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"question\":\"Hello?\"}"))
			.andExpect(status().isTooManyRequests())
			.andExpect(jsonPath("$.error").value("over limit"));
	}

	@Test
	void askReturns503WhenAiCircuitOpen() throws Exception {
		when(openAIService.getAnswer(any(Question.class))).thenThrow(new AiCircuitOpenException("paused"));

		mockMvc.perform(post("/ask")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"question\":\"Hello?\"}"))
			.andExpect(status().isServiceUnavailable())
			.andExpect(jsonPath("$.error").value("paused"));
	}
}
