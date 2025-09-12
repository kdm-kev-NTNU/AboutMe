package com.kevinmazali.portfolio.service;

import com.kevinmazali.portfolio.model.RequestLog;
import com.kevinmazali.portfolio.model.RequestLogRepository;
import com.kevinmazali.portfolio.model.dto.ConversationDtos;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class ConversationService {

    private final RequestLogRepository requestLogRepository;

    // Default idle gap to split conversations
    private static final Duration DEFAULT_GAP = Duration.ofMinutes(20);

    public ConversationService(RequestLogRepository requestLogRepository) {
        this.requestLogRepository = requestLogRepository;
    }

    public List<ConversationDtos.ConversationSummary> listConversations(Duration gap, String requesterId) {
        Duration splitGap = Objects.requireNonNullElse(gap, DEFAULT_GAP);
        List<List<RequestLog>> grouped = groupByGap(splitGap, requesterId);
        List<ConversationDtos.ConversationSummary> summaries = new ArrayList<>();
        long idx = 1;
        for (List<RequestLog> group : grouped) {
            if (group.isEmpty()) continue;
            OffsetDateTime start = group.getFirst().getCreatedAt();
            OffsetDateTime end = group.getLast().getCreatedAt();
            String preview = buildPreview(group);
            summaries.add(new ConversationDtos.ConversationSummary(idx++, start, end, group.size(), preview));
        }
        return summaries;
    }

    public ConversationDtos.Conversation getConversation(long conversationId, Duration gap, String requesterId) {
        Duration splitGap = Objects.requireNonNullElse(gap, DEFAULT_GAP);
        List<List<RequestLog>> grouped = groupByGap(splitGap, requesterId);
        if (conversationId < 1 || conversationId > grouped.size()) {
            return null;
        }
        List<RequestLog> group = grouped.get((int) conversationId - 1);
        List<ConversationDtos.Message> messages = new ArrayList<>();
        for (RequestLog rl : group) {
            messages.add(new ConversationDtos.Message(
                rl.getId(),
                roleFromPath(rl.getPath()),
                rl.getPayload(),
                rl.getCreatedAt()
            ));
        }
        OffsetDateTime start = group.get(0).getCreatedAt();
        OffsetDateTime end = group.get(group.size() - 1).getCreatedAt();
        return new ConversationDtos.Conversation(conversationId, start, end, messages);
    }

    private List<List<RequestLog>> groupByGap(Duration gap, String requesterId) {
        List<RequestLog> all = requesterId == null || requesterId.isBlank()
            ? requestLogRepository.findAllByOrderByCreatedAtAsc()
            : requestLogRepository.findByRequesterIdOrderByCreatedAtAsc(requesterId);
        List<List<RequestLog>> groups = new ArrayList<>();
        List<RequestLog> current = new ArrayList<>();
        OffsetDateTime prev = null;
        for (RequestLog rl : all) {
            if (prev == null) {
                current.add(rl);
                prev = rl.getCreatedAt();
                continue;
            }
            Duration diff = Duration.between(prev, rl.getCreatedAt());
            if (diff.compareTo(gap) > 0) {
                if (!current.isEmpty()) groups.add(current);
                current = new ArrayList<>();
            }
            current.add(rl);
            prev = rl.getCreatedAt();
        }
        if (!current.isEmpty()) groups.add(current);
        return groups;
    }

    private static String roleFromPath(String path) {
        if (path == null) return "system";
        if (path.endsWith(":response")) return "assistant";
        return "user";
    }

    private static String buildPreview(List<RequestLog> group) {
        // Prefer the first user question as preview, otherwise the first payload
        for (RequestLog rl : group) {
            if (!roleFromPath(rl.getPath()).equals("assistant")) {
                return truncate(rl.getPayload(), 140);
            }
        }
        return truncate(group.get(0).getPayload(), 140);
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }
}


