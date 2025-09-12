package com.kevinmazali.portfolio.controller;

import com.kevinmazali.portfolio.model.dto.ConversationDtos;
import com.kevinmazali.portfolio.service.ConversationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/conversations")
public class ConversationController {

    private final ConversationService conversationService;

    @GetMapping
    public List<ConversationDtos.ConversationSummary> list(
        @RequestParam(name = "gapMinutes", required = false) Integer gapMinutes,
        @RequestParam(name = "requesterId", required = false) String requesterId
    ) {
        Duration gap = gapMinutes != null ? Duration.ofMinutes(gapMinutes) : null;
        return conversationService.listConversations(gap, requesterId);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(
        @PathVariable("id") long id,
        @RequestParam(name = "gapMinutes", required = false) Integer gapMinutes,
        @RequestParam(name = "requesterId", required = false) String requesterId
    ) {
        Duration gap = gapMinutes != null ? Duration.ofMinutes(gapMinutes) : null;
        ConversationDtos.Conversation conv = conversationService.getConversation(id, gap, requesterId);
        if (conv == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(conv);
    }
}


