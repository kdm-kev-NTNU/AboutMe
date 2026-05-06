import { beforeEach, describe, expect, it, vi } from 'vitest'
import posthog from 'posthog-js'

vi.mock('posthog-js', () => ({
  default: {
    init: vi.fn(),
    opt_in_capturing: vi.fn(),
    opt_out_capturing: vi.fn(),
    reset: vi.fn(),
    startSessionRecording: vi.fn(),
    stopSessionRecording: vi.fn(),
    get_explicit_consent_status: vi.fn(() => undefined),
  },
}))

vi.mock('../posthog-sdk', () => ({
  initializePosthogSdk: vi.fn(() => true),
  isPosthogSdkInitialized: vi.fn(() => true),
}))

import {
  applyStoredTrackingConsent,
  grantAllCookies,
  grantNecessaryCookiesOnly,
  rejectOptionalCookies,
  saveGranularConsent,
  getConsentRecord,
  hasAnalyticsConsent,
  hasPageviewConsent,
  isCookieBannerDismissed,
  isPosthogEnabled,
  COOKIE_CONSENT_RECORD_KEY,
  CONSENT_RECORD_KEY_V1,
  PRIVACY_POLICY_VERSION,
  registerPosthogActivationHandler,
  __setPosthogTestEnv,
} from '../posthog-consent'
import { initializePosthogSdk, isPosthogSdkInitialized } from '../posthog-sdk'

const fullGrant = {
  pageviews: true,
  sessionRecording: true,
  errorTracking: true,
  featureFlags: true,
}

const fullDeny = {
  pageviews: false,
  sessionRecording: false,
  errorTracking: false,
  featureFlags: false,
}

