package com.kevinmazali.portfolio.service;

import com.kevinmazali.portfolio.config.RealtimeProperties;
import com.kevinmazali.portfolio.model.RealtimeModelOption;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Exposes configured OpenAI Realtime voice model options to the SPA.
 */
@Service
public class RealtimeModelCatalog {

  private static final String OPENAI = "OPENAI";

  private final RealtimeProperties realtimeProperties;
  private final String openAiApiKey;

  public RealtimeModelCatalog(
      RealtimeProperties realtimeProperties,
      @Value("${spring.ai.openai.api-key:}") String openAiApiKey) {
    this.realtimeProperties = realtimeProperties;
    this.openAiApiKey = openAiApiKey;
  }

  public List<RealtimeModelOption> listAvailableModels() {
    if (!realtimeProperties.isEnabled()) {
      return List.of();
    }

    List<RealtimeModelOption> options = new ArrayList<>();
    addOpenAiOptions(options);

    if (options.stream().noneMatch(RealtimeModelOption::defaultOption) && !options.isEmpty()) {
      RealtimeModelOption first = options.get(0);
      options.set(0, new RealtimeModelOption(first.provider(), first.id(), first.label(), true));
    }
    return List.copyOf(options);
  }

  public boolean hasAvailableModels() {
    if (!realtimeProperties.isEnabled()) {
      return false;
    }
    return hasOpenAiModels();
  }

  private boolean hasOpenAiModels() {
    return realtimeProperties.getProviders().getOpenai().isEnabled() && StringUtils.hasText(openAiApiKey);
  }

  public boolean isOpenAiModelConfigured(String modelId) {
    if (!realtimeProperties.isEnabled()
        || !realtimeProperties.getProviders().getOpenai().isEnabled()
        || !StringUtils.hasText(openAiApiKey)) {
      return false;
    }
    return listOpenAiModels().stream().anyMatch(m -> m.id().equals(resolveOpenAiModelId(modelId)));
  }

  public String resolveOpenAiModelId(String requested) {
    if (StringUtils.hasText(requested)) {
      return requested.trim();
    }
    return listOpenAiModels().stream()
        .filter(OpenAiCatalogModel::defaultOption)
        .findFirst()
        .map(OpenAiCatalogModel::id)
        .orElse(realtimeProperties.getModel());
  }

  private void addOpenAiOptions(List<RealtimeModelOption> options) {
    if (!realtimeProperties.getProviders().getOpenai().isEnabled() || !StringUtils.hasText(openAiApiKey)) {
      return;
    }
    for (OpenAiCatalogModel model : listOpenAiModels()) {
      options.add(new RealtimeModelOption(OPENAI, model.id(), model.label(), model.defaultOption()));
    }
  }

  private List<OpenAiCatalogModel> listOpenAiModels() {
    var openai = realtimeProperties.getProviders().getOpenai();
    String configuredDefault =
        StringUtils.hasText(openai.getDefaultModelId()) ? openai.getDefaultModelId().trim() : realtimeProperties.getModel();
    List<OpenAiCatalogModel> configured = openai.getModels().stream()
        .filter(m -> StringUtils.hasText(m.getId()))
        .map(m -> new OpenAiCatalogModel(
            m.getId().trim(),
            StringUtils.hasText(m.getLabel()) ? m.getLabel().trim() : m.getId().trim(),
            m.isDefaultModel() || m.getId().trim().equals(configuredDefault)))
        .toList();
    if (!configured.isEmpty()) {
      return configured;
    }
    String fallbackModel = realtimeProperties.getModel();
    return List.of(new OpenAiCatalogModel(fallbackModel, "OpenAI " + fallbackModel, true));
  }

  private record OpenAiCatalogModel(String id, String label, boolean defaultOption) {}
}
