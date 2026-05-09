<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { Mic, MicOff, Loader2, Info, MessageSquare } from 'lucide-vue-next'
import { useLangStore } from '@/stores/lang'
import { Button } from '@/components/ui/button'
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { useRealtimeVoice } from '@/composables/useRealtimeVoice'
import { fetchRealtimeVoiceEnabled } from '@/lib/realtime-voice'

const langStore = useLangStore()
const language = computed(() => langStore.language)

const voiceAvailable = ref<boolean | null>(null)

const {
  connectionState,
  errorMessage,
  sessionNotice,
  assistantTranscript,
  userTranscript,
  connect,
  disconnect,
  maxSessionMs,
} = useRealtimeVoice(language)

onMounted(async () => {
  voiceAvailable.value = await fetchRealtimeVoiceEnabled()
})

const copy = computed(() => {
  const en = language.value === 'en'
  return {
    title: en ? "Talk with Kevin's AI" : 'Snakk med Kevin sin AI',
    subtitle: en
      ? 'Live voice (OpenAI GPT-Realtime). Audio is sent to OpenAI for real-time processing — not the same as typed chat RAG retrieval. For document-grounded answers, use text chat.'
      : 'Live stemme (OpenAI GPT-Realtime). Lyd sendes til OpenAI i sanntid — ikke det samme som tekstchat med RAG. For svar forankret i dokumenter, bruk tekstchat.',
    chatAlt: en ? 'Use text chat instead' : 'Bruk tekstchat',
    unavailable: en
      ? 'Voice chat is not enabled on the server right now.'
      : 'Stemmechat er ikke slått på hos serveren akkurat nå.',
    connect: en ? 'Start voice' : 'Start stemme',
    disconnect: en ? 'End session' : 'Avslutt',
    connecting: en ? 'Connecting…' : 'Kobler til…',
    live: en ? 'Live' : 'Aktiv',
    you: en ? 'You (transcript)' : 'Du (transkripsjon)',
    assistant: en ? 'Assistant (transcript)' : 'Assistent (transkripsjon)',
    disclaimerTitle: en ? 'Before you use voice' : 'Før du bruker stemme',
    disclaimerBody: en
      ? 'You are talking to an AI, not Kevin himself. Replies can be wrong. Audio is processed by OpenAI in real time (this page uses WebRTC). Each session ends automatically after about 3 minutes.'
      : 'Du snakker med en KI, ikke Kevin selv. Svar kan være feil. Lyd behandles av OpenAI i sanntid (denne siden bruker WebRTC). Hver økt avsluttes automatisk etter ca. 3 minutter.',
  }
})

const statusLabel = computed(() => {
  if (connectionState.value === 'connecting') return copy.value.connecting
  if (connectionState.value === 'connected') return copy.value.live
  return ''
})
</script>

<template>
  <main
    class="relative flex min-h-0 flex-1 flex-col overflow-y-auto bg-gradient-to-br from-slate-100 via-blue-50 to-slate-100 pt-20"
  >
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
        <p class="mt-2 text-sm text-slate-600">
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

        <Alert v-if="errorMessage" variant="destructive" class="mt-6">
          <AlertDescription>{{ errorMessage }}</AlertDescription>
        </Alert>
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
