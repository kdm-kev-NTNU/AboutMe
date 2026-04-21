import { beforeEach, describe, expect, it, vi } from 'vitest'
import posthog from 'posthog-js'
import { captureClientException, initPosthogIfConfigured } from '../analytics'

vi.mock('posthog-js', () => ({
  default: {
    init: vi.fn(),
    captureException: vi.fn(),
  },
}))

describe('analytics helpers', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('does not initialize PostHog when token is missing', () => {
    expect(initPosthogIfConfigured(undefined, 'https://us.i.posthog.com')).toBe(false)
    expect(posthog.init).not.toHaveBeenCalled()
  })

  it('does not initialize PostHog when token is empty after trim', () => {
    expect(initPosthogIfConfigured('   ', 'https://us.i.posthog.com')).toBe(false)
    expect(posthog.init).not.toHaveBeenCalled()
  })

  it('initializes PostHog when token is provided', () => {
    expect(initPosthogIfConfigured('  phc_123  ', 'https://eu.i.posthog.com')).toBe(true)
    expect(posthog.init).toHaveBeenCalledWith('phc_123', {
      api_host: 'https://eu.i.posthog.com',
      defaults: '2026-01-30',
      opt_out_capturing_by_default: true,
      persistence: 'localStorage+cookie',
    })
  })

  it('uses default host when host is missing', () => {
    initPosthogIfConfigured('phc_123', undefined)
    expect(posthog.init).toHaveBeenCalledWith(
      'phc_123',
      expect.objectContaining({ api_host: 'https://us.i.posthog.com' }),
    )
  })

  it('forwards client exceptions to PostHog', () => {
    const err = new Error('boom')
    captureClientException(err)
    expect(posthog.captureException).toHaveBeenCalledWith(err)
  })
})
