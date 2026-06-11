package com.kevinmazali.portfolio.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Structural tests that verify the RAG prompt templates enforce language-matching behaviour.
 * These tests read the actual .st / template files shipped in the classpath and assert that
 * the critical language rules are present, preventing accidental removal during refactoring.
 */
class RagPromptLanguageRulesTest {

  private static final List<String> TEMPLATE_PATHS = List.of(
      "templates/rag-prompt-template-openai.st",
      "templates/rag-prompt-template-anthropic.st"
  );

  private String loadTemplate(String path) throws IOException {
    ClassPathResource res = new ClassPathResource(path);
    assertTrue(res.exists(), "Template resource must exist: " + path);
    return StreamUtils.copyToString(res.getInputStream(), StandardCharsets.UTF_8);
  }

  // --- Core language-matching rule ---

  @ParameterizedTest
  @ValueSource(strings = {
      "templates/rag-prompt-template-openai.st",
      "templates/rag-prompt-template-anthropic.st"
  })
  void templateContainsAnswerInSameLanguageRule(String path) throws IOException {
    String template = loadTemplate(path);
    assertTrue(
        template.toLowerCase().contains("same language as the user"),
        path + " must instruct the model to answer in the same language as the user's question"
    );
  }

  @ParameterizedTest
  @ValueSource(strings = {
      "templates/rag-prompt-template-openai.st",
      "templates/rag-prompt-template-anthropic.st"
  })
  void templateContainsDoNotSwitchLanguageRule(String path) throws IOException {
    String template = loadTemplate(path);
    assertTrue(
        template.toLowerCase().contains("do not switch language"),
        path + " must instruct the model not to switch language mid-answer"
    );
  }

  // --- Norwegian-specific examples present ---

  @ParameterizedTest
  @ValueSource(strings = {
      "templates/rag-prompt-template-openai.st",
      "templates/rag-prompt-template-anthropic.st"
  })
  void templateContainsNorwegianExamples(String path) throws IOException {
    String template = loadTemplate(path);
    assertTrue(
        template.contains("Norwegian") || template.contains("norsk") || template.contains("Norsk"),
        path + " must mention Norwegian as a supported language"
    );
  }

  @ParameterizedTest
  @ValueSource(strings = {
      "templates/rag-prompt-template-openai.st",
      "templates/rag-prompt-template-anthropic.st"
  })
  void templateContainsEnglishExamples(String path) throws IOException {
    String template = loadTemplate(path);
    assertTrue(
        template.contains("English"),
        path + " must mention English as a supported language"
    );
  }

  // --- Bilingual example pairs for off-topic, grades, and privacy ---

  @ParameterizedTest
  @ValueSource(strings = {
      "templates/rag-prompt-template-openai.st",
      "templates/rag-prompt-template-anthropic.st"
  })
  void templateContainsBilingualGradeRefusalExample(String path) throws IOException {
    String template = loadTemplate(path);
    boolean hasNorwegianExample = template.contains("karakterer") || template.contains("Godt forsøk");
    boolean hasEnglishExample = template.contains("grades") || template.contains("Nice try");
    assertTrue(hasNorwegianExample,
        path + " must contain a Norwegian example for grade refusal");
    assertTrue(hasEnglishExample,
        path + " must contain an English example for grade refusal");
  }

  @ParameterizedTest
  @ValueSource(strings = {
      "templates/rag-prompt-template-openai.st",
      "templates/rag-prompt-template-anthropic.st"
  })
  void templateContainsBachelorThesisGradeException(String path) throws IOException {
    String template = loadTemplate(path);
    assertTrue(
        template.contains("IDATT2901") || template.contains("Foresight AI"),
        path + " must allow disclosing the bachelor's thesis grade");
    assertTrue(
        template.toLowerCase().contains("any other course"),
        path + " must still refuse grades for other courses");
  }

  @ParameterizedTest
  @ValueSource(strings = {
      "templates/rag-prompt-template-openai.st",
      "templates/rag-prompt-template-anthropic.st"
  })
  void templateContainsBilingualOffTopicResponse(String path) throws IOException {
    String template = loadTemplate(path);
    assertTrue(
        template.toLowerCase().contains("in the user's language") || template.toLowerCase().contains("user's language"),
        path + " must instruct the model to respond in the user's language for off-topic questions"
    );
  }

