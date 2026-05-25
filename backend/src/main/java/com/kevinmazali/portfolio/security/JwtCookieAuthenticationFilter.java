package com.kevinmazali.portfolio.security;

import com.kevinmazali.portfolio.config.SessionCookieProperties;
import com.kevinmazali.portfolio.model.User;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Authenticates requests that carry a valid session JWT in the configured httpOnly cookie.
 */
@Component
public class JwtCookieAuthenticationFilter extends OncePerRequestFilter {

    private final SessionCookieProperties sessionProperties;
    private final JwtService jwtService;

    public JwtCookieAuthenticationFilter(SessionCookieProperties sessionProperties, JwtService jwtService) {
        this.sessionProperties = sessionProperties;
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() == null
                || !SecurityContextHolder.getContext().getAuthentication().isAuthenticated()) {
            readSessionCookie(request).flatMap(jwtService::parseToken).ifPresent(claims -> {
                String authority = "ROLE_" + claims.role().name();
                var auth = new UsernamePasswordAuthenticationToken(
                        claims.username(),
                        null,
                        List.of(new SimpleGrantedAuthority(authority)));
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(auth);
            });
        }
        filterChain.doFilter(request, response);
    }

    private java.util.Optional<String> readSessionCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return java.util.Optional.empty();
        }
        String name = sessionProperties.getCookieName();
        for (Cookie cookie : cookies) {
            if (name.equals(cookie.getName()) && cookie.getValue() != null && !cookie.getValue().isBlank()) {
                return java.util.Optional.of(cookie.getValue());
            }
        }
        return java.util.Optional.empty();
    }
}
