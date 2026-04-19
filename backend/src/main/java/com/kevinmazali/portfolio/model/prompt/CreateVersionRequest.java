package com.kevinmazali.portfolio.model.prompt;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for creating a new prompt version.
 */
public record CreateVersionRequest(
    @NotBlank @Size(max = 128) String name,
    @NotBlank String content,
    @Size(max = 8) String language,
    @Size(max = 32) String provider,
    String description
) {}
