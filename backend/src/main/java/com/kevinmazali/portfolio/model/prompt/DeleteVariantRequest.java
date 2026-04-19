package com.kevinmazali.portfolio.model.prompt;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for deleting all versions of a prompt variant.
 */
public record DeleteVariantRequest(
    @NotBlank @Size(max = 128) String name,
    @Size(max = 8) String language,
    @Size(max = 32) String provider
) {}
