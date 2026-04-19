import { fileURLToPath } from 'node:url'
import type { UserConfig } from 'vite'
import { mergeConfig, defineConfig, configDefaults } from 'vitest/config'
import viteConfig from './vite.config'

const ciTestOptions =
  process.env.CI === 'true'
    ? {
        reporters: ['default', 'junit'],
        outputFile: { junit: 'test-results/vitest-junit.xml' },
      }
    : {}

export default mergeConfig(
  viteConfig as UserConfig,
  defineConfig({
    test: {
      environment: 'jsdom',
      exclude: [...configDefaults.exclude, 'e2e/**'],
      root: fileURLToPath(new URL('./', import.meta.url)),
      ...ciTestOptions,
      coverage: {
        provider: 'v8',
        reporter: ['text', 'html'],
        include: ['src/**/*.{ts,vue}'],
        exclude: [
          'src/main.ts',
          '**/*.d.ts',
          '**/types/**',
          'src/api/generated/**',
        ],
        thresholds: {
          lines: 18,
          statements: 18,
          branches: 68,
          functions: 48,
        },
      },
    },
  }),
)
