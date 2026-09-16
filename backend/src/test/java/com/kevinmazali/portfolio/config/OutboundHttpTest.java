package com.kevinmazali.portfolio.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;

/**
 * Guards the outbound HTTP egress invariant.
 *
 * <p>Live voice and PostHog feature flags were both silently down in production because each had
 * independently chosen {@code java.net.http.HttpClient}, which commits to a single resolved address
 * and therefore cannot reach a dual-stack host when the JVM prefers an unroutable address family.
 * Unit tests could not catch it: every one of them stubbed the transport away.
 *
 * <p>These tests constrain the choice of client instead of the behaviour of a call, because the
 * choice is what went wrong -- twice, in unrelated features.
 */
class OutboundHttpTest {

  @Test
  void requestFactory_usesApacheHttpComponents_whichTriesEveryResolvedAddress() {
    assertThat(OutboundHttp.requestFactory(Duration.ofSeconds(3), Duration.ofSeconds(5)))
        .isInstanceOf(HttpComponentsClientHttpRequestFactory.class);
  }

  /**
   * Fails if any production source reaches for a single-address HTTP client again. Enforcement lives
   * here rather than in a comment because a comment did not stop the second occurrence.
   */
  @Test
  void productionSourcesDoNotUseSingleAddressHttpClients() throws IOException {
    List<String> forbidden = List.of("java.net.http.HttpClient", "JdkClientHttpRequestFactory");
    // Surefire runs with the module directory as the working directory.
    Path mainSources = Path.of("src", "main", "java");
    assertThat(mainSources)
        .as("production sources must be resolvable from the working directory")
        .isDirectory();

    try (Stream<Path> sources = Files.walk(mainSources)) {
      List<String> offenders = sources
          .filter(path -> path.toString().endsWith(".java"))
          .filter(path -> usesAny(path, forbidden))
          .map(mainSources::relativize)
          .map(Path::toString)
          .sorted()
          .toList();

      assertThat(offenders)
          .as(
              "These files use an HTTP client that connects to only one resolved address. "
                  + "Route them through OutboundHttp.requestFactory instead; see OutboundHttp for why.")
          .isEmpty();
    }
  }

  private static boolean usesAny(Path path, List<String> needles) {
    try {
      String code = stripComments(Files.readString(path));
      return needles.stream().anyMatch(code::contains);
    } catch (IOException e) {
      throw new IllegalStateException("Could not read " + path, e);
    }
  }

  /**
   * Drops block comments and full-line {@code //} comments so that documenting why a client is
   * forbidden does not count as using it. Trailing comments are left alone, since cutting at
   * {@code //} would also cut URLs inside string literals.
   */
  private static String stripComments(String source) {
    return source.replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("(?m)^\\s*//.*$", "");
  }
}
