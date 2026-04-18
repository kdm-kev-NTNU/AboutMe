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
- `scripts/` — Windows helper [`dev.ps1`](scripts/dev.ps1) (Docker services + backend/frontend in separate terminals)
- `docker-compose.yml` — MySQL, ChromaDB, backend API, and frontend (Nginx) for local / full-stack runs
- `.env.example` — template for root or `backend/` `.env` (never commit secrets)
- `.github/workflows/` — CI (e.g. Semgrep on `main`)

## Security

This section summarizes the main security mechanisms in the project:

- Authentication and authorization
  - Spring Security is enabled. The public endpoint `POST /ask` is open but rate‑limited.
  - Admin tools (`/admin/**`, including document ingest APIs) require an `ADMIN` user and HTTP Basic. The frontend stores a Base64‑encoded Basic token in `sessionStorage` after a successful `POST /auth/login` and sends it as `Authorization: Basic <token>` on protected calls.
- Rate limiting
  - Bucket4j enforces 5 requests per 10 seconds per user/IP for `POST /ask`.
- CORS
  - CORS uses an allowlist of origins: `http://localhost:5173`, `http://localhost:4173` (Vite preview / Cypress), `https://kevindmazali.me`, and `https://www.kevindmazali.me`. Credentials are allowed and standard headers (including `Authorization`) are permitted.
- Data privacy
  - Minimal request/response auditing is stored in MySQL for troubleshooting. Avoid sharing sensitive information.
- Input validation
  - Questions are validated and sanitized server‑side with a maximum length of 3000 characters.
- CI (static analysis)
  - A Semgrep workflow runs on pushes and pull requests to `main`, plus a weekly schedule. Uploading SARIF may require repository secrets (`SEMGREP_APP_TOKEN`, `SEMGREP_DEPLOYMENT_ID`) as configured in `.github/workflows/semgrep.yml`.

## Features

- AI chat about Kevin with RAG (loads context from documents like CV, courses, projects)
- Multilingual query understanding (NO/EN) with simple query expansion
- Vector index in **ChromaDB** (Docker)
- Internal **Admin tools** page (`/admin/tools`) to upload and manage indexed documents
- API rate limiting (Bucket4j) to prevent abuse
- Logs requests and answers to MySQL (for insights and troubleshooting)
- Vue 3 frontend with language toggle, quick questions, responsive chat UI, and chat history
- Local development with Vite proxy to Spring Boot
- Production setup with an Nginx container for the frontend and a Docker image for the backend

## Available Pages

The frontend provides access to the following pages:

- **Home Page** (`/`) - Landing page with quick questions and language toggle
- **Chat Page** (`/chat`) - Interactive AI chat interface for asking questions about Kevin
- **Projects Page** (`/projects`) - Showcase of Kevin's projects and work
- **Work Experience Page** (`/work-experience`) - Professional experience and career history
- **Education Page** (`/education`) - Academic background and coursework
- **Chat history** (`/chat-history`) - History of past chat interactions
- **Privacy policy** (`/privacy-policy`) - Privacy information for the site
- **Internal tools** (`/admin/tools`, admin only) - Document ingest, ChromaDB status, delete by `document_id`

## Tech Stack

### Frontend

- Vue 3 (Composition API) + TypeScript
- Pinia for state management
- Vue Router
- Vite 7 (dev/build)
- Tailwind CSS 4 and Reka UI (shadcn-style components), Lucide icons
- Vitest (unit tests) and Cypress (E2E)
- Nginx (production serving in Docker)

### Backend

- Spring Boot 3.5.5 (Java 21)
- Spring Web, Spring Data JPA, Lombok
- MySQL
- Spring AI 1.0.1 (OpenAI Chat + Embeddings) and Tika document reader
- ChromaDB (Spring AI vector store) for embeddings and metadata
- Bucket4j for rate limiting

## Getting Started

