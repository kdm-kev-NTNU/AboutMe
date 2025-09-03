package com.kevinmazali.portfolio.service;

import demo.ml.rag.model.Answer;
import demo.ml.rag.model.Question;

/**
 * Created by jt, Spring Framework Guru.
 */
public interface OpenAIService {

    Answer getAnswer(Question question);
}
