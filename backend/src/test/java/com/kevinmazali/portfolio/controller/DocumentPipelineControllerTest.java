package com.kevinmazali.portfolio.controller;

import com.kevinmazali.portfolio.MvcTestUserDetailsConfig;
import com.kevinmazali.portfolio.config.SecurityConfig;
import com.kevinmazali.portfolio.exception.ChromaFeatureDisabledException;
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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = DocumentPipelineController.class, properties = {
		"spring.ai.vectorstore.chroma.client.host=http://localhost",
		"spring.ai.vectorstore.chroma.client.port=8100"
})
@Import({ SecurityConfig.class, MvcTestUserDetailsConfig.class, DocumentPipelineControllerAdvice.class })
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
	void listReturns503WhenChromaCollectionMissing() throws Exception {
		when(documentIngestionService.listDocuments())
			.thenThrow(new IllegalStateException("Chroma collection not found: portfolio-documents"));

		mockMvc.perform(get("/admin/tools/documents"))
			.andExpect(status().isServiceUnavailable())
			.andExpect(jsonPath("$.error").value("Chroma collection not found: portfolio-documents"));
	}

	@Test
	@WithMockUser(username = "admin", roles = "ADMIN")
	void listReturns503OnRestClientFailure() throws Exception {
		when(documentIngestionService.listDocuments())
			.thenThrow(new ResourceAccessException("Connection refused"));

		mockMvc.perform(get("/admin/tools/documents"))
			.andExpect(status().isServiceUnavailable())
			.andExpect(jsonPath("$.error").value(containsString("Connection refused")))
			.andExpect(jsonPath("$.error").value(containsString("http://localhost:8100")));
	}

	@Test
	@WithMockUser(username = "admin", roles = "ADMIN")
	void listReturns501WhenChromaFeatureDisabled() throws Exception {
		when(documentIngestionService.listDocuments())
			.thenThrow(new ChromaFeatureDisabledException("Chroma is disabled for this deployment."));

		mockMvc.perform(get("/admin/tools/documents"))
			.andExpect(status().isNotImplemented())
			.andExpect(jsonPath("$.error").value("Chroma is disabled for this deployment."));
	}

	@Test
	@WithMockUser(username = "admin", roles = "ADMIN")
	void listReturns503OnGenericRestClientException() throws Exception {
		when(documentIngestionService.listDocuments())
			.thenThrow(new RestClientException("Bad gateway from Chroma proxy"));

		mockMvc.perform(get("/admin/tools/documents"))
			.andExpect(status().isServiceUnavailable())
			.andExpect(jsonPath("$.error").value(containsString("Bad gateway from Chroma proxy")))
			.andExpect(jsonPath("$.error").value(containsString("Hint:")));
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

	@Test
	@WithMockUser(username = "admin", roles = "ADMIN")
	void uploadBatchReturnsPerFileResults() throws Exception {
		when(documentIngestionService.ingestMultipart(any(), eq(null), eq(false)))
			.thenReturn(new IngestionResult("a", "a.pdf", 1, false, "OK"))
			.thenReturn(new IngestionResult("b", "b.pdf", 2, false, "OK"));

		mockMvc.perform(multipart("/admin/tools/documents/upload/batch")
				.file(new MockMultipartFile("files", "a.pdf", "application/pdf", "x".getBytes(StandardCharsets.UTF_8)))
				.file(new MockMultipartFile("files", "b.pdf", "application/pdf", "y".getBytes(StandardCharsets.UTF_8))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(2)))
			.andExpect(jsonPath("$[0].documentId").value("a"))
			.andExpect(jsonPath("$[1].chunksIngested").value(2));

		verify(documentIngestionService, times(2)).ingestMultipart(any(), eq(null), eq(false));
	}

	@Test
	@WithMockUser(username = "admin", roles = "ADMIN")
	void uploadBatchSkipsInvalidExtensionPerFile() throws Exception {
		when(documentIngestionService.ingestMultipart(any(), eq(null), eq(false)))
			.thenReturn(new IngestionResult("a", "a.pdf", 1, false, "OK"));

		mockMvc.perform(multipart("/admin/tools/documents/upload/batch")
				.file(new MockMultipartFile("files", "a.pdf", "application/pdf", "x".getBytes(StandardCharsets.UTF_8)))
				.file(new MockMultipartFile("files", "bad.exe", "application/octet-stream", new byte[] { 1 })))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(2)))
			.andExpect(jsonPath("$[0].documentId").value("a"))
			.andExpect(jsonPath("$[1].message").value(containsString("Unsupported")));

		verify(documentIngestionService).ingestMultipart(any(), eq(null), eq(false));
	}

	@Test
	@WithMockUser(username = "admin", roles = "ADMIN")
	void uploadBatchNoFilesReturns400() throws Exception {
		mockMvc.perform(multipart("/admin/tools/documents/upload/batch"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$[0].message").value("No files provided"));
	}

	@Test
	@WithMockUser(username = "admin", roles = "ADMIN")
	void ingestByPathDelegatesToService() throws Exception {
		when(documentIngestionService.ingestFromPaths(anyList(), eq(false)))
			.thenReturn(List.of(new IngestionResult("h", "doc.pdf", 3, false, "OK")));

		mockMvc.perform(post("/admin/tools/documents/ingest-by-path")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"paths\":[\"doc.pdf\"],\"force\":false}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].chunksIngested").value(3))
			.andExpect(jsonPath("$[0].filename").value("doc.pdf"));

		verify(documentIngestionService).ingestFromPaths(List.of("doc.pdf"), false);
	}

	@Test
	@WithMockUser(username = "admin", roles = "ADMIN")
	void ingestByPathEmptyPathsReturns400() throws Exception {
		mockMvc.perform(post("/admin/tools/documents/ingest-by-path")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"paths\":[]}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$[0].message").value("No paths provided"));
	}

	@Test
	@WithMockUser(username = "admin", roles = "ADMIN")
	void reseedClasspathDelegatesToService() throws Exception {
		when(documentIngestionService.reseedClasspathDocuments())
			.thenReturn(List.of(new IngestionResult("x", "seed.md", 1, false, "OK")));

		mockMvc.perform(post("/admin/tools/documents/reseed"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(1)))
			.andExpect(jsonPath("$[0].filename").value("seed.md"));

		verify(documentIngestionService).reseedClasspathDocuments();
	}
}
