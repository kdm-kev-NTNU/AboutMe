package com.kevinmazali.portfolio.controller;




import com.kevinmazali.portfolio.model.Answer;
import com.kevinmazali.portfolio.model.Question;
import com.kevinmazali.portfolio.service.OpenAIService;
import com.kevinmazali.portfolio.service.RequestLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by jt, Spring Framework Guru.
 */
@RequiredArgsConstructor
@RestController
public class QuestionController {

    private final OpenAIService openAIService;
    private final RequestLogService requestLogService;
    private static final int MAX_PROMPT_CHARS = 3000;

    @PostMapping("/ask")
    public Object askQuestion(@RequestBody Question question) {
        if (question.question() != null && question.question().length() > MAX_PROMPT_CHARS) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", "Prompt too long"));
        }
        requestLogService.save("/ask", "POST", question.question());
        Answer answer = openAIService.getAnswer(question);
        // Logg også svaret for historikk
        requestLogService.save("/ask:response", "POST", answer.answer());
        return answer;
    }

}
