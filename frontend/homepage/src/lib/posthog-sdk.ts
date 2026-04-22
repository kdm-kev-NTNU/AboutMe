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
