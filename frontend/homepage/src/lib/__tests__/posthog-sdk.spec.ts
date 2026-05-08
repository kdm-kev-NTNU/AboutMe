import { beforeEach, describe, expect, it, vi } from 'vitest'

const capture = vi.fn()
const getFeatureFlag = vi.fn()
const onFeatureFlags = vi.fn((cb: () => void) => {
  cb()
})
const register = vi.fn()

vi.mock('posthog-js', () => ({
  default: {
    init: vi.fn(),
    capture,
    getFeatureFlag,
    onFeatureFlags,
    register,
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
      disable_session_recording: false,
      session_recording: {
        maskAllInputs: true,
        maskTextSelector: '[data-ph-mask]',
      },
    })
    expect(isPosthogSdkInitialized()).toBe(true)
  })

  it('passes disable_session_recording when disableSessionRecording is true', async () => {
    const posthog = (await import('posthog-js')).default
    vi.resetModules()
    const { initializePosthogSdk } = await import('../posthog-sdk')

    expect(
      initializePosthogSdk({
        key: 'phc_x',
        host: 'https://eu.i.posthog.com',
        disableSessionRecording: true,
      }),
    ).toBe(true)

    expect(posthog.init).toHaveBeenCalledWith('phc_x', expect.objectContaining({
      disable_session_recording: true,
    }))
  })

  it('captureAnalyticsEvent is a no-op before init', async () => {
    const { captureAnalyticsEvent } = await import('../posthog-sdk')
    captureAnalyticsEvent('portfolio_chat_ask_submitted', { x: 1 })
    expect(capture).not.toHaveBeenCalled()
  })

  it('captureAnalyticsEvent forwards to posthog after init', async () => {
    const { initializePosthogSdk, captureAnalyticsEvent } = await import('../posthog-sdk')
    initializePosthogSdk({ key: 'phc_abc', host: 'https://eu.i.posthog.com' })
    captureAnalyticsEvent('portfolio_chat_answer_received', { http_status: 200 })
    expect(capture).toHaveBeenCalledWith('portfolio_chat_answer_received', { http_status: 200 })
  })

  it('getFeatureFlag returns undefined before init', async () => {
    const { getFeatureFlag: gf } = await import('../posthog-sdk')
    expect(gf('aboutme_chat_reply_experiment')).toBeUndefined()
  })

  it('getFeatureFlag delegates after init', async () => {
    const posthog = (await import('posthog-js')).default
    vi.mocked(posthog.getFeatureFlag).mockReturnValue('test')
    const { initializePosthogSdk, getFeatureFlag: gf } = await import('../posthog-sdk')
    initializePosthogSdk({ key: 'phc_abc', host: 'https://eu.i.posthog.com' })
    expect(gf('aboutme_chat_reply_experiment')).toBe('test')
    expect(posthog.getFeatureFlag).toHaveBeenCalledWith('aboutme_chat_reply_experiment')
  })

  it('captureAnalyticsEvent swallows errors from posthog.capture', async () => {
    const posthog = (await import('posthog-js')).default
    vi.mocked(posthog.capture).mockImplementationOnce(() => {
      throw new Error('network')
    })
    const { initializePosthogSdk, captureAnalyticsEvent } = await import('../posthog-sdk')
    initializePosthogSdk({ key: 'phc_abc', host: 'https://eu.i.posthog.com' })
    expect(() => captureAnalyticsEvent('portfolio_chat_answer_error')).not.toThrow()
  })

  it('getFeatureFlag returns undefined when posthog.getFeatureFlag throws', async () => {
    const posthog = (await import('posthog-js')).default
    vi.mocked(posthog.getFeatureFlag).mockImplementationOnce(() => {
      throw new Error('flag error')
    })
    const { initializePosthogSdk, getFeatureFlag: gf } = await import('../posthog-sdk')
    initializePosthogSdk({ key: 'phc_abc', host: 'https://eu.i.posthog.com' })
    expect(gf('aboutme_chat_reply_experiment')).toBeUndefined()
  })

  it('onFeatureFlagsReady invokes callback when SDK not initialized', async () => {
    const { onFeatureFlagsReady } = await import('../posthog-sdk')
    const cb = vi.fn()
    onFeatureFlagsReady(cb)
    expect(cb).toHaveBeenCalledTimes(1)
    expect(onFeatureFlags).not.toHaveBeenCalled()
  })

  it('onFeatureFlagsReady forwards to posthog when initialized', async () => {
    const posthog = (await import('posthog-js')).default
    const { initializePosthogSdk, onFeatureFlagsReady } = await import('../posthog-sdk')
    initializePosthogSdk({ key: 'phc_abc', host: 'https://eu.i.posthog.com' })
    const cb = vi.fn()
    onFeatureFlagsReady(cb)
    expect(posthog.onFeatureFlags).toHaveBeenCalledWith(cb)
    expect(cb).toHaveBeenCalled()
  })

  it('onFeatureFlagsReady invokes callback when onFeatureFlags throws', async () => {
    vi.mocked(onFeatureFlags).mockImplementationOnce(() => {
      throw new Error('subscriber')
    })
    const { initializePosthogSdk, onFeatureFlagsReady } = await import('../posthog-sdk')
    initializePosthogSdk({ key: 'phc_abc', host: 'https://eu.i.posthog.com' })
    const cb = vi.fn()
    onFeatureFlagsReady(cb)
    expect(cb).toHaveBeenCalledTimes(1)
  })

  it('registerAnalyticsProperties is a no-op before init', async () => {
    const { registerAnalyticsProperties } = await import('../posthog-sdk')
    registerAnalyticsProperties({ cohort: 'a' })
    expect(register).not.toHaveBeenCalled()
  })

  it('registerAnalyticsProperties forwards to posthog after init', async () => {
    const posthog = (await import('posthog-js')).default
    const { initializePosthogSdk, registerAnalyticsProperties } = await import('../posthog-sdk')
    initializePosthogSdk({ key: 'phc_abc', host: 'https://eu.i.posthog.com' })
    registerAnalyticsProperties({ cohort: 'b' })
    expect(posthog.register).toHaveBeenCalledWith({ cohort: 'b' })
  })

  it('registerAnalyticsProperties swallows register errors', async () => {
    const posthog = (await import('posthog-js')).default
    vi.mocked(posthog.register).mockImplementationOnce(() => {
      throw new Error('register fail')
    })
    const { initializePosthogSdk, registerAnalyticsProperties } = await import('../posthog-sdk')
    initializePosthogSdk({ key: 'phc_abc', host: 'https://eu.i.posthog.com' })
    expect(() => registerAnalyticsProperties({ x: 1 })).not.toThrow()
  })

  it('defaults api host when config host is whitespace-only', async () => {
    const posthog = (await import('posthog-js')).default
    const { initializePosthogSdk } = await import('../posthog-sdk')
    expect(initializePosthogSdk({ key: ' phc_trim ', host: '   ' })).toBe(true)
    expect(posthog.init).toHaveBeenCalledWith(
      'phc_trim',
      expect.objectContaining({ api_host: 'https://eu.i.posthog.com' }),
    )
  })
})
