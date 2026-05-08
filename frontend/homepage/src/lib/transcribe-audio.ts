import { customFetch } from '@/api/orval-mutator'

export type TranscribeSpeechResult = {
  data: unknown
  status: number
  headers: Headers
}

/**
 * POST multipart audio to the backend (same /api prefix and Basic auth as Orval-generated clients).
 */
export async function transcribeSpeech(
  blob: Blob,
  lang: 'no' | 'en',
): Promise<TranscribeSpeechResult> {
  const fd = new FormData()
  fd.append('file', blob, 'recording.webm')
  return customFetch<TranscribeSpeechResult>('/transcribe', {
    method: 'POST',
    body: fd,
    headers: { 'X-Chat-Language': lang },
  })
}
