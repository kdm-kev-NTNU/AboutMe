package com.kevinmazali.portfolio.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.info.GitProperties;

class DeployRevisionInfoContributorTest {

  @Test
  void prefersRailwayEnvOverGitProperties() {
    GitProperties git = mock(GitProperties.class);
    when(git.getCommitId()).thenReturn("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
    @SuppressWarnings("unchecked")
    ObjectProvider<GitProperties> provider = mock(ObjectProvider.class);
    when(provider.getIfAvailable()).thenReturn(git);

    DeployRevisionInfoContributor contributor =
        new DeployRevisionInfoContributor("bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb", provider);
    Info.Builder builder = new Info.Builder();
    contributor.contribute(builder);
    Info info = builder.build();
    @SuppressWarnings("unchecked")
    Map<String, Object> deploy = (Map<String, Object>) info.get("deploy");
    assertThat(deploy.get("revision")).isEqualTo("bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb");
  }

  @Test
  void fallsBackToGitPropertiesWhenEnvBlank() {
    GitProperties git = mock(GitProperties.class);
    when(git.getCommitId()).thenReturn("cccccccccccccccccccccccccccccccccccccccc");
    @SuppressWarnings("unchecked")
    ObjectProvider<GitProperties> provider = mock(ObjectProvider.class);
    when(provider.getIfAvailable()).thenReturn(git);

    DeployRevisionInfoContributor contributor = new DeployRevisionInfoContributor("", provider);
    Info.Builder builder = new Info.Builder();
    contributor.contribute(builder);
    Info info = builder.build();
    @SuppressWarnings("unchecked")
    Map<String, Object> deploy = (Map<String, Object>) info.get("deploy");
    assertThat(deploy.get("revision")).isEqualTo("cccccccccccccccccccccccccccccccccccccccc");
  }
}
