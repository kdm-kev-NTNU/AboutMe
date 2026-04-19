package com.kevinmazali.portfolio.controller;

import com.kevinmazali.portfolio.model.ApiError;
import com.kevinmazali.portfolio.model.FeedbackRequest;
import com.kevinmazali.portfolio.model.FeedbackSubmission;
import com.kevinmazali.portfolio.repository.FeedbackRepository;
import com.kevinmazali.portfolio.util.InputValidator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public endpoint for visitors to submit feedback about the portfolio site.
 * Rate limiting is configured in {@link com.kevinmazali.portfolio.config.WebConfig}.
 */
@Slf4j
@RestController
@Tag(name = "Feedback", description = "Visitor feedback for improving the site")
public class FeedbackController {

    private final FeedbackRepository feedbackRepository;

    public FeedbackController(FeedbackRepository feedbackRepository) {
        this.feedbackRepository = feedbackRepository;
    }

    @Operation(summary = "Submit feedback", description = "Stores a feedback message from a site visitor. Rate limited (3 requests / 60s per IP).")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Feedback saved"),
        @ApiResponse(responseCode = "400", description = "Invalid input",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping("/feedback")
    public ResponseEntity<?> submitFeedback(@RequestBody FeedbackRequest request) {
        if (!InputValidator.isValidFeedbackMessage(request.message())) {
            return ResponseEntity.badRequest().body(new ApiError("Invalid or empty feedback message"));
        }

        if (!InputValidator.isValidOptionalEmail(request.replyEmail())) {
            return ResponseEntity.badRequest().body(new ApiError("Invalid e-mail format"));
        }

        FeedbackSubmission submission = new FeedbackSubmission();
        submission.setMessage(InputValidator.sanitizeString(request.message()));
        if (request.replyEmail() != null && !request.replyEmail().isBlank()) {
            submission.setReplyEmail(request.replyEmail().trim());
        }

        feedbackRepository.save(submission);
        log.info("Feedback submitted (id={})", submission.getId());

        return ResponseEntity.noContent().build();
    }
}
