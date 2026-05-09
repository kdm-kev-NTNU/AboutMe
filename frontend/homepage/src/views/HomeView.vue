<script setup lang="ts">
import { useRouter, RouterLink } from 'vue-router'
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useLangStore } from '../stores/lang'
import { useChatModelStore } from '../stores/model'
import { ChatModelOptionProvider } from '@/api/generated/portfolio'
import { Input } from '@/components/ui/input'
import { Button } from '@/components/ui/button'
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import AudioWaveform from '@/components/AudioWaveform.vue'
import { useSpeechTranscription, MAX_SPEECH_PROMPT_CHARS } from '@/composables/useSpeechTranscription'
import { Info, MessageSquare, ChevronRight, Loader2, Mic, Square, Headphones } from 'lucide-vue-next'
import { fetchRealtimeVoiceEnabled } from '@/lib/realtime-voice'
import {
  pickRotatingShortcuts,
  shortcutRotationBucket,
} from '@/utils/shortcutQuestions'

const router = useRouter()

const langStore = useLangStore()
const chatModelStore = useChatModelStore()

const language = computed({
  get: () => langStore.language,
  set: (v: 'en' | 'no') => langStore.setLanguage(v),
})

/** Bumps when the 6h rotation bucket changes so shortcut list refreshes for long-lived tabs */
const rotationEpoch = ref(0)
const lastShortcutBucket = ref(shortcutRotationBucket(Date.now()))

const visibleQuestions = computed(() => {
  // Evaluate rotationEpoch first so this computed re-runs when the 6h bucket ticks (see onMounted).
  return (rotationEpoch.value, pickRotatingShortcuts(language.value, Date.now()))
})

const chatDisclaimer = computed(() => {
  if (language.value === 'no') {
    return {
      title: 'Før du chatter',
      body:
        'Svarene lages av en språkmodell og kan være helt feil. Ikke del private eller sensitive ting her. Kort tale kan sendes til server for transkripsjon; live stemmechat (når slått på) sender lyd direkte til OpenAI i sanntid via WebRTC — se stemmesiden.',
    }
  }
  return {
    title: 'Before you chat',
    body:
      'Replies come from a language model and can be wrong. Do not share private or sensitive information here. Short voice clips are sent to the server for transcription; live voice chat (when enabled) streams audio directly to OpenAI in real time via WebRTC — see the voice page.',
  }
})

const feedbackInvite = computed(() => {
  if (language.value === 'no') {
    return {
      body: 'Si gjerne fra hvis noe kan bli bedre, det hjelper meg å forbedre siden.',
      cta: 'Gi tilbakemelding',
      ariaLabel: 'Gå til tilbakemeldingsskjema',
    }
  }
  return {
    body: "I'd love a quick note if something could be better; it helps me improve the site.",
    cta: 'Share feedback',
    ariaLabel: 'Go to the feedback form',
  }
})

const futureWorkHomeLink = computed(() => {
  if (language.value === 'no') {
    return {
      label: 'Videre arbeid og forbedringer',
      ariaLabel: 'Gå til siden om planlagt utvikling av porteføljen',
    }
  }
  return {
    label: 'Future work and improvements',
    ariaLabel: 'Go to the roadmap for planned portfolio improvements',
  }
})

const quickQuestion = ref('')

/** Null until loaded from GET /realtime/status */
const voiceFeatureEnabled = ref<boolean | null>(null)

const voiceCta = computed(() =>
  language.value === 'no' ? 'Snakk med Kevin sin AI' : "Talk to Kevin's AI",
)
const voiceCtaAria = computed(() =>
  language.value === 'no' ? 'Gå til live stemmechat' : 'Go to live voice chat',
)

const speechUiLanguage = computed(() => language.value)

const speechBlocked = computed(() => false)

const {
  supportsSpeechInput,
  isRecording,
  isTranscribing,
  recordingMediaStream,
  voiceError,
  toggleVoiceInput,
} = useSpeechTranscription({
  language: speechUiLanguage,
  maxChars: MAX_SPEECH_PROMPT_CHARS,
  isBlocked: speechBlocked,
  onTranscript: (t) => {
    quickQuestion.value = t
    router.push({ name: 'chat', query: { q: t } })
  },
})

