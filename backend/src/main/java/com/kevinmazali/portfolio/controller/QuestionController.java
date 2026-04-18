package com.kevinmazali.portfolio.controller;

import com.kevinmazali.portfolio.model.Answer;
import com.kevinmazali.portfolio.model.ApiError;
import com.kevinmazali.portfolio.model.Question;
import com.kevinmazali.portfolio.service.OpenAIService;
import com.kevinmazali.portfolio.service.RequestLogService;
import com.kevinmazali.portfolio.util.InputValidator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
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
@Tag(name = "Chat", description = "RAG-backed question answering")
public class QuestionController {

    private final OpenAIService openAIService;
    private final RequestLogService requestLogService;

    @Operation(summary = "Ask a question", description = "Runs RAG over indexed documents and returns an answer. Rate limited (5 requests / 10s per IP).")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Generated answer",
            content = @Content(schema = @Schema(implementation = Answer.class))),
        @ApiResponse(responseCode = "400", description = "Empty, invalid, or too long question",
            content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "503", description = "OpenAI or Chroma unavailable",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping("/ask")
    public ResponseEntity<?> askQuestion(@RequestBody Question question) {
        if (question.question() == null || question.question().isBlank()) {
            return ResponseEntity.badRequest().body(new ApiError("Question cannot be empty"));
        }

        if (!InputValidator.isValidQuestion(question.question())) {
            return ResponseEntity.badRequest().body(new ApiError("Invalid question format"));
        }

        String sanitizedQuestion = InputValidator.sanitizeString(question.question());

        requestLogService.save("/ask", "POST", sanitizedQuestion, null);

        Question sanitizedQuestionObj = new Question(sanitizedQuestion);
        try {
            Answer answer = openAIService.getAnswer(sanitizedQuestionObj);
            requestLogService.save("/ask:response", "POST", answer.answer(), null);
            return ResponseEntity.ok(answer);
        } catch (Exception e) {
            log.warn("/ask failed (e.g. ChromaDB or OpenAI unavailable): {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ApiError("The AI service is temporarily unavailable. Please try again later."));
        }
    }
}
