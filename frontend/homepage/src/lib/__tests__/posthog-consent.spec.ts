import { beforeEach, describe, expect, it, vi } from 'vitest'
import posthog from 'posthog-js'

vi.mock('posthog-js', () => ({
  default: {
    init: vi.fn(),
    opt_in_capturing: vi.fn(),
    opt_out_capturing: vi.fn(),
    reset: vi.fn(),
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
  COOKIE_CONSENT_RECORD_KEY,
  PRIVACY_POLICY_VERSION,
  registerPosthogActivationHandler,
  __setPosthogTestEnv,
} from '../posthog-consent'
import { initializePosthogSdk, isPosthogSdkInitialized } from '../posthog-sdk'

describe('posthog-consent', () => {
  let localStorageMock: Record<string, string>

  beforeEach(() => {
    localStorageMock = {}
    vi.clearAllMocks()

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

  it('activates analytics when stored consent is granted', () => {
    localStorage.setItem(
      COOKIE_CONSENT_RECORD_KEY,
      JSON.stringify({
        dismissed: true,
        analytics: true,
        policyVersion: PRIVACY_POLICY_VERSION,
        updatedAt: new Date().toISOString(),
        source: 'banner_accept_all',
      }),
    )

    const handler = vi.fn()
    registerPosthogActivationHandler(handler)
    applyStoredTrackingConsent()

    expect(initializePosthogSdk).toHaveBeenCalled()
    expect(posthog.opt_in_capturing).toHaveBeenCalled()
    expect(handler).toHaveBeenCalled()
  })

  it('deactivates analytics when stored consent is denied', () => {
    vi.mocked(isPosthogSdkInitialized).mockReturnValue(true)
    localStorage.setItem(
      COOKIE_CONSENT_RECORD_KEY,
      JSON.stringify({
        dismissed: true,
        analytics: false,
        policyVersion: PRIVACY_POLICY_VERSION,
        updatedAt: new Date().toISOString(),
        source: 'banner_necessary_only',
      }),
    )

    applyStoredTrackingConsent()

    expect(posthog.opt_out_capturing).toHaveBeenCalled()
    expect(posthog.reset).toHaveBeenCalled()
  })

  it('persists audit fields on grantAllCookies', () => {
    const handler = vi.fn()
    registerPosthogActivationHandler(handler)
    grantAllCookies('banner_accept_all')

    const record = JSON.parse(localStorageMock[COOKIE_CONSENT_RECORD_KEY])
    expect(record.analytics).toBe(true)
    expect(record.source).toBe('banner_accept_all')
    expect(record.policyVersion).toBe(PRIVACY_POLICY_VERSION)
    expect(posthog.opt_in_capturing).toHaveBeenCalled()
  })

  it('persists denied analytics on grantNecessaryCookiesOnly', () => {
    grantNecessaryCookiesOnly('banner_necessary_only')
    const record = JSON.parse(localStorageMock[COOKIE_CONSENT_RECORD_KEY])
    expect(record.analytics).toBe(false)
    expect(localStorageMock).toHaveProperty(COOKIE_CONSENT_RECORD_KEY)
  })
})
