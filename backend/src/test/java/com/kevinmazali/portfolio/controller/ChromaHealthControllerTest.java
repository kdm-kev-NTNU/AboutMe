package com.kevinmazali.portfolio.controller;

import com.kevinmazali.portfolio.MvcTestUserDetailsConfig;
import com.kevinmazali.portfolio.config.PortfolioChromaProperties;
import com.kevinmazali.portfolio.config.SecurityConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chroma.vectorstore.ChromaApi;
import org.springframework.ai.vectorstore.chroma.autoconfigure.ChromaVectorStoreProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ChromaHealthController.class)
@Import({ SecurityConfig.class, MvcTestUserDetailsConfig.class })
class ChromaHealthControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private ChromaApi chromaApi;

	@MockBean
	private ChromaVectorStoreProperties chromaStoreProperties;

	@MockBean
	private PortfolioChromaProperties portfolioChromaProperties;

	@BeforeEach
	void wireChromaApi() {
		when(portfolioChromaProperties.isEnabled()).thenReturn(true);
	}

	@Test
	void chromaReturnsOkWhenCollectionExists() throws Exception {
		when(chromaStoreProperties.getTenantName()).thenReturn("t");
		when(chromaStoreProperties.getDatabaseName()).thenReturn("d");
		when(chromaStoreProperties.getCollectionName()).thenReturn("col");

		ChromaApi.Collection col = mock(ChromaApi.Collection.class);
		when(col.id()).thenReturn("cid-1");
		when(chromaApi.getCollection("t", "d", "col")).thenReturn(col);
		when(chromaApi.countEmbeddings("t", "d", "cid-1")).thenReturn(7L);

		mockMvc.perform(get("/health/chroma"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.healthy").value(true))
			.andExpect(jsonPath("$.collectionName").value("col"))
			.andExpect(jsonPath("$.embeddingCount").value(7));
	}

	@Test
	void chromaReturns503WhenCollectionMissing() throws Exception {
		when(chromaStoreProperties.getTenantName()).thenReturn("t");
		when(chromaStoreProperties.getDatabaseName()).thenReturn("d");
		when(chromaStoreProperties.getCollectionName()).thenReturn("col");
		when(chromaApi.getCollection(anyString(), anyString(), anyString())).thenReturn(null);

		mockMvc.perform(get("/health/chroma"))
			.andExpect(status().isServiceUnavailable())
			.andExpect(jsonPath("$.healthy").value(false))
			.andExpect(jsonPath("$.message").exists());
	}

	@Test
	void chromaReturns503OnException() throws Exception {
		when(chromaStoreProperties.getTenantName()).thenReturn("t");
		when(chromaStoreProperties.getDatabaseName()).thenReturn("d");
		when(chromaStoreProperties.getCollectionName()).thenReturn("col");
		when(chromaApi.getCollection(any(), any(), any())).thenThrow(new RuntimeException("boom"));

		mockMvc.perform(get("/health/chroma"))
			.andExpect(status().isServiceUnavailable())
			.andExpect(jsonPath("$.healthy").value(false));
	}

	@Test
	void chromaTreatsNullCountAsZero() throws Exception {
		when(chromaStoreProperties.getTenantName()).thenReturn("t");
		when(chromaStoreProperties.getDatabaseName()).thenReturn("d");
		when(chromaStoreProperties.getCollectionName()).thenReturn("col");
		ChromaApi.Collection col = mock(ChromaApi.Collection.class);
		when(col.id()).thenReturn("cid");
		when(chromaApi.getCollection("t", "d", "col")).thenReturn(col);
		when(chromaApi.countEmbeddings("t", "d", "cid")).thenReturn(null);

		mockMvc.perform(get("/health/chroma"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.embeddingCount").value(0));
	}

	@Test
	void chromaReturns501WhenFeatureDisabled() throws Exception {
		when(portfolioChromaProperties.isEnabled()).thenReturn(false);
		when(chromaStoreProperties.getCollectionName()).thenReturn("col");

		mockMvc.perform(get("/health/chroma"))
			.andExpect(status().isNotImplemented())
			.andExpect(jsonPath("$.healthy").value(false))
			.andExpect(jsonPath("$.message").value(containsString("disabled")));
	}
}
