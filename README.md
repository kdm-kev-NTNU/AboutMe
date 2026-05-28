# AboutMe

[![Tests](https://github.com/kevinmazali/AboutMe/actions/workflows/tests.yml/badge.svg)](https://github.com/kevinmazali/AboutMe/actions/workflows/tests.yml)
[![Coverage](https://github.com/kevinmazali/AboutMe/actions/workflows/coverage.yml/badge.svg)](https://github.com/kevinmazali/AboutMe/actions/workflows/coverage.yml)

Portfolio web app with a document-grounded AI chat, live voice, and admin tooling for keeping the knowledge base current. The UI supports Norwegian and English.

Core stack:

- **Frontend:** Vue 3, TypeScript, Vite 8, Pinia, Vue Router, Tailwind 4, Reka UI, Orval, Cypress, Vitest.
- **Backend:** Java 21, Spring Boot 4, Spring AI 2 BOM, Spring Security, Spring Data JPA, SpringDoc OpenAPI.
- **AI/RAG:** OpenAI and optional Anthropic chat models, OpenAI embeddings, OpenAI Realtime WebRTC voice, optional ElevenLabs Conversational AI voice agents, optional ONNX cross-encoder reranking, OpenNLP-backed sanitization.
- **Data/ops:** PostgreSQL 17 with pgvector for relational data and embeddings, Docker Compose, Nginx, Actuator, Prometheus, optional PostHog frontend and server-side LLM analytics.

## Tech Stack Details

Architecture is split into three practical tracks: AI/RAG, backend services, and frontend/runtime.

- **AI and RAG:** Spring AI 2 connects OpenAI (and optional Anthropic) chat models, stores embeddings and chunks in PostgreSQL/pgvector, and supports optional ONNX reranking.
- **Voice:** OpenAI Realtime voice runs over WebRTC with server-side session and RAG lookup support. Whisper-based transcription is used where needed.
- **Backend services:** Spring Boot 4 on Java 21 with Spring Security, JPA, and Bucket4j. Admin tooling supports document ingestion, prompt versioning, and RAG experiments.
- **Frontend runtime:** Vue 3 + TypeScript + Vite 8 with Pinia, Tailwind 4, Reka UI, and Orval-generated API client.
- **Observability and delivery:** Actuator, Micrometer, Prometheus metrics, OpenTelemetry tracing, GitHub Actions CI, and Railway deployment with Docker images.

## Repository Layout

| Path | What |
|------|------|
| `backend/` | Spring Boot API for chat, Realtime voice, auth, admin tools, RAG, experiments, budgets, and observability |
| `frontend/homepage/` | Vue SPA. See [frontend/homepage/README.md](frontend/homepage/README.md) for scripts, routes, analytics, and Orval notes |
| `scripts/dev.ps1` | Windows helper that starts Docker infrastructure and opens backend + Vite terminals (recommended daily dev) |
| `docker-compose.yml` | PostgreSQL/pgvector, backend, and Nginx-hosted frontend (prod-like images) |
| `docker-compose.dev.yml` | Full stack in Docker with Vite HMR and Spring DevTools auto-reload |
| `.env.example` | Backend secrets template (copy to repo-root `.env`) |
| `frontend/homepage/.env.example` | Frontend `VITE_*` build-time template |
| `.github/workflows/` | Maven/frontend tests, Semgrep, and Docker image publishing |
| `scripts/` | Dev helpers — see [scripts/README.md](scripts/README.md) |

Seed documents for the vector store go in `backend/data/docs/` (gitignored). The backend also ships a classpath seed document for a minimal local knowledge base.

## Prerequisites

- Node version from `frontend/homepage/package.json`
- JDK 21 and the Maven wrapper in `backend/`
- Docker + Compose
- OpenAI API key for normal chat/RAG usage
- Optional Anthropic API key for Claude models
- Optional PostHog keys for analytics and LLM observability

## Run Locally

**Recommended for daily development:** [Hybrid Dev](#hybrid-dev-recommended) or [Full Stack Dev in Docker](#full-stack-dev-in-docker). Both auto-reload on code changes.

**Prod-like Docker** (`docker compose up -d --build`) does **not** auto-reload — run `--build` again after each code change, or use [Compose Watch](#compose-watch-prod-image-smoke-test) to rebuild images automatically (slower).

### Full Stack in Docker

Copy `.env.example` to `.env` at the repo root, set at least `OPENAI_API_KEY` and `OPENAI_CHAT_ENABLED=true`. For Docker Compose database credentials, copy `.env.docker.example` to `.env.docker` at the repo root, then run:

```bash
cp .env.docker.example .env.docker   # first time only (Windows: copy .env.docker.example .env.docker)
docker compose up -d --build
```

This builds frozen images (JAR + Nginx static assets). For day-to-day coding with live reload, use **Hybrid Dev** or **Full Stack Dev in Docker** below instead.

Typical URLs:

- App: [http://localhost:5173](http://localhost:5173), with `/api` proxied to the backend
- API: [http://localhost:8080](http://localhost:8080)
- Swagger UI: [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)
- PostgreSQL: host `5432`, DB `aboutme`, user/password `postgres`/`postgres`

The backend container mounts `./backend/data` read-only, so `file:./data/docs/` resolves to `backend/data/docs/` inside the container.

### Hybrid Dev (recommended)

Run only the database in Docker, then start backend and frontend on the host with auto-reload:

```bash
cp .env.docker.example .env.docker   # first time only
docker compose up -d db
```

```bash
cd backend
./mvnw spring-boot:run
```

```bash
cd frontend/homepage
npm install
npm run dev
```

**Auto-reload:**

- **Frontend:** Vite HMR — changes in `.vue`, `.ts`, and CSS appear in the browser almost instantly.
- **Backend:** `spring-boot-devtools` restarts the app when compiled classes change (~5–15 s). Changes to `pom.xml` or new dependencies still require stopping and re-running Maven.

On Windows, **`.\scripts\dev.ps1`** is the fastest path: it starts Postgres, waits for port `5432`, then opens backend and frontend in separate terminals. Copy `.env.example` to repo-root `.env` first.

Typical URLs: same as full stack — app [http://localhost:5173](http://localhost:5173), API [http://localhost:8080](http://localhost:8080).

### Full Stack Dev in Docker

For auto-reload without a local JDK or Node install:

```bash
cp .env.docker.example .env.docker   # first time only
docker compose -f docker-compose.dev.yml up
```

Copy `.env.example` to repo-root `.env` before starting. The dev compose file runs `mvn spring-boot:run` and `npm run dev` inside containers with source mounted from the repo.

**Auto-reload:** same as hybrid — Vite HMR for the frontend, Spring DevTools restart for the backend. ONNX rerank assets from the prod Dockerfile are not bundled; set `PORTFOLIO_RETRIEVAL_RERANK_ENABLED=false` in repo-root `.env` if reranking is not needed locally.

### Compose Watch (prod-image smoke test)

To verify prod images while editing code (rebuilds entire images on change — slower than dev mode):

```bash
docker compose watch
```

Or start the stack and watch in one step:

```bash
docker compose up --watch
```

Use this before deploy to confirm the production Dockerfile still builds and runs; use **Hybrid Dev** or **docker-compose.dev.yml** for everyday iteration.

## Configuration

Configuration defaults live in `backend/src/main/resources/application.yaml`.

| File | Purpose |
|------|---------|
| `.env` (repo root) | Backend secrets — copy from `.env.example` |
| `.env.docker` (repo root) | Docker Compose Postgres credentials — copy from `.env.docker.example` |
| `frontend/homepage/.env` | Frontend `VITE_*` build-time values — copy from `frontend/homepage/.env.example` |
| `.cursor/mcp.json` | Cursor MCP keys — copy from `.cursor/mcp.json.example` |

If you still have `backend/.env`, move its contents to repo-root `.env` and delete the old file.

For Cursor MCP setup, run `.\scripts\setup-cursor-mcp.ps1` (see [Scripts](#scripts)).

Common backend settings:

- `OPENAI_API_KEY` and `OPENAI_CHAT_ENABLED=true` for OpenAI chat, embeddings, transcription, and Realtime.
- `ANTHROPIC_API_KEY` to expose Anthropic models in `/chat/models`.
- `PORTFOLIO_REALTIME_ENABLED=true` to enable `/voice` and OpenAI Realtime WebRTC sessions.
- `ADMIN_BOOTSTRAP_USERNAME` / `ADMIN_BOOTSTRAP_PASSWORD` to create the first admin user.
- `POSTHOG_ENABLED`, `POSTHOG_API_KEY`, `POSTHOG_HOST` for server-side `$ai_generation` capture.
- `AI_BUDGET_ANON_SALT` — required in production (random string; must not be the default `portfolio-ai-budget`).
- `PORTFOLIO_JWT_SECRET` — required in production (at least 32 characters) for httpOnly admin session cookies.
- `SPRING_PROFILES_ACTIVE=prod` for production deployments (disables Swagger, reduces logging, enables schema validation).

Managed Postgres providers must allow the pgvector extension:

```sql
CREATE EXTENSION IF NOT EXISTS vector;
```

Spring AI can initialize the `vector_store` table, but the extension itself must exist first.

## Current Functionality

- Public portfolio pages: home, career, projects, individual project story, project/tech stack, feedback, privacy policy.
- Text chat: `/chat` sends document-grounded questions through `/ask`, with selectable allow-listed models from `/chat/models`.
- Live voice: `/voice` lists configured voice options from `/realtime/models`. OpenAI Realtime uses `/realtime/session` and `/realtime/lookup`; ElevenLabs Conversational AI uses `/realtime/elevenlabs/token`. To match OpenAI's local `lookup_kevin_info` behavior, configure equivalent knowledge/tools on the ElevenLabs agent.
- Feedback: `/feedback` posts visitor feedback to the backend with server-side length limits.
- Admin tools: protected routes for AI status/budget kill switch, document uploads and ingestion, chunk browsing/export, generated question suggestions, prompt versions/diffs, and RAG experiments.
- Observability: Actuator health/metrics/Prometheus, optional PostHog frontend analytics after consent, and optional PostHog server-side LLM events.

## API Overview

Public endpoints:

- `POST /ask`: document-grounded chat. Body: `{ "question": "...", "model": "<optional model id>" }`.
- `GET /chat/models`: configured and allow-listed chat models.
- `POST /feedback`: visitor feedback.
- `POST /transcribe`: multipart audio transcription.
- `GET /realtime/status`: whether Realtime voice is enabled for the current deployment.
- `GET /realtime/models`: configured voice provider/model options exposed to visitors.
- `POST /realtime/session`: WebRTC SDP exchange with OpenAI Realtime.
- `POST /realtime/elevenlabs/token`: browser-safe ElevenLabs WebRTC conversation token for a configured agent.
- `POST /realtime/lookup`: RAG lookup tool used by the Realtime session.
- `GET /health/vectorstore` and `GET /health/chroma`: vector store health. `chroma` is a compatibility alias.

Admin endpoints require `ADMIN` credentials:

- `/admin/tools/ai/**`: AI budget status, usage, and kill switch.
- `/admin/tools/documents/**`: upload, batch upload, ingest-by-path, list server files, list documents, chunks, export, collections, reseed, remote sync, delete, and question suggestions.
- `/admin/tools/prompt-versions/**`: list prompt names, history, create, activate, seed, delete variants, and diff.
- `/admin/tools/experiments/**`: config, datasets, generated datasets, model list, experiment runs, and run status.
- `/auth/login`: Basic-auth backed login used by the SPA admin session.

The full contract is available from Swagger UI at `/swagger-ui/index.html` when the API is running.

## AI Budgeting and Safety

The backend estimates LLM spend per configured model, enforces daily/monthly caps, includes a spike guard, and exposes an admin kill switch. Defaults live under `portfolio.ai.budget` in `application.yaml`.

Public AI endpoints are rate-limited with Bucket4j. Admin routes are protected by Spring Security. The sanitizer pipeline can redact likely sensitive values before prompts are sent to model providers.

Treat database backups as sensitive. Conversations, documents, chunks, embeddings, prompts, feedback, and experiment datasets may contain personal or project-specific information.

**Local secrets (never commit):**

- Run `.\scripts\setup-cursor-mcp.ps1` once for Cursor MCP (creates `.cursor/mcp.json` from the example, installs Railway + Docker MCP). Then set `ELEVENLABS_API_KEY` and `RAPIDCHART_API_TOKEN` in `.cursor/mcp.json`.
- Copy [`.env.docker.example`](.env.docker.example) to `.env.docker` (gitignored) for Docker Compose Postgres credentials (`POSTGRES_PASSWORD`, `SPRING_DATASOURCE_PASSWORD`).

## Document Pipeline and RAG

The knowledge base is curated through admin tooling:

1. Add documents through upload, batch upload, server-file ingestion, classpath reseed, or optional remote vector-store sync.
2. Parse and chunk content through Spring AI/Tika-backed ingestion.
3. Store chunks and embeddings in PostgreSQL/pgvector.
4. Review chunks, export data, generate starter questions, and run RAG experiments.
5. Tune prompts and active prompt versions through the admin prompt-version UI.

Optional ONNX reranking can be enabled with model/tokenizer paths when local rerank weights are available.

## Scripts

Helper scripts live in [scripts/](scripts/README.md): hybrid dev (`dev.ps1`), Cursor MCP setup, OpenAPI/Orval refresh (`update-openapi.ps1`), local CI parity (`ci-verify.*`), Railway vector sync, and voice smoke tests.

## Tests

Backend:

```bash
cd backend
./mvnw test
./mvnw verify
```

Frontend:

```bash
cd frontend/homepage
npm ci
npm run test:unit
npm run test:unit:coverage
npm run lint:ci
```

Coverage parity (local, same gates as CI):

```powershell
.\scripts\coverage-check.ps1
```

## Git Hooks (Lefthook)

For continuous local git-change validation before push:

1. Install Lefthook: https://github.com/evilmartians/lefthook
2. From repo root, run:

```bash
lefthook install
```

Configured hooks:

- `pre-commit`: frontend lint + type-check, backend compile
- `pre-push`: frontend unit coverage and backend `verify`

Refresh the Orval client after backend API changes: `.\scripts\update-openapi.ps1` (or `node scripts/patch-openapi-extensions.mjs` then `npm run api:generate` in `frontend/homepage`).

### Realtime Voice Verification

Default CI does not run a live OpenAI Realtime browser session because it requires a real API key, network access, browser media/WebRTC support, and `PORTFOLIO_REALTIME_ENABLED=true`.

Use this split when reporting release coverage:

- Default automated coverage: backend and frontend deterministic tests cover session JSON, SDP handling, tool lookup config, error mapping, budget checks, and the browser loop.
- Lightweight deployed smoke: `.\scripts\voice-live-smoke.ps1 -BaseUrl https://<host> -ExpectRealtimeEnabled $true`.
- Full live OpenAI browser smoke: from `frontend/homepage/`, run `npm run test:e2e:voice-live:openai -- --config baseUrl=http://localhost:5173`.

Release notes should say "Live OpenAI Realtime E2E passed" only after the full live smoke passes.

## Docker Images

CI can build multi-platform backend and frontend images and publish them to Docker Hub when the required repository variables and secrets are configured:

- Variables: `DOCKER_ACCOUNT`, `CLOUD_BUILDER_NAME`
- Secrets: `DOCKER_ACCESS_TOKEN` for image publishing, plus optional frontend `VITE_POSTHOG_*` build values

Use prebuilt images by replacing the `build:` blocks in `docker-compose.yml` with:

```yaml
image: <DOCKER_ACCOUNT>/aboutme-backend:latest
image: <DOCKER_ACCOUNT>/aboutme-frontend:latest
```

Keep backend runtime secrets in repo-root `.env` and frontend build-time values in `frontend/homepage/.env`.

## Security

**Authentication:** Admin tools use an httpOnly session cookie (JWT) set by `POST /auth/login`. The SPA stores only username and role in `sessionStorage` for UI routing—not passwords or Basic auth tokens.

**Public AI endpoints:** `POST /ask`, `/transcribe`, and `/realtime/*` are intentionally unauthenticated. Abuse is mitigated with per-IP rate limits (Bucket4j), per-identity AI budgets, and a global kill switch—not with login walls.

**Production checklist:**

- Set `SPRING_PROFILES_ACTIVE=prod`, `PORTFOLIO_JWT_SECRET`, and `AI_BUDGET_ANON_SALT` (see [`backend/railway.env.example`](backend/railway.env.example)).
- Use a strong `SPRING_DATASOURCE_PASSWORD` (not `postgres`), or link Railway Postgres (`PGHOST`, etc.).
- Clear `ADMIN_BOOTSTRAP_PASSWORD` after the first admin user exists.
- TLS is terminated at the hosting edge (Railway/CDN); nginx adds CSP and related headers on the SPA shell.

**Railway deploy:** Before push, run `.\scripts\railway-prod-deploy.ps1` (backup + staging audit). After deploy, run `.\scripts\railway-post-deploy-verify.ps1` (health, session cookie, CSRF). Database steps: [`scripts/db/README.md`](scripts/db/README.md). Flyway runs V1–V14 on startup; failures after V11+ require restoring the `pg_dump` snapshot.

**CI:** Maven/frontend tests and Semgrep SAST run on pull requests.

## Feedback

The project is under active development. Feedback and suggestions: [kevindmazali@gmail.com](mailto:kevindmazali@gmail.com)
