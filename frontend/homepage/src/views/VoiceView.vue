<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { useLangStore } from '@/stores/lang'
import { Button } from '@/components/ui/button'
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import VoiceOrb from '@/components/VoiceOrb.vue'
import BudgetDialog from '@/components/BudgetDialog.vue'
import { useOpenAiRealtimeVoice } from '@/composables/useOpenAiRealtimeVoice'
import { ChevronLeft, Info, Loader2 } from 'lucide-vue-next'

const router = useRouter()
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

const budgetDialogOpen = ref(false)
const checkingVoice = ref(false)

/** Dual-mode gateway + voice session; strings aligned with HomeView gateway where possible. */
const copy = computed(() => {
  if (language.value === 'no') {
    return {
      heading: 'Velg hvordan du vil møte AI-en',
      headingMobile: 'Snakk først. Spør dypere ved behov.',
      talkTitle: 'Snakk',
      talkDesc: 'Sanntidsstemme for raske offentlige fakta om Kevin.',
      talkCta: 'Start stemme',
      askTitle: 'Spør',
      askTitleMobile: 'Spør i tekstchat',
      askDesc: 'Tekstchat for dypere dokumentbaserte svar.',
      askCta: 'Åpne chat',
      beforeVoiceTitle: 'Før du bruker stemme',
      beforeVoiceBody:
        'Dette er KI — ikke Kevin. Lyd går direkte til OpenAI over WebRTC fra nettleseren din. Foreslått økt ca. tre minutter; du kan avslutte når som helst.',
      connecting: 'Kobler til…',
      end: 'Avslutt',
      active: 'Aktiv',
      approx: 'ca.',
      min: 'min',
      you: 'DU (TRANSCRIPT)',
      assistant: 'ASSISTENT',
      back: 'Til forsiden',
      sessionHint: 'Økt timer:',
    }
  }
  return {
    heading: 'Choose how you want to meet the AI',
    headingMobile: 'Talk first. Ask deeper when needed.',
    talkTitle: 'Talk',
    talkDesc: 'Realtime voice for quick public facts about Kevin.',
    talkCta: 'Start voice',
    askTitle: 'Ask',
    askTitleMobile: 'Ask in text chat',
    askDesc: 'Text chat for deeper document-grounded answers.',
    askCta: 'Open chat',
    beforeVoiceTitle: 'Before you use voice',
    beforeVoiceBody:
      'This is AI — not Kevin. Audio goes to OpenAI over WebRTC from your browser. Sessions are about three minutes; you can disconnect anytime.',
    connecting: 'Connecting…',
    end: 'Hang up',
    active: 'Active',
    approx: '~',
    min: 'min',
    you: 'YOU (TRANSCRIPT)',
    assistant: 'ASSISTANT',
    back: 'Back to home',
    sessionHint: 'Session timer:',
  }
})

const sessionMinutesLabel = computed(() => status.value?.sessionMaxMinutes ?? 3)

const showGateway = computed(() => phase.value === 'idle' || phase.value === 'error')

async function handleStartVoice() {
  checkingVoice.value = true
  await loadStatus()
  checkingVoice.value = false

  if (!status.value?.enabled) {
    budgetDialogOpen.value = true
    return
  }
  await start()
}

async function handleBudgetRetry() {
  await loadStatus()
  if (status.value?.enabled) {
    await start()
  } else {
    budgetDialogOpen.value = true
  }
}

function handleOpenChat() {
  router.push({ name: 'chat' })
}

onMounted(async () => {
  await loadStatus()
  if (status.value && !status.value.enabled) {
    budgetDialogOpen.value = true
  }
})
</script>

