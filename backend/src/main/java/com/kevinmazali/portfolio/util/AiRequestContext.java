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
      String ip = req.getRemoteAddr() != null ? req.getRemoteAddr() : "unknown";
      return "anon:" + sha256Hex(ip + ":" + budgetProperties.getAnonIdentitySalt());
    }
    return "anon:unknown";
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
