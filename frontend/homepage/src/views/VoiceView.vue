<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { RouterLink } from 'vue-router'
import { useLangStore } from '@/stores/lang'
import { Button } from '@/components/ui/button'
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { useOpenAiRealtimeVoice } from '@/composables/useOpenAiRealtimeVoice'
import { ChevronLeft, Info, Loader2, Mic } from 'lucide-vue-next'

const langStore = useLangStore()
const language = computed({
  get: () => langStore.language,
  set: (v: 'en' | 'no') => langStore.setLanguage(v),
})

const {
  phase,
  errorMessage,
  status,
  userTranscript,
  assistantTranscript,
  assistantBuffer,
  remoteAudioRef,
  sessionMs,
  loadStatus,
  start,
  stop,
} = useOpenAiRealtimeVoice(language)

const copy = computed(() => {
  if (language.value === 'no') {
    return {
      title: 'Snakk med Kevin sin AI',
      subtitle: 'Live stemme (GPT-Realtime). For dokument-chat → tekst.',
      textChat: 'Bruk tekstchat →',
      docHint: 'Dokument-/kunnskapschat fungerer best som tekst på chat-siden.',
      beforeVoiceTitle: 'Før du bruker stemme',
      beforeVoiceBody:
        'Dette er KI — ikke Kevin. Lyd går direkte til OpenAI over WebRTC fra nettleseren din. Foreslått økt ca. tre minutter; du kan avslutte når som helst.',
      start: 'Start stemme',
      connecting: 'Kobler til…',
      end: 'Avslutt',
      active: 'Aktiv',
      approx: 'ca.',
      min: 'min',
      you: 'DU (TRANSCRIPT)',
      assistant: 'ASSISTENT',
      back: 'Til forsiden',
      unavailable: 'Live stemme er ikke tilgjengelig akkurat nå.',
    }
  }
  return {
    title: 'Talk with Kevin’s AI',
    subtitle: 'Live voice (GPT-Realtime). For document chat → text.',
    textChat: 'Use text chat →',
    docHint: 'Document / knowledge chat works best as text on the chat page.',
    beforeVoiceTitle: 'Before you use voice',
    beforeVoiceBody:
      'This is AI — not Kevin. Audio goes to OpenAI over WebRTC from your browser. Sessions are about three minutes; you can disconnect anytime.',
    start: 'Start voice',
    connecting: 'Connecting…',
    end: 'Hang up',
    active: 'Active',
    approx: '~',
    min: 'min',
    you: 'YOU (TRANSCRIPT)',
    assistant: 'ASSISTANT',
    back: 'Back to home',
    unavailable: 'Live voice isn’t available right now.',
  }
})

const sessionMinutesLabel = computed(() => status.value?.sessionMaxMinutes ?? 3)

onMounted(() => {
  void loadStatus()
})
</script>

<template>
  <main
    class="relative flex min-h-0 flex-1 flex-col overflow-y-auto bg-gradient-to-br from-slate-50 to-slate-100 pt-20 pb-28"
  >
    <div class="relative z-10 mx-auto w-full max-w-lg px-4 py-6 space-y-4">
      <RouterLink
        to="/"
        class="inline-flex items-center gap-1 text-sm font-medium text-blue-700 hover:text-blue-900 hover:underline"
      >
        <ChevronLeft class="size-4" aria-hidden="true" />
        {{ copy.back }}
      </RouterLink>

      <div class="space-y-1 text-center">
        <h1 class="text-2xl font-bold text-slate-900 sm:text-3xl">{{ copy.title }}</h1>
        <p class="text-sm text-slate-600">{{ copy.subtitle }}</p>
        <RouterLink
          to="/chat"
          class="inline-block text-sm font-semibold text-blue-700 hover:text-blue-900 hover:underline"
        >
          {{ copy.textChat }}
        </RouterLink>
        <p class="text-xs text-slate-500">{{ copy.docHint }}</p>
      </div>

      <Alert class="border-blue-200/80 bg-blue-50/90 text-slate-800 [&>svg]:text-blue-600">
        <Info class="size-4 shrink-0" aria-hidden="true" />
        <AlertTitle>{{ copy.beforeVoiceTitle }}</AlertTitle>
        <AlertDescription>
          <p>{{ copy.beforeVoiceBody }}</p>
        </AlertDescription>
      </Alert>

      <Alert
        v-if="status && !status.enabled"
        variant="destructive"
        class="border-amber-200 bg-amber-50 text-amber-950 [&>svg]:text-amber-700"
      >
        <AlertTitle>{{ language === 'no' ? 'Utilgjengelig' : 'Unavailable' }}</AlertTitle>
        <AlertDescription>{{ copy.unavailable }}</AlertDescription>
      </Alert>

      <div class="flex flex-col items-center gap-3">
        <audio
          ref="remoteAudioRef"
          class="sr-only"
          autoplay
          playsinline
        />

        <div
          v-if="phase === 'connected'"
          class="flex w-full flex-wrap items-center justify-between gap-2 rounded-xl border border-emerald-200 bg-emerald-50/90 px-4 py-3 text-sm text-emerald-950"
        >
          <span class="font-medium">
            {{ copy.active }} · {{ copy.approx }} {{ sessionMinutesLabel }} {{ copy.min }}
          </span>
          <Button type="button" variant="outline" class="border-emerald-300" @click="stop">
            {{ copy.end }}
          </Button>
        </div>

        <Button
          v-if="phase === 'idle' || phase === 'error'"
          type="button"
          size="lg"
          class="rounded-full bg-gradient-to-r from-blue-600 to-blue-700 px-8 font-semibold text-white shadow-lg hover:from-blue-700 hover:to-blue-800"
          :disabled="status !== null && !status.enabled"
          @click="start"
        >
          <Mic class="me-2 size-5" aria-hidden="true" />
          {{ copy.start }}
        </Button>

        <Button
          v-if="phase === 'connecting'"
          type="button"
          size="lg"
          disabled
          class="rounded-full opacity-90"
        >
          <Loader2 class="me-2 size-5 animate-spin" aria-hidden="true" />
          {{ copy.connecting }}
        </Button>
      </div>

      <Alert v-if="phase === 'error' && errorMessage" variant="destructive">
        <AlertDescription>{{ errorMessage }}</AlertDescription>
      </Alert>

      <div v-if="phase === 'connected'" class="grid gap-3">
        <section
          class="rounded-xl border border-slate-200 bg-white/90 p-3 shadow-sm backdrop-blur-sm"
          aria-label="user transcript"
        >
          <h2 class="mb-2 text-xs font-bold uppercase tracking-wide text-slate-500">{{ copy.you }}</h2>
          <p class="min-h-[4rem] whitespace-pre-wrap text-sm text-slate-800">
            {{ userTranscript || '…' }}
          </p>
        </section>
        <section
          class="rounded-xl border border-slate-200 bg-white/90 p-3 shadow-sm backdrop-blur-sm"
          aria-label="assistant transcript"
        >
          <h2 class="mb-2 text-xs font-bold uppercase tracking-wide text-slate-500">{{ copy.assistant }}</h2>
          <p class="min-h-[4rem] whitespace-pre-wrap text-sm text-slate-800">
            {{ assistantTranscript }}{{ assistantBuffer }}<template v-if="!assistantTranscript && !assistantBuffer">…</template>
          </p>
        </section>
      </div>

      <p v-if="phase === 'idle'" class="text-center text-xs text-slate-400">
        {{ language === 'no' ? 'Økt timer:' : 'Session timer:' }} {{ Math.round(sessionMs / 60000) }} min (auto-lukk)
      </p>
    </div>
  </main>
</template>
