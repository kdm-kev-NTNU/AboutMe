import posthog from 'posthog-js'
import {
  hasErrorTrackingConsent,
  hasPageviewConsent,
  isPosthogEnabled,
} from './posthog-consent'
import { captureAnalyticsEvent, isPosthogSdkInitialized } from './posthog-sdk'

function isTrackEventReady(): boolean {
  return isPosthogEnabled() && hasPageviewConsent() && isPosthogSdkInitialized()
}

function isErrorTrackingReady(): boolean {
  return isPosthogEnabled() && hasErrorTrackingConsent() && isPosthogSdkInitialized()
}

export function trackEvent(event: string, properties?: Record<string, unknown>): void {
  if (!isTrackEventReady()) return
  posthog.capture(event, properties ?? {})
}

/**
 * Custom / product events (e.g. chat funnel) — requires pageview-style analytics consent.
 * PostHog pageviews are gated separately in `posthog-app-hooks`.
 */
export function captureProductAnalyticsEvent(
  event: string,
  properties?: Record<string, unknown>,
): void {
  if (!isTrackEventReady()) return
  captureAnalyticsEvent(event, properties)
}

export function captureClientException(err: unknown): void {
  if (!isErrorTrackingReady()) return

  if (typeof posthog.captureException === 'function') {
    posthog.captureException(err)
    return
  }

  posthog.capture('client_exception', {
    message: err instanceof Error ? err.message : String(err),
    name: err instanceof Error ? err.name : 'UnknownError',
  })
}
