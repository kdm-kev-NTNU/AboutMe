import { describe, expect, it, vi, beforeEach } from 'vitest'
import posthog from 'posthog-js'

vi.mock('posthog-js', () => ({
  default: {
    capture: vi.fn(),
    captureException: vi.fn(),
  },
}))

vi.mock('../posthog-consent', async () => {
  const actual = await vi.importActual<typeof import('../posthog-consent')>('../posthog-consent')
  return {
    ...actual,
    hasAnalyticsConsent: vi.fn(() => true),
    isPosthogEnabled: vi.fn(() => true),
  }
})

vi.mock('../posthog-sdk', () => ({
  isPosthogSdkInitialized: vi.fn(() => true),
}))

import { captureClientException, trackEvent } from '../analytics'
import { __setPosthogTestEnv, hasAnalyticsConsent, isPosthogEnabled } from '../posthog-consent'
import { isPosthogSdkInitialized } from '../posthog-sdk'

describe('analytics helpers', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    __setPosthogTestEnv({ enabled: true, key: 'phc_test', host: 'https://eu.i.posthog.com' })
    ;(posthog as unknown as { capture: vi.Mock; captureException: vi.Mock }).capture = vi.fn()
    ;(
      posthog as unknown as {
        capture: vi.Mock
        captureException: vi.Mock
      }
    ).captureException = vi.fn()
    vi.mocked(isPosthogEnabled).mockReturnValue(true)
    vi.mocked(hasAnalyticsConsent).mockReturnValue(true)
    vi.mocked(isPosthogSdkInitialized).mockReturnValue(true)
  })

  it('tracks events when analytics is ready', () => {
    trackEvent('cta_click', { label: 'hero' })
    expect(posthog.capture).toHaveBeenCalledWith('cta_click', { label: 'hero' })
  })

  it('does not track events when consent is missing', () => {
    vi.mocked(hasAnalyticsConsent).mockReturnValue(false)
    trackEvent('cta_click')
    expect(posthog.capture).not.toHaveBeenCalled()
  })

  it('captures exceptions when analytics is ready', () => {
    const err = new Error('boom')
    captureClientException(err)
    expect(posthog.captureException).toHaveBeenCalledWith(err)
  })

  it('falls back to capture when captureException is unavailable', () => {
    // @ts-expect-error testing fallback
    posthog.captureException = undefined
    const err = new Error('boom')
    captureClientException(err)
    expect(posthog.capture).toHaveBeenCalledWith('client_exception', {
      message: 'boom',
      name: 'Error',
    })
  })

  it('does nothing when analytics is not enabled or initialized', () => {
    vi.mocked(isPosthogEnabled).mockReturnValue(false)
    vi.mocked(hasAnalyticsConsent).mockReturnValue(true)
    vi.mocked(isPosthogSdkInitialized).mockReturnValue(true)

    trackEvent('cta_click')
    captureClientException(new Error('boom'))

    expect(posthog.capture).not.toHaveBeenCalled()
    expect(posthog.captureException).not.toHaveBeenCalled()
  })
})
