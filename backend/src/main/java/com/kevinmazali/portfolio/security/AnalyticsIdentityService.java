package com.kevinmazali.portfolio.security;

import com.kevinmazali.portfolio.config.PostHogProperties;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * Derives stable, opaque PostHog {@code distinct_id} values for authenticated admins. Identity is
 * computed server-side only — never accept client-supplied analytics identifiers.
 */
@Service
public class AnalyticsIdentityService {

  private static final String ID_PREFIX = "owner_";
  private static final int ID_LENGTH = 22;
  private static final String DEFAULT_SALT = "aboutme-analytics-identity-v1";

  private final PostHogProperties postHogProperties;

  public AnalyticsIdentityService(PostHogProperties postHogProperties) {
    this.postHogProperties = postHogProperties;
  }

  /**
   * Stable PostHog distinct id for an admin username. Same username always yields the same id;
   * id never equals the username and carries no PII.
   */
  public String distinctIdFor(String username) {
    if (username == null || username.isBlank()) {
      throw new IllegalArgumentException("username is required");
    }
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(solveSaltBytes(), "HmacSHA256"));
      byte[] digest =
          mac.doFinal(("analytics:v1:" + username).getBytes(StandardCharsets.UTF_8));
      String encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
      int take = Math.min(ID_LENGTH, encoded.length());
      return ID_PREFIX + encoded.substring(0, take);
    } catch (Exception ex) {
      throw new IllegalStateException("Failed to derive analytics identity", ex);
    }
  }

  /** When the current security context is an authenticated admin, returns their analytics id. */
  public Optional<String> distinctIdForAuthenticatedAdmin() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
      return Optional.empty();
    }
    boolean isAdmin =
        auth.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    if (!isAdmin) {
      return Optional.empty();
    }
    return Optional.of(distinctIdFor(auth.getName()));
  }

  /** PostHog distinct id and anonymous flag for the current request (admin session overrides anon). */
  public PostHogCaptureIdentity captureIdentity(String fallbackDistinctId, boolean fallbackAnonymous) {
    return distinctIdForAuthenticatedAdmin()
        .map(id -> new PostHogCaptureIdentity(id, false))
        .orElse(new PostHogCaptureIdentity(fallbackDistinctId, fallbackAnonymous));
  }

  public record PostHogCaptureIdentity(String distinctId, boolean anonymous) {}

  private byte[] solveSaltBytes() {
    String salt = postHogProperties.getIdentitySalt();
    if (salt == null || salt.isBlank()) {
      salt = DEFAULT_SALT;
    }
    return salt.getBytes(StandardCharsets.UTF_8);
  }
}
