package com.kevinmazali.portfolio.service;


import com.kevinmazali.portfolio.model.Answer;
import com.kevinmazali.portfolio.model.Question;

/**
 * Service abstraction for generating answers to user questions.
 */
public interface OpenAIService {

    /**
     * Generates an answer for the provided question.
     *
     * @param question the user question
     * @return the generated answer
     */
    Answer getAnswer(Question question);

}
