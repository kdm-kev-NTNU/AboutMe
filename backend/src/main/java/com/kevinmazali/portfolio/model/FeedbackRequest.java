package com.kevinmazali.portfolio.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO for feedback submissions from site visitors.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Visitor feedback for improving the portfolio site")
public record FeedbackRequest(
    @Schema(description = "Feedback message", example = "Great site! The chat feature is very responsive.", maxLength = 4000)
    String message,
    @Schema(description = "Optional e-mail if the visitor wants a reply", example = "visitor@example.com")
    String replyEmail
) {}
