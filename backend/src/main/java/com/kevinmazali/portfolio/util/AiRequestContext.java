package com.kevinmazali.portfolio.util;

import com.kevinmazali.portfolio.config.AiBudgetProperties;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Resolves budget identity and anonymous flag from the current request / security context.
 */
public final class AiRequestContext {

  private AiRequestContext() {
  }

  public static boolean isAnonymousInteractiveUser() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !auth.isAuthenticated()) {
      return true;
    }
    return auth instanceof AnonymousAuthenticationToken;
  }

  /**
   * Stable per-user or per-anon-client key for quotas and usage rows.
   */
  public static String budgetUserIdentifier(AiBudgetProperties budgetProperties) {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
      return "user:" + auth.getName();
    }
    ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
    if (attrs != null) {
      HttpServletRequest req = attrs.getRequest();
      String ip = ClientIpResolver.resolve(req);
      return "anon:" + sha256Hex(ip + ":" + budgetProperties.getAnonIdentitySalt());
    }
    return "anon:unknown";
  }

  /**
   * Opaque value for OpenAI's {@code OpenAI-Safety-Identifier} header. Always 64 ASCII hex
   * characters (64 UTF-8 bytes), derived from the budget identity string. OpenAI rejects
   * identifiers longer than 64 bytes; long or non-ASCII usernames can exceed that when sent raw.
   */
  public static String openAiSafetyIdentifier(String budgetUserId) {
    if (budgetUserId == null || budgetUserId.isEmpty()) {
      return sha256Hex("");
    }
    return sha256Hex(budgetUserId);
  }

  private static String sha256Hex(String input) {
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(digest);
    } catch (NoSuchAlgorithmException e) {
      return Integer.toHexString(input.hashCode());
    }
  }
}
