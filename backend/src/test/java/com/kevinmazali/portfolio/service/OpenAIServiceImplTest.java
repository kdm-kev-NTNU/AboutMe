package com.kevinmazali.portfolio.service;

import com.kevinmazali.portfolio.model.Answer;
import com.kevinmazali.portfolio.model.Question;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OpenAIServiceImplTest {

	@Mock
	private ChatModel chatModel;

	@Mock
	private VectorStore vectorStore;

	private OpenAIServiceImpl openAIServiceImpl;

	@BeforeEach
	void setUp() {
		openAIServiceImpl = new OpenAIServiceImpl(chatModel, vectorStore);
	}

	@Test
	void getAnswerReturnsChatModelOutput() {
		when(vectorStore.similaritySearch(any(SearchRequest.class)))
			.thenReturn(List.of(new Document("ctx", new HashMap<>())));

		ChatResponse expand = mock(ChatResponse.class, RETURNS_DEEP_STUBS);
		when(expand.getResult().getOutput().getText()).thenReturn("{\"en\":\"Hello\",\"no\":\"Hei\"}");

		ChatResponse rag = mock(ChatResponse.class, RETURNS_DEEP_STUBS);
		when(rag.getResult().getOutput().getText()).thenReturn("final answer text");

		when(chatModel.call(any(Prompt.class))).thenReturn(expand, rag);

		Answer answer = openAIServiceImpl.getAnswer(new Question("Hi"));

		assertEquals("final answer text", answer.answer());
	}
}
