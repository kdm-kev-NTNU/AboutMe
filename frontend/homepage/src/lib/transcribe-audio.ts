import { customFetch } from '@/api/orval-mutator'

export type TranscribeSpeechResult = {
  data: unknown
  status: number
  headers: Headers
}

export type TranscribeLanguage = 'en' | 'no'

/**
 * POST multipart audio to the backend (same /api prefix and Basic auth as Orval-generated clients).
 *
 * Passes the UI language as the `X-Chat-Language` header so the backend can hint Whisper. The
 * backend retries with the other language on failure (see `TranscriptionService` fallback), so
 * mismatched UI/spoken language still produces a transcript.
 */
export async function transcribeSpeech(
  blob: Blob,
  language?: TranscribeLanguage,
): Promise<TranscribeSpeechResult> {
  const fd = new FormData()
  fd.append('file', blob, 'recording.webm')
  // Note: do NOT set Content-Type manually; the browser must set the multipart boundary.
  const headers: Record<string, string> = {}
  if (language === 'en' || language === 'no') {
    headers['X-Chat-Language'] = language
  }
  return customFetch<TranscribeSpeechResult>('/transcribe', {
    method: 'POST',
    body: fd,
    headers,
  })
}
