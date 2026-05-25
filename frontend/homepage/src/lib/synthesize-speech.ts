import { synthesizeSpeech as synthesizeSpeechApi, type ApiError } from '@/api/generated/portfolio'
import type { SpeechUiLang } from './realtime-voice'

export type SynthesizeResult =
  | { ok: true; blob: Blob }
  | { ok: false; status: number; message: string }

function isAudioBlob(value: unknown): value is Blob {
  return (
    value != null &&
    typeof value === 'object' &&
    typeof (value as Blob).arrayBuffer === 'function' &&
    typeof (value as Blob).size === 'number'
  )
}

export async function synthesizeSpeech(
  text: string,
  language: SpeechUiLang,
  signal?: AbortSignal,
): Promise<SynthesizeResult> {
  try {
    const res = await synthesizeSpeechApi(
      { text },
      {
        signal,
        headers: { 'X-Chat-Language': language },
      },
    )
    if (res.status === 200 && isAudioBlob(res.data)) {
      return { ok: true, blob: res.data }
    }
    if (res.data && typeof res.data === 'object' && 'error' in res.data) {
      const err = (res.data as ApiError).error
      if (typeof err === 'string' && err.trim() !== '') {
        return { ok: false, status: res.status, message: err }
      }
    }
    return { ok: false, status: res.status, message: `HTTP ${res.status}` }
  } catch (e) {
    if (e instanceof DOMException && e.name === 'AbortError') {
      throw e
    }
    return { ok: false, status: 0, message: 'Network error' }
  }
}
