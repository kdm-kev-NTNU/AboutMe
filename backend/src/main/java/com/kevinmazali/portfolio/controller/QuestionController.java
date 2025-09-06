package com.kevinmazali.portfolio.controller;




import com.kevinmazali.portfolio.model.Answer;
import com.kevinmazali.portfolio.model.Question;
import com.kevinmazali.portfolio.service.OpenAIService;
import com.kevinmazali.portfolio.service.RequestLogService;
import lombok.RequiredArgsConstructor;
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

    @PostMapping("/ask")
    public Answer askQuestion(@RequestBody Question question) {
        requestLogService.save("/ask", "POST", question.question());
        Answer answer = openAIService.getAnswer(question);
        // Logg også svaret for historikk
        requestLogService.save("/ask:response", "POST", answer.answer());
        return answer;
    }

}
