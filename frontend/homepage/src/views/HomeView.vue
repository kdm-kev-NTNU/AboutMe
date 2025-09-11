<script setup lang="ts">
import { useRouter } from 'vue-router'
import { ref, computed, onMounted } from 'vue'
import { useLangStore } from '../stores/lang'
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { Button } from '@/components/ui/button'

const router = useRouter()

const langStore = useLangStore()
const language = computed({
  get: () => langStore.language,
  set: (v: 'en' | 'no') => langStore.setLanguage(v),
})

const questionsByLang: Record<'en' | 'no', string[]> = {
  en: [
    'Why did Kevin create this website?',
    'Which courses has Kevin taken?',
    'Which projects has Kevin worked on?',
    'Who is Kevin?',
  ],
  no: [
    'Hvorfor lagde Kevin denne nettsiden?',
    'Hvilke emner har Kevin hatt?',
    'Hvilke prosjekter har Kevin jobbet med?',
    'Hvem er Kevin?',
  ],
}

const visibleQuestions = computed(() => questionsByLang[language.value])

const quickQuestion = ref('')
const showWelcomeDialog = ref(false)

function ask(q: string) {
  router.push({ name: 'chat', query: { q } })
}

function submitQuick() {
  const q = quickQuestion.value.trim()
  if (!q) return
  ask(q)
}

onMounted(() => {
  // Check if user has already seen the welcome dialog in this tab session
  const hasSeenWelcome = sessionStorage.getItem('hasSeenWelcome')
  if (!hasSeenWelcome) {
    showWelcomeDialog.value = true
    sessionStorage.setItem('hasSeenWelcome', 'true')
  }
})
</script>

<template>
  <main class="home flex flex-col min-h-screen pt-20">
    <!-- Welcome Dialog -->
    <Dialog v-model:open="showWelcomeDialog">
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Welcome to Kevin's Portfolio!</DialogTitle>
          <DialogDescription>
            The website is still under development.
          </DialogDescription>
        </DialogHeader>
      </DialogContent>
    </Dialog>

    <!-- Main Content - Centered -->
    <div class="flex-1 flex flex-col items-center justify-center py-8">
      <div class="flex flex-col items-center space-y-8">
        <section class="brand">
          <h1 class="text-4xl font-bold text-center mb-4">Kevin's <span>AI</span>.</h1>
          <div class="flex justify-center">
            <div class="relative bg-gray-200 rounded-full p-1 flex">
              <div 
                class="absolute top-1 bottom-1 w-16 bg-white rounded-full shadow-md transition-transform duration-300 ease-in-out"
                :class="language === 'en' ? 'translate-x-0' : 'translate-x-16'"
              ></div>
              <button 
                class="relative z-10 w-16 py-2 text-sm font-medium transition-colors duration-300"
                :class="language === 'en' ? 'text-gray-700' : 'text-gray-500'"
                @click="language = 'en'"
              >
                EN
              </button>
              <button 
                class="relative z-10 w-16 py-2 text-sm font-medium transition-colors duration-300"
                :class="language === 'no' ? 'text-gray-700' : 'text-gray-500'"
                @click="language = 'no'"
              >
                NO
              </button>
            </div>
          </div>
        </section>

        <section class="quick">
          <div class="grid grid-cols-2 gap-4 max-w-2xl">
            <button 
              v-for="q in visibleQuestions" 
              :key="q" 
              class="bg-white border border-gray-200 rounded-xl p-6 text-left hover:border-blue-300 hover:shadow-lg transition-all duration-300 group"
              @click="ask(q)"
            >
              <div class="text-gray-800 font-medium text-sm leading-relaxed group-hover:text-blue-600 transition-colors duration-300">
                {{ q }}
              </div>
            </button>
          </div>
        </section>
      </div>
    </div>

    <!-- Form at Bottom -->
    <div class="pb-8">
      <form class="home-composer flex gap-3 max-w-md mx-auto" @submit.prevent="submitQuick">
        <Input
          v-model="quickQuestion"
          type="text"
          class="flex-1"
          :placeholder="language === 'en' ? `Curious? Kevin's AI is here to answer!` : `Nysgjerrig? Kevin sin AI svarer gjerne!`"
        />
        <Button type="submit">Send →</Button>
      </form>
    </div>
  </main>
</template>


