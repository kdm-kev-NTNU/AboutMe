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
          // Reka UI slot wrappers; no app logic and not targeted by unit tests
          'src/components/ui/card/**',
          'src/components/ui/dialog/**',
        ],
        // Vitest 4 switched to AST-based v8 analysis; numbers shifted down vs the old v8-to-istanbul pipeline.
        // Branch % stays lowest on template-heavy Vue (many ternaries). If the branch gate fails, add tests
        // in the worst-covered files (often views) rather than micro-testing defensive catch paths elsewhere.
        thresholds: {
          lines: 84,
          statements: 82,
          branches: 70,
          functions: 75,
        },
      },
    },
  }),
)
