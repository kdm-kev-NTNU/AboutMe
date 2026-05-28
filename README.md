# AboutMe

[![Tests](https://github.com/kevinmazali/AboutMe/actions/workflows/tests.yml/badge.svg)](https://github.com/kevinmazali/AboutMe/actions/workflows/tests.yml)

Personal portfolio (NO/EN) with document-grounded AI chat, OpenAI Realtime voice, and admin RAG tooling.

**Stack:** Vue 3 + Vite · Spring Boot 4 + Spring AI · PostgreSQL/pgvector · Docker

## Quick start

```bash
cp .env.example .env          # set OPENAI_API_KEY
cp .env.example .env.docker   # keep POSTGRES_PASSWORD lines for Compose
docker compose up -d db
cd backend && ./mvnw spring-boot:run
cd frontend/homepage && npm install && npm run dev
```

- App: http://localhost:5173 (API proxied to :8080)
- Swagger: http://localhost:8080/swagger-ui/index.html

**Windows:** `.\scripts\dev.ps1` starts Postgres and opens backend + frontend terminals.

## Docker (full stack)

```bash
docker compose --profile prod up -d --build
```

## Tests

```bash
cd backend && ./mvnw verify
cd frontend/homepage && npm ci && npm run test:unit && npm run lint:ci
```

## Configuration

All variables: [`.env.example`](.env.example). Defaults live in `backend/src/main/resources/application.yaml`.

RAG documents go in `backend/data/docs/` (gitignored). Regenerate the API client after backend changes:

```powershell
.\scripts\update-openapi.ps1
```

## Deploy

Production runs on Railway with `SPRING_PROFILES_ACTIVE=prod`. Set `PORTFOLIO_JWT_SECRET` and `AI_BUDGET_ANON_SALT` before going live.
