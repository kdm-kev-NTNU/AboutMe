<script setup lang="ts">
import { computed, ref } from 'vue'
import { Mic, Square } from 'lucide-vue-next'
import { Button } from '@/components/ui/button'
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import AudioWaveform from '@/components/AudioWaveform.vue'
import { useStandardVoice } from '@/composables/useStandardVoice'

const props = defineProps<{
  language: 'en' | 'no'
  available: boolean | null
}>()

const selectedLanguage = ref<'en' | 'no'>(props.language)
const languageConfirmed = ref(false)

const languageComputed = computed(() => selectedLanguage.value)
const standard = useStandardVoice({
  language: languageComputed,
  languageConfirmed: computed(() => languageConfirmed.value),
})
const {
  stage,
  errorMessage,
  transcriptText,
  answerText,
  isWorking,
  isRecording,
  isTranscribing,
  recordingMediaStream,
  toggleRecording,
} = standard

const copy = computed(() => {
  const no = props.language === 'no'
  return {
    unavailable: no
      ? 'Standard stemmemodus er ikke tilgjengelig akkurat nå.'
      : 'Standard voice mode is not available right now.',
    pickLanguage: no ? 'Velg språk før opptak' : 'Choose language before recording',
    start: no ? 'Start opptak' : 'Start recording',
    stop: no ? 'Stopp opptak' : 'Stop recording',
    recording: no ? 'Tar opp...' : 'Recording...',
    transcript: no ? 'Du sa' : 'You said',
    answer: no ? 'Svar' : 'Answer',
    looking: no ? 'Søker i fakta...' : 'Looking up facts...',
    speaking: no ? 'Snakker...' : 'Speaking...',
    introTitle: no ? 'Robust stemmemodus' : 'Robust voice mode',
    introBody: no
      ? 'Denne modusen er tregere, men mer stabil. Den bruker transkribering, faktaoppslag og TTS i tur.'
      : 'This mode is slower, but more stable. It runs transcription, fact lookup, and TTS turn-by-turn.',
  }
})

function confirmLanguage(lang: 'en' | 'no') {
  selectedLanguage.value = lang
  languageConfirmed.value = true
}
</script>

<template>
  <div>
    <Alert class="mb-4 border-emerald-200 bg-emerald-50/90 text-emerald-900">
      <AlertTitle>{{ copy.introTitle }}</AlertTitle>
      <AlertDescription>{{ copy.introBody }}</AlertDescription>
    </Alert>

    <div v-if="available === false" class="rounded-2xl border border-amber-200 bg-amber-50/90 px-4 py-3 text-sm text-amber-900">
      {{ copy.unavailable }}
    </div>

    <div v-else class="space-y-4">
      <div class="rounded-2xl border border-blue-100 bg-white/85 p-4">
        <p class="mb-3 text-xs font-semibold uppercase tracking-wide text-slate-600">{{ copy.pickLanguage }}</p>
        <div class="grid grid-cols-2 gap-2">
          <button
            type="button"
            class="rounded-xl border px-3 py-2 text-sm font-semibold"
            :class="selectedLanguage === 'en' ? 'border-blue-300 bg-blue-50 text-blue-900' : 'border-slate-200 bg-white text-slate-700'"
            @click="confirmLanguage('en')"
          >
            English
          </button>
          <button
            type="button"
            class="rounded-xl border px-3 py-2 text-sm font-semibold"
            :class="selectedLanguage === 'no' ? 'border-blue-300 bg-blue-50 text-blue-900' : 'border-slate-200 bg-white text-slate-700'"
            @click="confirmLanguage('no')"
          >
            Norsk
          </button>
        </div>
      </div>

      <div class="rounded-2xl border border-blue-100 bg-white/85 p-4">
        <div class="flex items-center gap-3">
          <Button
            type="button"
            class="rounded-xl"
            :disabled="!languageConfirmed || isWorking"
            @click="toggleRecording"
          >
            <Square v-if="isRecording" class="me-2 size-4" aria-hidden="true" />
            <Mic v-else class="me-2 size-4" aria-hidden="true" />
            {{
              isRecording
                ? copy.stop
                : isTranscribing
                  ? copy.recording
                  : copy.start
            }}
          </Button>
          <span class="text-sm text-slate-600">
            {{ stage === 'looking_up' ? copy.looking : stage === 'speaking' ? copy.speaking : '' }}
          </span>
        </div>
        <AudioWaveform
          v-if="isRecording"
          class="mt-3 h-12"
          :stream="recordingMediaStream"
          :aria-label="copy.recording"
        />
      </div>

      <Alert v-if="errorMessage" class="border-amber-200 bg-amber-50 text-amber-900">
        <AlertDescription>{{ errorMessage }}</AlertDescription>
      </Alert>

      <div v-if="transcriptText || answerText" class="space-y-4 rounded-2xl border border-blue-100 bg-white/85 p-4">
        <div>
          <p class="text-xs font-semibold uppercase tracking-wide text-slate-500">{{ copy.transcript }}</p>
          <p class="mt-1 whitespace-pre-wrap text-sm text-slate-800">{{ transcriptText || '…' }}</p>
        </div>
        <div>
          <p class="text-xs font-semibold uppercase tracking-wide text-slate-500">{{ copy.answer }}</p>
          <p class="mt-1 whitespace-pre-wrap text-sm text-slate-800">{{ answerText || '…' }}</p>
        </div>
      </div>
    </div>
  </div>
</template>
