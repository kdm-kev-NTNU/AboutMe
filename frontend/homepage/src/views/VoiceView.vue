<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { RouterLink } from 'vue-router'
import { Headphones, MessageSquare } from 'lucide-vue-next'
import { useLangStore } from '@/stores/lang'
import { useVoiceModelStore } from '@/stores/voice-model'
import {
  fetchRealtimeVoiceStatus,
  type RealtimeReasoningEffort,
  type RealtimeVadEagerness,
  type RealtimeVoiceChoice,
} from '@/lib/realtime-voice'
import RealtimeVoicePanel from '@/components/voice/RealtimeVoicePanel.vue'
import { Button } from '@/components/ui/button'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'

const VOICE_PREP_DISMISSED_KEY = 'voicePrepDismissed.v1'

const langStore = useLangStore()
const voiceModelStore = useVoiceModelStore()
const language = computed(() => langStore.language)

const liveAvailable = ref<boolean | null>(null)
const voiceOptions = ref<RealtimeVoiceChoice[]>(['marin', 'cedar'])
const reasoningOptions = ref<RealtimeReasoningEffort[]>(['low', 'medium', 'high'])
const vadEagernessOptions = ref<RealtimeVadEagerness[]>(['low', 'medium', 'high', 'auto'])
const defaultVoice = ref<RealtimeVoiceChoice>('marin')
const defaultReasoningEffort = ref<RealtimeReasoningEffort>('low')
const defaultVadEagerness = ref<RealtimeVadEagerness>('low')
const showPrepPopup = ref(false)

const shouldShowPrepPopup = () => {
  try {
    return !localStorage.getItem(VOICE_PREP_DISMISSED_KEY)
  } catch {
    return true
  }
}

const markPrepPopupDismissed = () => {
  try {
    localStorage.setItem(VOICE_PREP_DISMISSED_KEY, 'true')
  } catch {
    /* ignore quota / private mode */
  }
}

const dismissPrepPopup = () => {
  showPrepPopup.value = false
  markPrepPopupDismissed()
}

watch(showPrepPopup, (isOpen, wasOpen) => {
  if (wasOpen === true && isOpen === false) {
    markPrepPopupDismissed()
  }
})

onMounted(async () => {
  if (shouldShowPrepPopup()) {
    showPrepPopup.value = true
  }

  const [status] = await Promise.all([
    fetchRealtimeVoiceStatus(),
    voiceModelStore.ensureModelsLoaded(),
  ])
  liveAvailable.value = status.liveEnabled && voiceModelStore.hasModels
  voiceOptions.value = status.voices
  reasoningOptions.value = status.reasoningEfforts
  vadEagernessOptions.value = status.vadEagernessOptions
  defaultVoice.value = status.voice
  defaultReasoningEffort.value = status.reasoningEffort
  defaultVadEagerness.value = status.vadEagerness
})

const copy = computed(() => {
  const en = language.value === 'en'
  return {
    title: en ? "Talk with Kevin's AI" : 'Snakk med Kevin sin AI',
    chatAlt: en ? 'Prefer typing? Use text chat' : 'Foretrekker du å skrive? Bruk tekstchat',
    chatAltHint: en
      ? 'Text chat works well if you are in a noisy place or do not have a headset.'
      : 'Tekstchat fungerer godt hvis du er et støyende sted eller ikke har headset.',
    prepTitle: en ? 'Before you start' : 'Før du starter',
    prepBody: en
      ? 'For clearer speech recognition, use a headset and sit somewhere quiet. If that is not possible, text chat is a better fit.'
      : 'For klarere talegjenkjenning, bruk headset og sitt et stille sted. Hvis det ikke passer, er tekstchat et bedre alternativ.',
    prepTips: en
      ? 'Headset · quiet room · speak clearly'
      : 'Headset · stille rom · snakk tydelig',
    prepChatCta: en ? 'Use text chat instead' : 'Bruk tekstchat i stedet',
    prepDismiss: en ? 'Got it — continue' : 'Forstått — fortsett',
  }
})
</script>

<template>
  <main
    id="main-content"
    class="relative flex min-h-0 flex-1 flex-col overflow-y-auto bg-gradient-to-br from-slate-100 via-blue-50 to-slate-100 pt-20"
  >
    <Dialog v-model:open="showPrepPopup">
      <DialogContent class="sm:max-w-xl">
        <DialogHeader>
          <DialogTitle>{{ copy.prepTitle }}</DialogTitle>
          <DialogDescription>{{ copy.prepBody }}</DialogDescription>
        </DialogHeader>
        <p class="flex items-start gap-2 text-sm text-slate-700">
          <Headphones class="mt-0.5 size-4 shrink-0 text-blue-700" aria-hidden="true" />
          <span>{{ copy.prepTips }}</span>
        </p>
        <DialogFooter class="flex-col gap-2 sm:flex-row sm:justify-end">
          <Button as-child variant="outline">
            <RouterLink to="/chat" @click="markPrepPopupDismissed">{{ copy.prepChatCta }}</RouterLink>
          </Button>
          <Button type="button" data-testid="voice-prep-dismiss" @click="dismissPrepPopup">
            {{ copy.prepDismiss }}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>

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
        <p class="mx-auto mt-2 max-w-md text-sm text-slate-600">
          {{ copy.chatAltHint }}
        </p>
        <RouterLink
          to="/chat"
          data-testid="voice-chat-alternative"
          class="mt-3 inline-flex items-center gap-1.5 rounded-lg border border-blue-200 bg-white/90 px-3 py-2 text-sm font-medium text-blue-700 shadow-sm hover:bg-blue-50 hover:text-blue-900"
        >
          <MessageSquare class="size-4 shrink-0" aria-hidden="true" />
          {{ copy.chatAlt }}
        </RouterLink>
      </div>

      <RealtimeVoicePanel
        :language="language"
        :available="liveAvailable"
        :voice-options="voiceOptions"
        :reasoning-options="reasoningOptions"
        :vad-eagerness-options="vadEagernessOptions"
        :default-voice="defaultVoice"
        :default-reasoning-effort="defaultReasoningEffort"
        :default-vad-eagerness="defaultVadEagerness"
      />
    </div>
  </main>
</template>
