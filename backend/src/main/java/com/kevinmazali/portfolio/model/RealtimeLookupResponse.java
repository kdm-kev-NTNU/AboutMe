package com.kevinmazali.portfolio.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Lookup response for the Realtime voice assistant knowledge tool")
public record RealtimeLookupResponse(
    @Schema(description = "True when at least one snippet was found")
    boolean found,
    @Schema(description = "At most five concise snippets")
    List<RealtimeLookupSnippet> snippets,
    @Schema(description = "Match confidence: high, low, or none")
    String confidence) {

  public RealtimeLookupResponse {
    if (confidence == null || confidence.isBlank()) {
      confidence = found ? "high" : "none";
    }
  }
}
