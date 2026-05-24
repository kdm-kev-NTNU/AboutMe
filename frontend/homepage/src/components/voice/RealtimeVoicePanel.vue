<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { Mic, MicOff, Loader2, TriangleAlert } from 'lucide-vue-next'
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import AiStatusDialog from '@/components/AiStatusDialog.vue'
import { useVoiceModelStore } from '@/stores/voice-model'
import { useRealtimeVoice } from '@/composables/useRealtimeVoice'
import type { RealtimeReasoningEffort, RealtimeVoiceChoice } from '@/lib/realtime-voice'

const props = defineProps<{
  language: 'en' | 'no'
  available: boolean | null
  voiceOptions: RealtimeVoiceChoice[]
  reasoningOptions: RealtimeReasoningEffort[]
  defaultVoice: RealtimeVoiceChoice
  defaultReasoningEffort: RealtimeReasoningEffort
}>()

const voiceModelStore = useVoiceModelStore()
const selectedVoice = ref<RealtimeVoiceChoice>(props.defaultVoice)
const selectedReasoningEffort = ref<RealtimeReasoningEffort>(props.defaultReasoningEffort)
const aiErrorDialogOpen = ref(false)
const selectedRealtimeOptions = computed(() => ({
  voice: selectedVoice.value,
  reasoningEffort: selectedReasoningEffort.value,
}))
const selectedVoiceModel = computed(() => voiceModelStore.selectedModel)
const selectedVoiceProvider = computed(() => selectedVoiceModel.value?.provider ?? 'OPENAI')

const {
  connectionState,
  errorMessage,
  sessionNotice,
  assistantTranscript,
  userTranscript,
  connect,
  disconnect,
  maxSessionMs,
} = useRealtimeVoice(computed(() => props.language), selectedRealtimeOptions, selectedVoiceModel)

const copy = computed(() => {
  const en = props.language === 'en'
  return {
    unavailable: en
      ? 'Live voice is not enabled on the server right now.'
      : 'Live stemmechat er ikke slått på hos serveren akkurat nå.',
    connect: en ? 'Start live voice' : 'Start live stemme',
    disconnect: en ? 'End session' : 'Avslutt',
    connecting: en ? 'Connecting…' : 'Kobler til…',
    live: en ? 'Live' : 'Aktiv',
    modelLabel: en ? 'Provider/model' : 'Leverandør/modell',
    voiceLabel: en ? 'Voice' : 'Stemme',
    reasoningLabel: en ? 'Reasoning' : 'Resonnering',
    you: en ? 'You (transcript)' : 'Du (transkripsjon)',
    assistant: en ? 'Assistant (transcript)' : 'Assistent (transkripsjon)',
    warningTitle: en ? 'Experimental mode' : 'Eksperimentell modus',
    warningBody: en
      ? 'Live WebRTC voice is faster, but can be unstable. Sessions can drop and each session ends after ~3 minutes.'
      : 'Live WebRTC-stemme er raskere, men kan være ustabil. Økter kan falle ut og avsluttes etter ca. 3 minutter.',
  }
})

const voiceLabels: Record<RealtimeVoiceChoice, string> = { marin: 'Marin', cedar: 'Cedar' }
const reasoningLabels = computed<Record<RealtimeReasoningEffort, string>>(() => ({
  low: props.language === 'en' ? 'Fast' : 'Rask',
  medium: props.language === 'en' ? 'Balanced' : 'Balansert',
  high: props.language === 'en' ? 'Thorough' : 'Grundig',
}))

const sessionControlsDisabled = computed(
  () => connectionState.value === 'connecting' || connectionState.value === 'connected',
)

const errorDialogCopy = computed(() => ({
  title: props.language === 'en' ? 'Voice could not start' : 'Stemme kunne ikke starte',
  description:
    props.language === 'en'
      ? 'The live AI service needs a fresh session before it can continue.'
      : 'Live AI-tjenesten trenger en ny økt før den kan fortsette.',
  retry: props.language === 'en' ? 'Try again' : 'Prøv igjen',
}))

watch(errorMessage, (message) => {
  aiErrorDialogOpen.value = message.trim() !== ''
})

function setVoiceModelFromEvent(event: Event) {
  const target = event.target
  if (target instanceof HTMLSelectElement) {
    voiceModelStore.setSelectedModelId(target.value)
  }
}
</script>

