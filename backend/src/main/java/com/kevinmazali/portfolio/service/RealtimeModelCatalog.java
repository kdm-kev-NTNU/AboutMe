package com.kevinmazali.portfolio.service;

import com.kevinmazali.portfolio.config.RealtimeProperties;
import com.kevinmazali.portfolio.model.RealtimeModelOption;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Exposes configured voice provider/model options to the SPA.
 */
@Service
public class RealtimeModelCatalog {

  private static final String OPENAI = "OPENAI";
  private static final String ELEVENLABS = "ELEVENLABS";

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
    addElevenLabsOptions(options);

    if (options.stream().noneMatch(RealtimeModelOption::defaultOption) && !options.isEmpty()) {
      RealtimeModelOption first = options.get(0);
      options.set(0, new RealtimeModelOption(first.provider(), first.id(), first.label(), true));
    }
    return List.copyOf(options);
  }

  public boolean hasAvailableModels() {
    return !listAvailableModels().isEmpty();
  }

  public boolean isOpenAiModelConfigured(String modelId) {
    if (!realtimeProperties.isEnabled()
        || !realtimeProperties.getProviders().getOpenai().isEnabled()
        || !StringUtils.hasText(openAiApiKey)) {
      return false;
    }
    return listOpenAiModels().stream().anyMatch(m -> m.id().equals(resolveOpenAiModelId(modelId)));
  }

  public RealtimeProperties.ElevenLabsAgent findElevenLabsAgent(String modelId) {
    if (!realtimeProperties.isEnabled()
        || !realtimeProperties.getProviders().getElevenlabs().isEnabled()) {
      return null;
    }
    String id = resolveElevenLabsAgentId(modelId);
    return realtimeProperties.getProviders().getElevenlabs().getAgents().stream()
        .filter(a -> StringUtils.hasText(a.getAgentId()))
        .filter(a -> a.getAgentId().trim().equals(id))
        .findFirst()
        .orElse(null);
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

  private void addElevenLabsOptions(List<RealtimeModelOption> options) {
    var elevenlabs = realtimeProperties.getProviders().getElevenlabs();
    if (!elevenlabs.isEnabled() || !StringUtils.hasText(elevenlabs.getApiKey())) {
      return;
    }
    String configuredDefault = StringUtils.hasText(elevenlabs.getDefaultAgentId())
        ? elevenlabs.getDefaultAgentId().trim()
        : "";
    for (var agent : elevenlabs.getAgents()) {
      if (!StringUtils.hasText(agent.getAgentId())) {
        continue;
      }
      String id = agent.getAgentId().trim();
      String label = StringUtils.hasText(agent.getLabel()) ? agent.getLabel().trim() : "ElevenLabs Agent";
      boolean defaultOption = agent.isDefaultAgent() || id.equals(configuredDefault);
      options.add(new RealtimeModelOption(ELEVENLABS, id, label, defaultOption));
    }
  }

  private String resolveElevenLabsAgentId(String requested) {
    if (StringUtils.hasText(requested)) {
      return requested.trim();
    }
    var elevenlabs = realtimeProperties.getProviders().getElevenlabs();
    if (StringUtils.hasText(elevenlabs.getDefaultAgentId())) {
      return elevenlabs.getDefaultAgentId().trim();
    }
    return elevenlabs.getAgents().stream()
        .filter(RealtimeProperties.ElevenLabsAgent::isDefaultAgent)
        .map(RealtimeProperties.ElevenLabsAgent::getAgentId)
        .filter(StringUtils::hasText)
        .map(String::trim)
        .findFirst()
        .orElseGet(() -> elevenlabs.getAgents().stream()
            .map(RealtimeProperties.ElevenLabsAgent::getAgentId)
            .filter(StringUtils::hasText)
            .map(String::trim)
            .findFirst()
            .orElse(""));
  }

  private record OpenAiCatalogModel(String id, String label, boolean defaultOption) {}
}
