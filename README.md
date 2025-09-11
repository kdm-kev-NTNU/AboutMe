# AboutMe (Kevin's AI)

## About

AboutMe is a personal portfolio with an AI chat that answers questions about Kevin based on his CV, coursework, and project documentation. The solution uses Retrieval‑Augmented Generation (RAG) to pull relevant context from documents, supports both Norwegian and English, and includes built‑in rate limiting and logging.

NB: Known issues

This project was built quickly as a personal initiative. Some edge cases and minor issues may exist. Feedback and improvement suggestions are very welcome. email: kevindmazali@gmail.com

- Privacy: Conversations may be stored in the database to improve answers and stability. Do not share sensitive information.
- Vector store privacy: The vector store is encrypted at rest (AES‑GCM) so personal documents are not accessible without the decryption key.
- Hallucinations: AI answers can be incorrect. Verify important information.

## Features

- AI chat about Kevin with RAG (loads context from documents like CV, courses, projects)
- Multilingual query understanding (NO/EN) with simple query expansion
- Vector index stored as JSON and can be encrypted (AES‑GCM) with a key
- API rate limiting (Bucket4j) to prevent abuse
- Logs requests and answers to MySQL (for insights and troubleshooting)
- Vue 3 frontend with language toggle, quick questions, and responsive chat UI
- Local development with Vite proxy to Spring Boot
- Production setup with an Nginx container for the frontend and a Docker image for the backend

## Tech Stack

### Frontend

- Vue 3 (Composition API) + TypeScript
- Pinia for state management
- Vue Router
- Vite 7 (dev/build)
- Vitest (unit tests) and Cypress (E2E)
- Nginx (production serving in Docker)

### Backend

- Spring Boot 3.5 (Java 21)
- Spring Web, Spring Data JPA, Lombok
- MySQL
- Spring AI (OpenAI Chat + Embeddings) and Tika document reader
- SimpleVectorStore (JSON) for the vector index
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

This starts a MySQL instance on port 3307 with database `aboutme` and user `root/root` (see `docker-compose.yml`).

### 3) Set environment variables

The backend uses the following variables:

- `OPENAI_API_KEY`: Required for Chat/Embeddings
- `VECTORSTORE_ENC_KEY`: Optional Base64‑encoded 32‑byte key for encrypting/decrypting vector content (AES‑256 GCM). When set, content is encrypted on build and decrypted on query.

Example (PowerShell):

```powershell
$env:OPENAI_API_KEY = "sk-..."
$env:VECTORSTORE_ENC_KEY = "BASE64_32BYTE_KEY=="
```

### 4) Run the backend

```bash
cd backend
./mvnw spring-boot:run
```

- Starts on port 8080 (can be overridden via `PORT`)
- On first run it will build the vector index from `backend/vectordatabase/` or from `classpath:/tmp/docs/` (see `application.yaml` and `VectorStoreConfig`).

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

## Credits

- Developed by Kevin Dennis Mazali (`kdm-kev-NTNU`)
- Document base: CV, course and project documents in `backend/vectordatabase/`