describe('posthog-consent', () => {
  let localStorageMock: Record<string, string>

  beforeEach(() => {
    localStorageMock = {}
    vi.clearAllMocks()
    vi.mocked(posthog.get_explicit_consent_status).mockReturnValue(undefined as never)

    __setPosthogTestEnv({
      enabled: true,
      key: 'phc_123',
      host: 'https://eu.i.posthog.com',
    })

    vi.stubGlobal('localStorage', {
      getItem: vi.fn((key: string) => localStorageMock[key] ?? null),
      setItem: vi.fn((key: string, value: string) => {
        localStorageMock[key] = value
      }),
      removeItem: vi.fn((key: string) => {
        delete localStorageMock[key]
      }),
      clear: vi.fn(() => {
        localStorageMock = {}
      }),
    })
  })

  it('activates analytics when stored consent grants all categories', () => {
    localStorage.setItem(
      COOKIE_CONSENT_RECORD_KEY,
      JSON.stringify({
        dismissed: true,
        ...fullGrant,
        policyVersion: PRIVACY_POLICY_VERSION,
        updatedAt: new Date().toISOString(),
        source: 'banner_accept_all',
      }),
    )

    const handler = vi.fn()
    registerPosthogActivationHandler(handler)
    applyStoredTrackingConsent()

    expect(initializePosthogSdk).toHaveBeenCalledWith({
      key: 'phc_123',
      host: 'https://eu.i.posthog.com',
      disableSessionRecording: false,
    })
    expect(posthog.opt_in_capturing).toHaveBeenCalled()
    expect(posthog.startSessionRecording).toHaveBeenCalled()
    expect(handler).toHaveBeenCalled()
  })

  it('initializes with session recording disabled when only pageviews are granted', () => {
    localStorage.setItem(
      COOKIE_CONSENT_RECORD_KEY,
      JSON.stringify({
        dismissed: true,
        pageviews: true,
        sessionRecording: false,
        errorTracking: false,
        featureFlags: false,
        policyVersion: PRIVACY_POLICY_VERSION,
        updatedAt: new Date().toISOString(),
        source: 'settings',
      }),
    )

    registerPosthogActivationHandler(vi.fn())
    applyStoredTrackingConsent()

    expect(initializePosthogSdk).toHaveBeenCalledWith({
      key: 'phc_123',
      host: 'https://eu.i.posthog.com',
      disableSessionRecording: true,
    })
    expect(posthog.opt_in_capturing).toHaveBeenCalled()
    expect(posthog.stopSessionRecording).toHaveBeenCalled()
    expect(posthog.startSessionRecording).not.toHaveBeenCalled()
  })

  it('deactivates analytics when stored consent is denied', () => {
    vi.mocked(isPosthogSdkInitialized).mockReturnValue(true)
    localStorage.setItem(
      COOKIE_CONSENT_RECORD_KEY,
      JSON.stringify({
        dismissed: true,
        ...fullDeny,
        policyVersion: PRIVACY_POLICY_VERSION,
        updatedAt: new Date().toISOString(),
        source: 'banner_necessary_only',
      }),
    )

    applyStoredTrackingConsent()

    expect(posthog.stopSessionRecording).toHaveBeenCalled()
    expect(posthog.opt_out_capturing).toHaveBeenCalled()
    expect(posthog.reset).toHaveBeenCalled()
  })

  it('persists audit fields on grantAllCookies', () => {
    const handler = vi.fn()
    registerPosthogActivationHandler(handler)
    grantAllCookies('banner_accept_all')

    const record = JSON.parse(localStorageMock[COOKIE_CONSENT_RECORD_KEY])
    expect(record.pageviews).toBe(true)
    expect(record.sessionRecording).toBe(true)
    expect(record.source).toBe('banner_accept_all')
    expect(record.policyVersion).toBe(PRIVACY_POLICY_VERSION)
    expect(posthog.opt_in_capturing).toHaveBeenCalled()
    expect(posthog.startSessionRecording).toHaveBeenCalled()
  })

  it('persists denied analytics on grantNecessaryCookiesOnly', () => {
    grantNecessaryCookiesOnly('banner_necessary_only')
    const record = JSON.parse(localStorageMock[COOKIE_CONSENT_RECORD_KEY])
    expect(record.pageviews).toBe(false)
    expect(localStorageMock).toHaveProperty(COOKIE_CONSENT_RECORD_KEY)
  })

  it('rejectOptionalCookies persists denied consent and deactivates sdk', () => {
    vi.mocked(isPosthogSdkInitialized).mockReturnValue(true)
    rejectOptionalCookies('banner_reject')

    const record = JSON.parse(localStorageMock[COOKIE_CONSENT_RECORD_KEY])
    expect(record.pageviews).toBe(false)
    expect(record.source).toBe('banner_reject')
    expect(posthog.stopSessionRecording).toHaveBeenCalledTimes(1)
    expect(posthog.opt_out_capturing).toHaveBeenCalledTimes(1)
    expect(posthog.reset).toHaveBeenCalledTimes(1)
  })

  it('saveGranularConsent stores and applies partial choices', () => {
    const handler = vi.fn()
    registerPosthogActivationHandler(handler)
    saveGranularConsent(
      {
        pageviews: true,
        sessionRecording: false,
        errorTracking: true,
        featureFlags: false,
      },
      'settings',
    )

    expect(hasAnalyticsConsent()).toBe(true)
    expect(hasPageviewConsent()).toBe(true)
    expect(isCookieBannerDismissed()).toBe(true)
    expect(posthog.opt_in_capturing).toHaveBeenCalledTimes(1)
    expect(posthog.stopSessionRecording).toHaveBeenCalled()
    expect(handler).toHaveBeenCalledTimes(1)
  })

  it('migrates legacy granted consent from posthog status', () => {
    vi.mocked(posthog.get_explicit_consent_status).mockReturnValue('granted')

    const record = getConsentRecord()
    expect(record?.pageviews).toBe(true)
    expect(record?.sessionRecording).toBe(true)
    expect(record?.source).toBe('legacy_migration')
  })

  it('migrates legacy denied consent from localStorage flag', () => {
    localStorage.setItem('posthog_tracking_consent', 'denied')

    const record = getConsentRecord()
    expect(record?.pageviews).toBe(false)
    expect(record?.source).toBe('legacy_migration')
  })

  it('migrates v1 record with analytics true to full granular grant', () => {
    localStorage.setItem(
      CONSENT_RECORD_KEY_V1,
      JSON.stringify({
        dismissed: true,
        analytics: true,
        policyVersion: '2026-04-22',
        updatedAt: new Date().toISOString(),
        source: 'banner_accept_all',
      }),
    )

    const record = getConsentRecord()
    expect(record?.pageviews).toBe(true)
    expect(record?.sessionRecording).toBe(true)
    expect(record?.errorTracking).toBe(true)
    expect(record?.featureFlags).toBe(true)
    expect(localStorageMock[COOKIE_CONSENT_RECORD_KEY]).toBeDefined()
  })

  it('isPosthogEnabled reflects env override state', () => {
    __setPosthogTestEnv({ enabled: false, key: '', host: '' })
    expect(isPosthogEnabled()).toBe(false)
    __setPosthogTestEnv({ enabled: true, key: 'phc_123', host: 'https://eu.i.posthog.com' })
    expect(isPosthogEnabled()).toBe(true)
  })

  it('does nothing when applyStoredTrackingConsent has no dismissed record', () => {
    applyStoredTrackingConsent()
    expect(initializePosthogSdk).not.toHaveBeenCalled()
    expect(posthog.opt_in_capturing).not.toHaveBeenCalled()
    expect(posthog.opt_out_capturing).not.toHaveBeenCalled()
  })
})
