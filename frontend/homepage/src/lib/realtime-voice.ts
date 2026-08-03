import { customFetch } from '@/api/orval-mutator'

export type SpeechUiLang = 'en' | 'no'
export type RealtimeVoiceProvider = 'OPENAI'

export type RealtimeLookupSnippet = {
  sourceType: 'profile' | 'rag'
  title: string
  text: string
}

export type RealtimeLookupConfidence = 'high' | 'low' | 'none'

export type RealtimeLookupResponse = {
  found: boolean
  snippets: RealtimeLookupSnippet[]
  confidence?: RealtimeLookupConfidence
}

export type RealtimeVoiceChoice = 'marin' | 'cedar'
export type RealtimeReasoningEffort = 'low' | 'medium' | 'high'
export type RealtimeVadEagerness = 'low' | 'medium' | 'high' | 'auto'

export type RealtimeVoiceSessionOptions = {
  voice: RealtimeVoiceChoice
  reasoningEffort: RealtimeReasoningEffort
  vadEagerness: RealtimeVadEagerness
}

export type RealtimeVoiceModelOption = {
  provider: RealtimeVoiceProvider
  id: string
  label: string
  defaultOption: boolean
}

export type RealtimeVoiceStatus = RealtimeVoiceSessionOptions & {
  enabled: boolean
  liveEnabled: boolean
  voices: RealtimeVoiceChoice[]
  reasoningEfforts: RealtimeReasoningEffort[]
  vadEagernessOptions: RealtimeVadEagerness[]
}

const DEFAULT_REALTIME_VOICE: RealtimeVoiceChoice = 'marin'
const DEFAULT_REALTIME_REASONING_EFFORT: RealtimeReasoningEffort = 'low'
const DEFAULT_REALTIME_VAD_EAGERNESS: RealtimeVadEagerness = 'low'
const ALLOWED_REALTIME_VOICES: readonly RealtimeVoiceChoice[] = ['marin', 'cedar']
const ALLOWED_REALTIME_REASONING_EFFORTS: readonly RealtimeReasoningEffort[] = ['low', 'medium', 'high']
const ALLOWED_REALTIME_VAD_EAGERNESS: readonly RealtimeVadEagerness[] = ['low', 'medium', 'high', 'auto']

/** Failure from POST /realtime/session (includes optional machine-readable `code` from backend). */
export type RealtimeSdpFailure = {
  ok: false
  status: number
  message: string
  code?: string
  /** From HTTP Retry-After when present (e.g. rate limit). */
  retryAfterSeconds?: number
}

function isRealtimeVoice(value: unknown): value is RealtimeVoiceChoice {
  return typeof value === 'string' && ALLOWED_REALTIME_VOICES.includes(value as RealtimeVoiceChoice)
}

function isRealtimeReasoningEffort(value: unknown): value is RealtimeReasoningEffort {
  return (
    typeof value === 'string' &&
    ALLOWED_REALTIME_REASONING_EFFORTS.includes(value as RealtimeReasoningEffort)
  )
}

function isRealtimeVadEagerness(value: unknown): value is RealtimeVadEagerness {
  return (
    typeof value === 'string' && ALLOWED_REALTIME_VAD_EAGERNESS.includes(value as RealtimeVadEagerness)
  )
}