  @ParameterizedTest
  @ValueSource(strings = {
      "templates/rag-prompt-template-openai.st",
      "templates/rag-prompt-template-anthropic.st"
  })
  void templateContainsPrivacyLanguageRule(String path) throws IOException {
    String template = loadTemplate(path);
    assertTrue(
        template.toLowerCase().contains("same language as the question")
            || template.toLowerCase().contains("in the user's language"),
        path + " must instruct privacy responses to match the question language"
    );
  }

  // --- Template contains both placeholders ---

  @ParameterizedTest
  @ValueSource(strings = {
      "templates/rag-prompt-template-openai.st",
      "templates/rag-prompt-template-anthropic.st"
  })
  void templateContainsInputAndDocumentsPlaceholders(String path) throws IOException {
    String template = loadTemplate(path);
    assertTrue(template.contains("{input}"),
        path + " must contain {input} placeholder for the user question");
    assertTrue(template.contains("{documents}"),
        path + " must contain {documents} placeholder for retrieved context");
  }

  // --- Templates should be consistent with each other ---

  @Test
  void bothTemplatesContainSameLanguageRuleCount() throws IOException {
    String openai = loadTemplate(TEMPLATE_PATHS.get(0)).toLowerCase();
    String anthropic = loadTemplate(TEMPLATE_PATHS.get(1)).toLowerCase();

    long openaiCount = countOccurrences(openai, "same language");
    long anthropicCount = countOccurrences(anthropic, "same language");

    assertTrue(openaiCount >= 2,
        "OpenAI template must reference 'same language' at least twice (once for general rule, once for specific cases)");
    assertTrue(anthropicCount >= 2,
        "Anthropic template must reference 'same language' at least twice");
    assertTrue(Math.abs(openaiCount - anthropicCount) <= 1,
        "Both templates should have a similar number of 'same language' references (keep in sync)");
  }

  @Test
  void bothTemplatesContainThirdPersonRule() throws IOException {
    for (String path : TEMPLATE_PATHS) {
      String template = loadTemplate(path);
      assertTrue(
          template.contains("third person"),
          path + " must contain the third-person rule"
      );
    }
  }

  @Test
  void bothTemplatesContainNorwegianPronounHandling() throws IOException {
    for (String path : TEMPLATE_PATHS) {
      String template = loadTemplate(path);
      assertTrue(
          template.contains("du") && template.contains("din"),
          path + " must handle Norwegian second-person pronouns (du, din)"
      );
    }
  }

  // --- Bilingual "not in documents" fallback messages ---

  @ParameterizedTest
  @ValueSource(strings = {
      "templates/rag-prompt-template-openai.st",
      "templates/rag-prompt-template-anthropic.st"
  })
  void templateContainsBilingualNotFoundMessage(String path) throws IOException {
    String template = loadTemplate(path);
    boolean hasEnglishFallback = template.contains("The information I have about Kevin doesn't include that")
        || template.contains("information I have about Kevin");
    boolean hasNorwegianFallback = template.contains("Det står ikke i informasjonen jeg har om Kevin")
        || template.contains("informasjonen jeg har om Kevin");
    assertTrue(hasEnglishFallback,
        path + " must contain an English 'information not found' fallback message");
    assertTrue(hasNorwegianFallback,
        path + " must contain a Norwegian 'information not found' fallback message");
  }

  // --- Norwegian persona examples ---

  @ParameterizedTest
  @ValueSource(strings = {
      "templates/rag-prompt-template-openai.st",
      "templates/rag-prompt-template-anthropic.st"
  })
  void templateContainsNorwegianPersonaExample(String path) throws IOException {
    String template = loadTemplate(path);
    assertTrue(
        template.contains("Kevin studerer dataingeniør ved NTNU"),
        path + " must contain a Norwegian persona example showing third-person + Norwegian"
    );
  }

  @ParameterizedTest
  @ValueSource(strings = {
      "templates/rag-prompt-template-openai.st",
      "templates/rag-prompt-template-anthropic.st"
  })
  void templateContainsEnglishPersonaExample(String path) throws IOException {
    String template = loadTemplate(path);
    assertTrue(
        template.contains("Kevin studies data engineering at NTNU"),
        path + " must contain an English persona example showing third-person + English"
    );
  }

  private static long countOccurrences(String text, String sub) {
    long count = 0;
    int idx = 0;
    while ((idx = text.indexOf(sub, idx)) >= 0) {
      count++;
      idx += sub.length();
    }
    return count;
  }
}
