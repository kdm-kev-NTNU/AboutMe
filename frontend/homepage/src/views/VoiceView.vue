<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { RouterLink } from 'vue-router'
import { Mic, MicOff, Loader2, Info, MessageSquare } from 'lucide-vue-next'
import { useLangStore } from '@/stores/lang'
import { useVoiceModelStore } from '@/stores/voice-model'
import { Button } from '@/components/ui/button'
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import AiStatusDialog from '@/components/AiStatusDialog.vue'
import { useRealtimeVoice } from '@/composables/useRealtimeVoice'
import {
  fetchRealtimeVoiceStatus,
  type RealtimeReasoningEffort,
  type RealtimeVoiceChoice,
} from '@/lib/realtime-voice'

const langStore = useLangStore()
const voiceModelStore = useVoiceModelStore()
const language = computed(() => langStore.language)

const voiceAvailable = ref<boolean | null>(null)
const voiceOptions = ref<RealtimeVoiceChoice[]>(['marin', 'cedar'])
const reasoningOptions = ref<RealtimeReasoningEffort[]>(['low', 'medium', 'high'])
const selectedVoice = ref<RealtimeVoiceChoice>('marin')
const selectedReasoningEffort = ref<RealtimeReasoningEffort>('low')
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
} = useRealtimeVoice(language, selectedRealtimeOptions, selectedVoiceModel)

onMounted(async () => {
  const [status] = await Promise.all([
    fetchRealtimeVoiceStatus(),
    voiceModelStore.ensureModelsLoaded(),
  ])
  voiceAvailable.value = status.enabled && voiceModelStore.hasModels
  voiceOptions.value = status.voices
  reasoningOptions.value = status.reasoningEfforts
  selectedVoice.value = status.voice
  selectedReasoningEffort.value = status.reasoningEffort
})

const copy = computed(() => {
  const en = language.value === 'en'
  return {
    title: en ? "Talk with Kevin's AI" : 'Snakk med Kevin sin AI',
    subtitle: en
      ? "Real-time voice with Kevin's AI — powered by OpenAI or ElevenLabs."
      : 'Sanntidsstemme med Kevin sin AI — drevet av OpenAI eller ElevenLabs.',
    chatAlt: en ? 'Use text chat instead' : 'Bruk tekstchat',
    unavailable: en
      ? 'Voice chat is not enabled on the server right now.'
      : 'Stemmechat er ikke slått på hos serveren akkurat nå.',
    connect: en ? 'Start voice' : 'Start stemme',
    disconnect: en ? 'End session' : 'Avslutt',
    connecting: en ? 'Connecting…' : 'Kobler til…',
    live: en ? 'Live' : 'Aktiv',
    modelLabel: en ? 'Provider/model' : 'Leverandør/modell',
    voiceLabel: en ? 'Voice' : 'Stemme',
    reasoningLabel: en ? 'Reasoning' : 'Reasoning',
    you: en ? 'You (transcript)' : 'Du (transkripsjon)',
    assistant: en ? 'Assistant (transcript)' : 'Assistent (transkripsjon)',
    disclaimerTitle: en ? 'Before you use voice' : 'Før du bruker stemme',
    disclaimerBody: en
      ? `You are talking to an AI, not Kevin himself. Replies can be wrong. Audio is processed by ${selectedVoiceProvider.value === 'ELEVENLABS' ? 'ElevenLabs' : 'OpenAI'} in real time (this page uses WebRTC). Each session ends automatically after about 3 minutes.`
      : `Du snakker med en KI, ikke Kevin selv. Svar kan være feil. Lyd behandles av ${selectedVoiceProvider.value === 'ELEVENLABS' ? 'ElevenLabs' : 'OpenAI'} i sanntid (denne siden bruker WebRTC). Hver økt avsluttes automatisk etter ca. 3 minutter.`,
  }
})

const voiceLabels: Record<RealtimeVoiceChoice, string> = {
  marin: 'Marin',
  cedar: 'Cedar',
}

const reasoningLabels = computed<Record<RealtimeReasoningEffort, string>>(() => {
  const en = language.value === 'en'
  return {
    low: en ? 'Fast' : 'Rask',
    medium: en ? 'Balanced' : 'Balansert',
    high: en ? 'Thorough' : 'Grundig',
  }
})

const sessionControlsDisabled = computed(
  () => connectionState.value === 'connecting' || connectionState.value === 'connected',
)

