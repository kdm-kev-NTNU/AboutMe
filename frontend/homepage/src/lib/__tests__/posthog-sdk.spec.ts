import { beforeEach, describe, expect, it, vi } from 'vitest'

const capture = vi.fn()
const getFeatureFlag = vi.fn()
const onFeatureFlags = vi.fn((cb: () => void) => {
  cb()
})

vi.mock('posthog-js', () => ({
  default: {
    init: vi.fn(),
    capture,
    getFeatureFlag,
    onFeatureFlags,
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
})
