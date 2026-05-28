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

RAG documents go in `backend/data/docs/` (gitignored). After backend API changes:

```bash
cd frontend/homepage && node scripts/patch-openapi-extensions.mjs
cd backend && ./mvnw test -Dtest=SecurityConfigIT#openApiSnapshotIncludesEndpointsAndOptionalExport -Dopenapi.export=true
cd frontend/homepage && npm run api:generate
```

Commit `openapi/openapi.json` and `src/api/generated/portfolio.ts` if they changed.

## Deploy

Production runs on Railway with `SPRING_PROFILES_ACTIVE=prod`. Set `PORTFOLIO_JWT_SECRET` and `AI_BUDGET_ANON_SALT` before going live.