const statusLabel = computed(() => {
  if (connectionState.value === 'connecting') return copy.value.connecting
  if (connectionState.value === 'connected') return copy.value.live
  return ''
})

const errorDialogCopy = computed(() => {
  const en = language.value === 'en'
  const message = errorMessage.value.toLowerCase()
  const microphoneDenied =
    message.includes('microphone permission denied') || message.includes('mikrofontilgang ble nektet')
  const rateLimited =
    message.includes('too many voice requests') ||
    message.includes('for mange stemmeforespørsler') ||
    message.includes('too many voice session starts from this network') ||
    message.includes('rate-limited') ||
    message.includes('rate limited')
  const agentDisconnected =
    message.includes('the voice agent could not start') ||
    message.includes('agent disconnected') ||
    message.includes('stemmeagenten kunne ikke starte')

  if (microphoneDenied) {
    return {
      title: en ? 'Microphone access is blocked' : 'Mikrofontilgang er blokkert',
      description: en
        ? 'Allow microphone access in your browser, then click Try again.'
        : 'Gi nettleseren tilgang til mikrofonen, og trykk Prøv igjen.',
      retry: en ? 'Try again' : 'Prøv igjen',
    }
  }

  if (rateLimited) {
    return {
      title: en ? 'Voice is rate-limited' : 'Stemme er rate-limitert',
      description: en
        ? 'Wait for the cooldown to expire, then try again.'
        : 'Vent til nedkjølingen er over, og prøv igjen.',
      retry: en ? 'Try again' : 'Prøv igjen',
    }
  }

  if (agentDisconnected) {
    return {
      title: en ? 'Voice agent disconnected' : 'Stemmeagenten koblet fra',
      description: en
        ? 'The live voice session ended unexpectedly. Start a new session and try again.'
        : 'Stemmeøkten stoppet uventet. Start en ny økt og prøv igjen.',
      retry: en ? 'Try again' : 'Prøv igjen',
    }
  }

  return {
    title: en ? 'Voice could not start' : 'Stemme kunne ikke starte',
    description: en
      ? 'The live AI service needs a fresh session before it can continue.'
      : 'Live AI-tjenesten trenger en ny økt før den kan fortsette.',
    retry: en ? 'Try again' : 'Prøv igjen',
  }
})

watch(errorMessage, (message) => {
  aiErrorDialogOpen.value = message.trim() !== ''
})

function retryVoice() {
  aiErrorDialogOpen.value = false
  void connect()
}

function setVoiceModelFromEvent(event: Event) {
  const target = event.target
  if (target instanceof HTMLSelectElement) {
    voiceModelStore.setSelectedModelId(target.value)
  }
}
</script>