const providerLabels = computed(() =>
  language.value === 'no'
    ? { heading: 'AI-leverandør', openai: 'OpenAI', anthropic: 'Anthropic' }
    : { heading: 'AI provider', openai: 'OpenAI', anthropic: 'Anthropic' },
)

const modelLabel = computed(() => (language.value === 'no' ? 'Modell' : 'Model'))

const modelsForActiveProvider = computed(() => {
  const p = chatModelStore.activeProvider
  if (!p) return chatModelStore.models
  return chatModelStore.modelsForProvider(p)
})

const selectedModelId = computed({
  get: () => chatModelStore.selectedModelId,
  set: (id: string) => chatModelStore.setSelectedModelId(id),
})

const showProviderToggle = computed(
  () => chatModelStore.hasOpenAI && chatModelStore.hasAnthropic,
)

let shortcutBucketInterval: ReturnType<typeof setInterval> | undefined

onMounted(() => {
  void chatModelStore.ensureModelsLoaded()
  void fetchRealtimeVoiceEnabled().then((ok) => {
    voiceFeatureEnabled.value = ok
  })
  lastShortcutBucket.value = shortcutRotationBucket(Date.now())
  shortcutBucketInterval = setInterval(() => {
    const b = shortcutRotationBucket(Date.now())
    if (b !== lastShortcutBucket.value) {
      lastShortcutBucket.value = b
      rotationEpoch.value += 1
    }
  }, 60_000)
})

onUnmounted(() => {
  if (shortcutBucketInterval !== undefined) {
    clearInterval(shortcutBucketInterval)
    shortcutBucketInterval = undefined
  }
})

function ask(q: string) {
  router.push({ name: 'chat', query: { q } })
}

function submitQuick() {
  const q = quickQuestion.value.trim()
  if (!q) return
  ask(q)
}
</script>

