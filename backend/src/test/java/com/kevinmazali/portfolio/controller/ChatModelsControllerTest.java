package com.kevinmazali.portfolio.controller;

import com.kevinmazali.portfolio.MvcTestUserDetailsConfig;
import com.kevinmazali.portfolio.config.AskRateLimitProperties;
import com.kevinmazali.portfolio.config.DatasetGenerateRateLimitProperties;
import com.kevinmazali.portfolio.config.ExperimentRunRateLimitProperties;
import com.kevinmazali.portfolio.config.SecurityConfig;
import com.kevinmazali.portfolio.config.WebConfig;
import com.kevinmazali.portfolio.model.ChatModelOption;
import com.kevinmazali.portfolio.model.chat.ChatProvider;
import com.kevinmazali.portfolio.model.chat.ModelTag;
import com.kevinmazali.portfolio.service.ChatModelCatalog;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.EnumSet;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ChatModelsController.class)
@EnableConfigurationProperties({
  AskRateLimitProperties.class,
  ExperimentRunRateLimitProperties.class,
  DatasetGenerateRateLimitProperties.class
})
@Import({ WebConfig.class, SecurityConfig.class, MvcTestUserDetailsConfig.class })
class ChatModelsControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private ChatModelCatalog chatModelCatalog;

  @Test
  void listModelsReturnsJsonArray() throws Exception {
    when(chatModelCatalog.listAvailableModels()).thenReturn(List.of(
        new ChatModelOption("gpt-5.4-mini", ChatProvider.OPENAI, "GPT-5.4 mini", EnumSet.of(ModelTag.FAST)),
        new ChatModelOption("gpt-5.4", ChatProvider.OPENAI, "GPT-5.4", EnumSet.of(ModelTag.REASONING))
    ));

    mockMvc.perform(get("/chat/models"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value("gpt-5.4-mini"))
        .andExpect(jsonPath("$[0].provider").value("OPENAI"))
        .andExpect(jsonPath("$[0].label").value("GPT-5.4 mini"))
        .andExpect(jsonPath("$[0].tags[0]").value("FAST"))
        .andExpect(jsonPath("$[1].tags[0]").value("REASONING"));
  }
}
