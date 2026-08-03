import { customFetch } from '@/api/orval-mutator'
import { formatAdminHttpError } from '@/lib/api-error'
import type {
  SpeechUiLang,
  RealtimeVoiceSessionOptions,
  RealtimeSdpFailure,
  RealtimeVoiceChoice,
  RealtimeReasoningEffort,
} from '@/lib/realtime-voice'

const DEFAULT_REALTIME_VOICE: RealtimeVoiceChoice = 'marin'
const DEFAULT_REALTIME_REASONING_EFFORT: RealtimeReasoningEffort = 'low'

export type InterviewTurn = {
  role: 'user' | 'interviewer'
  text: string
  sequenceNo: number
}

export type InterviewDocument = {
  id: string
  originalFilename: string
  mimeType?: string
  charCount: number
  createdBy?: string
  createdAt: string
}

export type InterviewSession = {
  id: string
  documentId: string
  language: string
  status: string
  voice?: string
  startedAt: string
  endedAt?: string
  transcriptId?: string
  cleanStatus?: string
  ingestedDocumentId?: string
  turns?: InterviewTurn[]
}

export type InterviewTranscript = {
  id: string
  sessionId: string
  rawText?: string
  cleanedText?: string
  cleanStatus: string
  ingestedDocumentId?: string
  createdAt: string
  cleanedAt?: string
}

export async function uploadInterviewDocument(file: File): Promise<InterviewDocument> {
  const form = new FormData()
  form.append('file', file)
  const r = await customFetch<{ data: InterviewDocument; status: number }>(
    '/admin/tools/interview/documents',
    { method: 'POST', body: form },
  )
  if (r.status !== 200) throw new Error(formatAdminHttpError(r.status, r.data))
  return r.data
}

export async function createInterviewTextDocument(text: string, filename?: string): Promise<InterviewDocument> {
  const r = await customFetch<{ data: InterviewDocument; status: number }>(
    '/admin/tools/interview/documents/text',
    {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ text, filename }),
    },
  )
  if (r.status !== 200) throw new Error(formatAdminHttpError(r.status, r.data))
  return r.data
}

export async function createInterviewSession(
  documentId: string,
  language: SpeechUiLang,
  voice?: string,
): Promise<InterviewSession> {
  const r = await customFetch<{ data: InterviewSession; status: number }>('/admin/tools/interview/sessions', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ documentId, language, voice }),
  })
  if (r.status !== 200) throw new Error(formatAdminHttpError(r.status, r.data))
  return r.data
}

export async function appendInterviewTurns(sessionId: string, turns: InterviewTurn[]): Promise<void> {
  const r = await customFetch<{ status: number; data?: unknown }>(
    `/admin/tools/interview/sessions/${sessionId}/turns`,
    {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        turns: turns.map((t) => ({ role: t.role, text: t.text, sequenceNo: t.sequenceNo })),
      }),
    },
  )
  if (r.status !== 200) throw new Error(formatAdminHttpError(r.status, r.data))
}

export async function finalizeInterviewSession(sessionId: string): Promise<InterviewTranscript> {
  const r = await customFetch<{ data: InterviewTranscript; status: number }>(
    `/admin/tools/interview/sessions/${sessionId}/finalize`,
    { method: 'POST' },
  )
  if (r.status !== 200) throw new Error(formatAdminHttpError(r.status, r.data))
  return r.data
}

export async function cleanInterviewTranscript(transcriptId: string): Promise<InterviewTranscript> {
  const r = await customFetch<{ data: InterviewTranscript; status: number }>(
    `/admin/tools/interview/transcripts/${transcriptId}/clean`,
    { method: 'POST' },
  )
  if (r.status !== 200) throw new Error(formatAdminHttpError(r.status, r.data))
  return r.data
}

export async function ingestInterviewTranscript(transcriptId: string, force = false): Promise<unknown> {
  const q = force ? '?force=true' : ''
  const r = await customFetch<{ data: unknown; status: number }>(
    `/admin/tools/interview/transcripts/${transcriptId}/ingest${q}`,
    { method: 'POST' },
  )
  if (r.status !== 200) throw new Error(formatAdminHttpError(r.status, r.data))
  return r.data
}

export async function exchangeInterviewRealtimeSdp(
  sessionId: string,
  offerSdp: string,
  language: SpeechUiLang,
  options?: Partial<RealtimeVoiceSessionOptions>,
  modelId?: string,
): Promise<{ ok: true; answerSdp: string } | RealtimeSdpFailure> {
  const voice =
    options?.voice === 'marin' || options?.voice === 'cedar' ? options.voice : DEFAULT_REALTIME_VOICE
  const reasoning =
    options?.reasoningEffort === 'low' ||
    options?.reasoningEffort === 'medium' ||
    options?.reasoningEffort === 'high'
      ? options.reasoningEffort
      : DEFAULT_REALTIME_REASONING_EFFORT
  const headers: Record<string, string> = {
    'Content-Type': 'application/sdp',
    'X-Chat-Language': language,
    'X-Realtime-Voice': voice,
    'X-Realtime-Reasoning-Effort': reasoning,
  }
  if (modelId && modelId.trim() !== '') {
    headers['X-Realtime-Model'] = modelId.trim()
  }
  const r = await customFetch<{ data: unknown; status: number; headers?: Headers }>(
    `/admin/tools/interview/sessions/${sessionId}/realtime/session`,
    { method: 'POST', body: offerSdp, headers },
  )
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
  return { ok: false, status: r.status, message: msg || `HTTP ${r.status}`, ...(code ? { code } : {}) }
}
