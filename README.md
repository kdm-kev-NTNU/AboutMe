# AboutMe

Portfolio web app with a document-grounded AI chat (RAG). The UI supports Norwegian and English. Stack: **Vue 3** (Vite 8), **Spring Boot 4.x** with **Spring AI 2.0.x** (BOM), and **PostgreSQL with pgvector** (relational data and embeddings in one database). **PostHog** can receive `$ai_generation` events from the backend when enabled, alongside consent-gated frontend analytics.

## Repository layout

| Path | What |
|------|------|
| `backend/` | Spring Boot 4.x API (Spring AI 2.x, RAG, auth, admin document pipeline) |
| `frontend/homepage/` | Vue 3 SPA: [frontend/homepage/README.md](frontend/homepage/README.md) for npm scripts and Orval |
| `scripts/dev.ps1` | Windows: Docker for infra, then opens API + Vite in separate terminals |
| `docker-compose.yml` | PostgreSQL (pgvector), backend, Nginx frontend |
| `.github/workflows/` | `tests.yml` (Maven verify + frontend unit coverage), `semgrep.yml`, `docker-publish.yml` (Docker Hub via Docker Build Cloud) |

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

The backend image mounts `./backend/data` read-only; `file:./data/docs/` resolves to that path inside the container.

### Option B: hybrid (DB in Docker, app on the host)

```bash
docker compose up -d db
```

Then:

