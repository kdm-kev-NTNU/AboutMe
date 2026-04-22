import posthog from 'posthog-js'
import { hasAnalyticsConsent, isPosthogEnabled } from './posthog-consent'
import { isPosthogSdkInitialized } from './posthog-sdk'

function isReady(): boolean {
  return isPosthogEnabled() && hasAnalyticsConsent() && isPosthogSdkInitialized()
}

export function trackEvent(event: string, properties?: Record<string, unknown>): void {
  if (!isReady()) return
  posthog.capture(event, properties ?? {})
}

export function captureClientException(err: unknown): void {
  if (!isReady()) return

  if (typeof posthog.captureException === 'function') {
    posthog.captureException(err)
    return
  }

  posthog.capture('client_exception', {
    message: err instanceof Error ? err.message : String(err),
    name: err instanceof Error ? err.name : 'UnknownError',
  })
}