<template>
  <main
    class="relative flex min-h-0 flex-1 flex-col overflow-y-auto bg-linear-to-r from-[#f7fcff] to-[#e3f0ff] pb-28"
  >
    <!-- Language toggle -->
    <div class="absolute top-6 right-6 z-20">
      <div
        class="flex rounded-full border border-slate-200/60 bg-white/80 p-1 shadow-sm backdrop-blur-sm"
      >
        <button
          type="button"
          class="rounded-full px-3 py-1.5 text-xs font-medium transition-all"
          :class="
            language === 'en' ? 'bg-white font-semibold text-blue-700 shadow-sm' : 'text-slate-500'
          "
          @click="language = 'en'"
        >
          EN
        </button>
        <button
          type="button"
          class="rounded-full px-3 py-1.5 text-xs font-medium transition-all"
          :class="
            language === 'no' ? 'bg-white font-semibold text-blue-700 shadow-sm' : 'text-slate-500'
          "
          @click="language = 'no'"
        >
          NO
        </button>
      </div>
    </div>

    <!-- Gateway: choose Talk vs Ask -->
    <template v-if="showGateway">
      <RouterLink
        to="/"
        class="absolute top-6 left-6 z-20 inline-flex items-center gap-1 text-sm font-medium text-[#2663eb] hover:text-blue-800 hover:underline"
      >
        <ChevronLeft class="size-4 shrink-0" aria-hidden="true" />
        {{ copy.back }}
      </RouterLink>

      <div
        class="relative z-10 mx-auto flex w-full max-w-6xl flex-1 flex-col justify-center px-6 py-16 pt-24 lg:py-8 lg:pt-20"
      >
        <!-- Desktop heading -->
        <h1 class="mb-10 hidden text-[58px] font-bold leading-[1.05] text-[#0f1729] lg:block">
          {{ copy.heading }}
        </h1>

        <!-- Mobile heading -->
        <h1 class="mb-8 text-[39px] font-bold leading-[1.08] text-[#0f1729] lg:hidden">
          {{ copy.headingMobile }}
        </h1>

        <!-- Cards -->
        <div class="flex flex-col gap-6 lg:flex-row lg:items-start lg:gap-8">
          <!-- Talk -->
          <div
            class="flex w-full flex-col overflow-hidden rounded-[24px] border border-[#a6c2f5] bg-white p-8 shadow-[0px_18px_32px_rgba(20,38,77,0.12)] lg:max-w-[560px] lg:rounded-[28px] lg:p-10"
          >
            <div class="flex items-start gap-6">
              <VoiceOrb size="md" class="hidden lg:block" />
              <VoiceOrb size="sm" class="lg:hidden" />
              <div class="flex flex-col">
                <h2 class="text-[32px] font-bold text-[#0f1729] lg:text-[44px] lg:leading-[52px]">
                  {{ copy.talkTitle }}
                </h2>
                <p class="mt-2 hidden text-lg leading-relaxed text-[#47546b] lg:block">
                  {{ copy.talkDesc }}
                </p>
              </div>
            </div>
            <div class="mt-6 lg:mt-8 lg:ml-[168px]">
              <Button
                type="button"
                :disabled="checkingVoice"
                class="h-[52px] rounded-2xl bg-[#2663eb] px-7 text-[15px] font-semibold text-white shadow-[0px_14px_24px_rgba(38,99,235,0.25)] hover:bg-blue-700 disabled:opacity-60"
                @click="handleStartVoice"
              >
                <Loader2
                  v-if="checkingVoice"
                  class="me-2 inline size-4 animate-spin"
                  aria-hidden="true"
                />
                {{ copy.talkCta }}
              </Button>
            </div>
          </div>

          <!-- Ask -->
          <div
            class="w-full overflow-hidden rounded-[22px] border border-[#d1def0] bg-white/82 p-8 lg:mt-12 lg:max-w-[470px] lg:rounded-[26px]"
          >
            <div class="hidden flex-col lg:flex">
              <h2 class="text-[38px] font-bold leading-[45px] text-[#0f1729]">
                {{ copy.askTitle }}
              </h2>
              <p class="mt-3 text-[17px] leading-relaxed text-[#47546b]">
                {{ copy.askDesc }}
              </p>
              <div class="mt-6">
                <Button
                  type="button"
                  variant="outline"
                  class="h-[52px] rounded-2xl border-[#c7d6ed] px-7 text-[15px] font-semibold text-[#1f293b] hover:bg-slate-50"
                  @click="handleOpenChat"
                >
                  {{ copy.askCta }}
                </Button>
              </div>
            </div>

            <div class="flex flex-col lg:hidden">
              <h2 class="text-2xl font-bold text-[#0f1729]">
                {{ copy.askTitleMobile }}
              </h2>
              <div class="mt-4">
                <Button
                  type="button"
                  variant="outline"
                  class="h-[52px] rounded-2xl border-[#c7d6ed] px-7 text-[15px] font-semibold text-[#1f293b] hover:bg-slate-50"
                  @click="handleOpenChat"
                >
                  {{ copy.askCta }}
                </Button>
              </div>
            </div>
          </div>
        </div>

        <p class="mt-8 text-center text-xs text-[#47546b]/80">
          {{ copy.sessionHint }} {{ Math.round(sessionMs / 60000) }} {{ copy.min }} (auto)
        </p>

        <Alert
          v-if="phase === 'error' && errorMessage"
          variant="destructive"
          class="mx-auto mt-6 max-w-xl"
        >
          <AlertDescription>{{ errorMessage }}</AlertDescription>
        </Alert>
      </div>
    </template>

    <!-- Active voice session -->
    <template v-else>
      <div
        class="relative z-10 mx-auto w-full max-w-2xl flex-1 space-y-4 px-6 py-16 pt-24 lg:pt-20"
      >
        <RouterLink
          to="/"
          class="inline-flex items-center gap-1 text-sm font-medium text-[#2663eb] hover:text-blue-800 hover:underline"
        >
          <ChevronLeft class="size-4 shrink-0" aria-hidden="true" />
          {{ copy.back }}
        </RouterLink>

        <Alert
          class="border-[#a6c2f5]/80 bg-white/90 text-[#0f1729] [&>svg]:text-[#2663eb]"
        >
          <Info class="size-4 shrink-0" aria-hidden="true" />
          <AlertTitle>{{ copy.beforeVoiceTitle }}</AlertTitle>
          <AlertDescription>
            <p>{{ copy.beforeVoiceBody }}</p>
          </AlertDescription>
        </Alert>

        <audio ref="remoteAudioRef" class="sr-only" autoplay playsinline />

        <div class="flex flex-col items-center gap-6">
          <VoiceOrb size="lg" :active="phase === 'connected'" />

          <Button
            v-if="phase === 'connecting'"
            type="button"
            disabled
            class="h-[52px] rounded-2xl px-8 opacity-90"
          >
            <Loader2 class="me-2 size-5 animate-spin" aria-hidden="true" />
            {{ copy.connecting }}
          </Button>

          <div
            v-if="phase === 'connected'"
            class="flex w-full flex-wrap items-center justify-between gap-2 rounded-2xl border border-emerald-200 bg-emerald-50/90 px-4 py-3 text-sm text-emerald-950"
          >
            <span class="font-medium">
              {{ copy.active }} · {{ copy.approx }} {{ sessionMinutesLabel }} {{ copy.min }}
            </span>
            <Button type="button" variant="outline" class="border-emerald-300" @click="stop">
              {{ copy.end }}
            </Button>
          </div>
        </div>

        <div v-if="phase === 'connected'" class="grid gap-3">
          <section
            class="rounded-2xl border border-[#d1def0] bg-white/90 p-4 shadow-sm backdrop-blur-sm"
            aria-label="user transcript"
          >
            <h2 class="mb-2 text-xs font-bold uppercase tracking-wide text-[#47546b]">
              {{ copy.you }}
            </h2>
            <p class="min-h-[4rem] whitespace-pre-wrap text-sm text-[#0f1729]">
              {{ userTranscript || '…' }}
            </p>
          </section>
          <section
            class="rounded-2xl border border-[#d1def0] bg-white/90 p-4 shadow-sm backdrop-blur-sm"
            aria-label="assistant transcript"
          >
            <h2 class="mb-2 text-xs font-bold uppercase tracking-wide text-[#47546b]">
              {{ copy.assistant }}
            </h2>
            <p class="min-h-[4rem] whitespace-pre-wrap text-sm text-[#0f1729]">
              {{ assistantTranscript }}{{ assistantBuffer
              }}<template v-if="!assistantTranscript && !assistantBuffer">…</template>
            </p>
          </section>
        </div>
      </div>
    </template>

    <BudgetDialog
      v-model:open="budgetDialogOpen"
      @retry="handleBudgetRetry"
    />
  </main>
</template>
