package com.kevinmazali.portfolio.security;

import com.kevinmazali.portfolio.config.SessionCookieProperties;
import com.kevinmazali.portfolio.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Issues and validates signed JWTs stored in the admin session cookie.
 */
@Service
public class JwtService {

    private final SessionCookieProperties properties;
    private final SecretKey signingKey;

    public JwtService(SessionCookieProperties properties) {
        this.properties = properties;
        byte[] keyBytes = properties.getJwtSecret().getBytes(StandardCharsets.UTF_8);
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    public String issueToken(String username, User.Role role) {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(properties.getTtlSeconds());
        return Jwts.builder()
                .subject(username)
                .claim("role", role.name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(signingKey)
                .compact();
    }

    public Optional<SessionClaims> parseToken(String token) {
        if (!StringUtils.hasText(token)) {
            return Optional.empty();
        }
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            String username = claims.getSubject();
            String roleRaw = claims.get("role", String.class);
            if (!StringUtils.hasText(username) || !StringUtils.hasText(roleRaw)) {
                return Optional.empty();
            }
            User.Role role = User.Role.valueOf(roleRaw);
            return Optional.of(new SessionClaims(username, role));
        } catch (JwtException | IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    public record SessionClaims(String username, User.Role role) {}
}
