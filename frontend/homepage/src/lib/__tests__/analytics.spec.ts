import { describe, expect, it, vi, beforeEach, type Mock } from 'vitest'
import posthog from 'posthog-js'

vi.mock('posthog-js', () => ({
  default: {
    capture: vi.fn(),
    captureException: vi.fn(),
    get_session_id: vi.fn(),
  },
}))

vi.mock('../posthog-consent', async () => {
  const actual = await vi.importActual<typeof import('../posthog-consent')>('../posthog-consent')
  return {
    ...actual,
    hasPageviewConsent: vi.fn(() => true),
    hasErrorTrackingConsent: vi.fn(() => true),
    isPosthogEnabled: vi.fn(() => true),
  }
})

vi.mock('../posthog-sdk', () => ({
  isPosthogSdkInitialized: vi.fn(() => true),
  captureAnalyticsEvent: vi.fn(),
}))

import {
  captureClientException,
  captureProductAnalyticsEvent,
  getPosthogSessionIdForVoiceAnalytics,
  trackEvent,
} from '../analytics'
import {
  __setPosthogTestEnv,
  hasErrorTrackingConsent,
  hasPageviewConsent,
  isPosthogEnabled,
} from '../posthog-consent'
import { captureAnalyticsEvent, isPosthogSdkInitialized } from '../posthog-sdk'

describe('analytics helpers', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    __setPosthogTestEnv({ enabled: true, key: 'phc_test', host: 'https://eu.i.posthog.com' })
    ;(posthog as unknown as { capture: Mock; captureException: Mock }).capture = vi.fn()
    ;(
      posthog as unknown as {
        capture: Mock
        captureException: Mock
      }
    ).captureException = vi.fn()
    ;(posthog as unknown as { get_session_id: Mock }).get_session_id = vi.fn(() => 'sess-1')
    vi.mocked(isPosthogEnabled).mockReturnValue(true)
    vi.mocked(hasPageviewConsent).mockReturnValue(true)
    vi.mocked(hasErrorTrackingConsent).mockReturnValue(true)
    vi.mocked(isPosthogSdkInitialized).mockReturnValue(true)
  })

  it('tracks events when pageview consent is ready', () => {
    trackEvent('cta_click', { label: 'hero' })
    expect(posthog.capture).toHaveBeenCalledWith('cta_click', { label: 'hero' })
  })

  it('does not track events when pageview consent is missing', () => {
    vi.mocked(hasPageviewConsent).mockReturnValue(false)
    trackEvent('cta_click')
    expect(posthog.capture).not.toHaveBeenCalled()
  })

  it('captureProductAnalyticsEvent forwards via SDK when ready', () => {
    captureProductAnalyticsEvent('portfolio_chat_ask_submitted', { x: 1 })
    expect(captureAnalyticsEvent).toHaveBeenCalledWith('portfolio_chat_ask_submitted', { x: 1 })
  })

  it('does not capture product events when pageview consent is missing', () => {
    vi.mocked(hasPageviewConsent).mockReturnValue(false)
    captureProductAnalyticsEvent('portfolio_chat_ask_submitted')
    expect(captureAnalyticsEvent).not.toHaveBeenCalled()
  })

  it('captures exceptions when error-tracking consent is ready', () => {
    const err = new Error('boom')
    captureClientException(err)
    expect(posthog.captureException).toHaveBeenCalledWith(err)
  })

  it('forwards non-Error values to captureException when available', () => {
    captureClientException('not-an-error')
    expect(posthog.captureException).toHaveBeenCalledWith('not-an-error')
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

  it('uses UnknownError metadata for non-Error when captureException is unavailable', () => {
    // @ts-expect-error testing fallback
    posthog.captureException = undefined
    captureClientException(42)
    expect(posthog.capture).toHaveBeenCalledWith('client_exception', {
      message: '42',
      name: 'UnknownError',
    })
  })

  it('does not capture exceptions when error-tracking consent is missing', () => {
    vi.mocked(hasErrorTrackingConsent).mockReturnValue(false)
    captureClientException(new Error('boom'))
    expect(posthog.captureException).not.toHaveBeenCalled()
  })

  it('returns trimmed PostHog session id for voice analytics when ready', () => {
    vi.mocked(posthog.get_session_id as Mock).mockReturnValue('  abc  ')
    expect(getPosthogSessionIdForVoiceAnalytics()).toBe('abc')
  })

  it('returns undefined for voice session id when get_session_id is missing or blank', () => {
    vi.mocked(posthog.get_session_id as Mock).mockReturnValue(undefined)
    expect(getPosthogSessionIdForVoiceAnalytics()).toBeUndefined()
    vi.mocked(posthog.get_session_id as Mock).mockReturnValue('   ')
    expect(getPosthogSessionIdForVoiceAnalytics()).toBeUndefined()
  })

  it('returns undefined when get_session_id throws', () => {
    vi.mocked(posthog.get_session_id as Mock).mockImplementation(() => {
      throw new Error('no session')
    })
    expect(getPosthogSessionIdForVoiceAnalytics()).toBeUndefined()
  })

  it('does not expose session id when track pipeline is not ready', () => {
    vi.mocked(hasPageviewConsent).mockReturnValue(false)
    vi.mocked(posthog.get_session_id as Mock).mockReturnValue('sess')
    expect(getPosthogSessionIdForVoiceAnalytics()).toBeUndefined()
  })

  it('does nothing when analytics is not enabled or initialized', () => {
    vi.mocked(isPosthogEnabled).mockReturnValue(false)
    vi.mocked(hasPageviewConsent).mockReturnValue(true)
    vi.mocked(hasErrorTrackingConsent).mockReturnValue(true)
    vi.mocked(isPosthogSdkInitialized).mockReturnValue(true)

    trackEvent('cta_click')
    captureProductAnalyticsEvent('x')
    captureClientException(new Error('boom'))

    expect(posthog.capture).not.toHaveBeenCalled()
    expect(posthog.captureException).not.toHaveBeenCalled()
    expect(captureAnalyticsEvent).not.toHaveBeenCalled()
  })
})
