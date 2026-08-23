package com.kevinmazali.portfolio.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Successful login payload")
public record LoginResponse(
    @Schema(example = "admin")
    String username,
    @Schema(example = "ADMIN", allowableValues = {"USER", "ADMIN"})
    String role,
    @Schema(
        description = "Stable PostHog distinct id for ADMIN sessions; null for non-admin roles",
        example = "owner_7Kq3xYz9mNpL2vR8wQfJhT",
        nullable = true)
    String analyticsId
) {
}
