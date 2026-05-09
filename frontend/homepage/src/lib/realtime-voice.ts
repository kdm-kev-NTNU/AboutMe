import { customFetch } from '@/api/orval-mutator'

export type SpeechUiLang = 'en' | 'no'

/**
 * Whether the backend exposes Realtime voice (feature flag + API key).
 */
export async function fetchRealtimeVoiceEnabled(): Promise<boolean> {
  const r = await customFetch<{ data: unknown; status: number }>('/realtime/status', { method: 'GET' })
  if (r.status !== 200 || r.data === null || typeof r.data !== 'object') return false
  const d = r.data as { enabled?: boolean }
  return d.enabled === true
}

/**
 * POST WebRTC SDP offer to the backend; returns SDP answer text for setRemoteDescription.
 */
export async function exchangeRealtimeSdp(
  offerSdp: string,
  language: SpeechUiLang,
): Promise<{ ok: true; answerSdp: string } | { ok: false; status: number; message: string }> {
  const headers: Record<string, string> = {
    'Content-Type': 'application/sdp',
    'X-Chat-Language': language,
  }
  const r = await customFetch<{ data: unknown; status: number }>('/realtime/session', {
    method: 'POST',
    body: offerSdp,
    headers,
  })
  if (r.status >= 200 && r.status < 300 && typeof r.data === 'string') {
    return { ok: true, answerSdp: r.data }
  }
  const msg =
    r.data && typeof r.data === 'object' && r.data !== null && 'error' in r.data
      ? String((r.data as { error?: unknown }).error ?? '')
      : ''
  return {
    ok: false,
    status: r.status,
    message: msg || `HTTP ${r.status}`,
  }
}

/** Auto-disconnect after this many ms (aligned with backend cost controls). */
export const REALTIME_SESSION_MAX_MS = 180_000
