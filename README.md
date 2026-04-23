# AboutMe

Portfolio web app with a document-grounded AI chat (RAG). The UI supports Norwegian and English. Stack: **Vue 3**, **Spring Boot**, and **PostgreSQL with pgvector** (relational data and embeddings in one database). **Phoenix** can receive OTLP traces when OTLP export is enabled in configuration.

## Repository layout

| Path | What |
|------|------|
| `backend/` | Spring Boot API (RAG, auth, admin document pipeline) |
| `frontend/homepage/` | Vue 3 SPA: [frontend/homepage/README.md](frontend/homepage/README.md) for npm scripts and Orval |
| `scripts/dev.ps1` | Windows: Docker for infra, then opens API + Vite in separate terminals |
| `docker-compose.yml` | PostgreSQL (pgvector), Phoenix, backend, Nginx frontend |
| `.github/workflows/` | `tests.yml` (Maven verify + frontend unit coverage), `semgrep.yml` |

Seed documents for the vector store go in **`backend/data/docs/`** (gitignored). With hybrid dev, create that folder and add PDFs/DOCX/MD as needed.

## Prerequisites

- **Node**: see `engines` in [frontend/homepage/package.json](frontend/homepage/package.json)
- **JDK 21** and the Maven wrapper in `backend/` (`./mvnw` / `mvnw.cmd`)
- **Docker** + Compose
- **OpenAI API key** (embeddings + RAG; required for a normal setup)
- Optional: **Anthropic** key for Claude models in the chat UI

## Run locally

### Option A: full stack in Docker

From the repo root (set `OPENAI_API_KEY` in the environment or in a root `.env` used by Compose):

```bash
docker compose up -d --build
```

Typical URLs:

- App (Nginx): [http://localhost:5173](http://localhost:5173). `/api` proxied to the backend
- API: [http://localhost:8080](http://localhost:8080)
- PostgreSQL: host **5432**, DB `aboutme`, user/password `postgres`/`postgres`
- Phoenix UI: [http://localhost:6006](http://localhost:6006), OTLP gRPC **4317**

The backend image mounts `./backend/data` read-only; `file:./data/docs/` resolves to that path inside the container.

### Option B: hybrid (DB in Docker, app on the host)

```bash
docker compose up -d db phoenix
```

Then:

- Backend: from `backend/`, `./mvnw spring-boot:run` (Windows: `.\mvnw.cmd spring-boot:run`)
- Frontend: from `frontend/homepage/`, `npm install` and `npm run dev` → [http://localhost:5173](http://localhost:5173) (Vite proxies `/api` to port 8080)

On Windows you can use **`.\scripts\dev.ps1`** after copying `.env.example` to `.env`. It starts the same infra and launches API + Vite.

## Configuration

Copy [`.env.example`](.env.example) to **`.env`** at the repo root or under `backend/` (Spring loads it via `spring.config.import` in `application.yaml`). Never commit secrets.

**Usually required:** `OPENAI_API_KEY`, PostgreSQL user/password (defaults align with Compose: `postgres` / `postgres`), `PORT` for the API (default **8080**).

**Common optional:** `ANTHROPIC_API_KEY`, OTLP (`OTLP_EXPORT_ENABLED`, `PHOENIX_OTLP_ENDPOINT`), PostHog backend LLM capture (`POSTHOG_ENABLED`, `POSTHOG_API_KEY`, `POSTHOG_HOST`), `ADMIN_BOOTSTRAP_*` for first admin user, `PORTFOLIO_CHAT_DEFAULT_MODEL_ID`. Details and comments live in `.env.example`.

## Tests

- **Backend** (from `backend/`): `./mvnw test` or `./mvnw verify` (includes JaCoCo gate; open `target/site/jacoco/index.html` after verify)
- **Frontend** (from `frontend/homepage/`): `npm ci`, `npm run test:unit` or `npm run test:unit:coverage`

## API (short)

- **`POST /ask`**: JSON `{ "question": "...", "model": "<optional>" }` → `{ "answer": "..." }`. Rate limited; max question length enforced server-side.
- **`GET /chat/models`**: models available for configured providers
- **`POST /auth/login`**: JSON credentials; admin UI uses HTTP Basic on protected routes
- **`GET /health/chroma`** (alias) and **`GET /health/vectorstore`**: vector store / pgvector health for ops
- **Admin**: document upload, pipeline, chunks, prompts under `/admin/tools/**` (requires `ADMIN` role). Full contract: **Swagger UI** at `/swagger-ui/index.html` when the API runs (e.g. [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)).

## Security and privacy (summary)

Spring Security protects admin routes; public **`POST /ask`** is rate-limited. Production should use **`SPRING_PROFILES_ACTIVE=prod`**. Use TLS-backed JDBC URLs in production; treat database backups as sensitive if documents are personal.

**Managed Postgres (e.g. Railway):** ensure the **`vector`** extension exists once on the database (Railway **Data** → **Query** / `psql`): `CREATE EXTENSION IF NOT EXISTS vector;` Spring AI can create the `vector_store` table when `spring.ai.vectorstore.pgvector.initialize-schema` is true, but the extension must be allowed by the provider.

**Privacy:** conversations may be stored for troubleshooting and improvement. Do not send secrets. **RAG chunks and embeddings** live in PostgreSQL (`vector_store`); protect that data like any PII-bearing store. **AI output** can be wrong; verify anything important.

## Feedback

The project is under active development; some edge cases may remain. Feedback and suggestions: [kevindmazali@gmail.com](mailto:kevindmazali@gmail.com)
