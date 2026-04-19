# AboutMe

Personal portfolio with an AI chat that answers from your own documents (RAG). Norwegian and English in the UI; stack is **Vue 3**, **Spring Boot**, **MySQL**, and **ChromaDB**. Optional OTLP traces to **Phoenix** when you enable it.

## Repository layout

| Path | What |
|------|------|
| `backend/` | Spring Boot API (RAG, auth, admin document pipeline) |
| `frontend/homepage/` | Vue 3 SPA — [frontend/homepage/README.md](frontend/homepage/README.md) for npm scripts and Orval |
| `scripts/dev.ps1` | Windows: Docker for infra, then opens API + Vite in separate terminals |
| `docker-compose.yml` | MySQL, ChromaDB, Phoenix, backend, Nginx frontend |
| `.github/workflows/` | `tests.yml` (Maven verify + frontend unit coverage), `semgrep.yml` |

Seed documents for Chroma go in **`backend/data/docs/`** (gitignored). With hybrid dev, create that folder and add PDFs/DOCX/MD as needed.

## Prerequisites

- **Node** — see `engines` in [frontend/homepage/package.json](frontend/homepage/package.json)
- **JDK 21** and the Maven wrapper in `backend/` (`./mvnw` / `mvnw.cmd`)
- **Docker** + Compose
- **OpenAI API key** (embeddings + RAG; required for a normal setup)
- Optional: **Anthropic** key for Claude models in the chat UI

## Run locally

### Option A — full stack in Docker

From the repo root (set `OPENAI_API_KEY` in the environment or in a root `.env` used by Compose):

```bash
docker compose up -d --build
```

Typical URLs:

- App (Nginx): [http://localhost:5173](http://localhost:5173) — `/api` proxied to the backend
- API: [http://localhost:8080](http://localhost:8080)
- MySQL: host **3307** → container 3306, DB `aboutme`, user/password `root`/`root`
- Chroma: host **8100**
- Phoenix UI: [http://localhost:6006](http://localhost:6006), OTLP gRPC **4317**

The backend image mounts `./backend/data` read-only; `file:./data/docs/` resolves to that path inside the container.

### Option B — hybrid (DBs in Docker, app on the host)

```bash
docker compose up -d db chromadb phoenix
```

Then:

- Backend: from `backend/`, `./mvnw spring-boot:run` (Windows: `.\mvnw.cmd spring-boot:run`)
- Frontend: from `frontend/homepage/`, `npm install` and `npm run dev` → [http://localhost:5173](http://localhost:5173) (Vite proxies `/api` to port 8080)

On Windows you can use **`.\scripts\dev.ps1`** after copying `.env.example` to `.env` — it starts the same infra and launches API + Vite.

## Configuration

Copy [`.env.example`](.env.example) to **`.env`** at the repo root or under `backend/` (Spring loads it via `spring.config.import` in `application.yaml`). Never commit secrets.

**Usually required:** `OPENAI_API_KEY`, MySQL user/password (defaults align with Compose: `root` / `root`), `PORT` for the API (default **8080**).

**Common optional:** `ANTHROPIC_API_KEY`, `CHROMA_*` / `CHROMA_ENABLED`, OTLP (`OTLP_EXPORT_ENABLED`, `PHOENIX_OTLP_ENDPOINT`), `ADMIN_BOOTSTRAP_*` for first admin user, `PORTFOLIO_CHAT_DEFAULT_MODEL_ID`. Details and comments live in `.env.example`.

## Tests

- **Backend** (from `backend/`): `./mvnw test` or `./mvnw verify` (includes JaCoCo gate; open `target/site/jacoco/index.html` after verify)
- **Frontend** (from `frontend/homepage/`): `npm ci`, `npm run test:unit` or `npm run test:unit:coverage`

## API (short)

- **`POST /ask`** — JSON `{ "question": "...", "model": "<optional>" }` → `{ "answer": "..." }`. Rate limited; max question length enforced server-side.
- **`GET /chat/models`** — models available for configured providers
- **`POST /auth/login`** — JSON credentials; admin UI uses HTTP Basic on protected routes
- **`GET /health/chroma`** — Chroma health for ops
- **Admin** — document upload, pipeline, chunks, prompts under `/admin/tools/**` (requires `ADMIN` role). Full contract: **Swagger UI** at `/swagger-ui/index.html` when the API runs (e.g. [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)).

## Security and privacy (summary)

Spring Security protects admin routes; public **`POST /ask`** is rate-limited. Production should use **`SPRING_PROFILES_ACTIVE=prod`**. Use TLS-backed JDBC URLs in production; treat DB and Chroma backups as sensitive if documents are personal.

**Privacy:** conversations may be stored for troubleshooting and improvement — do not send secrets. **RAG chunks and embeddings** live in Chroma; protect that data like any PII-bearing store. **AI output** can be wrong; verify anything important.

## Feedback

Built as a personal project; edge cases may exist. Suggestions welcome: [kevindmazali@gmail.com](mailto:kevindmazali@gmail.com)