<template>
  <main class="relative flex min-h-0 flex-1 flex-col overflow-y-auto bg-gradient-to-br from-slate-50 to-slate-100 pt-20">
    <!-- Gradient Background Overlay -->
    <div class="absolute inset-0 pointer-events-none">
      <div class="absolute top-0 left-0 w-full h-full" style="background: radial-gradient(circle at 20% 80%, rgba(59, 130, 246, 0.1) 0%, transparent 50%), radial-gradient(circle at 80% 20%, rgba(37, 99, 235, 0.1) 0%, transparent 50%);"></div>
    </div>

    <!-- Blue Blob Shapes -->
    <div class="blob-container">
      <div class="blob blob-1"></div>
      <div class="blob blob-2"></div>
      <div class="blob blob-3"></div>
      <div class="blob blob-4"></div>
      <div class="blob blob-5"></div>
      <div class="blob blob-6"></div>
    </div>

    <!-- Main Content - Centered -->
    <div class="relative z-10 flex min-h-full flex-col items-center justify-center px-4 py-8">
      <div class="flex flex-col items-center space-y-8">
        <section class="brand">
          <h1 class="mb-4 px-1 text-center text-4xl font-bold sm:text-5xl md:text-6xl lg:text-7xl">
            Kevin's <span class="bg-gradient-to-r from-blue-600 via-blue-700 to-blue-800 bg-clip-text text-transparent animate-gradient-x">AI</span>.
          </h1>
          <div class="flex justify-center">
            <div class="relative rounded-full p-1 flex bg-gradient-to-r from-slate-200 to-slate-300 shadow-md border-2 border-transparent bg-clip-padding">
              <div
                class="absolute top-1 bottom-1 w-16 rounded-full shadow-lg transition-transform duration-300 ease-in-out bg-gradient-to-r from-white to-slate-50 border border-blue-200"
                :class="language === 'en' ? 'translate-x-0' : 'translate-x-16'"
              ></div>
              <button
                class="relative z-10 w-16 py-2 text-sm font-medium transition-all duration-300 cursor-pointer rounded-full overflow-hidden"
                :class="language === 'en' ? 'text-blue-700 font-semibold' : 'text-gray-500'"
                @click="language = 'en'"
              >
                EN
              </button>
              <button
                class="relative z-10 w-16 py-2 text-sm font-medium transition-all duration-300 cursor-pointer rounded-full overflow-hidden"
                :class="language === 'no' ? 'text-blue-700 font-semibold' : 'text-gray-500'"
                @click="language = 'no'"
              >
                NO
              </button>
            </div>
          </div>

          <!-- Provider + model (same session keys as chat page) -->
          <div
            v-if="chatModelStore.models.length > 0"
            class="mt-6 flex flex-col items-center gap-4 w-full max-w-md px-2"
          >
            <p class="text-xs font-medium uppercase tracking-wide text-slate-500">
              {{ providerLabels.heading }}
            </p>
            <div v-if="showProviderToggle" class="flex justify-center w-full">
              <div
                class="relative rounded-full p-1 flex bg-gradient-to-r from-slate-200 to-slate-300 shadow-md border-2 border-transparent bg-clip-padding"
              >
                <div
                  class="absolute top-1 bottom-1 w-28 rounded-full shadow-lg transition-transform duration-300 ease-in-out bg-gradient-to-r from-white to-slate-50 border border-blue-200"
                  :class="
                    chatModelStore.activeProvider === ChatModelOptionProvider.OPENAI
                      ? 'translate-x-0'
                      : 'translate-x-28'
                  "
                ></div>
                <button
                  type="button"
                  class="relative z-10 w-28 py-2 text-sm font-medium transition-all duration-300 cursor-pointer rounded-full overflow-hidden"
                  :class="
                    chatModelStore.activeProvider === ChatModelOptionProvider.OPENAI
                      ? 'text-blue-700 font-semibold'
                      : 'text-gray-500'
                  "
                  :disabled="!chatModelStore.hasOpenAI"
                  @click="chatModelStore.selectFirstForProvider(ChatModelOptionProvider.OPENAI)"
                >
                  {{ providerLabels.openai }}
                </button>
                <button
                  type="button"
                  class="relative z-10 w-28 py-2 text-sm font-medium transition-all duration-300 cursor-pointer rounded-full overflow-hidden"
                  :class="
                    chatModelStore.activeProvider === ChatModelOptionProvider.ANTHROPIC
                      ? 'text-blue-700 font-semibold'
                      : 'text-gray-500'
                  "
                  :disabled="!chatModelStore.hasAnthropic"
                  @click="chatModelStore.selectFirstForProvider(ChatModelOptionProvider.ANTHROPIC)"
                >
                  {{ providerLabels.anthropic }}
                </button>
              </div>
            </div>
            <div class="flex flex-col gap-2 w-full">
              <label
                for="home-model-select"
                class="text-center text-xs font-medium uppercase tracking-wide text-slate-500"
              >
                {{ modelLabel }}
              </label>
              <select
                id="home-model-select"
                v-model="selectedModelId"
                class="w-full rounded-lg border-2 border-blue-200/40 bg-white/90 px-3 py-2 text-sm text-slate-800 focus:border-blue-400 focus:outline-none"
              >
                <option v-for="m in modelsForActiveProvider" :key="m.id" :value="m.id">
                  {{ m.label }} ({{ m.provider }})
                </option>
              </select>
            </div>
          </div>
        </section>

        <section class="quick">
          <div class="grid w-full max-w-2xl grid-cols-1 gap-4 sm:grid-cols-2">
            <button
              v-for="q in visibleQuestions"
              :key="q"
              class="relative overflow-hidden rounded-xl border border-gray-200 bg-white p-4 text-left transition-all duration-300 hover:border-blue-300 hover:shadow-xl group hover:bg-gradient-to-br hover:from-white/90 hover:to-slate-50/90 hover:backdrop-blur-sm sm:p-6"
              @click="ask(q)"
            >
              <div class="text-gray-800 font-medium text-sm leading-relaxed group-hover:text-gray-900 transition-colors duration-300 cursor-pointer">
                {{ q }}
              </div>
            </button>
          </div>
        </section>

        <RouterLink
          to="/project#future-work"
          class="group flex w-full max-w-2xl items-center justify-center gap-1 rounded-xl border border-blue-200/80 bg-white/90 px-4 py-3 text-center text-sm font-medium text-blue-800 shadow-sm backdrop-blur-sm transition-all duration-300 hover:border-blue-300 hover:bg-white hover:shadow-md hover:shadow-blue-500/10"
          :aria-label="futureWorkHomeLink.ariaLabel"
        >
          {{ futureWorkHomeLink.label }}
          <ChevronRight
            class="size-4 shrink-0 transition-transform duration-300 group-hover:translate-x-0.5"
            aria-hidden="true"
          />
        </RouterLink>
      </div>
    </div>

    <!-- Chat area: inline disclaimer, links, and input -->
    <div class="pb-8 flex-shrink-0 relative z-10 w-full max-w-2xl mx-auto px-4 space-y-3">
      <Alert
        class="border-blue-200/80 bg-blue-50/90 text-slate-800 shadow-sm backdrop-blur-sm [&>svg]:text-blue-600"
      >
        <Info class="size-4 shrink-0" aria-hidden="true" />
        <AlertTitle>{{ chatDisclaimer.title }}</AlertTitle>
        <AlertDescription>
          <p>{{ chatDisclaimer.body }}</p>
        </AlertDescription>
      </Alert>

      <div class="flex justify-center gap-3">
        <a
          href="https://github.com/kdm-kev-NTNU"
          target="_blank"
          rel="noopener noreferrer"
          class="inline-flex h-10 w-10 items-center justify-center rounded-full border border-gray-200 bg-white text-gray-700 shadow-sm transition hover:border-gray-300 hover:bg-slate-50 hover:text-gray-900"
          :aria-label="language === 'en' ? 'Kevin on GitHub' : 'Kevin på GitHub'"
        >
          <svg class="size-5" viewBox="0 0 24 24" aria-hidden="true" fill="currentColor">
            <path
              d="M12 0c-6.626 0-12 5.373-12 12 0 5.302 3.438 9.8 8.207 11.387.599.111.793-.261.793-.577v-2.234c-3.338.726-4.033-1.416-4.033-1.416-.546-1.387-1.333-1.756-1.333-1.756-1.089-.745.083-.729.083-.729 1.205.084 1.839 1.237 1.839 1.237 1.07 1.834 2.807 1.304 3.492.997.107-.775.418-1.305.762-1.604-2.665-.305-5.467-1.334-5.467-5.931 0-1.311.469-2.381 1.236-3.221-.124-.303-.535-1.524.117-3.176 0 0 1.008-.322 3.301 1.23.957-.266 1.983-.399 3.003-.404 1.02.005 2.047.138 3.006.404 2.291-1.552 3.297-1.23 3.297-1.23.653 1.653.242 2.874.118 3.176.77.84 1.235 1.911 1.235 3.221 0 4.609-2.807 5.624-5.479 5.921.43.372.823 1.102.823 2.222v3.293c0 .319.192.694.801.576 4.765-1.589 8.199-6.086 8.199-11.386 0-6.627-5.373-12-12-12z"
            />
          </svg>
        </a>
        <a
          href="https://linkedin.com/in/kevin-dennis-mazali/"
          target="_blank"
          rel="noopener noreferrer"
          class="inline-flex h-10 w-10 items-center justify-center rounded-full border border-gray-200 bg-white text-blue-700 shadow-sm transition hover:border-blue-200 hover:bg-blue-50"
          :aria-label="language === 'en' ? 'Kevin on LinkedIn' : 'Kevin på LinkedIn'"
        >
          <svg class="size-5" viewBox="0 0 24 24" aria-hidden="true" fill="currentColor">
            <path
              d="M20.447 20.452h-3.554v-5.569c0-1.328-.027-3.037-1.852-3.037-1.853 0-2.136 1.445-2.136 2.939v5.667H9.351V9h3.414v1.561h.046c.477-.9 1.637-1.85 3.37-1.85 3.601 0 4.267 2.37 4.267 5.455v6.286zM5.337 7.433c-1.144 0-2.063-.926-2.063-2.065 0-1.138.92-2.063 2.063-2.063 1.14 0 2.064.925 2.064 2.063 0 1.139-.925 2.065-2.064 2.065zm1.782 13.019H3.555V9h3.564v11.452zM22.225 0H1.771C.792 0 0 .774 0 1.729v20.542C0 23.227.792 24 1.771 24h20.451C23.2 24 24 23.227 24 22.271V1.729C24 .774 23.2 0 22.222 0h.003z"
            />
          </svg>
        </a>
      </div>

      <form
        class="relative mx-auto flex max-w-md gap-3 rounded-xl border-2 border-blue-200/20 bg-white/90 p-2 backdrop-blur-sm transition-all duration-300 hover:border-blue-300/40 hover:bg-white/95 hover:shadow-lg hover:shadow-blue-500/15 focus-within:border-blue-300/60 focus-within:bg-white/98 focus-within:shadow-lg focus-within:shadow-blue-500/25"
        @submit.prevent="submitQuick"
      >
        <Button
          v-if="supportsSpeechInput"
          type="button"
          variant="outline"
          :disabled="isTranscribing"
          :aria-pressed="isRecording"
          :aria-label="language === 'en' ? 'Voice input' : 'Taleinndata'"
          class="relative shrink-0 rounded-lg border border-blue-200/80 bg-white/90 px-2 text-slate-700 hover:bg-blue-50/80 disabled:opacity-50 sm:px-3"
          :class="{ 'animate-pulse ring-2 ring-red-400 ring-offset-1': isRecording }"
          @click="toggleVoiceInput"
        >
          <Loader2 v-if="isTranscribing" class="h-5 w-5 shrink-0 animate-spin text-blue-600" />
          <Square v-else-if="isRecording" class="h-5 w-5 shrink-0 text-red-600" />
          <Mic v-else class="h-5 w-5 shrink-0" />
          <span class="hidden min-w-[3rem] ps-1 text-sm font-medium sm:inline">{{
            language === 'no' ? 'Snakk' : 'Speak'
          }}</span>
        </Button>
        <AudioWaveform
          v-if="isRecording"
          :stream="recordingMediaStream"
          :aria-label="language === 'en' ? 'Audio level while recording' : 'Lydnivå under opptak'"
        />
        <Input
          v-else
          v-model="quickQuestion"
          type="text"
          class="flex-1 border-2 border-blue-200/20 bg-white/80 rounded-lg transition-all duration-300 focus:bg-white/95 focus:border-blue-300/50 focus:shadow-sm focus:shadow-blue-500/10 focus:outline-none placeholder:text-blue-600/60 placeholder:font-medium"
          :disabled="isTranscribing"
          :placeholder="language === 'en' ? `Curious? Kevin's AI is here to answer!` : `Nysgjerrig? Kevin sin AI svarer gjerne!`"
        />
        <Button
          type="submit"
          :disabled="isTranscribing || isRecording || !quickQuestion.trim()"
          class="cursor-pointer shrink-0 bg-gradient-to-r from-blue-600 to-blue-700 font-semibold text-white transition-all duration-300 hover:-translate-y-0.5 hover:from-blue-700 hover:to-blue-800 hover:shadow-lg hover:shadow-blue-500/40 relative overflow-hidden"
        >
          Send →
        </Button>
      </form>

      <Alert v-if="voiceError" variant="destructive" class="mx-auto max-w-md">
        <AlertDescription>{{ voiceError }}</AlertDescription>
      </Alert>

      <RouterLink
        v-if="voiceFeatureEnabled === true"
        to="/voice"
        class="mx-auto flex max-w-md items-center justify-center gap-2 rounded-xl border-2 border-blue-300/80 bg-gradient-to-r from-blue-600 to-indigo-700 px-4 py-3 text-center text-sm font-semibold text-white shadow-md shadow-blue-500/20 transition hover:from-blue-700 hover:to-indigo-800 hover:shadow-lg focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-white"
        :aria-label="voiceCtaAria"
      >
        <Headphones class="size-5 shrink-0" aria-hidden="true" />
        {{ voiceCta }}
      </RouterLink>
    </div>

    <!-- Mobile: compact FAB-style feedback; sm+: card with copy -->
    <RouterLink
      to="/feedback"
      class="feedback-corner group fixed bottom-4 left-3 z-[60] flex items-center justify-center rounded-full border-2 border-blue-300/70 bg-white/95 p-3 shadow-lg shadow-blue-900/10 ring-1 ring-blue-500/15 backdrop-blur-md transition hover:-translate-y-0.5 hover:border-blue-500 hover:bg-white hover:shadow-xl hover:shadow-blue-500/20 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-blue-600 sm:bottom-6 sm:left-5 sm:max-w-[min(20.5rem,calc(100vw-1.5rem))] sm:items-start sm:justify-start sm:gap-4 sm:rounded-2xl sm:p-4"
      :aria-label="feedbackInvite.ariaLabel"
    >
      <div
        class="flex size-12 shrink-0 items-center justify-center rounded-full bg-gradient-to-br from-blue-500 to-blue-700 text-white shadow-md sm:size-16 sm:rounded-xl"
        aria-hidden="true"
      >
        <MessageSquare class="size-7 sm:size-9" stroke-width="2" />
      </div>
      <div class="hidden min-w-0 flex-1 pt-0.5 text-left sm:block">
        <p class="text-sm font-medium leading-snug text-slate-800 sm:text-[0.95rem] sm:leading-snug">
          {{ feedbackInvite.body }}
        </p>
        <span
          class="mt-2 inline-flex items-center gap-0.5 text-sm font-semibold text-blue-700 transition group-hover:text-blue-900"
        >
          {{ feedbackInvite.cta }}
          <ChevronRight class="size-4 shrink-0" aria-hidden="true" />
        </span>
      </div>
    </RouterLink>
  </main>
