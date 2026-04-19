# AboutMe (Kevin's AI)

## About

AboutMe is a personal portfolio with an AI chat that answers questions about Kevin based on his CV, coursework, and project documentation. The solution uses Retrieval‑Augmented Generation (RAG) to pull relevant context from documents, supports both Norwegian and English, and includes built‑in rate limiting and logging.

NB: Known issues

This project was built quickly as a personal initiative. Some edge cases and minor issues may exist. Feedback and improvement suggestions are very welcome. email: [kevindmazali@gmail.com](mailto:kevindmazali@gmail.com)

- Privacy: Conversations may be stored in the database to improve answers and stability. Do not share sensitive information.
- Vector store privacy: Chunk text and embeddings live in ChromaDB; treat the database and backups as sensitive if documents are personal.
- Hallucinations: AI answers can be incorrect. Verify important information.

## Repository structure

Monorepo layout:

- `backend/` — Spring Boot API (RAG, auth, document pipeline). Seed files for Chroma ingestion live in `backend/data/docs/` (directory is gitignored; create it locally with your PDFs/DOCX/MD).
- `frontend/homepage/` — Vue 3 SPA (see [frontend/homepage/README.md](frontend/homepage/README.md) for IDE setup and npm scripts)
- `scripts/` — Windows helper [`dev.ps1`](scripts/dev.ps1) (Docker infra: MySQL, ChromaDB, Phoenix; then backend and Vite in separate terminals)
- `docker-compose.yml` — MySQL, ChromaDB, Arize Phoenix (OTLP + UI), backend API, and frontend (Nginx) for full-stack Docker runs
- `.env.example` — template for root or `backend/` `.env` (never commit secrets)
- `.github/workflows/` — CI: [tests.yml](.github/workflows/tests.yml) (backend `./mvnw verify`, frontend `npm run test:unit:coverage` on pushes to `main`/`master` and on pull requests) and [semgrep.yml](.github/workflows/semgrep.yml) (Semgrep on `main`, weekly schedule)

## Security

This section summarizes the main security mechanisms in the project:

- Authentication and authorization
  - Spring Security is enabled. The public endpoint `POST /ask` is open but rate‑limited.
  - Admin tools (`/admin/**`, including document ingest APIs) require an `ADMIN` user and HTTP Basic. The frontend stores a Base64‑encoded Basic token in `sessionStorage` after a successful `POST /auth/login` and sends it as `Authorization: Basic <token>` on protected calls.
- Rate limiting
  - Bucket4j enforces 5 requests per 10 seconds for `POST /ask`, keyed by **client IP** or by **authenticated principal** when present (so logged-in users are not pooled with anonymous traffic). Can be disabled with `portfolio.ask-rate-limit.enabled=false` (see [WebConfig.java](backend/src/main/java/com/kevinmazali/portfolio/config/WebConfig.java)).
- CORS
  - CORS uses an allowlist of origins: `http://localhost:5173`, `http://localhost:4173` (Vite preview / Cypress), `https://kevindmazali.me`, and `https://www.kevindmazali.me`. Credentials are allowed and standard headers (including `Authorization`) are permitted.
- Data privacy
  - Minimal request/response auditing is stored in MySQL for troubleshooting. Avoid sharing sensitive information.
- Input validation
  - Questions are validated and sanitized server‑side with a maximum length of 3000 characters.
