package com.kevinmazali.portfolio.service;


import com.kevinmazali.portfolio.model.Answer;
import com.kevinmazali.portfolio.model.Question;

/**
 * Created by jt, Spring Framework Guru.
 */
public interface OpenAIService {

    Answer getAnswer(Question question);

}
