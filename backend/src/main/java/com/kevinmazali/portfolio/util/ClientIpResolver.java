package com.kevinmazali.portfolio.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;

/**
 * Resolves the client IP for rate limits and anonymous budget keys, honoring {@code X-Forwarded-For}
 * when the request was proxied (nginx, Railway, etc.).
 */
public final class ClientIpResolver {

    private static final String FORWARDED_FOR = "X-Forwarded-For";

    private ClientIpResolver() {}

    public static String resolve(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }
        String forwarded = request.getHeader(FORWARDED_FOR);
        if (StringUtils.hasText(forwarded)) {
            String first = forwarded.split(",")[0].trim();
            if (StringUtils.hasText(first)) {
                return first;
            }
        }
        String remote = request.getRemoteAddr();
        return StringUtils.hasText(remote) ? remote : "unknown";
    }
}
