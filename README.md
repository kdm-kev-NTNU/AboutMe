# AboutMe

Portfolio web app with a document-grounded AI chat, live voice, and admin tooling for keeping the knowledge base current. The UI supports Norwegian and English.

Core stack:

- **Frontend:** Vue 3, TypeScript, Vite 8, Pinia, Vue Router, Tailwind 4, Reka UI, Orval, Cypress, Vitest.
- **Backend:** Java 21, Spring Boot 4, Spring AI 2 BOM, Spring Security, Spring Data JPA, SpringDoc OpenAPI.
- **AI/RAG:** OpenAI and optional Anthropic chat models, OpenAI embeddings, OpenAI Realtime WebRTC voice, optional ElevenLabs Conversational AI voice agents, optional ONNX cross-encoder reranking, OpenNLP-backed sanitization.
- **Data/ops:** PostgreSQL 17 with pgvector for relational data and embeddings, Docker Compose, Nginx, Actuator, Prometheus, optional PostHog frontend and server-side LLM analytics.

## Repository Layout

| Path | What |
|------|------|
| `backend/` | Spring Boot API for chat, Realtime voice, auth, admin tools, RAG, experiments, budgets, and observability |
| `frontend/homepage/` | Vue SPA. See [frontend/homepage/README.md](frontend/homepage/README.md) for scripts, routes, analytics, and Orval notes |
| `scripts/dev.ps1` | Windows helper that starts Docker infrastructure and opens backend + Vite terminals (recommended daily dev) |
| `docker-compose.yml` | PostgreSQL/pgvector, backend, and Nginx-hosted frontend (prod-like images) |
| `docker-compose.dev.yml` | Full stack in Docker with Vite HMR and Spring DevTools auto-reload |
| `.env.example` | Documented runtime configuration for backend secrets and optional integrations |
| `.github/workflows/` | Maven/frontend tests, GitGuardian secret scanning, Semgrep, and Docker image publishing |

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

Copy `.env.example` to `backend/.env`, set at least `OPENAI_API_KEY` and `OPENAI_CHAT_ENABLED=true`, then run:

```bash
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

On Windows, **`.\scripts\dev.ps1`** is the fastest path: it starts Postgres, waits for port `5432`, then opens backend and frontend in separate terminals. Copy `.env.example` to `backend/.env` (or repo-root `.env` as documented in `application.yaml`) first.

Typical URLs: same as full stack — app [http://localhost:5173](http://localhost:5173), API [http://localhost:8080](http://localhost:8080).

### Full Stack Dev in Docker

For auto-reload without a local JDK or Node install:

```bash
docker compose -f docker-compose.dev.yml up
```

Copy `.env.example` to `backend/.env` before starting. The dev compose file runs `mvn spring-boot:run` and `npm run dev` inside containers with source mounted from the repo.

**Auto-reload:** same as hybrid — Vite HMR for the frontend, Spring DevTools restart for the backend. ONNX rerank assets from the prod Dockerfile are not bundled; set `PORTFOLIO_RETRIEVAL_RERANK_ENABLED=false` in `backend/.env` if reranking is not needed locally.

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

Configuration defaults live in `backend/src/main/resources/application.yaml`. Copy `.env.example` to `backend/.env` for local secrets. Frontend build-time `VITE_*` values belong in `frontend/homepage/.env`.

Common backend settings:

- `OPENAI_API_KEY` and `OPENAI_CHAT_ENABLED=true` for OpenAI chat, embeddings, transcription, and Realtime.
- `ANTHROPIC_API_KEY` to expose Anthropic models in `/chat/models`.
- `PORTFOLIO_REALTIME_ENABLED=true` to enable `/voice` and OpenAI Realtime WebRTC sessions.
- `ADMIN_BOOTSTRAP_USERNAME` / `ADMIN_BOOTSTRAP_PASSWORD` to create the first admin user.
- `POSTHOG_ENABLED`, `POSTHOG_API_KEY`, `POSTHOG_HOST` for server-side `$ai_generation` capture.
- `AI_BUDGET_ANON_SALT` for stable anonymous AI budget identities in production.
- `SPRING_PROFILES_ACTIVE=prod` for production deployments.

Managed Postgres providers must allow the pgvector extension:

```sql
CREATE EXTENSION IF NOT EXISTS vector;
```

Spring AI can initialize the `vector_store` table, but the extension itself must exist first.

## Current Functionality

- Public portfolio pages: home, career, projects, individual project story, project/tech stack, feedback, privacy policy.
- Text chat: `/chat` sends document-grounded questions through `/ask`, with selectable allow-listed models from `/chat/models`.
- Chat history: `/chat-history` lists stored conversations and can reopen a conversation in `/chat`.
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

CI uses GitGuardian as the PR/push secret scanning gate and Semgrep for SAST. Add `GITGUARDIAN_API_KEY` to GitHub Actions secrets before enabling required checks; the GitGuardian workflow fails hard when the token is missing.

## Document Pipeline and RAG

The knowledge base is curated through admin tooling:

1. Add documents through upload, batch upload, server-file ingestion, classpath reseed, or optional remote vector-store sync.
2. Parse and chunk content through Spring AI/Tika-backed ingestion.
3. Store chunks and embeddings in PostgreSQL/pgvector.
4. Review chunks, export data, generate starter questions, and run RAG experiments.
5. Tune prompts and active prompt versions through the admin prompt-version UI.

Optional ONNX reranking can be enabled with model/tokenizer paths when local rerank weights are available.

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
- Secrets: `GITGUARDIAN_API_KEY` for PR/push secret scanning, `DOCKER_ACCESS_TOKEN` for image publishing, plus optional frontend `VITE_POSTHOG_*` build values

Use prebuilt images by replacing the `build:` blocks in `docker-compose.yml` with:

```yaml
image: <DOCKER_ACCOUNT>/aboutme-backend:latest
image: <DOCKER_ACCOUNT>/aboutme-frontend:latest
```

Keep runtime secrets in `backend/.env` and frontend build-time values in `frontend/homepage/.env`.

## Feedback

The project is under active development. Feedback and suggestions: [kevindmazali@gmail.com](mailto:kevindmazali@gmail.com)
