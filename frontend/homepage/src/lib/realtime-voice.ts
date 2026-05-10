import { customFetch } from '@/api/orval-mutator'

/** Mirrors backend {@link com.kevinmazali.portfolio.model.analytics.RealtimeVoiceAnalyticsContext} headers. */
export type RealtimeVoiceAnalytics = {
  traceId: string
  sessionId?: string
}

export const VOICE_ANALYTICS_HEADERS = {
  AI_TRACE_ID: 'X-AI-Trace-Id',
  POSTHOG_SESSION_ID: 'X-PostHog-Session-Id',
} as const

function voiceAnalyticsHeaders(a?: RealtimeVoiceAnalytics): Record<string, string> {
  if (!a?.traceId || a.traceId.trim() === '') return {}
  const h: Record<string, string> = { [VOICE_ANALYTICS_HEADERS.AI_TRACE_ID]: a.traceId.trim() }
  if (a.sessionId && a.sessionId.trim() !== '') {
    h[VOICE_ANALYTICS_HEADERS.POSTHOG_SESSION_ID] = a.sessionId.trim()
  }
  return h
}

export type SpeechUiLang = 'en' | 'no'
export type RealtimeVoiceProvider = 'OPENAI' | 'ELEVENLABS'

export type RealtimeLookupSnippet = {
  sourceType: 'profile' | 'rag'
  title: string
  text: string
}

export type RealtimeLookupResponse = {
  found: boolean
  snippets: RealtimeLookupSnippet[]
}

export type RealtimeVoiceChoice = 'marin' | 'cedar'
export type RealtimeReasoningEffort = 'low' | 'medium' | 'high'

export type RealtimeVoiceSessionOptions = {
  voice: RealtimeVoiceChoice
  reasoningEffort: RealtimeReasoningEffort
}

export type RealtimeVoiceModelOption = {
  provider: RealtimeVoiceProvider
  id: string
  label: string
  defaultOption: boolean
}

export type RealtimeVoiceStatus = RealtimeVoiceSessionOptions & {
  enabled: boolean
  voices: RealtimeVoiceChoice[]
  reasoningEfforts: RealtimeReasoningEffort[]
}

const DEFAULT_REALTIME_VOICE: RealtimeVoiceChoice = 'marin'
const DEFAULT_REALTIME_REASONING_EFFORT: RealtimeReasoningEffort = 'low'
const ALLOWED_REALTIME_VOICES: readonly RealtimeVoiceChoice[] = ['marin', 'cedar']
const ALLOWED_REALTIME_REASONING_EFFORTS: readonly RealtimeReasoningEffort[] = ['low', 'medium', 'high']

/** Failure from POST /realtime/session (includes optional machine-readable `code` from backend). */
export type RealtimeSdpFailure = {
  ok: false
  status: number
  message: string
  code?: string
  /** From HTTP Retry-After when present (e.g. rate limit). */
  retryAfterSeconds?: number
}

export type RealtimeTokenFailure = RealtimeSdpFailure

function isRealtimeVoice(value: unknown): value is RealtimeVoiceChoice {
  return typeof value === 'string' && ALLOWED_REALTIME_VOICES.includes(value as RealtimeVoiceChoice)
}

function isRealtimeReasoningEffort(value: unknown): value is RealtimeReasoningEffort {
  return (
    typeof value === 'string' &&
    ALLOWED_REALTIME_REASONING_EFFORTS.includes(value as RealtimeReasoningEffort)
  )
}

function isRealtimeVoiceProvider(value: unknown): value is RealtimeVoiceProvider {
  return value === 'OPENAI' || value === 'ELEVENLABS'
}

function isRealtimeVoiceModelOption(value: unknown): value is RealtimeVoiceModelOption {
  if (value === null || typeof value !== 'object') return false
  const v = value as Record<string, unknown>
  return (
    isRealtimeVoiceProvider(v.provider) &&
    typeof v.id === 'string' &&
    v.id.trim() !== '' &&
    typeof v.label === 'string' &&
    typeof v.defaultOption === 'boolean'
  )
}

function parseRealtimeVoiceStatus(data: unknown): RealtimeVoiceStatus | null {
  if (data === null || typeof data !== 'object') return null
  const d = data as {
    enabled?: unknown
    voices?: unknown
    reasoningEfforts?: unknown
    defaultVoice?: unknown
    defaultReasoningEffort?: unknown
  }
  const voices = Array.isArray(d.voices) ? d.voices.filter(isRealtimeVoice) : [...ALLOWED_REALTIME_VOICES]
  const reasoningEfforts = Array.isArray(d.reasoningEfforts)
    ? d.reasoningEfforts.filter(isRealtimeReasoningEffort)
    : [...ALLOWED_REALTIME_REASONING_EFFORTS]
  const defaultVoice = isRealtimeVoice(d.defaultVoice) && voices.includes(d.defaultVoice)
    ? d.defaultVoice
    : DEFAULT_REALTIME_VOICE
  const defaultReasoningEffort =
    isRealtimeReasoningEffort(d.defaultReasoningEffort) && reasoningEfforts.includes(d.defaultReasoningEffort)
      ? d.defaultReasoningEffort
      : DEFAULT_REALTIME_REASONING_EFFORT

  return {
    enabled: d.enabled === true,
    voices: voices.length > 0 ? voices : [...ALLOWED_REALTIME_VOICES],
    reasoningEfforts: reasoningEfforts.length > 0 ? reasoningEfforts : [...ALLOWED_REALTIME_REASONING_EFFORTS],
    voice: defaultVoice,
    reasoningEffort: defaultReasoningEffort,
  }
}

