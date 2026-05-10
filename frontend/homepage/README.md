# Homepage (Vue 3)

Vue SPA for the AboutMe portfolio. It includes public portfolio pages, document-grounded chat, live voice, chat history, feedback, privacy copy, and protected admin tools for RAG operations.

Repo-wide setup and backend configuration live in [../../README.md](../../README.md).

## Current Screens

- `/`: home page with quick chat entry points and live voice CTA.
- `/chat`: text chat backed by `/ask`, with selectable models from `/chat/models`.
- `/voice`: live voice over WebRTC when the backend enables Realtime — **OpenAI Realtime** and/or **ElevenLabs** depending on which models `/realtime/models` returns (user-selectable in the UI).
- `/chat-history`: stored conversations that can reopen in `/chat`.
- `/feedback` and `/privacy-policy`: visitor feedback and privacy information.
- `/career`, `/projects`, `/projects/heathen-army`, `/project`: portfolio, project story, bachelor/future-work, and tech stack content.
- `/admin/tools`, `/admin/pipeline`, `/admin/chunks`, `/admin/question-suggestions`, `/admin/prompts`, `/admin/experiments`: admin-only tools for AI status, document ingestion, chunk review, prompt versions, and RAG experiments.

## Prerequisites

Use the Node version from `engines` in [package.json](package.json). Recommended editor: VS Code with [Volar](https://marketplace.visualstudio.com/items?itemName=Vue.volar).

ElevenLabs voice sessions load the runtime package **`@elevenlabs/client`** (and types via **`@elevenlabs/types`**) when that provider is selected — no extra Vite env vars are required for the SDK itself; tokens come from the backend.

Portfolio listing and career content load from `src/types/*.json` files (`projects`, `education`, `workExperience`, `courses`) — those JSON files are **gitignored** in this repo. If they are missing, Vite falls back to the tracked `*.stub.json` files (see [src/types/FILES.example](src/types/FILES.example)).

## Scripts

| Command | Purpose |
|---------|---------|
| `npm install` | Install dependencies |
| `npm run dev` | Vite dev server, usually [http://localhost:5173](http://localhost:5173), with `/api` proxied to backend `:8080` |
| `npm run build` | Type-check and production build |
| `npm run preview` | Preview the production build |
| `npm run type-check` | `vue-tsc --build` |
| `npm run test:unit` | Vitest unit tests |
| `npm run test:unit:coverage` | Unit tests with coverage thresholds from `vitest.config.ts` |
| `npm run test:e2e:dev` | Cypress open mode against a Vite dev server on `:5173` |
| `npm run test:e2e` | Cypress run against `vite preview` |
| `npm run test:e2e:voice-live` | Realtime voice smoke without requiring a real OpenAI SDP session |
| `npm run test:e2e:voice-live:openai` | Full live OpenAI Realtime browser smoke |
| `npm run test:e2e:voice-live:elevenlabs` | Calls the backend to mint a real ElevenLabs conversation token (requires ElevenLabs configured on the server) |
| `npm run lint` | ESLint with fixes |
| `npm run lint:ci` | ESLint without fixes and with zero warnings |
| `npm run format` | Prettier over `src/` |

## OpenAPI Client (Orval)

Generated client code lives under `src/api/generated/`. Do not edit generated files by hand.

1. Run the backend on port `8080`.
2. Run `npm run api:pull` to fetch the OpenAPI document. `OPENAPI_URL` can override the default `http://localhost:8080/v3/api-docs`.
3. Run `npm run api:generate`.

HTTP calls use [src/api/orval-mutator.ts](src/api/orval-mutator.ts). It adds the `/api` prefix and attaches `Authorization: Basic ...` when the auth store has an admin session. Admin route guards restore credentials from `sessionStorage` before protected route navigation.

Voice helpers in [src/lib/realtime-voice.ts](src/lib/realtime-voice.ts) call realtime endpoints (including `POST /realtime/elevenlabs/token`, which `customFetch` prefixes to `/api/…` like other backend routes) with the shared fetch helper instead of the Orval client. After backend API changes, run `api:pull` then `api:generate` if you want those paths reflected in `src/api/generated/`.

## Configuration

Vite build-time values belong in `frontend/homepage/.env`.

PostHog browser analytics are opt-in and consent-gated:

- Set `VITE_POSTHOG_ENABLED=true` and `VITE_POSTHOG_KEY=<phc_xxx>`.
- Optional `VITE_POSTHOG_HOST` defaults to `https://eu.i.posthog.com`.
- Initialization waits until the visitor accepts analytics in the cookie banner/settings modal.
- Pageviews, custom events, error capture, feature flags, and optional session replay remain disabled until consent allows them.
- The app does not identify users; events are anonymous.

Server-side LLM analytics are configured in the Spring backend with `POSTHOG_ENABLED`, `POSTHOG_API_KEY`, and optional `POSTHOG_HOST`.

## Voice Testing

The voice UI is covered by deterministic unit tests and Cypress smoke scripts.

The full **OpenAI** smoke requires:

- A running app and backend.
- `PORTFOLIO_REALTIME_ENABLED=true`.
- A valid `OPENAI_API_KEY`.
- Browser media/WebRTC support.

Run it with:

```bash
npm run test:e2e:voice-live:openai -- --config baseUrl=http://localhost:5173
```

The **ElevenLabs** token smoke mints a real conversation JWT via the backend (it does not open a full browser WebRTC session). It requires:

- A running app and backend with Realtime enabled.
- `PORTFOLIO_REALTIME_ELEVENLABS_ENABLED=true`.
- `ELEVENLABS_API_KEY` and a valid `ELEVENLABS_AGENT_ID` (see repo root `.env.example`).

Run it with:

```bash
npm run test:e2e:voice-live:elevenlabs -- --config baseUrl=http://localhost:5173
```
