import { customFetch } from '@/api/orval-mutator'

export type TranscribeSpeechResult = {
  data: unknown
  status: number
  headers: Headers
}

/**
 * POST multipart audio to the backend (same /api prefix and Basic auth as Orval-generated clients).
 * Language is detected by the transcription model unless the server exposes an optional header later.
 */
export async function transcribeSpeech(blob: Blob): Promise<TranscribeSpeechResult> {
  const fd = new FormData()
  fd.append('file', blob, 'recording.webm')
  return customFetch<TranscribeSpeechResult>('/transcribe', {
    method: 'POST',
    body: fd,
  })
}
