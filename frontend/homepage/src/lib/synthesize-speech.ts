import type { SpeechUiLang } from './realtime-voice'

export type SynthesizeResult =
  | { ok: true; blob: Blob }
  | { ok: false; status: number; message: string }

export async function synthesizeSpeech(
  text: string,
  language: SpeechUiLang,
  signal?: AbortSignal,
): Promise<SynthesizeResult> {
  try {
    const res = await fetch('/api/synthesize', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'X-Chat-Language': language,
      },
      credentials: 'include',
      body: JSON.stringify({ text }),
      signal,
    })
    if (!res.ok) {
      let message = `HTTP ${res.status}`
      try {
        const json = (await res.json()) as { error?: unknown }
        if (typeof json.error === 'string' && json.error.trim() !== '') {
          message = json.error
        }
      } catch {
        // ignore JSON parsing fallback
      }
      return { ok: false, status: res.status, message }
    }
    return { ok: true, blob: await res.blob() }
  } catch (e) {
    if (e instanceof DOMException && e.name === 'AbortError') {
      throw e
    }
    return { ok: false, status: 0, message: 'Network error' }
  }
}
