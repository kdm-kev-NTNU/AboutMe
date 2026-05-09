import { customFetch } from '@/api/orval-mutator'

export type SpeechUiLang = 'en' | 'no'

export type RealtimeLookupSnippet = {
  sourceType: 'profile' | 'rag'
  title: string
  text: string
}

export type RealtimeLookupResponse = {
  found: boolean
  snippets: RealtimeLookupSnippet[]
}

/** Failure from POST /realtime/session (includes optional machine-readable `code` from backend). */
export type RealtimeSdpFailure = {
  ok: false
  status: number
  message: string
  code?: string
  /** From HTTP Retry-After when present (e.g. rate limit). */
  retryAfterSeconds?: number
}

/**
 * Whether the backend exposes Realtime voice (feature flag + API key).
 */
export async function fetchRealtimeVoiceEnabled(): Promise<boolean> {
  try {
    const r = await customFetch<{ data: unknown; status: number }>('/realtime/status', { method: 'GET' })
    if (r.status !== 200 || r.data === null || typeof r.data !== 'object') return false
    const d = r.data as { enabled?: boolean }
    return d.enabled === true
  } catch {
    return false
  }
}

function isLookupSnippet(value: unknown): value is RealtimeLookupSnippet {
  if (value === null || typeof value !== 'object') return false
  const v = value as Record<string, unknown>
  return (
    (v.sourceType === 'profile' || v.sourceType === 'rag') &&
    typeof v.title === 'string' &&
    typeof v.text === 'string'
  )
}

/**
 * Small public knowledge lookup for the Realtime tool loop.
 */
export async function lookupRealtimeInfo(
  query: string,
  language: SpeechUiLang,
): Promise<RealtimeLookupResponse> {
  try {
    const r = await customFetch<{ data: unknown; status: number }>('/realtime/lookup', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ query, language }),
    })
    if (r.status < 200 || r.status >= 300 || r.data === null || typeof r.data !== 'object') {
      return { found: false, snippets: [] }
    }
    const data = r.data as { found?: unknown; snippets?: unknown }
    const snippets = Array.isArray(data.snippets) ? data.snippets.filter(isLookupSnippet) : []
    return { found: data.found === true && snippets.length > 0, snippets }
  } catch {
    return { found: false, snippets: [] }
  }
}

/**
 * POST WebRTC SDP offer to the backend; returns SDP answer text for setRemoteDescription.
 */
export async function exchangeRealtimeSdp(
  offerSdp: string,
  language: SpeechUiLang,
): Promise<{ ok: true; answerSdp: string } | RealtimeSdpFailure> {
  const headers: Record<string, string> = {
    'Content-Type': 'application/sdp',
    'X-Chat-Language': language,
  }
  const r = await customFetch<{ data: unknown; status: number; headers?: Headers }>('/realtime/session', {
    method: 'POST',
    body: offerSdp,
    headers,
  })
  if (r.status >= 200 && r.status < 300 && typeof r.data === 'string') {
    return { ok: true, answerSdp: r.data }
  }
  let msg = ''
  let code: string | undefined
  if (r.data && typeof r.data === 'object' && r.data !== null && 'error' in r.data) {
    msg = String((r.data as { error?: unknown }).error ?? '')
  }
  const obj = r.data && typeof r.data === 'object' && r.data !== null ? (r.data as { code?: unknown }) : null
  if (obj && 'code' in obj && obj.code != null && String(obj.code).trim() !== '') {
    code = String(obj.code)
  }
  let retryAfterSeconds: number | undefined
  const ra = r.headers?.get?.('Retry-After')
  if (ra != null && ra !== '') {
    const n = parseInt(ra, 10)
    if (!Number.isNaN(n) && n > 0) retryAfterSeconds = n
  }
  return {
    ok: false,
    status: r.status,
    message: msg || `HTTP ${r.status}`,
    ...(code !== undefined ? { code } : {}),
    ...(retryAfterSeconds !== undefined ? { retryAfterSeconds } : {}),
  }
}

/** Auto-disconnect after this many ms (aligned with backend cost controls). */
export const REALTIME_SESSION_MAX_MS = 180_000
