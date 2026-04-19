package com.kevinmazali.portfolio.model.prompt;

import jakarta.validation.constraints.Positive;

/**
 * Request body for activating a prompt version by id.
 */
public record ActivateRequest(
    @Positive long id
) {}
