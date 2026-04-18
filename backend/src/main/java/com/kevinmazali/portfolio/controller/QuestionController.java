package com.kevinmazali.portfolio.controller;

import com.kevinmazali.portfolio.model.Answer;
import com.kevinmazali.portfolio.model.Question;
import com.kevinmazali.portfolio.service.OpenAIService;
import com.kevinmazali.portfolio.service.RequestLogService;
import com.kevinmazali.portfolio.util.InputValidator;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller exposing the question answering endpoint.
 * Validates input length, logs both request and response, and delegates to the AI service.
 * Rate limiting and CORS are configured in {@link com.kevinmazali.portfolio.config.WebConfig}.
 */
@Slf4j
@RequiredArgsConstructor
@RestController
public class QuestionController {

    private final OpenAIService openAIService;
    private final RequestLogService requestLogService;

    /**
     * Answers a user question using the RAG-enabled AI service.
     *
     * <p>Guards against overly long prompts, persists a request/response audit trail,
     * then returns the generated answer.</p>
     *
     * @param question input containing the natural-language question
     * @return {@link Answer} on success, 400 for invalid input, or 503 when the AI / vector store is unavailable
     */
    @PostMapping("/ask")
    public Object askQuestion(
        @RequestBody Question question
    ) {
        // Validate question
        if (question.question() == null || question.question().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Question cannot be empty"));
        }
        
        if (!InputValidator.isValidQuestion(question.question())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid question format"));
        }
        
        // Sanitize inputs
        String sanitizedQuestion = InputValidator.sanitizeString(question.question());
        
        requestLogService.save("/ask", "POST", sanitizedQuestion, null);
        
        // Create sanitized question object
        Question sanitizedQuestionObj = new Question(sanitizedQuestion);
        try {
            Answer answer = openAIService.getAnswer(sanitizedQuestionObj);
            requestLogService.save("/ask:response", "POST", answer.answer(), null);
            return answer;
        }
        catch (Exception e) {
            log.warn("/ask failed (e.g. ChromaDB or OpenAI unavailable): {}", e.getMessage());
            return ResponseEntity.status(503)
                .body(Map.of("error", "The AI service is temporarily unavailable. Please try again later."));
        }
    }

}
