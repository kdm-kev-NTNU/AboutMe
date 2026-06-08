# AboutMe

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

Docker dev profile (one command; **use port 5174**, not 5173):

```powershell
.\scripts\dev-up.ps1
```

- App: http://127.0.0.1:5174 (API proxied to :8080)
- If http://localhost:5173 shows another project, that is expected — AboutMe is on **5174**
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

Two Railway services share this monorepo: **AboutMe** (backend, `backend/railway.toml`) and **resilient-emotion** (frontend, `frontend/homepage/railway.toml`). Each has `rootDirectory` set in the dashboard. Backend uses `watchPatterns = ["backend/**"]` (repo-root paths). Frontend omits watch patterns so every push rebuilds. A push that only touches frontend files will show **SKIPPED** on the backend service — that is expected. If a service should have deployed but shows SKIPPED, redeploy manually:

```powershell
railway redeploy --service resilient-emotion   # frontend
railway redeploy --service AboutMe             # backend
```