- Backend: from `backend/`, `./mvnw spring-boot:run` (Windows: `.\mvnw.cmd spring-boot:run`)
- Frontend: from `frontend/homepage/`, `npm install` and `npm run dev` → [http://localhost:5173](http://localhost:5173) (Vite proxies `/api` to port 8080)

On Windows you can use **`.\scripts\dev.ps1`** after copying `.env.example` to `.env`. It starts the same infra and launches API + Vite.

### Container images (Docker Hub)

CI builds **multi-platform** (`linux/amd64`, `linux/arm64`) images with **Docker Build Cloud** and pushes them on pushes to `main`, semver tags `v*.*.*`, or manual **Actions → Docker publish → Run workflow**. Replace `<DOCKER_ACCOUNT>` with your Docker Hub username or org.

**Pull (private repos require `docker login`):**

```bash
docker login
docker pull <DOCKER_ACCOUNT>/aboutme-backend:latest
docker pull <DOCKER_ACCOUNT>/aboutme-frontend:latest
```

**Hub:** open [hub.docker.com](https://hub.docker.com/) → Repositories → `aboutme-backend` / `aboutme-frontend`.

**Use prebuilt images instead of local `build:`:** copy `docker-compose.yml` and replace the `backend` / `frontend` `build:` blocks with `image: <DOCKER_ACCOUNT>/aboutme-backend:latest` and `image: <DOCKER_ACCOUNT>/aboutme-frontend:latest` (keep `db` as-is). Pass runtime secrets the same way as local Compose (`backend/.env`, etc.).

**GitHub Actions setup:** Repository → **Settings** → **Secrets and variables** → **Actions**

- Variables: `DOCKER_ACCOUNT`, `CLOUD_BUILDER_NAME` (your Docker Build Cloud builder name).
- Secrets: `DOCKER_ACCESS_TOKEN` (Hub PAT with read/write), `VITE_POSTHOG_KEY`, `VITE_POSTHOG_HOST`, `VITE_POSTHOG_ENABLED` (optional; baked into the frontend at build time).

**Verify after merge:** Actions → **Docker publish** → run workflow on `main`; confirm both jobs succeed, then `docker pull` both `:latest` tags and smoke-test with Compose using `image:` overrides.

**If `docker pull` says `not found`:** the image is not on Hub under that name yet, or the tag differs. Check GitHub → **Actions** → **Docker publish** for a **green** run (merge the workflow file and set `DOCKER_ACCOUNT` / `CLOUD_BUILDER_NAME` / `DOCKER_ACCESS_TOKEN` first). Confirm the Hub username matches the repo variable (`kevindm1066/...` only works if `DOCKER_ACCOUNT` is `kevindm1066`). Until `:latest` exists, try `docker pull <DOCKER_ACCOUNT>/aboutme-backend:main` (branch tag from metadata-action).

## Configuration

Copy [`.env.example`](.env.example) to **`.env`** at the repo root or under `backend/` (Spring loads it via `spring.config.import` in `application.yaml`). Never commit secrets.

**Usually required:** `OPENAI_API_KEY`, PostgreSQL user/password (defaults align with Compose: `postgres` / `postgres`), `PORT` for the API (default **8080**).

**Common optional:** `ANTHROPIC_API_KEY`, PostHog backend LLM capture (`POSTHOG_ENABLED`, `POSTHOG_API_KEY`, `POSTHOG_HOST`), `ADMIN_BOOTSTRAP_*` for first admin user, `PORTFOLIO_CHAT_DEFAULT_MODEL_ID`, `SANITIZER_ENABLED`, `AI_BUDGET_ANON_SALT`. Details and comments live in `.env.example`.

**AI usage budgeting:** The backend tracks estimated LLM spend (per-model rates in [backend/src/main/resources/application.yaml](backend/src/main/resources/application.yaml) under `portfolio.ai.budget`) with daily/monthly caps for authenticated and anonymous users, a spike guard, and an optional kill switch. Override defaults with `PORTFOLIO_AI_BUDGET_*` style env vars (Spring relaxed binding) if you deploy with different limits; set `AI_BUDGET_ANON_SALT` to a stable secret in production so anonymous budget keys stay consistent across restarts.

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

## Knowledge pipeline (capture → curate → RAG)

Optional **conversational capture** (for example a voice UI such as ElevenLabs) is a **product choice** for richer spoken input. Academic RAG papers do not tell you to add that layer. **Raw exports or transcripts are not public or in the vector store by default.** The operator **structures, trims, redacts, and tunes** content into drafts, uploads through the **admin document pipeline**, then **re-embeds and checks retrieval** before curated chunks power document-grounded chat.

**What the papers cover:** corpus preparation, chunking, indexing, retrieval, and evaluation, not voice vendors. Primary references:

- [Wang et al., *Searching for Best Practices in Retrieval-Augmented Generation* (arXiv:2407.01219)](https://arxiv.org/abs/2407.01219): empirical RAG component choices and evaluation (EMNLP 2024).
- [Gao et al., *Retrieval-Augmented Generation for Large Language Models: A Survey* (arXiv:2312.10997)](https://arxiv.org/abs/2312.10997): Dec 2023 preprint; widely cited as “2024” in secondary sources.

**Human-grounded evaluation (also arXiv):**

- [Abbasiantaeb et al., *Conversational Gold*: human gold nuggets (arXiv:2503.09902)](https://arxiv.org/abs/2503.09902).
- [*Retrieval Augmented Generation Evaluation in the Era of Large Language Models: A Comprehensive Survey* (arXiv:2504.14891)](https://arxiv.org/abs/2504.14891).

**Practice guides (not peer-reviewed):** [AWS: securing the RAG ingestion pipeline](https://aws.amazon.com/blogs/security/securing-the-rag-ingestion-pipeline-filtering-mechanisms/), [Anyscale: RAG data ingestion strategies](https://docs.anyscale.com/rag/quality-improvement/data-ingestion-strategies).

**Privacy:** purpose limitation, consent, and minimisation for any capture channel follow applicable law (for example GDPR), not the RAG literature above. On-site copy also lives under **The project** → **Future work** in the Vue app (`frontend/homepage/src/views/ProjectPageView.vue` and `frontend/homepage/src/components/project/ProjectFutureWorkSection.vue`).

## Feedback

The project is under active development; some edge cases may remain. Feedback and suggestions: [kevindmazali@gmail.com](mailto:kevindmazali@gmail.com)
