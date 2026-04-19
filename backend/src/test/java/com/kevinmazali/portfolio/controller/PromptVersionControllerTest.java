package com.kevinmazali.portfolio.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kevinmazali.portfolio.MvcTestUserDetailsConfig;
import com.kevinmazali.portfolio.config.SecurityConfig;
import com.kevinmazali.portfolio.model.prompt.ActivateRequest;
import com.kevinmazali.portfolio.model.prompt.CreateVersionRequest;
import com.kevinmazali.portfolio.model.prompt.DeleteVariantRequest;
import com.kevinmazali.portfolio.model.prompt.PromptDiffResponse;
import com.kevinmazali.portfolio.model.prompt.PromptNameEntry;
import com.kevinmazali.portfolio.model.prompt.PromptVersionResponse;
import com.kevinmazali.portfolio.service.PromptVersionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PromptVersionController.class)
@Import({ SecurityConfig.class, MvcTestUserDetailsConfig.class })
class PromptVersionControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockBean
	private PromptVersionService promptVersionService;

	@Test
	void namesRequiresAdmin() throws Exception {
		mockMvc.perform(get("/admin/tools/prompt-versions/names"))
			.andExpect(status().isUnauthorized());
	}

	@Test
	@WithMockUser(username = "admin", roles = "ADMIN")
	void namesReturnsJsonArray() throws Exception {
		when(promptVersionService.listActiveNames()).thenReturn(List.of(
			new PromptNameEntry("rag_portfolio", null, "openai", 1, 10L, "2020-01-01T00:00:00Z")
		));

		mockMvc.perform(get("/admin/tools/prompt-versions/names"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].name").value("rag_portfolio"))
			.andExpect(jsonPath("$[0].activeVersion").value(1))
			.andExpect(jsonPath("$[0].activeId").value(10));
	}

	@Test
	@WithMockUser(username = "admin", roles = "ADMIN")
	void historyPassesQueryParamsToService() throws Exception {
		when(promptVersionService.listHistory("rag_portfolio", "en", "openai")).thenReturn(List.of());

		mockMvc.perform(get("/admin/tools/prompt-versions/history")
				.param("name", "rag_portfolio")
				.param("language", "en")
				.param("provider", "openai"))
			.andExpect(status().isOk());

		verify(promptVersionService).listHistory("rag_portfolio", "en", "openai");
	}

	@Test
	@WithMockUser(username = "admin", roles = "ADMIN")
	void createDelegatesToService() throws Exception {
		var body = new CreateVersionRequest("my_prompt", "content here", null, null, "desc");
		when(promptVersionService.createVersion(eq("my_prompt"), eq("content here"), eq(null), eq(null), eq("desc")))
			.thenReturn(new PromptVersionResponse(
				5L, "my_prompt", 2, null, null, "content here", "abc", false, "desc", null));

		mockMvc.perform(post("/admin/tools/prompt-versions/create")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(body)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(5))
			.andExpect(jsonPath("$.version").value(2))
			.andExpect(jsonPath("$.content").value("content here"));
	}

	@Test
	@WithMockUser(username = "admin", roles = "ADMIN")
	void createRejectsBlankName() throws Exception {
		var body = new CreateVersionRequest("   ", "c", null, null, null);

		mockMvc.perform(post("/admin/tools/prompt-versions/create")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(body)))
			.andExpect(status().isBadRequest());
	}

	@Test
	@WithMockUser(username = "admin", roles = "ADMIN")
	void activateReturnsOkWhenServiceSucceeds() throws Exception {
		when(promptVersionService.activateVersion(99L))
			.thenReturn(new PromptVersionResponse(
				99L, "n", 1, null, null, "x", "h", true, null, null));

		mockMvc.perform(post("/admin/tools/prompt-versions/activate")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(new ActivateRequest(99L))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(99));
	}

	@Test
	@WithMockUser(username = "admin", roles = "ADMIN")
	void activateReturnsBadRequestWhenVersionMissing() throws Exception {
		when(promptVersionService.activateVersion(1L))
			.thenThrow(new IllegalArgumentException("Prompt version id=1 not found"));

		mockMvc.perform(post("/admin/tools/prompt-versions/activate")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(new ActivateRequest(1L))))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.error").value("Prompt version id=1 not found"));
	}

	@Test
	@WithMockUser(username = "admin", roles = "ADMIN")
	void activateRejectsInvalidId() throws Exception {
		mockMvc.perform(post("/admin/tools/prompt-versions/activate")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"id\":0}"))
			.andExpect(status().isBadRequest());
	}

	@Test
	@WithMockUser(username = "admin", roles = "ADMIN")
	void seedReturnsCounts() throws Exception {
		when(promptVersionService.seedFromClasspath())
			.thenReturn(Map.of("created", 1, "skipped", 1, "total_fallbacks", 2));

		mockMvc.perform(post("/admin/tools/prompt-versions/seed"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.created").value(1))
			.andExpect(jsonPath("$.skipped").value(1));
	}

	@Test
	@WithMockUser(username = "admin", roles = "ADMIN")
	void deleteVariantDelegatesAndInvalidatesCache() throws Exception {
		when(promptVersionService.deleteVariant("p", null, "openai")).thenReturn(3);

		var body = new DeleteVariantRequest("  p  ", null, "openai");
		mockMvc.perform(delete("/admin/tools/prompt-versions/variant")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(body)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.deleted").value(3));

		verify(promptVersionService).deleteVariant("p", null, "openai");
		verify(promptVersionService).invalidateCache("p");
	}

	@Test
	@WithMockUser(username = "admin", roles = "ADMIN")
	void diffReturnsPayload() throws Exception {
		when(promptVersionService.diff("rag_portfolio", null, "openai"))
			.thenReturn(new PromptDiffResponse(
				"rag_portfolio", null, "openai", true, true, false, "a", "b"));

		mockMvc.perform(get("/admin/tools/prompt-versions/diff")
				.param("name", "rag_portfolio")
				.param("provider", "openai"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.hasDbActive").value(true))
			.andExpect(jsonPath("$.isEqual").value(false))
			.andExpect(jsonPath("$.dbContent").value("a"))
			.andExpect(jsonPath("$.fallbackContent").value("b"));
	}
}
