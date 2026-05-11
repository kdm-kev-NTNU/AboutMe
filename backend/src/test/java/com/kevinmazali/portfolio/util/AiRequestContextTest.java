package com.kevinmazali.portfolio.util;

import com.kevinmazali.portfolio.config.AiBudgetProperties;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AiRequestContextTest {

  @AfterEach
  void clear() {
    SecurityContextHolder.clearContext();
    RequestContextHolder.resetRequestAttributes();
  }

  @Test
  void isAnonymousInteractiveUser_trueWhenNoAuth() {
    assertTrue(AiRequestContext.isAnonymousInteractiveUser());
  }

  @Test
  void isAnonymousInteractiveUser_trueWhenNotAuthenticated() {
    Authentication a = mock(Authentication.class);
    when(a.isAuthenticated()).thenReturn(false);
    SecurityContextHolder.getContext().setAuthentication(a);
    assertTrue(AiRequestContext.isAnonymousInteractiveUser());
  }

  @Test
  void isAnonymousInteractiveUser_trueForAnonymousToken() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken("k", "anon", List.of(new SimpleGrantedAuthority("ROLE_ANON"))));
    assertTrue(AiRequestContext.isAnonymousInteractiveUser());
  }

  @Test
  void isAnonymousInteractiveUser_falseForLoggedInUser() {
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken("alice", "x", List.of()));
    assertFalse(AiRequestContext.isAnonymousInteractiveUser());
  }

  @Test
  void budgetUserIdentifier_usesAuthenticatedName() {
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken("alice", "x", List.of()));
    assertEquals("user:alice", AiRequestContext.budgetUserIdentifier(new AiBudgetProperties()));
  }

  @Test
  void budgetUserIdentifier_hashesAnonymousIpFromRequest() {
    MockHttpServletRequest req = new MockHttpServletRequest();
    req.setRemoteAddr("203.0.113.5");
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(req));
    AiBudgetProperties budget = new AiBudgetProperties();
    budget.setAnonIdentitySalt("salt");
    String id = AiRequestContext.budgetUserIdentifier(budget);
    assertTrue(id.startsWith("anon:"));
    assertEquals(id, AiRequestContext.budgetUserIdentifier(budget));
  }

  @Test
  void budgetUserIdentifier_unknownWhenNoRequestAttributes() {
    String id = AiRequestContext.budgetUserIdentifier(new AiBudgetProperties());
    assertEquals("anon:unknown", id);
  }

  @Test
  void budgetUserIdentifier_handlesNullRemoteAddr() {
    HttpServletRequest req = org.mockito.Mockito.mock(HttpServletRequest.class);
    org.mockito.Mockito.when(req.getRemoteAddr()).thenReturn(null);
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(req));
    String id = AiRequestContext.budgetUserIdentifier(new AiBudgetProperties());
    assertTrue(id.startsWith("anon:"));
  }

  @Test
  void openAiSafetyIdentifier_handlesNullAndEmptyInput() {
    String nullId = AiRequestContext.openAiSafetyIdentifier(null);
    String emptyId = AiRequestContext.openAiSafetyIdentifier("");
    String userId = AiRequestContext.openAiSafetyIdentifier("alice");

    assertEquals(64, nullId.length());
    assertEquals(64, emptyId.length());
    assertEquals(nullId, emptyId);
    assertNotEquals(nullId, userId);
  }
}
