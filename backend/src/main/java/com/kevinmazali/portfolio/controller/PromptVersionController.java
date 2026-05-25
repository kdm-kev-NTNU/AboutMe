package com.kevinmazali.portfolio.controller;

import com.kevinmazali.portfolio.config.OpenApiConfig;
import com.kevinmazali.portfolio.model.prompt.ActivateRequest;
import com.kevinmazali.portfolio.model.prompt.CreateVersionRequest;
import com.kevinmazali.portfolio.model.prompt.DeleteVariantRequest;
import com.kevinmazali.portfolio.model.prompt.PromptDiffResponse;
import com.kevinmazali.portfolio.model.prompt.PromptNameEntry;
import com.kevinmazali.portfolio.model.prompt.PromptVersionResponse;
import com.kevinmazali.portfolio.service.PromptVersionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Admin API for prompt version management (list, create, activate, seed, delete, diff).
 * Mirrors Piscada's prompt-versions routes, adapted for Spring Boot + MySQL.
 */
@RestController
@RequestMapping("/admin/tools/prompt-versions")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Admin prompt versions", description = "Versioned prompt management (ADMIN + HTTP Basic)")
@SecurityRequirement(name = OpenApiConfig.BASIC_AUTH_SCHEME)
public class PromptVersionController {

    private final PromptVersionService promptVersionService;

    @Operation(summary = "List active prompt variants")
    @ApiResponse(responseCode = "200",
        content = @Content(array = @ArraySchema(schema = @Schema(implementation = PromptNameEntry.class))))
    @GetMapping("/names")
    public List<PromptNameEntry> listNames() {
        return promptVersionService.listActiveNames();
    }

    @Operation(summary = "Version history for a prompt variant")
    @ApiResponse(responseCode = "200",
        content = @Content(array = @ArraySchema(schema = @Schema(implementation = PromptVersionResponse.class))))
    @GetMapping("/history")
    public List<PromptVersionResponse> history(
        @Parameter(description = "Prompt name", required = true) @RequestParam String name,
        @Parameter(description = "Language code (nullable)") @RequestParam(required = false) String language,
        @Parameter(description = "Provider (nullable)") @RequestParam(required = false) String provider
    ) {
        return promptVersionService.listHistory(name, language, provider);
    }

    @Operation(summary = "Create a new prompt version")
    @ApiResponses({
        @ApiResponse(responseCode = "200",
            content = @Content(schema = @Schema(implementation = PromptVersionResponse.class))),
        @ApiResponse(responseCode = "400", description = "Validation error")
    })
    @PostMapping("/create")
    public PromptVersionResponse create(@Valid @RequestBody CreateVersionRequest request) {
        return promptVersionService.createVersion(
            request.name().trim(),
            request.content(),
            request.language(),
            request.provider(),
            request.description()
        );
    }

    @Operation(summary = "Activate a prompt version by id",
        description = "Deactivates all sibling versions for the same variant in one transaction.")
    @ApiResponses({
        @ApiResponse(responseCode = "200",
            content = @Content(schema = @Schema(implementation = PromptVersionResponse.class))),
        @ApiResponse(responseCode = "400", description = "Version id not found")
    })
    @PostMapping("/activate")
    public ResponseEntity<?> activate(@Valid @RequestBody ActivateRequest request) {
        try {
            PromptVersionResponse activated = promptVersionService.activateVersion(request.id());
            return ResponseEntity.ok(activated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @Operation(summary = "Seed prompt versions from classpath templates",
        description = "Creates version 1 (active) for each known classpath template that has no DB rows yet.")
    @ApiResponse(responseCode = "200")
    @PostMapping("/seed")
    public Map<String, Object> seed() {
        return promptVersionService.seedFromClasspath();
    }

    @Operation(summary = "Delete all versions of a prompt variant")
    @ApiResponse(responseCode = "200")
    @DeleteMapping("/variant")
    public Map<String, Object> deleteVariant(@Valid @RequestBody DeleteVariantRequest request) {
        int deleted = promptVersionService.deleteVariant(
            request.name().trim(), request.language(), request.provider());
        promptVersionService.invalidateCache(request.name().trim());
        return Map.of("success", true, "deleted", deleted);
    }

    @Operation(summary = "Diff active DB version vs classpath fallback")
    @ApiResponse(responseCode = "200",
        content = @Content(schema = @Schema(implementation = PromptDiffResponse.class)))
    @GetMapping("/diff")
    public PromptDiffResponse diff(
        @Parameter(description = "Prompt name", required = true) @RequestParam String name,
        @Parameter(description = "Language code (nullable)") @RequestParam(required = false) String language,
        @Parameter(description = "Provider (nullable)") @RequestParam(required = false) String provider
    ) {
        return promptVersionService.diff(name, language, provider);
    }
}
