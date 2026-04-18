import { defineConfig } from 'orval'

export default defineConfig({
  portfolio: {
    input: './openapi/openapi.json',
    output: {
      mode: 'single',
      target: './src/api/generated/portfolio.ts',
      client: 'fetch',
      httpClient: 'fetch',
      baseUrl: '',
      override: {
        mutator: {
          path: './src/api/orval-mutator.ts',
          name: 'customFetch',
        },
      },
    },
  },
})
