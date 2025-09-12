package com.kevinmazali.portfolio.model.dto;

import java.time.OffsetDateTime;
import java.util.List;

public final class ConversationDtos {

    private ConversationDtos() {}

    public record Message(Long id, String role, String text, OffsetDateTime createdAt) {}

    public record Conversation(Long id, OffsetDateTime startedAt, OffsetDateTime endedAt, List<Message> messages) {}

    public record ConversationSummary(Long id, OffsetDateTime startedAt, OffsetDateTime endedAt, int messageCount, String preview) {}
}


