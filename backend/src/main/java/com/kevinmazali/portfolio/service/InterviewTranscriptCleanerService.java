package com.kevinmazali.portfolio.service;

import com.kevinmazali.portfolio.config.AiBudgetProperties;
import com.kevinmazali.portfolio.config.InterviewProperties;
import com.kevinmazali.portfolio.model.SanitizeResult;
import com.kevinmazali.portfolio.model.interview.InterviewTurnDto;
import com.kevinmazali.portfolio.util.AiRequestContext;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.io.ClassPathResource;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;

@Slf4j
@Service
public class InterviewTranscriptCleanerService {

  private final NoiseCleaner noiseCleaner;
  private final ObjectProvider<PiiSanitizerService> piiSanitizerProvider;
  private final ObjectProvider<OpenAiChatModel> openAiChatModel;
  private final AiBudgetService aiBudgetService;
  private final AiBudgetProperties budgetProperties;
  private final AiCircuitBreaker aiCircuitBreaker;
  private final InterviewProperties interviewProperties;

  private volatile String cleanPromptEn;
  private volatile String cleanPromptNo;

  public InterviewTranscriptCleanerService(
      NoiseCleaner noiseCleaner,
      ObjectProvider<PiiSanitizerService> piiSanitizerProvider,
      ObjectProvider<OpenAiChatModel> openAiChatModel,
      AiBudgetService aiBudgetService,
      AiBudgetProperties budgetProperties,
      AiCircuitBreaker aiCircuitBreaker,
      InterviewProperties interviewProperties) {
    this.noiseCleaner = noiseCleaner;
    this.piiSanitizerProvider = piiSanitizerProvider;
    this.openAiChatModel = openAiChatModel;
    this.aiBudgetService = aiBudgetService;
    this.budgetProperties = budgetProperties;
    this.aiCircuitBreaker = aiCircuitBreaker;
    this.interviewProperties = interviewProperties;
  }

  public String structureRawTranscript(List<InterviewTurnDto> turns) {
    if (turns == null || turns.isEmpty()) {
      return "";
    }
    StringBuilder sb = new StringBuilder();
    for (InterviewTurnDto turn : turns) {
      if (!StringUtils.hasText(turn.text())) {
        continue;
      }
      String heading = roleHeading(turn.role());
      sb.append("## ").append(heading).append("\n\n");
      sb.append(turn.text().trim()).append("\n\n");
    }
    return sb.toString().trim();
  }

  public String cleanForIngest(String rawText, String language) {
    if (!StringUtils.hasText(rawText)) {
      return "";
    }
    String cleaned = noiseCleaner.cleanNoise(rawText).cleanedText();
    PiiSanitizerService sanitizer = piiSanitizerProvider.getIfAvailable();
    if (sanitizer != null) {
      SanitizeResult result = sanitizer.sanitize(cleaned);
      cleaned = result.sanitizedText();
    }
    return normalizeWithLlm(cleaned, language);
  }

  @Nullable
  String normalizeWithLlm(String structuredText, String language) {
    OpenAiChatModel model = openAiChatModel.getIfAvailable();
    if (model == null || !StringUtils.hasText(structuredText)) {
      return structuredText;
    }
    String lang = normalizeLang(language);
    String template = cleanPromptForLanguage(lang);
    String promptText = template.replace("{transcript}", structuredText);
    String budgetUserId = AiRequestContext.budgetUserIdentifier(budgetProperties);
    boolean anonymous = AiRequestContext.isAnonymousInteractiveUser();
    try {
      aiCircuitBreaker.assertClosed();
      aiBudgetService.assertWithinBudget(budgetUserId, anonymous);
      String cleanModel = interviewProperties.resolvedCleanModel();
      int maxTokens = interviewProperties.resolvedCleanMaxTokens();
      OpenAiChatOptions options =
          OpenAiChatOptions.builder().model(cleanModel).maxTokens(maxTokens).temperature(0.2).build();
      ChatResponse response = model.call(new Prompt(promptText, options));
      String output =
          response.getResult() != null && response.getResult().getOutput() != null
              ? response.getResult().getOutput().getText()
              : null;
      if (!StringUtils.hasText(output)) {
        return structuredText;
      }
      aiBudgetService.recordUsage(
          budgetUserId,
          cleanModel,
          500,
          Math.min(1500, maxTokens),
          anonymous,
          null,
          "interview_transcript_clean");
      return output.trim();
    } catch (Exception e) {
      log.warn("Interview transcript LLM clean failed, using structured text: {}", e.getMessage());
      return structuredText;
    }
  }

  private String cleanPromptForLanguage(String lang) {
    if ("no".equals(lang)) {
      if (cleanPromptNo == null) {
        cleanPromptNo = loadPrompt("prompts/interview-transcript-clean-no.txt");
      }
      return cleanPromptNo;
    }
    if (cleanPromptEn == null) {
      cleanPromptEn = loadPrompt("prompts/interview-transcript-clean-en.txt");
    }
    return cleanPromptEn;
  }

  private static String roleHeading(String role) {
    if (role == null) {
      return "Unknown";
    }
    return switch (role.toLowerCase(Locale.ROOT)) {
      case "interviewer", "assistant" -> "Interviewer";
      case "user", "kevin" -> "Kevin";
      default -> role;
    };
  }

  private static String normalizeLang(String raw) {
    if (!StringUtils.hasText(raw)) {
      return "en";
    }
    String v = raw.trim().toLowerCase(Locale.ROOT);
    if ("no".equals(v) || "nb".equals(v) || "nn".equals(v)) {
      return "no";
    }
    return "en";
  }

  private static String loadPrompt(String path) {
    try {
      var res = new ClassPathResource(path);
      return StreamUtils.copyToString(res.getInputStream(), StandardCharsets.UTF_8).trim();
    } catch (IOException e) {
      return "Clean this interview transcript into third-person facts about Kevin:\n\n{transcript}";
    }
  }
}