<template>
  <AiStatusDialog
    v-model:open="aiErrorDialogOpen"
    :title="errorDialogCopy.title"
    :description="errorDialogCopy.description"
    :message="errorMessage"
    :retry-label="errorDialogCopy.retry"
    show-retry
    @retry="connect"
  />

  <Alert class="mb-4 border-amber-300 bg-amber-50/90 text-amber-900">
    <TriangleAlert class="size-4" aria-hidden="true" />
    <AlertTitle>{{ copy.warningTitle }}</AlertTitle>
    <AlertDescription>{{ copy.warningBody }}</AlertDescription>
  </Alert>

  <div
    v-if="available === false"
    class="rounded-2xl border border-amber-200 bg-amber-50/90 px-4 py-3 text-center text-sm text-amber-900"
  >
    {{ copy.unavailable }}
  </div>

  <template v-else-if="available === true">
    <div class="flex flex-col items-center gap-6">
      <div
        class="relative flex h-32 w-32 items-center justify-center rounded-full bg-gradient-to-br from-blue-600 to-indigo-700 shadow-xl shadow-blue-500/25"
        :class="{ 'ring-4 ring-blue-400/50 animate-pulse': connectionState === 'connected' }"
      >
        <Loader2 v-if="connectionState === 'connecting'" class="size-14 animate-spin text-white" aria-hidden="true" />
        <Mic v-else class="size-14 text-white opacity-90" aria-hidden="true" />
      </div>

      <p v-if="connectionState === 'connected'" class="text-sm font-medium text-green-700">
        {{ copy.live }} · ~{{ Math.round(maxSessionMs / 60_000) }} min max
      </p>

      <label class="w-full max-w-md text-left text-xs font-semibold uppercase text-slate-600">
        {{ copy.modelLabel }}
        <select
          :value="voiceModelStore.selectedModelId"
          data-testid="voice-model-select"
          class="mt-1 h-10 w-full rounded-lg border border-blue-100 bg-white/90 px-3 text-sm font-medium normal-case text-slate-800 shadow-sm disabled:cursor-not-allowed disabled:opacity-60"
          :disabled="sessionControlsDisabled"
          @change="setVoiceModelFromEvent"
        >
          <option
            v-for="model in voiceModelStore.models"
            :key="model.provider + ':' + model.id"
            :value="model.provider + ':' + model.id"
          >
            {{ model.label }}
          </option>
        </select>
      </label>

      <div v-if="selectedVoiceProvider === 'OPENAI'" class="grid w-full max-w-md grid-cols-1 gap-3 sm:grid-cols-2">
        <label class="text-left text-xs font-semibold uppercase text-slate-600">
          {{ copy.voiceLabel }}
          <select
            v-model="selectedVoice"
            data-testid="voice-select"
            class="mt-1 h-10 w-full rounded-lg border border-blue-100 bg-white/90 px-3 text-sm font-medium normal-case text-slate-800 shadow-sm disabled:cursor-not-allowed disabled:opacity-60"
            :disabled="sessionControlsDisabled"
          >
            <option v-for="voice in voiceOptions" :key="voice" :value="voice">{{ voiceLabels[voice] }}</option>
          </select>
        </label>

        <label class="text-left text-xs font-semibold uppercase text-slate-600">
          {{ copy.reasoningLabel }}
          <select
            v-model="selectedReasoningEffort"
            data-testid="reasoning-select"
            class="mt-1 h-10 w-full rounded-lg border border-blue-100 bg-white/90 px-3 text-sm font-medium normal-case text-slate-800 shadow-sm disabled:cursor-not-allowed disabled:opacity-60"
            :disabled="sessionControlsDisabled"
          >
            <option v-for="effort in reasoningOptions" :key="effort" :value="effort">{{ reasoningLabels[effort] }}</option>
          </select>
        </label>
      </div>

      <div class="flex flex-wrap justify-center gap-3">
        <Button v-if="connectionState === 'idle' || connectionState === 'error'" type="button" @click="connect">
          {{ copy.connect }}
        </Button>
        <Button v-if="connectionState === 'connecting'" type="button" variant="secondary" disabled>
          {{ copy.connecting }}
        </Button>
        <Button
          v-if="connectionState === 'connected'"
          type="button"
          variant="outline"
          class="rounded-2xl border-red-200 text-red-700 hover:bg-red-50"
          @click="disconnect"
        >
          <MicOff class="me-2 inline size-4" aria-hidden="true" />
          {{ copy.disconnect }}
        </Button>
      </div>
    </div>

    <Alert v-if="sessionNotice" class="mt-6 border-blue-200 bg-blue-50 text-slate-800">
      <AlertDescription>{{ sessionNotice }}</AlertDescription>
    </Alert>

    <div
      v-if="connectionState === 'connected' || userTranscript || assistantTranscript"
      class="mt-8 space-y-4 rounded-2xl border border-blue-100 bg-white/85 p-4 shadow-sm backdrop-blur-md"
    >
      <div>
        <p class="text-xs font-semibold uppercase tracking-wide text-slate-500">{{ copy.you }}</p>
        <p class="mt-1 whitespace-pre-wrap text-sm text-slate-800">{{ userTranscript || '…' }}</p>
      </div>
      <div>
        <p class="text-xs font-semibold uppercase tracking-wide text-slate-500">{{ copy.assistant }}</p>
        <p class="mt-1 whitespace-pre-wrap text-sm text-slate-800">{{ assistantTranscript || '…' }}</p>
      </div>
    </div>
  </template>

  <div v-else class="flex justify-center py-12">
    <Loader2 class="size-8 animate-spin text-blue-600" aria-hidden="true" />
  </div>
</template>
