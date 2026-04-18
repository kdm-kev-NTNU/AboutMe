# Homepage Frontend

This is the frontend application for Kevin's AboutMe portfolio website, built with Vue 3 and modern web technologies.

## Tech Stack & Libraries

### Core Framework
- **Vue 3** - Progressive JavaScript framework with Composition API
- **TypeScript** - Type-safe JavaScript development
- **Vite 7** - Fast build tool and development server
- **Vue Router 4** - Client-side routing
- **Pinia** - State management for Vue

### UI & Styling
- **Tailwind CSS 4** - Utility-first CSS framework
- **shadcn/ui** - High-quality, accessible component library (using Reka UI)
- **Reka UI** - Vue 3 component library (shadcn/ui implementation)
- **Lucide Vue Next** - Beautiful & consistent icon toolkit
- **Class Variance Authority (CVA)** - Component variant management
- **Tailwind Merge** - Utility for merging Tailwind classes
- **clsx** - Utility for constructing className strings

### Development Tools
- **Vitest** - Unit testing framework
- **Cypress** - End-to-end testing
- **ESLint** - Code linting
- **Prettier** - Code formatting
- **Vue DevTools** - Browser extension for debugging

### Additional Libraries
- **VueUse** - Collection of Vue composition utilities
- **Vue Markdown Render** - Markdown rendering for Vue
- **tw-animate-css** - Tailwind CSS animations

## Recommended IDE Setup

[VSCode](https://code.visualstudio.com/) + [Volar](https://marketplace.visualstudio.com/items?itemName=Vue.volar) (and disable Vetur).

## Type Support for `.vue` Imports in TS

TypeScript cannot handle type information for `.vue` imports by default, so we replace the `tsc` CLI with `vue-tsc` for type checking. In editors, we need [Volar](https://marketplace.visualstudio.com/items?itemName=Vue.volar) to make the TypeScript language service aware of `.vue` types.

## Customize configuration

See [Vite Configuration Reference](https://vite.dev/config/).

## Project Setup

```sh
npm install
```

### Compile and Hot-Reload for Development

```sh
npm run dev
```

### API client (Orval)

The TypeScript client is generated from the OpenAPI description in [`openapi/openapi.json`](openapi/openapi.json). HTTP calls go through [`src/api/orval-mutator.ts`](src/api/orval-mutator.ts), which prefixes paths with `/api` (matching the Vite dev proxy and production Nginx) and attaches `Authorization: Basic` when `sessionStorage` contains the auth payload from login.

When you change backend endpoints or DTOs:

1. Start the API on port **8080** (see repo root README).
2. Refresh the spec: `npm run api:pull` (optional env `OPENAPI_URL`, default `http://localhost:8080/v3/api-docs`).
3. Regenerate: `npm run api:generate`.

Generated files live under `src/api/generated/`; do not edit them by hand.

### Type-Check, Compile and Minify for Production

```sh
npm run build
```

### Run Unit Tests with [Vitest](https://vitest.dev/)

```sh
npm run test:unit
```

Coverage (HTML under `coverage/`):

```sh
npm run test:unit:coverage
```

### Run End-to-End Tests with [Cypress](https://www.cypress.io/)

```sh
npm run test:e2e:dev
```

This runs the end-to-end tests against the Vite development server.
It is much faster than the production build.

But it's still recommended to test the production build with `test:e2e` before deploying (e.g. in CI environments):

```sh
npm run build
npm run test:e2e
```

### Lint with [ESLint](https://eslint.org/)

```sh
npm run lint
```
