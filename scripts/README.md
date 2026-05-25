# Scripts

Run from the repository root unless noted otherwise.

| Script | Purpose |
|--------|---------|
| `dev.ps1` | Windows hybrid dev: Docker Postgres + backend and frontend terminals |
| `setup-cursor-mcp.ps1` | One-time Cursor MCP setup (`.cursor/mcp.json`, Railway, Docker MCP) |
| `update-openapi.ps1` | Patch `openapi.json`, export from Springdoc (test), run `npm run api:generate` |
| `patch-openapi-extensions.mjs` | Merge experiment/realtime/synthesize paths into committed OpenAPI (used by update-openapi) |
| `ci-verify.ps1` / `ci-verify.sh` | Local parity with CI: Maven verify + frontend checks |
| `sync-from-railway.ps1` / `.sh` | Copy `vector_store` from Railway Postgres to local |
| `voice-live-smoke.ps1` | Deployed Realtime API smoke test |
| `validate-models.ps1` | Manual OpenAI/Anthropic model smoke check |
| `posthog-llm-evaluations-setup.ps1` | PostHog LLM evaluation setup (see `docs/posthog-llm-evaluations-setup.md`) |
