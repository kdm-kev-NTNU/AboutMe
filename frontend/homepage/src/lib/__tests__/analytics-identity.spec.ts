import { beforeEach, describe, expect, it, vi } from 'vitest'

const { identify, register, unregister, reset } = vi.hoisted(() => ({
  identify: vi.fn(),
  register: vi.fn(),
  unregister: vi.fn(),
  reset: vi.fn(),
}))

const { hasAnalyticsConsent, isPosthogEnabled } = vi.hoisted(() => ({
  hasAnalyticsConsent: vi.fn(() => true),
  isPosthogEnabled: vi.fn(() => true),
}))

const { isPosthogSdkInitialized } = vi.hoisted(() => ({
  isPosthogSdkInitialized: vi.fn(() => true),
}))

vi.mock('posthog-js', () => ({
  default: {
    identify,
    register,
    unregister,
    reset,
  },
}))

vi.mock('../posthog-consent', () => ({
  hasAnalyticsConsent,
  isPosthogEnabled,
}))

vi.mock('../posthog-sdk', () => ({
  isPosthogSdkInitialized,
}))

import {
  __resetAnalyticsIdentityForTests,
  applyPendingOwnerIdentity,
  revokeOwnerIdentity,
  setOwnerIdentity,
} from '../analytics-identity'

describe('analytics-identity', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    __resetAnalyticsIdentityForTests()
    hasAnalyticsConsent.mockReturnValue(true)
    isPosthogEnabled.mockReturnValue(true)
    isPosthogSdkInitialized.mockReturnValue(true)
  })

  it('does not identify without analytics consent', () => {
    hasAnalyticsConsent.mockReturnValue(false)
    setOwnerIdentity('owner_abc')
    expect(identify).not.toHaveBeenCalled()
  })

  it('does not identify before SDK init', () => {
    isPosthogSdkInitialized.mockReturnValue(false)
    setOwnerIdentity('owner_abc')
    expect(identify).not.toHaveBeenCalled()
  })

  it('applies pending identity when activation runs after login', () => {
    isPosthogSdkInitialized.mockReturnValue(false)
    setOwnerIdentity('owner_abc')
    expect(identify).not.toHaveBeenCalled()

    isPosthogSdkInitialized.mockReturnValue(true)
    applyPendingOwnerIdentity()

    expect(identify).toHaveBeenCalledWith('owner_abc', {
      is_internal: true,
      is_owner: true,
    })
    expect(register).toHaveBeenCalledWith({ is_internal: true })
  })

  it('identifies only once for the same distinct id', () => {
    setOwnerIdentity('owner_abc')
    setOwnerIdentity('owner_abc')
    applyPendingOwnerIdentity()

    expect(identify).toHaveBeenCalledTimes(1)
  })

  it('revoke clears super property and resets PostHog', () => {
    setOwnerIdentity('owner_abc')
    revokeOwnerIdentity()

    expect(unregister).toHaveBeenCalledWith('is_internal')
    expect(reset).toHaveBeenCalled()
  })
})
