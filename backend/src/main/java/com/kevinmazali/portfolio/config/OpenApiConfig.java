package com.kevinmazali.portfolio.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    public static final String BASIC_AUTH_SCHEME = "basicAuth";

    @Bean
    public OpenAPI portfolioOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("AboutMe Portfolio API")
                .description("""
                    REST API for the personal portfolio: RAG chat (`POST /ask`), auth (`POST /auth/login`), \
                    Chroma health (`GET /health/chroma`), and admin document tools under `/admin/tools/documents` \
                    (HTTP Basic, ADMIN role).

                    Local SPA (Vite) calls these routes with an `/api` prefix via proxy, e.g. `http://localhost:5173/api/ask` \
                    maps to `POST /ask` on the backend at port 8080.""")
                .version("1.0.0"))
            .servers(List.of(
                new Server().url("http://localhost:8080").description("Direct backend (Swagger / curl)"),
                new Server().url("http://localhost:5173/api").description("Vue dev server (Vite proxy to backend)")
            ))
            .components(new Components()
                .addSecuritySchemes(BASIC_AUTH_SCHEME,
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("basic")
                        .description("Admin endpoints require an account with ROLE_ADMIN (same credentials as HTTP Basic).")));
    }
}
