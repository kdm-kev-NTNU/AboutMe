import posthog from 'posthog-js'

let sdkInitialized = false

type PosthogConfig = {
  key: string
  host: string
}

function resolveConfig(config?: Partial<PosthogConfig>): PosthogConfig | null {
  const key = (config?.key ?? '').trim()
  if (!key) return null

  return {
    key,
    host: (config?.host ?? 'https://eu.i.posthog.com').trim() || 'https://eu.i.posthog.com',
  }
}

/** Custom product events (aligned with PostHog experiments / goals). */
export const POSTHOG_CHAT_EVENTS = {
  ASK_SUBMITTED: 'portfolio_chat_ask_submitted',
  ANSWER_RECEIVED: 'portfolio_chat_answer_received',
  ANSWER_ERROR: 'portfolio_chat_answer_error',
} as const

/** Feature flag keys created in PostHog (MCP / UI). */
export const POSTHOG_FEATURE_FLAGS = {
  CHAT_REPLY_EXPERIMENT: 'aboutme_chat_reply_experiment',
} as const

/**
 * Initializes PostHog exactly once. Call only after the user has granted analytics consent.
 */
export function initializePosthogSdk(config?: Partial<PosthogConfig>): boolean {
  if (sdkInitialized) return true

  const resolved = resolveConfig(config)
  if (!resolved) {
    console.warn('[analytics] PostHog key is missing. Skipping initialization.')
    return false
  }

  posthog.init(resolved.key, {
    api_host: resolved.host,
    capture_pageview: false,
    autocapture: false,
    persistence: 'localStorage',
    disable_session_recording: true,
  })

  sdkInitialized = true
  return true
}

export function isPosthogSdkInitialized(): boolean {
  return sdkInitialized
}

/** Safe capture when SDK is active and user has not opted out. */
export function captureAnalyticsEvent(
  event: string,
  properties?: Record<string, unknown>,
): void {
  if (!sdkInitialized) return
  try {
    posthog.capture(event, properties)
  } catch {
    // ignore capture failures
  }
}

export function getFeatureFlag(flagKey: string): boolean | string | undefined {
  if (!sdkInitialized) return undefined
  try {
    return posthog.getFeatureFlag(flagKey) as boolean | string | undefined
  } catch {
    return undefined
  }
}

export function onFeatureFlagsReady(callback: () => void): void {
  if (!sdkInitialized) {
    callback()
    return
  }
  try {
    posthog.onFeatureFlags(callback)
  } catch {
    callback()
  }
}
