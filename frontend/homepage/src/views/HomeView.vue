<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useLangStore } from '@/stores/lang'
import { useOpenAiRealtimeVoice } from '@/composables/useOpenAiRealtimeVoice'
import { Button } from '@/components/ui/button'
import VoiceOrb from '@/components/VoiceOrb.vue'
import BudgetDialog from '@/components/BudgetDialog.vue'

const router = useRouter()
const langStore = useLangStore()

const language = computed({
  get: () => langStore.language,
  set: (v: 'en' | 'no') => langStore.setLanguage(v),
})

const { status, loadStatus } = useOpenAiRealtimeVoice(language)

const budgetDialogOpen = ref(false)
const checkingVoice = ref(false)

onMounted(() => {
  void loadStatus()
})

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
  }
})

async function handleStartVoice() {
  checkingVoice.value = true
  await loadStatus()
  checkingVoice.value = false

  if (!status.value?.enabled) {
    budgetDialogOpen.value = true
    return
  }
  router.push({ name: 'voice' })
}

async function handleRetry() {
  await handleStartVoice()
}

function handleOpenChat() {
  router.push({ name: 'chat' })
}
</script>

<template>
  <main class="relative flex min-h-0 flex-1 flex-col overflow-y-auto bg-linear-to-r from-[#f7fcff] to-[#e3f0ff]">
    <!-- Language toggle -->
    <div class="absolute top-6 right-6 z-20">
      <div class="flex rounded-full bg-white/80 p-1 shadow-sm backdrop-blur-sm border border-slate-200/60">
        <button
          class="rounded-full px-3 py-1.5 text-xs font-medium transition-all"
          :class="language === 'en' ? 'bg-white text-blue-700 shadow-sm font-semibold' : 'text-slate-500'"
          @click="language = 'en'"
        >
          EN
        </button>
        <button
          class="rounded-full px-3 py-1.5 text-xs font-medium transition-all"
          :class="language === 'no' ? 'bg-white text-blue-700 shadow-sm font-semibold' : 'text-slate-500'"
          @click="language = 'no'"
        >
          NO
        </button>
      </div>
    </div>

    <!-- Content -->
    <div class="relative z-10 mx-auto flex w-full max-w-6xl flex-1 flex-col justify-center px-6 py-16 pt-24 lg:py-8 lg:pt-20">
      <!-- Desktop heading -->
      <h1 class="mb-10 hidden text-[58px] font-bold leading-[1.05] text-[#0f1729] lg:block">
        {{ copy.heading }}
      </h1>

      <!-- Mobile heading -->
      <h1 class="mb-8 text-[39px] font-bold leading-[1.08] text-[#0f1729] lg:hidden">
        {{ copy.headingMobile }}
      </h1>

      <!-- Cards container -->
      <div class="flex flex-col gap-6 lg:flex-row lg:items-start lg:gap-8">
        <!-- Talk card (primary) -->
        <div class="flex w-full flex-col overflow-hidden rounded-[24px] border border-[#a6c2f5] bg-white p-8 shadow-[0px_18px_32px_rgba(20,38,77,0.12)] lg:max-w-[560px] lg:rounded-[28px] lg:p-10">
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
              :disabled="checkingVoice"
              class="h-[52px] rounded-2xl bg-[#2663eb] px-7 text-[15px] font-semibold text-white shadow-[0px_14px_24px_rgba(38,99,235,0.25)] hover:bg-blue-700 disabled:opacity-60"
              @click="handleStartVoice"
            >
              {{ copy.talkCta }}
            </Button>
          </div>
        </div>

        <!-- Ask card (secondary) -->
        <div class="w-full overflow-hidden rounded-[22px] border border-[#d1def0] bg-white/82 p-8 lg:mt-12 lg:max-w-[470px] lg:rounded-[26px]">
          <!-- Desktop layout -->
          <div class="hidden flex-col lg:flex">
            <h2 class="text-[38px] font-bold leading-[45px] text-[#0f1729]">
              {{ copy.askTitle }}
            </h2>
            <p class="mt-3 text-[17px] leading-relaxed text-[#47546b]">
              {{ copy.askDesc }}
            </p>
            <div class="mt-6">
              <Button
                variant="outline"
                class="h-[52px] rounded-2xl border-[#c7d6ed] px-7 text-[15px] font-semibold text-[#1f293b] hover:bg-slate-50"
                @click="handleOpenChat"
              >
                {{ copy.askCta }}
              </Button>
            </div>
          </div>

          <!-- Mobile layout -->
          <div class="flex flex-col lg:hidden">
            <h2 class="text-2xl font-bold text-[#0f1729]">
              {{ copy.askTitleMobile }}
            </h2>
            <div class="mt-4">
              <Button
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
    </div>

    <!-- Budget dialog -->
    <BudgetDialog
      :open="budgetDialogOpen"
      @update:open="budgetDialogOpen = $event"
      @retry="handleRetry"
    />
  </main>
</template>
