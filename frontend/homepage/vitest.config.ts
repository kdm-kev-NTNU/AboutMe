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
          'src/router/index.ts',
          // Vue starter scaffolding not used by the portfolio shell
          'src/components/TheWelcome.vue',
          'src/components/WelcomeItem.vue',
          'src/components/icons/**',
        ],
        // Line/function gates match product code; global branch % stays lower on template-heavy Vue (many ternaries).
        thresholds: {
          lines: 85,
          statements: 85,
          branches: 76,
          functions: 84,
        },
      },
    },
  }),
)
