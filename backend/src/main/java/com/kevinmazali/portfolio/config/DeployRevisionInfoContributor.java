package com.kevinmazali.portfolio.config;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.boot.info.GitProperties;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Publishes a stable {@code deploy.revision} on {@code /actuator/info} for the production canary.
 *
 * <p>Railway Dockerfile builds do not include {@code .git}, so {@code git-commit-id-maven-plugin}
 * cannot embed a commit at image-build time. Railway injects {@code RAILWAY_GIT_COMMIT_SHA} at
 * runtime; locally and in CI the Maven plugin's {@link GitProperties} fills the same slot.
 */
@Component
public class DeployRevisionInfoContributor implements InfoContributor {

  private final String railwayOrOverrideSha;
  @Nullable private final GitProperties gitProperties;

  public DeployRevisionInfoContributor(
      @Value("${RAILWAY_GIT_COMMIT_SHA:${GIT_COMMIT_SHA:}}") String railwayOrOverrideSha,
      ObjectProvider<GitProperties> gitProperties) {
    this.railwayOrOverrideSha = railwayOrOverrideSha != null ? railwayOrOverrideSha.trim() : "";
    this.gitProperties = gitProperties.getIfAvailable();
  }

  @Override
  public void contribute(Info.Builder builder) {
    String revision = resolveRevision();
    if (!StringUtils.hasText(revision)) {
      return;
    }
    Map<String, Object> deploy = new LinkedHashMap<>();
    deploy.put("revision", revision);
    builder.withDetail("deploy", deploy);
  }

  private String resolveRevision() {
    if (StringUtils.hasText(railwayOrOverrideSha)) {
      return railwayOrOverrideSha;
    }
    return Optional.ofNullable(gitProperties)
        .map(GitProperties::getCommitId)
        .filter(StringUtils::hasText)
        .orElse("");
  }
}
