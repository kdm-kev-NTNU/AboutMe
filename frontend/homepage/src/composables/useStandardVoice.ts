import { computed, onUnmounted, ref, type ComputedRef } from 'vue'
import { lookupRealtimeInfo, type RealtimeLookupResponse, type SpeechUiLang } from '@/lib/realtime-voice'
import { formatLookupForSpeech } from '@/lib/voice-answer'
import { synthesizeSpeech } from '@/lib/synthesize-speech'
import { isTranscriptUncertain, repeatRequestMessage } from '@/lib/transcript-quality'
import { useSpeechTranscription } from '@/composables/useSpeechTranscription'
import { captureProductAnalyticsEvent } from '@/lib/analytics'
import { POSTHOG_VOICE_EVENTS } from '@/lib/posthog-sdk'

type StandardVoiceStage = 'idle' | 'recording' | 'transcribing' | 'looking_up' | 'speaking' | 'error'

const STANDARD_RETRY_DELAY_MS = 400

function sleep(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

function isRetryableSynthesisFailure(result: { ok: false; status: number }): boolean {
  return result.status === 0 || result.status >= 500
}

type UseStandardVoiceOptions = {
  language: ComputedRef<SpeechUiLang>
  languageConfirmed: ComputedRef<boolean>
}

export function useStandardVoice(options: UseStandardVoiceOptions) {
  const stage = ref<StandardVoiceStage>('idle')
  const errorMessage = ref('')
  const transcriptText = ref('')
  const answerText = ref('')
  const isWorking = computed(
    () =>
      stage.value === 'transcribing' ||
      stage.value === 'looking_up' ||
      stage.value === 'speaking' ||
      stage.value === 'recording',
  )
  const canCancel = computed(
    () =>
      stage.value === 'recording' ||
      stage.value === 'transcribing' ||
      stage.value === 'looking_up' ||
      stage.value === 'speaking',
  )

  let currentAudio: HTMLAudioElement | null = null
  let currentObjectUrl: string | null = null
  let pipelineAbort: AbortController | null = null
  let turnCancelled = false

  function stopPlayback() {
    if (currentAudio) {
      currentAudio.pause()
      currentAudio.onended = null
      currentAudio = null
    }
    if (currentObjectUrl) {
      URL.revokeObjectURL(currentObjectUrl)
      currentObjectUrl = null
    }
  }

  function beginTurn() {
    turnCancelled = false
    pipelineAbort?.abort()
    pipelineAbort = new AbortController()
    return pipelineAbort.signal
  }

  function isTurnActive(signal: AbortSignal): boolean {
    return !turnCancelled && !signal.aborted
  }

  const transcriptionApi = useSpeechTranscription({
    language: options.language,
    maxChars: 3000,
    isBlocked: computed(
      () =>
        stage.value === 'transcribing' ||
        stage.value === 'looking_up' ||
        stage.value === 'speaking' ||
        !options.languageConfirmed.value,
    ),
    onTranscript: (text) => {
      void processTranscript(text)
    },
  })

  async function synthesizeWithRetry(text: string, signal: AbortSignal) {
    let result = await synthesizeSpeech(text, options.language.value, signal)
    if (!result.ok && isRetryableSynthesisFailure(result) && isTurnActive(signal)) {
      await sleep(STANDARD_RETRY_DELAY_MS)
      if (isTurnActive(signal)) {
        result = await synthesizeSpeech(text, options.language.value, signal)
      }
    }
    return result
  }

  async function speakAnswer(text: string, signal: AbortSignal) {
    answerText.value = text
    const synthesized = await synthesizeWithRetry(text, signal)
    if (!isTurnActive(signal)) {
      return
    }
    if (!synthesized.ok) {
      stage.value = 'error'
      errorMessage.value = synthesized.message
      captureProductAnalyticsEvent(POSTHOG_VOICE_EVENTS.STANDARD_TURN_COMPLETED, {
        status: 'error',
        stage: 'synthesis',
      })
      return
    }

    stopPlayback()
    currentObjectUrl = URL.createObjectURL(synthesized.blob)
    currentAudio = new Audio(currentObjectUrl)
    stage.value = 'speaking'
    currentAudio.onended = () => {
      if (isTurnActive(signal)) {
        stage.value = 'idle'
      }
    }
    try {
      await currentAudio.play()
      if (isTurnActive(signal)) {
        captureProductAnalyticsEvent(POSTHOG_VOICE_EVENTS.STANDARD_TURN_COMPLETED, {
          status: 'success',
        })
      }
    } catch {
      if (!isTurnActive(signal)) {
        return
      }
      stage.value = 'error'
      errorMessage.value =
        options.language.value === 'no'
          ? 'Kunne ikke spille av syntetisert lyd.'
          : 'Could not play synthesized audio.'
      captureProductAnalyticsEvent(POSTHOG_VOICE_EVENTS.STANDARD_TURN_COMPLETED, {
        status: 'error',
        stage: 'playback',
      })
    }
  }

  async function processTranscript(text: string) {
    if (turnCancelled) {
      return
    }
    const signal = pipelineAbort?.signal ?? beginTurn()
    transcriptText.value = text
    errorMessage.value = ''

    if (isTranscriptUncertain(text)) {
      await speakAnswer(repeatRequestMessage(options.language.value), signal)
      return
    }

    stage.value = 'looking_up'
    let lookup: RealtimeLookupResponse = { found: false, snippets: [], confidence: 'none' }
    try {
      lookup = await lookupRealtimeInfo(text, options.language.value, signal)
    } catch (e) {
      if (e instanceof DOMException && e.name === 'AbortError') {
        return
      }
      if (isTurnActive(signal)) {
        await sleep(STANDARD_RETRY_DELAY_MS)
        if (isTurnActive(signal)) {
          try {
            lookup = await lookupRealtimeInfo(text, options.language.value, signal)
          } catch (retryError) {
            if (retryError instanceof DOMException && retryError.name === 'AbortError') {
              return
            }
            throw retryError
          }
        } else {
          throw e
        }
      } else {
        throw e
      }
    }
    if (!isTurnActive(signal)) {
      return
    }

    const answer = formatLookupForSpeech(lookup, options.language.value)
    try {
      await speakAnswer(answer, signal)
    } catch (e) {
      if (e instanceof DOMException && e.name === 'AbortError') {
        return
      }
      throw e
    }
  }

  function cancel() {
    turnCancelled = true
    pipelineAbort?.abort()
    pipelineAbort = null
    stopPlayback()
    transcriptionApi.cancel()
    stage.value = 'idle'
    errorMessage.value = ''
  }

  async function toggleRecording() {
    if (!options.languageConfirmed.value) {
      errorMessage.value =
        options.language.value === 'no'
          ? 'Velg språk først for robust stemmemodus.'
          : 'Choose language first for standard voice mode.'
      stage.value = 'error'
      return
    }
    if (!transcriptionApi.isRecording.value) {
      beginTurn()
      stage.value = 'recording'
      errorMessage.value = ''
    }
    await transcriptionApi.toggleVoiceInput()
    if (turnCancelled) {
      return
    }
    if (transcriptionApi.isRecording.value) {
      stage.value = 'recording'
    } else if (transcriptionApi.isTranscribing.value) {
      stage.value = 'transcribing'
    }
    if (transcriptionApi.voiceError.value) {
      stage.value = 'error'
      errorMessage.value = transcriptionApi.voiceError.value
    }
  }

  onUnmounted(() => {
    cancel()
  })

  return {
    stage,
    errorMessage,
    transcriptText,
    answerText,
    isWorking,
    canCancel,
    isRecording: transcriptionApi.isRecording,
    isTranscribing: transcriptionApi.isTranscribing,
    recordingMediaStream: transcriptionApi.recordingMediaStream,
    supportsSpeechInput: transcriptionApi.supportsSpeechInput,
    toggleRecording,
    cancel,
    stopPlayback,
  }
}