</template>

<style scoped>
@keyframes gradient-x {
  0%, 100% {
    background-position: 0% 50%;
  }
  50% {
    background-position: 100% 50%;
  }
}

@keyframes float {
  0%, 100% {
    transform: translate(0, 0) scale(1);
  }
  25% {
    transform: translate(20px, -30px) scale(1.1);
  }
  50% {
    transform: translate(-15px, -20px) scale(0.9);
  }
  75% {
    transform: translate(-25px, 10px) scale(1.05);
  }
}

.animate-gradient-x {
  background-size: 200% 200%;
  animation: gradient-x 3s ease-in-out infinite;
}

/* Blue Blob Shapes */
.blob-container {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  pointer-events: none;
  overflow: hidden;
  z-index: 1;
}

.blob {
  position: absolute;
  border-radius: 50%;
  filter: blur(40px);
  animation: float 6s ease-in-out infinite;
}

.blob-1 {
  width: 300px;
  height: 300px;
  background: radial-gradient(circle, rgba(59, 130, 246, 0.6) 0%, rgba(37, 99, 235, 0.5) 50%, transparent 70%);
  top: 10%;
  left: 5%;
  animation-delay: 0s;
  animation-duration: 8s;
}

.blob-2 {
  width: 200px;
  height: 200px;
  background: radial-gradient(circle, rgba(96, 165, 250, 0.55) 0%, rgba(59, 130, 246, 0.5) 50%, transparent 70%);
  top: 60%;
  right: 10%;
  animation-delay: 2s;
  animation-duration: 10s;
}

