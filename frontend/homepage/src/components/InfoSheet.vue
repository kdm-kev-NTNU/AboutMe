<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useLangStore } from '../stores/lang'
import { useDialogState } from '../composables/useDialogState'
import { Info, Github, Linkedin, AlertTriangle } from 'lucide-vue-next'

const open = ref(false)
const router = useRouter()
const langStore = useLangStore()
const { isInfoDialogOpen } = useDialogState()
const language = computed(() => langStore.language)

function toggle() {
  open.value = !open.value
}

function goToChatHistory() {
  open.value = false
  router.push({ name: 'chat-history' })
}

// Computed property for button classes based on focus state
const buttonClasses = computed(() => {
  const baseClasses = 'fixed left-4 bottom-4 w-11 h-11 rounded-full border border-gray-300 bg-white cursor-pointer shadow-lg flex items-center justify-center transition-all duration-200 ease-in-out text-gray-700 hover:bg-gray-50 hover:border-gray-400 hover:scale-105 hover:text-gray-900 active:scale-95'
  
  if (isInfoDialogOpen.value) {
    // Higher z-index to appear above dialog overlay
    return `${baseClasses} z-[60]`
  }
  
  // Normal styling
  return `${baseClasses} z-50`
})
</script>

<template>
  <button :class="buttonClasses" @click="toggle" :aria-label="language === 'en' ? 'Open info' : 'Åpne info'">
    <Info class="w-5 h-5" />
  </button>

  <div v-if="open" class="fixed inset-0 bg-black/18 flex items-center justify-center p-4 z-100" @click.self="toggle">
    <div class="w-full max-w-lg bg-white rounded-xl shadow-2xl p-4.5 pb-5.5">
      <div class="flex items-center justify-center relative">
        <h2 class="m-0 text-xl">About Me</h2>
        <button class="absolute right-0 border-none bg-transparent cursor-pointer text-lg" @click="toggle" aria-label="Close">✕</button>
      </div>

      <p class="text-gray-500 my-1.5 mb-4 text-center">{{ language === 'en' ? 'Welcome to my portfolio!' : 'Velkommen til mitt portfolio!' }}</p>

      <h3 class="my-4.5 mt-0 text-base">{{ language === 'en' ? 'Social Links' : 'Sosiale lenker' }}</h3>
      <div class="flex flex-row gap-3 mb-5">
        <a href="https://github.com/kdm-kev-NTNU" target="_blank" rel="noopener" class="flex items-center gap-3 px-4 py-3 rounded-lg no-underline text-gray-900 border border-gray-200 bg-gray-50 transition-all duration-200 ease-in-out flex-1 hover:bg-gray-100 hover:border-gray-300 hover:-translate-y-0.5 hover:shadow-md hover:border-gray-800 hover:bg-gray-50">
          <Github class="w-4.5 h-4.5 text-blue-600 stroke-1.5" />
          <span>{{ language === 'en' ? 'GitHub' : 'GitHub' }}</span>
        </a>
        <a href="https://linkedin.com/in/kevin-dennis-mazali/" target="_blank" rel="noopener" class="flex items-center gap-3 px-4 py-3 rounded-lg no-underline text-gray-900 border border-gray-200 bg-gray-50 transition-all duration-200 ease-in-out flex-1 hover:bg-gray-100 hover:border-gray-300 hover:-translate-y-0.5 hover:shadow-md hover:border-blue-600 hover:bg-blue-50">
          <Linkedin class="w-4.5 h-4.5 text-blue-600 stroke-1.5" />
          <span>{{ language === 'en' ? 'LinkedIn' : 'LinkedIn' }}</span>
        </a>
      </div>

      <h3 class="my-4.5 mt-0 text-base">{{ language === 'en' ? "How Kevin's AI Knows About Me" : 'Hvordan Kevin sin AI vet om meg' }}</h3>
      <p class="text-gray-700 leading-relaxed my-0 mb-5 p-4 bg-gradient-to-br from-slate-50 to-slate-100 rounded-lg border-l-4 border-blue-600 text-sm shadow-sm">{{ language === 'en'
        ? "I provide an AI model context about my background and education and how it should answer your questions. This can still hallucinate, so don't trust everything blindly."
        : 'Jeg gir en AI-modell kontekst om min bakgrunn og utdanning og hvordan den skal svare på spørsmål. Den kan fortsatt hallusinere, så ikke stol blindt på alt.'
      }}</p>

      <div class="mt-5">
        <button
          @click="goToChatHistory"
          class="w-full px-4 py-3 rounded-lg border border-gray-200 bg-gray-50 text-gray-700 text-sm cursor-pointer hover:bg-gray-100 hover:border-gray-300 hover:text-gray-900 transition-all duration-200 ease-in-out hover:-translate-y-0.5 hover:shadow-md flex items-center justify-center gap-2"
        >
          <AlertTriangle class="w-4 h-4 flex-shrink-0 text-amber-400" />
          <span>{{ language === 'en' ? "Don't click here" : 'Ikke klikk her' }}</span>
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* No additional styles needed - fully converted to Tailwind */
</style>


