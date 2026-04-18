package com.kevinmazali.portfolio.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Successful login payload")
public record LoginResponse(
    @Schema(example = "admin")
    String username,
    @Schema(example = "ADMIN", allowableValues = {"USER", "ADMIN"})
    String role
) {
}
