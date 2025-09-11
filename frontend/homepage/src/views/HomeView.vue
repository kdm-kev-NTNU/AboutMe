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
  <main class="home flex flex-col h-screen pt-20 relative">
    <!-- Gradient Background Overlay -->
    <div class="home-gradient-overlay"></div>
    
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
    <div class="flex-1 flex flex-col items-center justify-center py-8 overflow-y-auto">
      <div class="flex flex-col items-center space-y-8">
        <section class="brand">
          <h1 class="text-7xl font-bold text-center mb-4">
            Kevin's <span class="gradient-text">AI</span>.
          </h1>
          <div class="flex justify-center">
            <div class="gradient-toggle-container relative rounded-full p-1 flex">
              <div
                class="gradient-toggle-slider absolute top-1 bottom-1 w-16 rounded-full shadow-lg transition-transform duration-300 ease-in-out"
                :class="language === 'en' ? 'translate-x-0' : 'translate-x-16'"
              ></div>
              <button
                class="gradient-toggle-button relative z-10 w-16 py-2 text-sm font-medium transition-all duration-300 cursor-pointer rounded-full"
                :class="language === 'en' ? 'text-blue-700 font-semibold' : 'text-gray-500'"
                @click="language = 'en'"
              >
                EN
              </button>
              <button
                class="gradient-toggle-button relative z-10 w-16 py-2 text-sm font-medium transition-all duration-300 cursor-pointer rounded-full"
                :class="language === 'no' ? 'text-blue-700 font-semibold' : 'text-gray-500'"
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
              class="gradient-card bg-white border border-gray-200 rounded-xl p-6 text-left hover:border-blue-300 hover:shadow-xl transition-all duration-300 group"
              @click="ask(q)"
            >
              <div class="text-gray-800 font-medium text-sm leading-relaxed group-hover:text-gray-900 transition-colors duration-300 cursor-pointer">
                {{ q }}
              </div>
            </button>
          </div>
        </section>
      </div>
    </div>

    <!-- Form at Bottom -->
    <div class="pb-8 flex-shrink-0">
      <form class="home-composer flex gap-3 max-w-md mx-auto gradient-form" @submit.prevent="submitQuick">
        <Input
          v-model="quickQuestion"
          type="text"
          class="flex-1"
          :placeholder="language === 'en' ? `Curious? Kevin's AI is here to answer!` : `Nysgjerrig? Kevin sin AI svarer gjerne!`"
        />
        <Button type="submit" class="cursor-pointer">Send →</Button>
      </form>
    </div>
  </main>
</template>

<style scoped>
.home {
  min-height: 100vh;
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
  position: relative;
}

.home-gradient-overlay {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: radial-gradient(circle at 20% 80%, rgba(59, 130, 246, 0.1) 0%, transparent 50%),
              radial-gradient(circle at 80% 20%, rgba(37, 99, 235, 0.1) 0%, transparent 50%);
  pointer-events: none;
}

.gradient-text {
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 50%, #1d4ed8 100%);
  background-clip: text;
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-size: 200% 200%;
  animation: gradientShift 3s ease-in-out infinite;
}

@keyframes gradientShift {
  0%, 100% { background-position: 0% 50%; }
  50% { background-position: 100% 50%; }
}

.gradient-card {
  position: relative;
  overflow: hidden;
}

.gradient-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(59, 130, 246, 0.1), transparent);
  transition: left 0.5s ease;
}

.gradient-card:hover::before {
  left: 100%;
}

.gradient-card:hover {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.9) 0%, rgba(248, 250, 252, 0.9) 100%);
  backdrop-filter: blur(10px);
}

.gradient-form {
  position: relative;
}

.gradient-form::before {
  content: '';
  position: absolute;
  top: -2px;
  left: -2px;
  right: -2px;
  bottom: -2px;
  background: linear-gradient(135deg, #3b82f6, #2563eb, #1d4ed8);
  border-radius: 12px;
  opacity: 0;
  transition: opacity 0.3s ease;
  z-index: -1;
}

.gradient-form:hover::before {
  opacity: 0.1;
}

/* Enhanced language toggle with gradient */
.gradient-toggle-container {
  background: linear-gradient(135deg, #e2e8f0 0%, #cbd5e1 100%);
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06);
  border: 2px solid transparent;
  background-clip: padding-box;
  position: relative;
}

.gradient-toggle-container::before {
  content: '';
  position: absolute;
  top: -2px;
  left: -2px;
  right: -2px;
  bottom: -2px;
  background: linear-gradient(135deg, #3b82f6, #2563eb, #1d4ed8);
  border-radius: 50px;
  opacity: 0;
  transition: opacity 0.3s ease;
  z-index: -1;
}

.gradient-toggle-container:hover::before {
  opacity: 0.6;
}

.gradient-toggle-slider {
  background: linear-gradient(135deg, #ffffff 0%, #f8fafc 100%);
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06);
  border: 1px solid rgba(59, 130, 246, 0.2);
}

.gradient-toggle-button {
  position: relative;
  overflow: hidden;
}

.gradient-toggle-button::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(59, 130, 246, 0.1), transparent);
  transition: left 0.3s ease;
}

.gradient-toggle-button:hover::before {
  left: 100%;
}

.gradient-toggle-button:hover {
  background: rgba(59, 130, 246, 0.05);
}

/* Responsive adjustments */
@media (max-width: 768px) {
  .gradient-text {
    font-size: 3rem;
  }
  
  .gradient-card {
    padding: 1rem;
  }
}
</style>
