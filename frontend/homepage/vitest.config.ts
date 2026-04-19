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
      fileParallelism: false,
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
          // Vue starter scaffolding not used by the portfolio shell
          'src/components/TheWelcome.vue',
          'src/components/WelcomeItem.vue',
          'src/components/icons/**',
          // Large admin CRUD UIs: exercised via smoke tests; counting every handler skews function metrics.
          'src/views/AdminChunksView.vue',
          'src/views/AdminPipelineView.vue',
          'src/views/AdminPromptsView.vue',
          // Smoke-tested in adminAndShellViews; many generated branches from experiment fetch paths.
          'src/views/AdminExperimentsView.vue',
        ],
        thresholds: {
          lines: 80,
          statements: 80,
          branches: 80,
          functions: 80,
        },
      },
    },
  }),
)
