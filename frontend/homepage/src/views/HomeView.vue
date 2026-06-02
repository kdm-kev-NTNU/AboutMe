<script setup lang="ts">
import { useRouter, RouterLink } from 'vue-router'
import { ref, computed, onMounted } from 'vue'
import { useLangStore } from '../stores/lang'
import { Button } from '@/components/ui/button'
import { MessageSquare, ChevronRight, Mic, Headphones } from 'lucide-vue-next'
import { fetchRealtimeVoiceStatus } from '@/lib/realtime-voice'

const router = useRouter()

const langStore = useLangStore()

const language = computed({
  get: () => langStore.language,
  set: (v: 'en' | 'no') => langStore.setLanguage(v),
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

/** Null until loaded from GET /realtime/status */
const voiceEnabled = ref<boolean | null>(null)

const voiceCtaAria = computed(() =>
  language.value === 'no' ? 'Gå til stemmemodus' : 'Go to voice mode',
)

function goToVoiceChat() {
  router.push({ name: 'voice' })
}

const voiceStatus = computed(() => {
  if (language.value === 'no') {
    if (voiceEnabled.value === true) return 'Stemme er tilgjengelig'
    if (voiceEnabled.value === false) return 'Stemme er midlertidig av'
    return 'Sjekker stemmestatus'
  }
  if (voiceEnabled.value === true) return 'Voice is available'
  if (voiceEnabled.value === false) return 'Voice is temporarily off'
  return 'Checking voice status'
})

onMounted(() => {
  void fetchRealtimeVoiceStatus().then((status) => {
    voiceEnabled.value = status.liveEnabled
  })
})
</script>

<template>
  <main id="main-content" class="relative flex min-h-0 flex-1 flex-col overflow-y-auto bg-gradient-to-br from-slate-50 via-blue-50 to-slate-100 pt-20">
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
    <div class="relative z-10 flex min-h-full flex-col items-center justify-start gap-8 px-4 py-8">
      <section
        class="grid w-full max-w-6xl items-center gap-8 overflow-hidden rounded-[2rem] border border-blue-100/80 bg-white/82 p-5 shadow-2xl shadow-blue-950/10 backdrop-blur-xl sm:p-8 lg:grid-cols-[1.1fr_0.9fr] lg:p-10"
        aria-labelledby="voice-first-title"
      >
        <div class="min-w-0">
          <div class="mb-5 inline-flex items-center gap-2 rounded-full border border-blue-100 bg-blue-50/80 px-3 py-1.5 text-xs font-semibold text-blue-800">
            <span
              class="size-2 rounded-full"
              :class="voiceEnabled === false ? 'bg-amber-500' : 'bg-emerald-500'"
              aria-hidden="true"
            ></span>
            {{ voiceStatus }}
          </div>
          <h1
            id="voice-first-title"
            class="max-w-3xl text-3xl font-bold tracking-tight text-slate-950 sm:text-5xl lg:text-6xl"
          >
            {{ language === 'no' ? 'Snakk med Kevin sin AI først.' : "Talk with Kevin's AI first." }}
          </h1>
          <p class="mt-5 max-w-2xl text-base leading-7 text-slate-600 sm:text-lg">
            {{
              language === 'no'
                ? 'Snakk med en AI som kjenner porteføljen min og kan svare på spørsmål om prosjekter, erfaring og teknologi.'
                : 'Talk with an AI that knows my portfolio and can answer questions about projects, experience, and tech.'
            }}
          </p>
          <div class="mt-7 flex flex-col gap-3 sm:flex-row">
            <Button
              type="button"
              :aria-label="voiceCtaAria"
              class="h-14 w-full justify-center rounded-2xl bg-gradient-to-r from-blue-600 to-indigo-700 px-5 text-sm font-semibold text-white shadow-xl shadow-blue-500/25 transition hover:-translate-y-0.5 hover:from-blue-700 hover:to-indigo-800 sm:w-auto sm:px-7 sm:text-base"
              @click="goToVoiceChat"
            >
              <Headphones class="me-2 size-5" aria-hidden="true" />
              {{ language === 'no' ? 'Start stemmechat' : 'Start voice chat' }}
            </Button>
            <Button
              as-child
              variant="outline"
              class="h-14 w-full justify-center rounded-2xl border-blue-200 bg-white/85 px-5 text-sm font-semibold text-slate-800 hover:bg-blue-50 sm:w-auto sm:px-7 sm:text-base"
            >
              <RouterLink to="/chat" class="inline-flex items-center">
                <MessageSquare class="me-2 size-5" aria-hidden="true" />
                {{ language === 'no' ? 'Bruk tekstchat' : 'Use text chat' }}
              </RouterLink>
            </Button>
          </div>
        </div>

        <div class="relative mx-auto flex min-h-[21rem] w-full max-w-md flex-col items-center justify-center">
          <div class="absolute inset-6 rounded-full bg-blue-500/10 blur-3xl" aria-hidden="true"></div>
          <button
            type="button"
            :aria-label="voiceCtaAria"
            class="group relative flex aspect-square w-64 max-w-[80vw] items-center justify-center rounded-full bg-gradient-to-br from-cyan-400 via-blue-600 to-indigo-800 text-white shadow-2xl shadow-blue-700/30 ring-8 ring-white/70 transition hover:-translate-y-1 hover:shadow-blue-700/40 sm:w-72"
            @click="goToVoiceChat"
          >
            <span class="absolute inset-8 rounded-full border border-white/30" aria-hidden="true"></span>
            <Mic class="size-20 transition group-hover:scale-105" stroke-width="1.8" aria-hidden="true" />
          </button>
        </div>
      </section>

      <div class="flex flex-col items-center space-y-8">
        <section class="brand">
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
        </section>
      </div>
    </div>

    <!-- Social links -->
    <div class="pb-8 flex-shrink-0 relative z-10 w-full max-w-2xl mx-auto px-4">
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
    </div>

    <!-- Mobile: compact FAB; sm+: card with copy -->
    <RouterLink
      to="/feedback"
      class="feedback-corner-mobile group fixed bottom-6 left-5 z-[60] flex size-14 items-center justify-center rounded-full border-2 border-blue-300/70 bg-white/95 text-blue-700 shadow-lg shadow-blue-900/10 ring-1 ring-blue-500/15 backdrop-blur-md transition hover:-translate-y-0.5 hover:border-blue-500 hover:bg-white hover:shadow-xl hover:shadow-blue-500/20 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-blue-600 sm:hidden"
      :aria-label="feedbackInvite.ariaLabel"
    >
      <MessageSquare class="size-7" stroke-width="2" aria-hidden="true" />
    </RouterLink>

    <RouterLink
      to="/feedback"
      class="feedback-corner group fixed bottom-6 left-5 z-[60] hidden max-w-[min(20.5rem,calc(100vw-1.5rem))] items-start justify-start gap-4 rounded-2xl border-2 border-blue-300/70 bg-white/95 p-4 shadow-lg shadow-blue-900/10 ring-1 ring-blue-500/15 backdrop-blur-md transition hover:-translate-y-0.5 hover:border-blue-500 hover:bg-white hover:shadow-xl hover:shadow-blue-500/20 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-blue-600 sm:flex"
      :aria-label="feedbackInvite.ariaLabel"
    >
      <div
        class="flex size-12 shrink-0 items-center justify-center rounded-full bg-gradient-to-br from-blue-500 to-blue-700 text-white shadow-md sm:size-16 sm:rounded-xl"
        aria-hidden="true"
      >
        <MessageSquare class="size-7 sm:size-9" stroke-width="2" />
      </div>
      <div class="min-w-0 flex-1 pt-0.5 text-left">
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
  opacity: 0.38;
}

.blob {
  position: absolute;
  border-radius: 50%;
  filter: blur(52px);
  animation: float 10s ease-in-out infinite;
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
