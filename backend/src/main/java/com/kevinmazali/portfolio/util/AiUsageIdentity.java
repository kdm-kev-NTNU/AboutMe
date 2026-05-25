package com.kevinmazali.portfolio.util;

import com.kevinmazali.portfolio.model.AiUsageEvent.IdentityType;

public record AiUsageIdentity(IdentityType identityType, String identityKey, Long userId) {

  public static AiUsageIdentity fromBudgetUserId(String budgetUserId, Long userId) {
    String key = budgetUserId != null ? budgetUserId : "unknown";
    IdentityType type;
    if (key.startsWith("user:")) {
      type = IdentityType.authenticated;
    } else if (key.startsWith("anon:")) {
      type = IdentityType.anonymous;
    } else if (key.startsWith("system:")) {
      type = IdentityType.system;
    } else {
      type = IdentityType.anonymous;
    }
    return new AiUsageIdentity(type, key, userId);
  }
}
