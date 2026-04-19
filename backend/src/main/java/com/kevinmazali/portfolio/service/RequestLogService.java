package com.kevinmazali.portfolio.service;

import com.kevinmazali.portfolio.model.RequestLog;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import com.kevinmazali.portfolio.repository.RequestLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Persists minimal request/response audit information for the public API.
 * {@code requesterId} may be supplied by the client; otherwise a coarse label is derived from
 * {@link org.springframework.security.core.context.SecurityContextHolder} (legacy display values
 * remain in the DB for existing rows).
 */
@Service
public class RequestLogService {

    private static final int MAX_PAYLOAD_LENGTH = 500;
    private static final String TRUNCATED_SUFFIX = "...[truncated]";

    private final RequestLogRepository requestLogRepository;

    public RequestLogService(RequestLogRepository requestLogRepository) {
        this.requestLogRepository = requestLogRepository;
    }

    /**
     * Stores a single audit log entry.
     *
     * @param path the request path (e.g. /ask)
     * @param method the HTTP method
     * @param payload the request or response payload
     * @param requesterId optional chat/requester identifier
     */
    @Transactional
    public void save(String path, String method, String payload, String requesterId) {
        RequestLog log = new RequestLog();
        log.setPath(path);
        log.setMethod(method);
        log.setPayload(truncatePayload(payload));
        // Prefer explicit requester id from the API; fall back to username or a generic end-user label.
        String computedRequester;
        if (requesterId != null) {
            computedRequester = requesterId;
        } else {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && auth.getName() != null) {
                String username = auth.getName();
                if ("GOAT".equals(username)) {
                    computedRequester = "GOAT";
                } else {
                    computedRequester = "Bruker";
                }
            } else {
                computedRequester = "Bruker";
            }
        }
        log.setRequesterId(computedRequester);
        requestLogRepository.save(log);
    }

    static String truncatePayload(String payload) {
        if (payload == null) {
            return null;
        }
        if (payload.length() <= MAX_PAYLOAD_LENGTH) {
            return payload;
        }
        return payload.substring(0, MAX_PAYLOAD_LENGTH) + TRUNCATED_SUFFIX;
    }
}