function isRealtimeVoiceProvider(value: unknown): value is RealtimeVoiceProvider {
  return value === 'OPENAI'
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
    vadEagernessOptions?: unknown
    defaultVoice?: unknown
    defaultReasoningEffort?: unknown
    defaultVadEagerness?: unknown
    liveEnabled?: unknown
  }
  const voices = Array.isArray(d.voices) ? d.voices.filter(isRealtimeVoice) : [...ALLOWED_REALTIME_VOICES]
  const reasoningEfforts = Array.isArray(d.reasoningEfforts)
    ? d.reasoningEfforts.filter(isRealtimeReasoningEffort)
    : [...ALLOWED_REALTIME_REASONING_EFFORTS]
  const vadEagernessOptions = Array.isArray(d.vadEagernessOptions)
    ? d.vadEagernessOptions.filter(isRealtimeVadEagerness)
    : [...ALLOWED_REALTIME_VAD_EAGERNESS]
  const defaultVoice = isRealtimeVoice(d.defaultVoice) && voices.includes(d.defaultVoice)
    ? d.defaultVoice
    : DEFAULT_REALTIME_VOICE
  const defaultReasoningEffort =
    isRealtimeReasoningEffort(d.defaultReasoningEffort) && reasoningEfforts.includes(d.defaultReasoningEffort)
      ? d.defaultReasoningEffort
      : DEFAULT_REALTIME_REASONING_EFFORT
  const defaultVadEagerness =
    isRealtimeVadEagerness(d.defaultVadEagerness) && vadEagernessOptions.includes(d.defaultVadEagerness)
      ? d.defaultVadEagerness
      : DEFAULT_REALTIME_VAD_EAGERNESS

  return {
    enabled: d.enabled === true,
    liveEnabled: d.liveEnabled === true,
    voices: voices.length > 0 ? voices : [...ALLOWED_REALTIME_VOICES],
    reasoningEfforts: reasoningEfforts.length > 0 ? reasoningEfforts : [...ALLOWED_REALTIME_REASONING_EFFORTS],
    vadEagernessOptions:
      vadEagernessOptions.length > 0 ? vadEagernessOptions : [...ALLOWED_REALTIME_VAD_EAGERNESS],
    voice: defaultVoice,
    reasoningEffort: defaultReasoningEffort,
    vadEagerness: defaultVadEagerness,
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
      liveEnabled: false,
      voices: [...ALLOWED_REALTIME_VOICES],
      reasoningEfforts: [...ALLOWED_REALTIME_REASONING_EFFORTS],
      vadEagernessOptions: [...ALLOWED_REALTIME_VAD_EAGERNESS],
      voice: DEFAULT_REALTIME_VOICE,
      reasoningEffort: DEFAULT_REALTIME_REASONING_EFFORT,
      vadEagerness: DEFAULT_REALTIME_VAD_EAGERNESS,
    }
  } catch {
    return {
      enabled: false,
      liveEnabled: false,
      voices: [...ALLOWED_REALTIME_VOICES],
      reasoningEfforts: [...ALLOWED_REALTIME_REASONING_EFFORTS],
      vadEagernessOptions: [...ALLOWED_REALTIME_VAD_EAGERNESS],
      voice: DEFAULT_REALTIME_VOICE,
      reasoningEffort: DEFAULT_REALTIME_REASONING_EFFORT,
      vadEagerness: DEFAULT_REALTIME_VAD_EAGERNESS,
    }
  }
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
  signal?: AbortSignal,
): Promise<RealtimeLookupResponse> {
  try {
    const r = await customFetch<{ data: unknown; status: number }>('/realtime/lookup', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ query, language }),
      signal,
    })
    if (r.status < 200 || r.status >= 300 || r.data === null || typeof r.data !== 'object') {
      return { found: false, snippets: [], confidence: 'none' }
    }
    const data = r.data as { found?: unknown; snippets?: unknown; confidence?: unknown }
    const snippets = Array.isArray(data.snippets) ? data.snippets.filter(isLookupSnippet) : []
    const confidence = parseLookupConfidence(data.confidence, data.found === true && snippets.length > 0)
    return { found: data.found === true && snippets.length > 0, snippets, confidence }
  } catch (e) {
    if (e instanceof DOMException && e.name === 'AbortError') {
      throw e
    }
    return { found: false, snippets: [], confidence: 'none' }
  }
}

function parseLookupConfidence(value: unknown, found: boolean): RealtimeLookupConfidence {
  if (value === 'high' || value === 'low' || value === 'none') {
    return value
  }
  return found ? 'high' : 'none'
}

/**
 * POST WebRTC SDP offer to the backend; returns SDP answer text for setRemoteDescription.
 */
export async function exchangeRealtimeSdp(
  offerSdp: string,
  language: SpeechUiLang,
  options?: Partial<RealtimeVoiceSessionOptions>,
  modelId?: string,
): Promise<{ ok: true; answerSdp: string } | RealtimeSdpFailure> {
  const headers: Record<string, string> = {
    'Content-Type': 'application/sdp',
    'X-Chat-Language': language,
    'X-Realtime-Voice': isRealtimeVoice(options?.voice) ? options.voice : DEFAULT_REALTIME_VOICE,
    'X-Realtime-Reasoning-Effort': isRealtimeReasoningEffort(options?.reasoningEffort)
      ? options.reasoningEffort
      : DEFAULT_REALTIME_REASONING_EFFORT,
    'X-Realtime-Vad-Eagerness': isRealtimeVadEagerness(options?.vadEagerness)
      ? options.vadEagerness
      : DEFAULT_REALTIME_VAD_EAGERNESS,
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
  return parseRealtimeApiFailure(r)
}

/** Client-enforced live session cap for public voice (aligned with UI copy). */
export const REALTIME_SESSION_MAX_MS = 300_000

/** Client-enforced live session cap for admin voice interviews. */
export const INTERVIEW_REALTIME_SESSION_MAX_MS = 1_800_000

type RealtimeFetchFailureResponse = {
  status: number
  data: unknown
  headers?: Headers
}

function parseRealtimeApiFailure(r: RealtimeFetchFailureResponse): RealtimeSdpFailure {
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
