package com.kevinmazali.portfolio.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Getter;

import java.time.OffsetDateTime;

/**
 * Stores visitor-submitted feedback intended for improving the portfolio site.
 */
@Getter
@Entity
@Table(name = "feedback_submission")
public class FeedbackSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(length = 320)
    private String replyEmail;

    @Column(nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    public void setMessage(String message) {
        this.message = message;
    }

    public void setReplyEmail(String replyEmail) {
        this.replyEmail = replyEmail;
    }
}
