# AboutMe (Kevin's AI)

## About

AboutMe is a personal portfolio with an AI chat that answers questions about Kevin based on his CV, coursework, and project documentation. The solution uses Retrieval‑Augmented Generation (RAG) to pull relevant context from documents, supports both Norwegian and English, and includes built‑in rate limiting and logging.

NB: Known issues

This project was built quickly as a personal initiative. Some edge cases and minor issues may exist. Feedback and improvement suggestions are very welcome. email: [kevindmazali@gmail.com](mailto:kevindmazali@gmail.com)

- Privacy: Conversations may be stored in the database to improve answers and stability. Do not share sensitive information.
- Vector store privacy: The vector store is encrypted at rest (AES‑GCM) so personal documents are not accessible without the decryption key.
- Hallucinations: AI answers can be incorrect. Verify important information.

## Repository structure

Monorepo layout:

- `backend/` — Spring Boot API (RAG, auth, document pipeline)
- `frontend/homepage/` — Vue 3 SPA (see [frontend/homepage/README.md](frontend/homepage/README.md) for IDE setup and npm scripts)
- `docker-compose.yml` — MySQL and ChromaDB for local development
- `.github/workflows/` — CI (e.g. Semgrep on `main`)

## Security

This section summarizes the main security mechanisms in the project:

- Authentication and authorization
  - Spring Security is enabled. The public endpoint `POST /ask` is open but rate‑limited.
  - Admin tools (`/admin/**`, including document ingest APIs) require an `ADMIN` user and HTTP Basic. The frontend stores a Base64‑encoded Basic token in `sessionStorage` after a successful `POST /auth/login` and sends it as `Authorization: Basic <token>` on protected calls.
- Rate limiting
  - Bucket4j enforces 5 requests per 10 seconds per user/IP for `POST /ask`.
- CORS
  - CORS uses an allowlist of origins (local development and `https://kevindmazali.me`). Credentials are allowed and standard headers (including `Authorization`) are permitted.
- Data privacy and encryption
  - The vector index can be encrypted at rest using AES‑GCM. Provide a Base64‑encoded 32‑byte key via `VECTORSTORE_ENC_KEY`.
  - Minimal request/response auditing is stored in MySQL for troubleshooting. Avoid sharing sensitive information.
- Input validation
  - Questions are validated and sanitized server‑side with a maximum length of 3000 characters.
- CI (static analysis)
  - A Semgrep workflow runs on pushes and pull requests to `main`, plus a weekly schedule. Uploading SARIF may require repository secrets (`SEMGREP_APP_TOKEN`, `SEMGREP_DEPLOYMENT_ID`) as configured in `.github/workflows/semgrep.yml`.

## Features

- AI chat about Kevin with RAG (loads context from documents like CV, courses, projects)
- Multilingual query understanding (NO/EN) with simple query expansion
- Vector index in **ChromaDB** (Docker) with optional AES‑GCM on chunk text before storage
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
- Spring AI (OpenAI Chat + Embeddings) and Tika document reader
- ChromaDB (Spring AI vector store) for embeddings and metadata
- Bucket4j for rate limiting

## Getting Started

### Prerequisites

- Node.js (v20+)
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

### 2) Start the database

```bash
docker-compose up -d
```

This starts **MySQL** on port **3307** (database `aboutme`, user `root/root`) and **ChromaDB** on port **8100** (see `docker-compose.yml`).

### 3) Set environment variables

The backend reads `application.yaml` and may load optional `.env` / `.env.properties` from the repository root or `backend/` (see `spring.config.import` in [backend/src/main/resources/application.yaml](backend/src/main/resources/application.yaml)). Do not commit secrets.

Required for a typical local run:

- `OPENAI_API_KEY`: Required for Chat/Embeddings
- `PORT`: HTTP port for the API (e.g. `8080`; there is no default in `application.yaml`)
- `DB_USERNAME` / `DB_PASSWORD`: MySQL credentials (with `docker-compose` as written, use `root` / `root`)

Optional:

- `VECTORSTORE_ENC_KEY`: Base64‑encoded 32‑byte key for AES‑256 GCM on vector chunk text. The app is configured with content encryption enabled (`encryptContent: true`); set this key so ingest/query use encryption consistently (without it, the server logs a warning and encryption may be skipped).
- `CHROMA_HTTP_HOST` / `CHROMA_PORT`: Overrides for the Chroma HTTP client (defaults `http://localhost` and `8100` for host‑mapped Docker). In the backend Docker image, defaults target the `chromadb` service on port `8000`.

Example (PowerShell):

```powershell
$env:OPENAI_API_KEY = "sk-..."
$env:PORT = "8080"
$env:DB_USERNAME = "root"
$env:DB_PASSWORD = "root"
$env:VECTORSTORE_ENC_KEY = "BASE64_32BYTE_KEY=="
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

- Serves on the port given by `PORT` (e.g. 8080)
- When the Chroma collection is **empty**, the app seeds documents from `classpath:/tmp/docs/` (or from `sfg.aiapp.documentsToLoad` if configured). Use **Admin → Internal tools** (`/admin/tools`) to upload additional files into ChromaDB.

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

Admin document pipeline (HTTP Basic, `ROLE_ADMIN`):

- `POST /admin/tools/documents/upload` — multipart field `file`, optional `title`, optional `force=true`
- `GET /admin/tools/documents` — aggregated documents in the active collection
- `DELETE /admin/tools/documents/{documentId}` — delete all chunks for a `document_id`
- `GET /admin/tools/documents/collections` — collection list and embedding count

## Credits

- Developed by Kevin Dennis Mazali (`kdm-kev-NTNU`)
- Document base: CV, course and project documents (classpath seed and/or admin ingest into ChromaDB)

