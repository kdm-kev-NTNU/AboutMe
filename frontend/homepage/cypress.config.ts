import { defineConfig } from 'cypress'

export default defineConfig({
  e2e: {
    specPattern: 'cypress/e2e/**/*.{cy,spec}.{js,jsx,ts,tsx}',
    // Matches `npm run test:e2e` (vite preview on 4173). Dev server uses 5173 — override via CLI in test:e2e:dev.
    baseUrl: 'http://localhost:4173',
  },
})
