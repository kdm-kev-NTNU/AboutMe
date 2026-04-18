package com.kevinmazali.portfolio.controller;

import com.kevinmazali.portfolio.MvcTestUserDetailsConfig;
import com.kevinmazali.portfolio.config.SecurityConfig;
import com.kevinmazali.portfolio.model.DocumentListEntry;
import com.kevinmazali.portfolio.model.IngestionResult;
import com.kevinmazali.portfolio.service.DocumentIngestionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = DocumentPipelineController.class)
@Import({ SecurityConfig.class, MvcTestUserDetailsConfig.class })
class DocumentPipelineControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private DocumentIngestionService documentIngestionService;

	@Test
	@WithMockUser(username = "admin", roles = "ADMIN")
	void uploadRejectsUnsupportedExtension() throws Exception {
		mockMvc.perform(multipart("/admin/tools/documents/upload")
				.file(new MockMultipartFile("file", "x.exe", "application/octet-stream", new byte[] { 1 })))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").value("Unsupported file type: exe"));
	}

	@Test
	@WithMockUser(username = "admin", roles = "ADMIN")
	void uploadRejectsMissingFilename() throws Exception {
		mockMvc.perform(multipart("/admin/tools/documents/upload")
				.file(new MockMultipartFile("file", null, "text/plain", "x".getBytes(StandardCharsets.UTF_8))))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").value("Missing filename"));
	}

	@Test
	@WithMockUser(username = "admin", roles = "ADMIN")
	void uploadDelegatesToService() throws Exception {
		when(documentIngestionService.ingestMultipart(any(), any(), anyBoolean()))
			.thenReturn(new IngestionResult("doc-1", "a.pdf", 2, false, "ok"));

		mockMvc.perform(multipart("/admin/tools/documents/upload")
				.file(new MockMultipartFile("file", "a.pdf", "application/pdf", "data".getBytes(StandardCharsets.UTF_8))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.documentId").value("doc-1"))
			.andExpect(jsonPath("$.chunksIngested").value(2));

		verify(documentIngestionService).ingestMultipart(any(), eq(null), eq(false));
	}

	@Test
	@WithMockUser(username = "admin", roles = "ADMIN")
	void listReturnsDocuments() throws Exception {
		when(documentIngestionService.listDocuments()).thenReturn(List.of(
			new DocumentListEntry("id1", "a.md", 3, null)
		));

		mockMvc.perform(get("/admin/tools/documents"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].documentId").value("id1"));
	}

	@Test
	@WithMockUser(username = "admin", roles = "ADMIN")
	void deleteCallsService() throws Exception {
		mockMvc.perform(delete("/admin/tools/documents/doc-99"))
			.andExpect(status().isNoContent());

		verify(documentIngestionService).deleteByDocumentId("doc-99");
	}

	@Test
	@WithMockUser(username = "admin", roles = "ADMIN")
	void deleteWithBlankIdReturnsBadRequest() throws Exception {
		mockMvc.perform(delete("/admin/tools/documents/{id}", "   "))
			.andExpect(status().isBadRequest());

		verify(documentIngestionService, never()).deleteByDocumentId(anyString());
	}
}