- CI (static analysis)
  - Semgrep runs on `main` (see [CI/CD](#cicd)); uploading SARIF uses repository secrets (`SEMGREP_APP_TOKEN`, `SEMGREP_DEPLOYMENT_ID`) in [semgrep.yml](.github/workflows/semgrep.yml).

## Features

- AI chat about Kevin with RAG (loads context from documents like CV, courses, projects)
- Multilingual query understanding (NO/EN) with simple query expansion
- Vector index in **ChromaDB** (Docker)
- Internal **admin** pages (`/admin/tools`, pipeline, chunks, prompts) for documents, ingestion pipeline, chunk inspection, and versioned RAG prompts
- Optional **Anthropic Claude** chat models when `ANTHROPIC_API_KEY` is set (embeddings still use OpenAI)
- **Opt-in OTLP tracing** to Phoenix (`management.otlp` in Spring Boot): set `OTLP_EXPORT_ENABLED=true`, `PHOENIX_OTLP_ENDPOINT` (gRPC), and optionally `PHOENIX_API_KEY` (Bearer) — see [application.yaml](backend/src/main/resources/application.yaml) and [`.env.example`](.env.example)
- API rate limiting (Bucket4j) to prevent abuse
- Logs requests and answers to MySQL (for insights and troubleshooting)
- Vue 3 frontend with language toggle, quick questions, responsive chat UI, and chat history
- Local development with Vite proxy to Spring Boot
- Production setup with an Nginx container for the frontend and a Docker image for the backend
- **OpenAPI / Swagger UI** on the backend (`http://localhost:8080/swagger-ui/index.html` when the API runs on 8080; OpenAPI JSON at `http://localhost:8080/v3/api-docs`). The Vue app can sync types and clients with [Orval](https://orval.dev/) — see [frontend/homepage/README.md](frontend/homepage/README.md) (`npm run api:pull`, `npm run api:generate`). With Nginx, the same UI is available under `/api/swagger-ui/index.html` on the site origin.

## Available Pages

The frontend provides access to the following pages:

- **Home Page** (`/`) - Landing page with quick questions and language toggle
- **Chat Page** (`/chat`) - Interactive AI chat interface for asking questions about Kevin
- **Projects Page** (`/projects`) - Showcase of Kevin's projects and work
- **Work Experience Page** (`/work-experience`) - Professional experience and career history
- **Education Page** (`/education`) - Academic background and coursework
- **Tech stack** (`/tech-stack`) - Technologies and tools used in the portfolio
- **Chat history** (`/chat-history`) - History of past chat interactions
- **Privacy policy** (`/privacy-policy`) - Privacy information for the site
- **Admin — tools** (`/admin/tools`, admin only) - Document upload, collections, Chroma health, delete by `document_id`
- **Admin — pipeline** (`/admin/pipeline`, admin only) - Batch uploads, ingest-by-path, reseed, and related pipeline actions
- **Admin — chunks** (`/admin/chunks`, admin only) - Inspect stored chunks and previews per document
- **Admin — prompts** (`/admin/prompts`, admin only) - Create, activate, diff, and manage versioned RAG prompt templates (backed by `/admin/tools/prompt-versions/*` APIs)

## Tech Stack

### Frontend

- Vue 3 (Composition API) + TypeScript
- Pinia 3 for state management
- Vue Router
- Vite 7 (dev/build)
- Tailwind CSS 4 and Reka UI (shadcn-style components), Lucide icons
- Vitest (unit tests) and Cypress (E2E)
- Nginx (production serving in Docker)

### Backend

- Spring Boot 3.5.5 (Java 21)
- Spring Web, Spring Data JPA, Spring Boot Actuator, Lombok
- Springdoc OpenAPI 2.8.9 (Swagger UI + `/v3/api-docs`)
- MySQL
- Spring AI 1.0.1 (OpenAI chat + embeddings, optional Anthropic chat) and Tika document reader
- ChromaDB (Spring AI vector store) for embeddings and metadata
- Spring Boot OTLP tracing to an OTLP gRPC endpoint (local default: Phoenix on port 4317)
- Bucket4j for rate limiting

## Getting Started

**Windows shortcut:** From the repo root, run `.\scripts\dev.ps1` (after [Prerequisites](#prerequisites) and a root `.env` from `.env.example`) to run `docker compose up -d db chromadb phoenix` (MySQL, ChromaDB, Phoenix only — leaves **8080** / **5173** free) and open the Spring Boot API and Vite dev server in separate windows; then open `http://localhost:5173`.

### Prerequisites

- Node.js — match [`engines` in `frontend/homepage/package.json`](frontend/homepage/package.json) (currently `^20.19.0` or `>=22.12.0`)
- npm (bundled with Node)
- JDK 21
- Maven (optional if you use the Maven Wrapper in `backend/`: `./mvnw` / `mvnw.cmd`)
- Docker and Docker Compose
- **OpenAI API key** (required for embeddings/RAG and for OpenAI chat models)
- Optional: **Anthropic API key** if you want Claude models in the chat UI

### 1) Clone the repo

```bash
git clone https://github.com/kdm-kev-NTNU/AboutMe.git
cd AboutMe
```

### 2) Start Docker services

**Full stack in Docker** (API + SPA + data stores + observability):

```bash
docker compose up -d --build
```

(If you only have the legacy CLI, the same file works as `docker-compose up -d --build`.)

Set `OPENAI_API_KEY` in your shell (or use a root `.env` file and `docker compose --env-file .env up -d --build`) before starting so the **backend** container can call OpenAI.

This starts:

- **MySQL** on host port **3307** (database `aboutme`, user `root/root`)
- **ChromaDB** on host port **8100**
- **Phoenix** (Arize Phoenix): UI on **6006** (`http://localhost:6006`), OTLP gRPC on **4317** (the `backend` service sets `PHOENIX_OTLP_ENDPOINT=http://phoenix:4317` so traces go to Phoenix)
- **Backend** (Spring Boot) on **8080**
- **Frontend** (Nginx + static build) on **5173** (proxies `/api/*` to the backend)

**Hybrid development** (Spring Boot + Vite on the host, databases in Docker): use [`scripts/dev.ps1`](scripts/dev.ps1) or run only infra:

```bash
docker compose up -d db chromadb phoenix
```

Then start the API and frontend locally (sections **4) Run the backend** and **5) Run the frontend** below). For OTLP from a local JVM, set `OTLP_EXPORT_ENABLED=true` and point `PHOENIX_OTLP_ENDPOINT` at `http://localhost:4317` (see [`.env.example`](.env.example)).

Chroma connectivity check (no auth): `GET http://localhost:8080/health/chroma`. Admin re-seed of seed documents: `POST http://localhost:8080/admin/tools/documents/reseed` (HTTP Basic, `ADMIN` user).

The backend image does not bundle seed documents. `docker-compose.yml` mounts `./backend/data` read-only into the container at `/app/data`, so `file:./data/docs/` resolves to `/app/data/docs/` (container `WORKDIR` is `/app`). Populate `backend/data/docs/` on the host before the first run if you want startup seeding in Docker.

### 3) Set environment variables

The backend reads `application.yaml` and, via `spring.config.import`, optionally loads `.env` / `.env.properties` from the process working directory (typically the repo root when using `scripts/dev.ps1`), the current directory, or `backend/` — see the first lines of [backend/src/main/resources/application.yaml](backend/src/main/resources/application.yaml). Copy [`.env.example`](.env.example) to `.env` at the repo root (or under `backend/`). Do not commit secrets.

Required for a typical local run:

- `OPENAI_API_KEY`: Required for **embeddings/RAG** and for **OpenAI** chat models
- `PORT`: HTTP port for the API (defaults to **8080** in `application.yaml` if unset)
- `SPRING_DATASOURCE_USERNAME` / `SPRING_DATASOURCE_PASSWORD`: MySQL credentials (with `docker compose` as written, use `root` / `root`; defaults in `application.yaml` are also `root` if unset)

Optional:

- `ANTHROPIC_API_KEY`: Enables **Anthropic** chat models in the UI; embeddings still use OpenAI
- `SPRING_DATASOURCE_URL`: JDBC URL override (see [`.env.example`](.env.example); the `backend` service in Compose sets this to the `db` container)
- `CHROMA_COLLECTION`: Active Chroma collection name (default `portfolio-documents`, see `application.yaml`)
- `CHROMA_HTTP_HOST` / `CHROMA_PORT`: Overrides for the Chroma HTTP client (defaults in `application.yaml`: `http://localhost` and `8100`). The `backend` service in Compose sets `http://chromadb` and `8000`. On Railway, set these to your Chroma service private URL and port.
- `CHROMA_ENABLED`: Set to `false` when Chroma is not deployed; chat can run without RAG and admin document APIs return **501** (see `portfolio.chroma.enabled` in [application.yaml](backend/src/main/resources/application.yaml))
- `OTLP_EXPORT_ENABLED`: Set to `true` to export traces to OTLP (default `false`)
- `PHOENIX_OTLP_ENDPOINT`: OTLP gRPC tracing endpoint (default in `application.yaml`: `http://localhost:4317`; in full-stack Compose the backend uses `http://phoenix:4317`)
- `PHOENIX_API_KEY`: Optional Bearer token for OTLP export headers when your collector requires it
- `PORTFOLIO_CHAT_DEFAULT_MODEL_ID`: Default chat model when `POST /ask` omits `model` (Spring property `portfolio.chat.default-model-id`, default `gpt-4o-mini`)
- `ADMIN_BOOTSTRAP_USERNAME` / `ADMIN_BOOTSTRAP_PASSWORD`: If both are set and that username is not already in the `users` table, the backend creates an `ADMIN` user on startup (BCrypt). Clear the password variable after first login on shared hosts.
- `OPENAPI_URL`: Used by the frontend’s `npm run api:pull` (default `http://localhost:8080/v3/api-docs` per [`.env.example`](.env.example))

Example (PowerShell):

```powershell
$env:OPENAI_API_KEY = "sk-..."
$env:PORT = "8080"
$env:SPRING_DATASOURCE_USERNAME = "root"
$env:SPRING_DATASOURCE_PASSWORD = "root"
```

### Tests and coverage (backend)

From `backend/`, run unit and slice tests (no Docker required for the default suite):

```bash
./mvnw test
```

After tests, generate a JaCoCo HTML report with:

```bash
./mvnw verify
```

Open `backend/target/site/jacoco/index.html` in a browser. For a report without running `verify`, use `./mvnw test org.jacoco:jacoco-maven-plugin:report`.

`./mvnw verify` also runs a JaCoCo **line coverage check** on the backend bundle (minimum **22%** as configured in [backend/pom.xml](backend/pom.xml)); the job fails if coverage drops below that threshold.

### Frontend tests

From `frontend/homepage/`:

```bash
npm ci
npm run test:unit
```

Coverage report with Vitest thresholds (see [frontend/homepage/vitest.config.ts](frontend/homepage/vitest.config.ts)):

```bash
npm run test:unit:coverage
```

### 4) Run the backend

From `backend/`:

```bash
./mvnw spring-boot:run
```

On Windows (same directory):

```powershell
.\mvnw.cmd spring-boot:run
```

- Serves on the port given by `PORT` (default **8080** if unset)
- When the Chroma collection is **empty**, the app seeds documents from `sfg.aiapp.documentsToLoadDir` (default `file:./data/docs/`, i.e. `backend/data/docs/` when the process working directory is `backend/`). That directory is **gitignored**; copy your seed PDFs/DOCX/MD there locally. You can instead set `sfg.aiapp.documents-to-load` to a list of Spring `Resource` locations. With `sfg.aiapp.force-reindex: true`, startup seeding re-ingests seed files even when the collection already has embeddings (replacing chunks per content hash). Use **Admin → Internal tools** (`/admin/tools`) to upload additional files into ChromaDB.

### 5) Run the frontend

```bash
cd ../frontend/homepage
npm install
npm run dev
```

- Dev server usually runs at `http://localhost:5173`
- The proxy forwards requests from `/api/*` to `http://localhost:8080/*`

### 6) Open the application

Go to `http://localhost:5173` and try the quick questions or ask your own in the chat.

## API

- `POST /ask`
  - Body: `{ "question": "...", "model": "<optional>" }` — if `model` is omitted, the server uses `portfolio.chat.default-model-id` (default `gpt-4o-mini`)
  - Allowed `model` values (must match exactly): `gpt-4o-mini`, `gpt-4o`, `claude-sonnet-4-20250514`, `claude-3-5-haiku-20241022`. Unknown ids or models whose provider has no API key configured return **400**.
  - Response: `{ "answer": "..." }`
  - Validation: Max 3000 characters in `question`
  - Rate limit: 5 requests per 10 seconds per user/IP (HTTP 429 on violation)
  - **503** when the LLM provider or Chroma is unavailable

The frontend calls this as `/api/ask` in dev/prod, where `/api` is proxied to the backend.

- `GET /chat/models` (public)
  - Returns allow-listed chat models for providers that have API keys configured (drives the chat UI model picker).

- `POST /auth/login` (public)
  - Body: `{ "username": "...", "password": "..." }`
  - Success: JSON including `username` and `role` (`ADMIN` or `USER`). The admin UI then stores a Base64-encoded HTTP Basic credential in `sessionStorage` for subsequent protected calls.

- `GET /health/chroma` (public)
  - JSON fields: `healthy` (boolean), `collectionName`, `embeddingCount` (nullable long), `message` (nullable string, e.g. error detail). Returns **501** when Chroma is disabled (`CHROMA_ENABLED=false` / `portfolio.chroma.enabled=false`), **503** when Chroma is unreachable, the `ChromaApi` bean is missing while Chroma is enabled, or the collection is missing.

Admin document pipeline (HTTP Basic, `ROLE_ADMIN`). Multipart uploads are limited to **50 MB** per file and **55 MB** per request (`application.yaml`).

- `POST /admin/tools/documents/upload` — multipart field `file`, optional `title`, optional `force=true`. Allowed extensions: `pdf`, `docx`, `doc`, `txt`, `md`, `png`, `jpg`, `jpeg`, `gif`, `bmp`, `tiff`, `webp`, `svg`.
- `POST /admin/tools/documents/upload/batch` — multipart field `files` (array), optional `force=true`; returns per-file ingestion results
- `POST /admin/tools/documents/ingest-by-path` — JSON body `{ "paths": ["..."], "force": <optional boolean> }`; each path is **relative to** `sfg.aiapp.documentsToLoadDir` (no `..` or absolute segments)
- `GET /admin/tools/documents/files` — relative paths with supported extensions under `sfg.aiapp.documentsToLoadDir` (Spring `file:` URL base)
- `GET /admin/tools/documents` — aggregated documents in the active collection
- `GET /admin/tools/documents/chunks` — paginated chunk inspection (`documentId`, `limit` max 200, `offset` query params)
- `DELETE /admin/tools/documents/{documentId}` — delete all chunks for a `document_id`
- `GET /admin/tools/documents/collections` — collection list and embedding count
- `POST /admin/tools/documents/reseed` — re-ingest using the **same seed resolution as startup** (`sfg.aiapp.documents-to-load` if set, otherwise glob under `sfg.aiapp.documentsToLoadDir`); replaces chunks per content hash; returns ingestion results

Admin **prompt versions** (HTTP Basic, `ROLE_ADMIN`): REST under `/admin/tools/prompt-versions/*` (list names, history, create, activate, seed, delete variant, diff). Prefer [Swagger UI](http://localhost:8080/swagger-ui/index.html) for the full contract.

## CI/CD

GitHub Actions workflows in [`.github/workflows/`](.github/workflows/):

- **[tests.yml](.github/workflows/tests.yml)** — on push to `main` or `master` and on all pull requests: runs `backend/./mvnw -B verify` (tests + JaCoCo report and line coverage gate) and, in `frontend/homepage`, `npm ci` plus `npm run test:unit:coverage` (Node 22).
- **[semgrep.yml](.github/workflows/semgrep.yml)** — on push and pull requests targeting `main`, plus a weekly schedule: Semgrep scan and SARIF upload. Uses `SEMGREP_APP_TOKEN` and `SEMGREP_DEPLOYMENT_ID` when publishing to Semgrep Cloud.

## Credits

- Developed by Kevin Dennis Mazali (`kdm-kev-NTNU`)
- Document base: CV, course and project documents (filesystem seed under `backend/data/docs/` and/or admin ingest into ChromaDB)

