import { beforeEach, describe, expect, it, vi } from 'vitest'

vi.mock('posthog-js', () => ({
  default: {
    init: vi.fn(),
  },
}))

describe('posthog-sdk', () => {
  beforeEach(() => {
    vi.resetModules()
    vi.clearAllMocks()
  })

  it('returns false when key is missing', async () => {
    const warnSpy = vi.spyOn(console, 'warn').mockImplementation(() => {})
    const { initializePosthogSdk, isPosthogSdkInitialized } = await import('../posthog-sdk')

    expect(initializePosthogSdk({ host: 'https://eu.i.posthog.com' })).toBe(false)
    expect(isPosthogSdkInitialized()).toBe(false)
    expect(warnSpy).toHaveBeenCalledTimes(1)
  })

  it('initializes once and returns true on subsequent calls', async () => {
    const posthog = (await import('posthog-js')).default
    const { initializePosthogSdk, isPosthogSdkInitialized } = await import('../posthog-sdk')

    expect(initializePosthogSdk({ key: ' phc_123 ', host: ' https://eu.i.posthog.com ' })).toBe(
      true,
    )
    expect(initializePosthogSdk({ key: 'phc_123', host: 'https://us.i.posthog.com' })).toBe(true)

    expect(posthog.init).toHaveBeenCalledTimes(1)
    expect(posthog.init).toHaveBeenCalledWith('phc_123', {
      api_host: 'https://eu.i.posthog.com',
      capture_pageview: false,
      autocapture: false,
      persistence: 'localStorage',
      disable_session_recording: true,
    })
    expect(isPosthogSdkInitialized()).toBe(true)
  })
})
