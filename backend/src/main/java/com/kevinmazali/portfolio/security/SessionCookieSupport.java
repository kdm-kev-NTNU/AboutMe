package com.kevinmazali.portfolio.security;

import com.kevinmazali.portfolio.config.SessionCookieProperties;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/**
 * Builds Set-Cookie headers for the portfolio session JWT.
 */
@Component
public class SessionCookieSupport {

    private final SessionCookieProperties properties;

    public SessionCookieSupport(SessionCookieProperties properties) {
        this.properties = properties;
    }

    public void writeSessionCookie(HttpServletResponse response, String jwt) {
        ResponseCookie cookie = ResponseCookie.from(properties.getCookieName(), jwt)
                .httpOnly(true)
                .secure(properties.isSecureCookie())
                .sameSite(properties.getSameSite())
                .path("/")
                .maxAge(properties.getTtlSeconds())
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    public void clearSessionCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(properties.getCookieName(), "")
                .httpOnly(true)
                .secure(properties.isSecureCookie())
                .sameSite(properties.getSameSite())
                .path("/")
                .maxAge(0)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
