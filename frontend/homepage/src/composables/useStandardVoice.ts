import { computed, onUnmounted, ref, type ComputedRef } from 'vue'
import { lookupRealtimeInfo, type SpeechUiLang } from '@/lib/realtime-voice'
import { formatLookupForSpeech } from '@/lib/voice-answer'
import { synthesizeSpeech } from '@/lib/synthesize-speech'
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
    () => stage.value === 'transcribing' || stage.value === 'looking_up' || stage.value === 'speaking',
  )

  let currentAudio: HTMLAudioElement | null = null
  let currentObjectUrl: string | null = null

  function stopPlayback() {
    if (currentAudio) {
      currentAudio.pause()
      currentAudio = null
    }
    if (currentObjectUrl) {
      URL.revokeObjectURL(currentObjectUrl)
      currentObjectUrl = null
    }
  }

  const transcriptionApi = useSpeechTranscription({
    language: options.language,
    maxChars: 3000,
    isBlocked: computed(() => isWorking.value || !options.languageConfirmed.value),
    onTranscript: (text) => {
      void processTranscript(text)
    },
  })

  async function processTranscript(text: string) {
    transcriptText.value = text
    errorMessage.value = ''
    stage.value = 'looking_up'
    const lookup = await lookupRealtimeInfo(text, options.language.value)
    const answer = formatLookupForSpeech(lookup, options.language.value)
    answerText.value = answer

    const synthesized = await synthesizeSpeech(answer, options.language.value)
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
      stage.value = 'idle'
    }
    try {
      await currentAudio.play()
      captureProductAnalyticsEvent(POSTHOG_VOICE_EVENTS.STANDARD_TURN_COMPLETED, {
        status: 'success',
      })
    } catch {
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
      stage.value = 'recording'
      errorMessage.value = ''
    }
    await transcriptionApi.toggleVoiceInput()
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

  onUnmounted(() => stopPlayback())

  return {
    stage,
    errorMessage,
    transcriptText,
    answerText,
    isWorking,
    isRecording: transcriptionApi.isRecording,
    isTranscribing: transcriptionApi.isTranscribing,
    recordingMediaStream: transcriptionApi.recordingMediaStream,
    supportsSpeechInput: transcriptionApi.supportsSpeechInput,
    toggleRecording,
    stopPlayback,
  }
}