.blob-3 {
  width: 250px;
  height: 250px;
  background: radial-gradient(circle, rgba(37, 99, 235, 0.6) 0%, rgba(29, 78, 216, 0.5) 50%, transparent 70%);
  top: 30%;
  right: 20%;
  animation-delay: 4s;
  animation-duration: 7s;
}

.blob-4 {
  width: 180px;
  height: 180px;
  background: radial-gradient(circle, rgba(147, 197, 253, 0.65) 0%, rgba(96, 165, 250, 0.55) 50%, transparent 70%);
  bottom: 20%;
  left: 15%;
  animation-delay: 1s;
  animation-duration: 9s;
}

.blob-5 {
  width: 220px;
  height: 220px;
  background: radial-gradient(circle, rgba(29, 78, 216, 0.6) 0%, rgba(30, 64, 175, 0.5) 50%, transparent 70%);
  top: 70%;
  left: 60%;
  animation-delay: 3s;
  animation-duration: 11s;
}

.blob-6 {
  width: 160px;
  height: 160px;
  background: radial-gradient(circle, rgba(59, 130, 246, 0.7) 0%, rgba(37, 99, 235, 0.6) 50%, transparent 70%);
  top: 15%;
  left: 70%;
  animation-delay: 5s;
  animation-duration: 6s;
}

/* Decorative blobs: smaller blur/size on narrow viewports */
@media (max-width: 768px) {
  .blob {
    filter: blur(30px);
  }

  .blob-1,
  .blob-3 {
    width: 200px;
    height: 200px;
  }

  .blob-2,
  .blob-4,
  .blob-5 {
    width: 150px;
    height: 150px;
  }

  .blob-6 {
    width: 120px;
    height: 120px;
  }
}
</style>