**Windows shortcut:** From the repo root, run `.\scripts\dev.ps1` (after [Prerequisites](#prerequisites) and a root `.env` from `.env.example`) to run `docker compose up -d` and open the Spring Boot API and Vite dev server in separate windows; then open `http://localhost:5173`.

### Prerequisites

- Node.js — match [`engines` in `frontend/homepage/package.json`](frontend/homepage/package.json) (currently `^20.19.0` or `>=22.12.0`)
- npm (bundled with Node)
- JDK 21
- Maven
- Docker and Docker Compose
- OpenAI API key

### 1) Clone the repo

```bash
git clone https://github.com/kdm-kev-NTNU/AboutMe.git
cd AboutMe
```

### 2) Start Docker services

```bash
docker compose up -d --build
```

(If you only have the legacy CLI, the same file works as `docker-compose up -d --build`.)

Set `OPENAI_API_KEY` in your shell (or use a root `.env` file and `docker compose --env-file .env up -d --build`) before starting so the **backend** container can call OpenAI.

This starts:

- **MySQL** on host port **3307** (database `aboutme`, user `root/root`)
- **ChromaDB** on host port **8100**
- **Backend** (Spring Boot) on **8080**
- **Frontend** (Nginx + static build) on **5173** (proxies `/api/*` to the backend)

Chroma connectivity check (no auth): `GET http://localhost:8080/health/chroma`. Admin re-seed of seed documents: `POST http://localhost:8080/admin/tools/documents/reseed` (HTTP Basic, `ADMIN` user).

The backend image does not bundle seed documents. `docker-compose.yml` mounts `./backend/data` read-only into the container at `/app/data`, so `file:./data/docs/` resolves to `/app/data/docs/` (container `WORKDIR` is `/app`). Populate `backend/data/docs/` on the host before the first run if you want startup seeding in Docker.

### 3) Set environment variables

The backend reads `application.yaml` and, via `spring.config.import`, optionally loads `.env` / `.env.properties` from the process working directory (typically the repo root when using `scripts/dev.ps1`), the current directory, or `backend/` — see the first lines of [backend/src/main/resources/application.yaml](backend/src/main/resources/application.yaml). Copy [`.env.example`](.env.example) to `.env` at the repo root (or under `backend/`). Do not commit secrets.

Required for a typical local run:

- `OPENAI_API_KEY`: Required for Chat/Embeddings
- `PORT`: HTTP port for the API (e.g. `8080`; there is no default in `application.yaml`)
- `SPRING_DATASOURCE_USERNAME` / `SPRING_DATASOURCE_PASSWORD`: MySQL credentials (with `docker compose` as written, use `root` / `root`; defaults in `application.yaml` are also `root` if unset)

Optional:

- `SPRING_DATASOURCE_URL`: JDBC URL override (see [`.env.example`](.env.example); the `backend` service in Compose sets this to the `db` container)
- `CHROMA_COLLECTION`: Active Chroma collection name (default `portfolio-documents`, see `application.yaml`)
- `CHROMA_HTTP_HOST` / `CHROMA_PORT`: Overrides for the Chroma HTTP client (defaults in `application.yaml`: `http://localhost` and `8100`). The `backend` service in Compose sets `http://chromadb` and `8000`. On Railway, set these to your Chroma service private URL and port.
- `ADMIN_BOOTSTRAP_USERNAME` / `ADMIN_BOOTSTRAP_PASSWORD`: If both are set and that username is not already in the `users` table, the backend creates an `ADMIN` user on startup (BCrypt). Clear the password variable after first login on shared hosts.

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

### 4) Run the backend

From `backend/`:

```bash
./mvnw spring-boot:run
```

On Windows (same directory):

```powershell
.\mvnw.cmd spring-boot:run
```

- Serves on the port given by `PORT` (e.g. 8080)
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
  - Body: `{ "question": "..." }`
  - Response: `{ "answer": "..." }`
  - Validation: Max 3000 characters in `question`
  - Rate limit: 5 requests per 10 seconds per user/IP (HTTP 429 on violation)

The frontend calls this as `/api/ask` in dev/prod, where `/api` is proxied to the backend.

- `POST /auth/login` (public)
  - Body: `{ "username": "...", "password": "..." }`
  - Success: JSON including `username` and `role` (`ADMIN` or `USER`). The admin UI then stores a Base64-encoded HTTP Basic credential in `sessionStorage` for subsequent protected calls.

- `GET /health/chroma` (public)
  - JSON fields: `healthy` (boolean), `collectionName`, `embeddingCount` (nullable long), `message` (nullable string, e.g. error detail). Returns HTTP 503 when Chroma is unreachable or the collection is missing.

Admin document pipeline (HTTP Basic, `ROLE_ADMIN`). Multipart uploads are limited to **50 MB** per file and **55 MB** per request (`application.yaml`).

- `POST /admin/tools/documents/upload` — multipart field `file`, optional `title`, optional `force=true`. Allowed extensions: `pdf`, `docx`, `doc`, `txt`, `md`, `png`, `jpg`, `jpeg`, `gif`, `bmp`, `tiff`, `webp`, `svg`.
- `GET /admin/tools/documents` — aggregated documents in the active collection
- `DELETE /admin/tools/documents/{documentId}` — delete all chunks for a `document_id`
- `GET /admin/tools/documents/collections` — collection list and embedding count
- `POST /admin/tools/documents/reseed` — re-ingest seed documents from `documentsToLoadDir` (same sources as startup seed); returns a list of ingestion results

## Credits

- Developed by Kevin Dennis Mazali (`kdm-kev-NTNU`)
- Document base: CV, course and project documents (filesystem seed under `backend/data/docs/` and/or admin ingest into ChromaDB)

