# Homepage (Vue 3)

SPA for the AboutMe portfolio: chat, projects, admin tools for documents and RAG. Repo-wide setup: [../../README.md](../../README.md).

## Prerequisites

[Node](https://nodejs.org/) version from `engines` in [package.json](package.json). Recommended editor: VS Code + [Volar](https://marketplace.visualstudio.com/items?itemName=Vue.volar) (disable Vetur if it is still enabled).

## Scripts

| Command | Purpose |
|---------|---------|
| `npm install` | Install dependencies |
| `npm run dev` | Vite dev server (default [http://localhost:5173](http://localhost:5173)), `/api` → backend :8080 |
| `npm run build` | Production build |
| `npm run test:unit` | Vitest unit tests |
| `npm run test:unit:coverage` | Unit tests + coverage (thresholds in `vitest.config.ts`) |
| `npm run test:e2e:dev` | Cypress against dev server |
| `npm run lint` | ESLint |

E2E against a production build: `npm run build` then `npm run test:e2e`.

## OpenAPI client (Orval)

Generated client lives under `src/api/generated/`. Do not edit by hand.

1. Run the backend on **8080** (see root README).
2. `npm run api:pull`: optional env `OPENAPI_URL` (default `http://localhost:8080/v3/api-docs`, see root `.env.example`).
3. `npm run api:generate`.

HTTP calls use [`src/api/orval-mutator.ts`](src/api/orval-mutator.ts): `/api` prefix and `Authorization: Basic` when the auth store has credentials after login.

## Configuration

Vite and tooling: [https://vite.dev/config/](https://vite.dev/config/). Type-check for `.vue` files uses `vue-tsc` rather than plain `tsc`.

### Analytics (PostHog)

- Anonymous, consented-only tracking mirrors the Krisefikser setup. Defaults to the EU host.
- Enable by setting `VITE_POSTHOG_ENABLED=true` and `VITE_POSTHOG_KEY=<phc_xxx>` (optional `VITE_POSTHOG_HOST`, default `https://eu.i.posthog.com`).
- Initialization is deferred until the user accepts analytics in the cookie banner/settings modal. Pageviews and events are ignored until consent is granted.
- Session replay is disabled by default. Do not identify users; only anonymous events are sent.
