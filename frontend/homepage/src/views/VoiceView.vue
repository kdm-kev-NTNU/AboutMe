<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { MessageSquare } from 'lucide-vue-next'
import { useLangStore } from '@/stores/lang'
import { useVoiceModelStore } from '@/stores/voice-model'
import { useVoiceModeStore, type VoiceMode } from '@/stores/voice-mode'
import { captureProductAnalyticsEvent } from '@/lib/analytics'
import { POSTHOG_VOICE_EVENTS } from '@/lib/posthog-sdk'
import {
  fetchRealtimeVoiceStatus,
  type RealtimeReasoningEffort,
  type RealtimeVoiceChoice,
} from '@/lib/realtime-voice'
import VoiceModeSwitcher from '@/components/voice/VoiceModeSwitcher.vue'
import StandardVoicePanel from '@/components/voice/StandardVoicePanel.vue'
import RealtimeVoicePanel from '@/components/voice/RealtimeVoicePanel.vue'

const langStore = useLangStore()
const voiceModelStore = useVoiceModelStore()
const voiceModeStore = useVoiceModeStore()
const language = computed(() => langStore.language)
const route = useRoute()
const router = useRouter()

const liveAvailable = ref<boolean | null>(null)
const standardAvailable = ref<boolean | null>(null)
const voiceOptions = ref<RealtimeVoiceChoice[]>(['marin', 'cedar'])
const reasoningOptions = ref<RealtimeReasoningEffort[]>(['low', 'medium', 'high'])
const defaultVoice = ref<RealtimeVoiceChoice>('marin')
const defaultReasoningEffort = ref<RealtimeReasoningEffort>('low')
const selectedMode = computed<VoiceMode>({
  get: () => voiceModeStore.mode,
  set: (value) => voiceModeStore.setMode(value),
})

onMounted(async () => {
  voiceModeStore.load()
  if (route.query.mode === 'live') {
    voiceModeStore.setMode('live')
  }
  const [status] = await Promise.all([
    fetchRealtimeVoiceStatus(),
    voiceModelStore.ensureModelsLoaded(),
  ])
  liveAvailable.value = status.liveEnabled && voiceModelStore.hasModels
  standardAvailable.value = status.standardEnabled
  voiceOptions.value = status.voices
  reasoningOptions.value = status.reasoningEfforts
  defaultVoice.value = status.voice
  defaultReasoningEffort.value = status.reasoningEffort
})

const copy = computed(() => {
  const en = language.value === 'en'
  return {
    title: en ? "Talk with Kevin's AI" : 'Snakk med Kevin sin AI',
    chatAlt: en ? 'Use text chat instead' : 'Bruk tekstchat',
    modeHint: en
      ? 'Choose between robust standard voice and experimental live mode.'
      : 'Velg mellom robust standard stemme og eksperimentell live-modus.',
  }
})

watch(
  () => selectedMode.value,
  (mode) => {
    captureProductAnalyticsEvent(POSTHOG_VOICE_EVENTS.MODE_SELECTED, { mode })
    const nextQuery = { ...route.query }
    if (mode === 'live') nextQuery.mode = 'live'
    else delete nextQuery.mode
    void router.replace({ query: nextQuery })
  }
)
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
        <RouterLink
          to="/chat"
          class="mt-3 inline-flex items-center gap-1.5 text-sm font-medium text-blue-700 hover:text-blue-900"
        >
          <MessageSquare class="size-4 shrink-0" aria-hidden="true" />
          {{ copy.chatAlt }}
        </RouterLink>
      </div>
      <p class="mb-4 text-sm text-slate-600">{{ copy.modeHint }}</p>
      <VoiceModeSwitcher v-model="selectedMode" :language="language" />

      <StandardVoicePanel
        v-if="selectedMode === 'standard'"
        :language="language"
        :available="standardAvailable"
      />
      <RealtimeVoicePanel
        v-else
        :language="language"
        :available="liveAvailable"
        :voice-options="voiceOptions"
        :reasoning-options="reasoningOptions"
        :default-voice="defaultVoice"
        :default-reasoning-effort="defaultReasoningEffort"
      />
    </div>
  </main>
</template>
