import { computed, onUnmounted, ref, type ComputedRef } from 'vue'
import { lookupRealtimeInfo, type SpeechUiLang } from '@/lib/realtime-voice'
import { formatLookupForSpeech } from '@/lib/voice-answer'
import { synthesizeSpeech } from '@/lib/synthesize-speech'
import { isTranscriptUncertain, repeatRequestMessage } from '@/lib/transcript-quality'
import { useSpeechTranscription } from '@/composables/useSpeechTranscription'
import { captureProductAnalyticsEvent } from '@/lib/analytics'
import { POSTHOG_VOICE_EVENTS } from '@/lib/posthog-sdk'

type StandardVoiceStage = 'idle' | 'recording' | 'transcribing' | 'looking_up' | 'speaking' | 'error'

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

  async function speakAnswer(text: string, signal: AbortSignal) {
    answerText.value = text
    const synthesized = await synthesizeSpeech(text, options.language.value, signal)
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
    let lookup
    try {
      lookup = await lookupRealtimeInfo(text, options.language.value, signal)
    } catch (e) {
      if (e instanceof DOMException && e.name === 'AbortError') {
        return
      }
      throw e
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
