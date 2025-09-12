package com.kevinmazali.portfolio.controller;

import com.kevinmazali.portfolio.model.dto.ConversationDtos;
import com.kevinmazali.portfolio.service.ConversationService;
import com.kevinmazali.portfolio.util.InputValidator;
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
        // Validate requesterId if provided
        if (requesterId != null && !InputValidator.isValidRequesterId(requesterId)) {
            throw new IllegalArgumentException("Invalid requester ID format");
        }
        
        // Validate gapMinutes if provided
        if (gapMinutes != null && (gapMinutes < 0 || gapMinutes > 1440)) { // Max 24 hours
            throw new IllegalArgumentException("Gap minutes must be between 0 and 1440");
        }
        
        Duration gap = gapMinutes != null ? Duration.ofMinutes(gapMinutes) : null;
        String sanitizedRequesterId = requesterId != null ? InputValidator.sanitizeString(requesterId) : null;
        
        return conversationService.listConversations(gap, sanitizedRequesterId);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(
        @PathVariable("id") String id,
        @RequestParam(name = "gapMinutes", required = false) Integer gapMinutes,
        @RequestParam(name = "requesterId", required = false) String requesterId
    ) {
        // Validate conversation ID
        if (!InputValidator.isValidPathParameter(id)) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", "Invalid conversation ID"));
        }
        
        long conversationId = Long.parseLong(id);
        
        // Validate requesterId if provided
        if (requesterId != null && !InputValidator.isValidRequesterId(requesterId)) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", "Invalid requester ID format"));
        }
        
        // Validate gapMinutes if provided
        if (gapMinutes != null && (gapMinutes < 0 || gapMinutes > 1440)) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", "Gap minutes must be between 0 and 1440"));
        }
        
        Duration gap = gapMinutes != null ? Duration.ofMinutes(gapMinutes) : null;
        String sanitizedRequesterId = requesterId != null ? InputValidator.sanitizeString(requesterId) : null;
        
        ConversationDtos.Conversation conv = conversationService.getConversation(conversationId, gap, sanitizedRequesterId);
        if (conv == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(conv);
    }
}


