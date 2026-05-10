package com.kevinmazali.portfolio.controller;

import com.kevinmazali.portfolio.MvcTestUserDetailsConfig;
import com.kevinmazali.portfolio.config.ApiErrorConfiguration;
import com.kevinmazali.portfolio.config.SecurityConfig;
import com.kevinmazali.portfolio.config.SyncProperties;
import com.kevinmazali.portfolio.model.ChunkExportResponse;
import com.kevinmazali.portfolio.model.ChunkItem;
import com.kevinmazali.portfolio.model.DefaultQuestionSuggestionResponse;
import com.kevinmazali.portfolio.model.DocumentListEntry;
import com.kevinmazali.portfolio.model.IngestionResult;
import com.kevinmazali.portfolio.model.VectorStoreSyncResult;
import com.kevinmazali.portfolio.service.DefaultQuestionSuggestionService;
import com.kevinmazali.portfolio.service.DocumentIngestionService;
import com.kevinmazali.portfolio.service.VectorStoreSyncService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
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

@WebMvcTest(controllers = DocumentPipelineController.class)
@Import({
  SecurityConfig.class,
  MvcTestUserDetailsConfig.class,
  DocumentPipelineControllerAdvice.class,
  ApiErrorConfiguration.class
})
class DocumentPipelineControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private DocumentIngestionService documentIngestionService;

	@MockitoBean
	private DefaultQuestionSuggestionService defaultQuestionSuggestionService;

	@MockitoBean
	private VectorStoreSyncService vectorStoreSyncService;

	@MockitoBean
	private SyncProperties syncProperties;

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
	void exportChunksDelegatesToService() throws Exception {
		when(documentIngestionService.exportChunks(any())).thenReturn(
			new ChunkExportResponse(
				Instant.parse("2026-01-01T00:00:00Z"),
				"vector_store",
				null,
				1L,
				List.of(new ChunkItem("c1", "f.pdf", 0, "hello", Map.of()))));

		mockMvc.perform(get("/admin/tools/documents/chunks/export"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.totalChunks").value(1))
			.andExpect(jsonPath("$.chunks[0].id").value("c1"));

		verify(documentIngestionService).exportChunks(isNull());
	}

	@Test
	@WithMockUser(username = "admin", roles = "ADMIN")
	void questionSuggestionsDelegatesToService() throws Exception {
		when(defaultQuestionSuggestionService.suggest(any()))
			.thenReturn(new DefaultQuestionSuggestionResponse(List.of("Q?"), "test-model"));

		mockMvc.perform(post("/admin/tools/documents/question-suggestions")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"source\":\"currentChunks\",\"model\":\"test-model\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.suggestions[0]").value("Q?"))
			.andExpect(jsonPath("$.modelUsed").value("test-model"));

		verify(defaultQuestionSuggestionService).suggest(any());
	}

	@Test
	@WithMockUser(username = "admin", roles = "ADMIN")
	void syncFromRemoteReturns403WhenDisabled() throws Exception {
		when(syncProperties.isEnabled()).thenReturn(false);

		mockMvc.perform(post("/admin/tools/documents/sync-from-remote").param("clean", "true"))
			.andExpect(status().isForbidden());

		verify(vectorStoreSyncService, never()).syncFromRemote(org.mockito.ArgumentMatchers.anyBoolean());
	}

	@Test
	@WithMockUser(username = "admin", roles = "ADMIN")
	void syncFromRemoteReturns400WhenUrlMissing() throws Exception {
		when(syncProperties.isEnabled()).thenReturn(true);
		when(syncProperties.getSourceUrl()).thenReturn("  ");
		when(syncProperties.getSourceUsername()).thenReturn("u");

		mockMvc.perform(post("/admin/tools/documents/sync-from-remote"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.error").value(containsString("Sync source URL")));

		verify(vectorStoreSyncService, never()).syncFromRemote(org.mockito.ArgumentMatchers.anyBoolean());
	}

	@Test
	@WithMockUser(username = "admin", roles = "ADMIN")
	void syncFromRemoteReturns400WhenUsernameMissing() throws Exception {
		when(syncProperties.isEnabled()).thenReturn(true);
		when(syncProperties.getSourceUrl()).thenReturn("jdbc:postgresql://h:5432/db");
		when(syncProperties.getSourceUsername()).thenReturn("  ");

		mockMvc.perform(post("/admin/tools/documents/sync-from-remote"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.error").value(containsString("username")));

		verify(vectorStoreSyncService, never()).syncFromRemote(org.mockito.ArgumentMatchers.anyBoolean());
	}

	@Test
	@WithMockUser(username = "admin", roles = "ADMIN")
	void syncFromRemoteDelegatesToService() throws Exception {
		when(syncProperties.isEnabled()).thenReturn(true);
		when(syncProperties.getSourceUrl()).thenReturn("jdbc:postgresql://h:5432/db");
		when(syncProperties.getSourceUsername()).thenReturn("u");
		when(vectorStoreSyncService.syncFromRemote(true))
			.thenReturn(new VectorStoreSyncResult(3L, 12L, "h:5432/db", true));

		mockMvc.perform(post("/admin/tools/documents/sync-from-remote").param("clean", "true"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.rowsSynced").value(3))
			.andExpect(jsonPath("$.truncatedLocalFirst").value(true));

		verify(vectorStoreSyncService).syncFromRemote(true);
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
