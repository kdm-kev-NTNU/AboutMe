import posthog from 'posthog-js'

function normalizeToken(token: string | undefined): string {
  return (token ?? '').trim()
}

export function initPosthogIfConfigured(token: string | undefined, host: string | undefined): boolean {
  const normalizedToken = normalizeToken(token)
  if (!normalizedToken) return false

  posthog.init(normalizedToken, {
    api_host: host || 'https://us.i.posthog.com',
    defaults: '2026-01-30',
    opt_out_capturing_by_default: true,
    persistence: 'localStorage+cookie',
  })

  return true
}

export function captureClientException(err: unknown): void {
  posthog.captureException(err)
}