/**
 * Whether the backend exposes Realtime voice (feature flag + API key).
 */
export async function fetchRealtimeVoiceStatus(): Promise<RealtimeVoiceStatus> {
  try {
    const r = await customFetch<{ data: unknown; status: number }>('/realtime/status', { method: 'GET' })
    if (r.status !== 200) {
      throw new Error(`HTTP ${r.status}`)
    }
    return parseRealtimeVoiceStatus(r.data) ?? {
      enabled: false,
      voices: [...ALLOWED_REALTIME_VOICES],
      reasoningEfforts: [...ALLOWED_REALTIME_REASONING_EFFORTS],
      voice: DEFAULT_REALTIME_VOICE,
      reasoningEffort: DEFAULT_REALTIME_REASONING_EFFORT,
    }
  } catch {
    return {
      enabled: false,
      voices: [...ALLOWED_REALTIME_VOICES],
      reasoningEfforts: [...ALLOWED_REALTIME_REASONING_EFFORTS],
      voice: DEFAULT_REALTIME_VOICE,
      reasoningEffort: DEFAULT_REALTIME_REASONING_EFFORT,
    }
  }
}

export async function fetchRealtimeVoiceEnabled(): Promise<boolean> {
  return (await fetchRealtimeVoiceStatus()).enabled
}

export async function fetchRealtimeVoiceModels(): Promise<RealtimeVoiceModelOption[]> {
  try {
    const r = await customFetch<{ data: unknown; status: number }>('/realtime/models', { method: 'GET' })
    if (r.status !== 200 || !Array.isArray(r.data)) return []
    return r.data.filter(isRealtimeVoiceModelOption)
  } catch {
    return []
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
  analytics?: RealtimeVoiceAnalytics,
): Promise<RealtimeLookupResponse> {
  try {
    const r = await customFetch<{ data: unknown; status: number }>('/realtime/lookup', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        ...voiceAnalyticsHeaders(analytics),
      },
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
  options?: Partial<RealtimeVoiceSessionOptions>,
  modelId?: string,
  analytics?: RealtimeVoiceAnalytics,
): Promise<{ ok: true; answerSdp: string } | RealtimeSdpFailure> {
  const headers: Record<string, string> = {
    'Content-Type': 'application/sdp',
    'X-Chat-Language': language,
    'X-Realtime-Voice': isRealtimeVoice(options?.voice) ? options.voice : DEFAULT_REALTIME_VOICE,
    'X-Realtime-Reasoning-Effort': isRealtimeReasoningEffort(options?.reasoningEffort)
      ? options.reasoningEffort
      : DEFAULT_REALTIME_REASONING_EFFORT,
    ...voiceAnalyticsHeaders(analytics),
  }
  if (modelId && modelId.trim() !== '') {
    headers['X-Realtime-Model'] = modelId.trim()
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

export async function createElevenLabsConversationToken(
  modelId: string,
  analytics?: RealtimeVoiceAnalytics,
): Promise<{ ok: true; token: string } | RealtimeTokenFailure> {
  const r = await customFetch<{ data: unknown; status: number; headers?: Headers }>(
    '/realtime/elevenlabs/token',
    {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        ...voiceAnalyticsHeaders(analytics),
      },
      body: JSON.stringify({ modelId }),
    },
  )
  if (
    r.status >= 200 &&
    r.status < 300 &&
    r.data !== null &&
    typeof r.data === 'object' &&
    typeof (r.data as { token?: unknown }).token === 'string' &&
    (r.data as { token: string }).token.trim() !== ''
  ) {
    return { ok: true, token: (r.data as { token: string }).token }
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

/** POST {@code /realtime/analytics/voice-trace} — closes PostHog {@code $ai_trace} for the voice session. */
export async function completeVoiceTrace(payload: {
  traceId: string
  sessionId?: string
  durationSeconds: number
  error: boolean
  errorMessage?: string
}): Promise<void> {
  try {
    await customFetch<{ data: unknown; status: number }>('/realtime/analytics/voice-trace', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        traceId: payload.traceId,
        ...(payload.sessionId !== undefined ? { sessionId: payload.sessionId } : {}),
        durationSeconds: payload.durationSeconds,
        error: payload.error,
        ...(payload.errorMessage !== undefined ? { errorMessage: payload.errorMessage } : {}),
      }),
    })
  } catch {
    /* best-effort beacon */
  }
}
