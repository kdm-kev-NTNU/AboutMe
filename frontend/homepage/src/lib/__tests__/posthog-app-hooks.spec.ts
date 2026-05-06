import { beforeEach, describe, expect, it, vi } from 'vitest'

const hasPageviewConsent = vi.fn(() => true)

vi.mock('../posthog-consent', () => ({
  hasPageviewConsent,
}))

vi.mock('posthog-js', () => ({
  default: {
    capture: vi.fn(),
  },
}))

describe('posthog-app-hooks', () => {
  beforeEach(() => {
    vi.resetModules()
    vi.clearAllMocks()
    hasPageviewConsent.mockReturnValue(true)
  })

  it('registers afterEach hook and captures pageview when pageview consent is granted', async () => {
    const posthog = (await import('posthog-js')).default
    const { setupPosthogAppHooks } = await import('../posthog-app-hooks')

    let handler: ((to: { path: string; name: unknown; query: Record<string, unknown> }) => void) | null =
      null
    const router = {
      afterEach: vi.fn(
        (cb: (to: { path: string; name: unknown; query: Record<string, unknown> }) => void) => {
          handler = cb
        },
      ),
    }
    const app = { config: { globalProperties: {} as Record<string, unknown> } }

    setupPosthogAppHooks(app as never, router as never)

    expect(router.afterEach).toHaveBeenCalledTimes(1)
    expect(app.config.globalProperties.$posthog).toBe(posthog)

    expect(handler).not.toBeNull()
    handler!({ path: '/chat', name: 'chat', query: { q: 'x' } })
    expect(posthog.capture).toHaveBeenCalledWith('$pageview', {
      path: '/chat',
      routeName: 'chat',
      query: { q: 'x' },
    })

    handler!({ path: '/anon', name: 123, query: {} })
    expect(posthog.capture).toHaveBeenLastCalledWith('$pageview', {
      path: '/anon',
      routeName: null,
      query: {},
    })
  })

  it('does not capture pageview when pageview consent is denied', async () => {
    hasPageviewConsent.mockReturnValue(false)
    const posthog = (await import('posthog-js')).default
    const { setupPosthogAppHooks } = await import('../posthog-app-hooks')

    let handler: ((to: { path: string; name: unknown; query: Record<string, unknown> }) => void) | null =
      null
    const router = {
      afterEach: vi.fn(
        (cb: (to: { path: string; name: unknown; query: Record<string, unknown> }) => void) => {
          handler = cb
        },
      ),
    }
    const app = { config: { globalProperties: {} as Record<string, unknown> } }

    setupPosthogAppHooks(app as never, router as never)
    handler!({ path: '/x', name: 'x', query: {} })
    expect(posthog.capture).not.toHaveBeenCalled()
  })

  it('does not register hooks twice', async () => {
    const { setupPosthogAppHooks } = await import('../posthog-app-hooks')

    const router = { afterEach: vi.fn() }
    const app = { config: { globalProperties: {} as Record<string, unknown> } }

    setupPosthogAppHooks(app as never, router as never)
    setupPosthogAppHooks(app as never, router as never)

    expect(router.afterEach).toHaveBeenCalledTimes(1)
  })
})
