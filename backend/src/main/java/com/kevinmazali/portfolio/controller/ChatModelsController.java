package com.kevinmazali.portfolio.controller;

import com.kevinmazali.portfolio.model.ChatModelOption;
import com.kevinmazali.portfolio.service.ChatModelCatalog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Chat", description = "RAG-backed question answering")
public class ChatModelsController {

  private final ChatModelCatalog chatModelCatalog;

  @Operation(summary = "List chat models", description = "Returns allow-listed models for each provider that has an API key configured.")
  @ApiResponse(responseCode = "200", description = "Models available for the chat UI",
      content = @Content(array = @ArraySchema(schema = @Schema(implementation = ChatModelOption.class))))
  @GetMapping("/chat/models")
  public List<ChatModelOption> listModels() {
    return chatModelCatalog.listAvailableModels();
  }
}
