package com.kevinmazali.portfolio.controller;

import com.kevinmazali.portfolio.model.Answer;
import com.kevinmazali.portfolio.model.Question;
import com.kevinmazali.portfolio.service.OpenAIService;
import com.kevinmazali.portfolio.service.RequestLogService;
import com.kevinmazali.portfolio.util.InputValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller exposing the question answering endpoint.
 * Validates input length, logs both request and response, and delegates to the AI service.
 * Rate limiting and CORS are configured in {@link com.kevinmazali.portfolio.config.WebConfig}.
 */
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
     * @return {@link Answer} on success, or a 400 response with an error when the prompt is too long
     */
    @PostMapping("/ask")
    public Object askQuestion(
        @RequestBody Question question,
        @RequestHeader(name = "X-Chat-Id", required = false) String chatId
    ) {
        // Get chatId from request attribute if not in header
        if (chatId == null || chatId.isBlank()) {
            var requestAttributes = org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
            if (requestAttributes != null) {
                var request = (jakarta.servlet.http.HttpServletRequest) requestAttributes
                    .resolveReference(org.springframework.web.context.request.RequestAttributes.REFERENCE_REQUEST);
                if (request != null) {
                    Object attr = request.getAttribute("chatId");
                    if (attr instanceof String s && !s.isBlank()) {
                        chatId = s;
                    }
                }
            }
        }
        
        // Validate chatId
        if (!InputValidator.isValidChatId(chatId)) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", "Invalid chat ID"));
        }
        
        // Validate question
        if (question.question() == null || question.question().isBlank()) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", "Question cannot be empty"));
        }
        
        if (!InputValidator.isValidQuestion(question.question())) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", "Invalid question format"));
        }
        
        // Sanitize inputs
        String sanitizedQuestion = InputValidator.sanitizeString(question.question());
        String sanitizedChatId = InputValidator.sanitizeString(chatId);
        
        requestLogService.save("/ask", "POST", sanitizedQuestion, sanitizedChatId);
        
        // Create sanitized question object
        Question sanitizedQuestionObj = new Question(sanitizedQuestion);
        Answer answer = openAIService.getAnswer(sanitizedQuestionObj);
        
        // Also log the answer for history
        requestLogService.save("/ask:response", "POST", answer.answer(), sanitizedChatId);
        return answer;
    }

}
