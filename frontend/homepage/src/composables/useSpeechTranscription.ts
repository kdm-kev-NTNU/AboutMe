import { computed, onUnmounted, ref, type ComputedRef } from 'vue'
import { transcribeSpeech } from '@/lib/transcribe-audio'

export type SpeechUiLang = 'en' | 'no'

export const MAX_SPEECH_PROMPT_CHARS = 3000

function readApiError(data: unknown): string | undefined {
  if (data && typeof data === 'object' && 'error' in data) {
    const err = (data as { error?: unknown }).error
    return typeof err === 'string' && err.length > 0 ? err : undefined
  }
  return undefined
}

export type UseSpeechTranscriptionOptions = {
  language: ComputedRef<SpeechUiLang>
  maxChars: number
  /** When true, starting/toggling recording is ignored (e.g. chat while answering). */
  isBlocked: ComputedRef<boolean>
  /** Called with trimmed transcript after successful STT (already capped by caller usage). */
  onTranscript: (text: string) => void
}

/**
 * Microphone capture + POST /transcribe; shared by chat and home landing.
 */
export function useSpeechTranscription(options: UseSpeechTranscriptionOptions) {
  const voiceError = ref('')
  const isRecording = ref(false)
  const isTranscribing = ref(false)
  const recordingMediaStream = ref<MediaStream | null>(null)

  let mediaRecorder: MediaRecorder | null = null
  const audioChunks: Blob[] = []

  const supportsSpeechInput = computed(
    () => typeof navigator !== 'undefined' && !!navigator.mediaDevices?.getUserMedia,
  )

  function stopMediaTracks() {
    recordingMediaStream.value?.getTracks().forEach((t) => t.stop())
    recordingMediaStream.value = null
  }

  async function startRecording() {
    voiceError.value = ''
    try {
      const stream = await navigator.mediaDevices!.getUserMedia({ audio: true })
      recordingMediaStream.value = stream
      const preferred =
        typeof MediaRecorder !== 'undefined' && MediaRecorder.isTypeSupported('audio/webm;codecs=opus')
          ? 'audio/webm;codecs=opus'
          : typeof MediaRecorder !== 'undefined' && MediaRecorder.isTypeSupported('audio/webm')
            ? 'audio/webm'
            : ''
      mediaRecorder = preferred
        ? new MediaRecorder(stream, { mimeType: preferred })
        : new MediaRecorder(stream)
      audioChunks.length = 0
      mediaRecorder.ondataavailable = (e) => {
        if (e.data.size > 0) audioChunks.push(e.data)
      }
      mediaRecorder.start(250)
      isRecording.value = true
    } catch {
      voiceError.value =
        options.language.value === 'en'
          ? 'Microphone permission is required for voice input.'
          : 'Mikrofontilgang kreves for taleinndata.'
      stopMediaTracks()
      mediaRecorder = null
    }
  }

  async function stopRecordingAndTranscribe() {
    const rec = mediaRecorder
    if (!rec || rec.state === 'inactive') {
      isRecording.value = false
      stopMediaTracks()
      mediaRecorder = null
      return
    }
    await new Promise<void>((resolve) => {
      rec.addEventListener('stop', () => resolve(), { once: true })
      try {
        rec.requestData()
      } catch {
        /* ignore */
      }
      rec.stop()
    })
    isRecording.value = false
    stopMediaTracks()
    mediaRecorder = null

    const blobType = audioChunks[0]?.type ?? 'audio/webm'
    const blob = new Blob(audioChunks, { type: blobType })
    audioChunks.length = 0
    if (blob.size === 0) {
      voiceError.value =
        options.language.value === 'en'
          ? 'No audio captured. Try again.'
          : 'Ingen lyd fanget opp. Prøv igjen.'
      return
    }

    isTranscribing.value = true
    voiceError.value = ''
    try {
      const auth = (await import('@/stores/auth')).useAuthStore()
      auth.restore()
      const r = await transcribeSpeech(blob)
      if (r.status === 200 && r.data && typeof r.data === 'object' && r.data !== null && 'text' in r.data) {
        const t = String((r.data as { text: unknown }).text ?? '').trim().slice(0, options.maxChars)
        if (t) {
          options.onTranscript(t)
        }
        return
      }
      if (r.status === 429) {
        voiceError.value =
          options.language.value === 'en'
            ? 'Too many requests or budget limit. Wait and try again.'
            : 'For mange forespørsler eller budsjettgrense. Vent litt og prøv igjen.'
        return
      }
      voiceError.value =
        readApiError(r.data) ??
        (options.language.value === 'en'
          ? 'Could not transcribe audio.'
          : 'Kunne ikke transkribere lyd.')
    } catch {
      voiceError.value =
        options.language.value === 'en'
          ? 'Network error during transcription.'
          : 'Nettverksfeil ved transkripsjon.'
    } finally {
      isTranscribing.value = false
    }
  }

  async function toggleVoiceInput() {
    if (!supportsSpeechInput.value || options.isBlocked.value || isTranscribing.value) return
    if (isRecording.value) {
      await stopRecordingAndTranscribe()
      return
    }
    await startRecording()
  }

  onUnmounted(() => {
    if (mediaRecorder && mediaRecorder.state !== 'inactive') {
      try {
        mediaRecorder.stop()
      } catch {
        /* ignore */
      }
    }
    stopMediaTracks()
  })

  return {
    supportsSpeechInput,
    isRecording,
    isTranscribing,
    recordingMediaStream,
    voiceError,
    toggleVoiceInput,
  }
}