<template>
  <main
    class="relative flex min-h-0 flex-1 flex-col overflow-y-auto bg-gradient-to-br from-slate-100 via-blue-50 to-slate-100 pt-20"
  >
    <AiStatusDialog
      v-model:open="aiErrorDialogOpen"
      :title="errorDialogCopy.title"
      :description="errorDialogCopy.description"
      :message="errorMessage"
      :retry-label="errorDialogCopy.retry"
      show-retry
      @retry="retryVoice"
    />

    <div class="absolute inset-0 pointer-events-none">
      <div
        class="absolute top-0 left-0 h-full w-full"
        style="
          background: radial-gradient(circle at 20% 80%, rgba(59, 130, 246, 0.08) 0%, transparent 50%),
            radial-gradient(circle at 80% 20%, rgba(37, 99, 235, 0.08) 0%, transparent 50%);
        "
      />
    </div>

    <div class="relative z-10 mx-auto w-full max-w-2xl flex-1 px-4 pb-16 pt-8 sm:px-6">
      <div class="mb-6 text-center">
        <h1 class="text-2xl font-bold tracking-tight text-slate-800 sm:text-3xl">
          {{ copy.title }}
        </h1>
        <p class="mt-2 max-w-xl mx-auto text-sm text-slate-600 sm:text-base">
          {{ copy.subtitle }}
        </p>
        <RouterLink
          to="/chat"
          class="mt-3 inline-flex items-center gap-1.5 text-sm font-medium text-blue-700 hover:text-blue-900"
        >
          <MessageSquare class="size-4 shrink-0" aria-hidden="true" />
          {{ copy.chatAlt }}
        </RouterLink>
      </div>

      <Alert
        class="mb-6 border-blue-200/80 bg-blue-50/90 text-slate-800 shadow-sm backdrop-blur-sm [&>svg]:text-blue-600"
      >
        <Info class="size-4 shrink-0" aria-hidden="true" />
        <AlertTitle>{{ copy.disclaimerTitle }}</AlertTitle>
        <AlertDescription>{{ copy.disclaimerBody }}</AlertDescription>
      </Alert>

      <div
        v-if="voiceAvailable === false"
        class="rounded-2xl border border-amber-200 bg-amber-50/90 px-4 py-3 text-center text-sm text-amber-900"
      >
        {{ copy.unavailable }}
      </div>

      <template v-else-if="voiceAvailable === true">
        <div class="flex flex-col items-center gap-6">
          <div
            class="relative flex h-40 w-40 items-center justify-center rounded-full bg-gradient-to-br from-blue-600 to-indigo-700 shadow-xl shadow-blue-500/25"
            :class="{
              'ring-4 ring-blue-400/50 animate-pulse': connectionState === 'connected',
            }"
          >
            <Loader2
              v-if="connectionState === 'connecting'"
              class="size-14 text-white animate-spin"
              aria-hidden="true"
            />
            <Mic
              v-else-if="connectionState === 'connected'"
              class="size-14 text-white"
              aria-hidden="true"
            />
            <Mic v-else class="size-14 text-white opacity-90" aria-hidden="true" />
            <span class="sr-only">{{ statusLabel }}</span>
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
              <option v-for="model in voiceModelStore.models" :key="model.provider + ':' + model.id" :value="model.id">
                {{ model.label }}
              </option>
            </select>
          </label>

          <div
            v-if="selectedVoiceProvider === 'OPENAI'"
            class="grid w-full max-w-md grid-cols-1 gap-3 sm:grid-cols-2"
          >
            <label class="text-left text-xs font-semibold uppercase text-slate-600">
              {{ copy.voiceLabel }}
              <select
                v-model="selectedVoice"
                data-testid="voice-select"
                class="mt-1 h-10 w-full rounded-lg border border-blue-100 bg-white/90 px-3 text-sm font-medium normal-case text-slate-800 shadow-sm disabled:cursor-not-allowed disabled:opacity-60"
                :disabled="sessionControlsDisabled"
              >
                <option v-for="voice in voiceOptions" :key="voice" :value="voice">
                  {{ voiceLabels[voice] }}
                </option>
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
                <option v-for="effort in reasoningOptions" :key="effort" :value="effort">
                  {{ reasoningLabels[effort] }}
                </option>
              </select>
            </label>
          </div>

          <div class="flex flex-wrap justify-center gap-3">
            <Button
              v-if="connectionState === 'idle' || connectionState === 'error'"
              type="button"
              class="rounded-2xl bg-gradient-to-r from-blue-600 to-blue-700 px-8 py-6 text-base font-semibold"
              @click="connect"
            >
              {{ copy.connect }}
            </Button>
            <Button
              v-if="connectionState === 'connecting'"
              type="button"
              variant="secondary"
              disabled
            >
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

        <Alert
          v-if="sessionNotice"
          class="mt-6 border-blue-200 bg-blue-50 text-slate-800"
        >
          <AlertDescription>{{ sessionNotice }}</AlertDescription>
        </Alert>

        <div
          v-if="connectionState === 'connected' || userTranscript || assistantTranscript"
          class="mt-8 space-y-4 rounded-2xl border border-blue-100 bg-white/85 p-4 shadow-sm backdrop-blur-md"
        >
          <div>
            <p class="text-xs font-semibold uppercase tracking-wide text-slate-500">
              {{ copy.you }}
            </p>
            <p class="mt-1 whitespace-pre-wrap text-sm text-slate-800">
              {{ userTranscript || '…' }}
            </p>
          </div>
          <div>
            <p class="text-xs font-semibold uppercase tracking-wide text-slate-500">
              {{ copy.assistant }}
            </p>
            <p class="mt-1 whitespace-pre-wrap text-sm text-slate-800">
              {{ assistantTranscript || '…' }}
            </p>
          </div>
        </div>
      </template>

      <div v-else class="flex justify-center py-12">
        <Loader2 class="size-8 animate-spin text-blue-600" aria-hidden="true" />
      </div>
    </div>
  </main>
</template>
