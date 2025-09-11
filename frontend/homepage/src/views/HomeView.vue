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
    
    <!-- Blue Blob Shapes -->
    <div class="blob-container">
      <div class="blob blob-1"></div>
      <div class="blob blob-2"></div>
      <div class="blob blob-3"></div>
      <div class="blob blob-4"></div>
      <div class="blob blob-5"></div>
      <div class="blob blob-6"></div>
    </div>
    
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
    <div class="flex-1 flex flex-col items-center justify-center py-8 overflow-y-auto relative z-10">
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
    <div class="pb-8 flex-shrink-0 relative z-10">
      <form class="home-composer flex gap-3 max-w-md mx-auto gradient-form" @submit.prevent="submitQuick">
        <Input
          v-model="quickQuestion"
          type="text"
          class="flex-1 blue-accent-input"
          :placeholder="language === 'en' ? `Curious? Kevin's AI is here to answer!` : `Nysgjerrig? Kevin sin AI svarer gjerne!`"
        />
        <Button type="submit" class="cursor-pointer blue-accent-button">Send →</Button>
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
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(10px);
  border: 2px solid rgba(59, 130, 246, 0.2);
  border-radius: 12px;
  padding: 8px;
  transition: all 0.3s ease;
}

.gradient-form::before {
  content: '';
  position: absolute;
  top: -2px;
  left: -2px;
  right: -2px;
  bottom: -2px;
  background: linear-gradient(135deg, #3b82f6, #2563eb, #1d4ed8);
  border-radius: 14px;
  opacity: 0;
  transition: opacity 0.3s ease;
  z-index: -1;
}

.gradient-form:hover {
  border-color: rgba(59, 130, 246, 0.4);
  background: rgba(255, 255, 255, 0.95);
  box-shadow: 0 8px 25px rgba(59, 130, 246, 0.15);
}

.gradient-form:hover::before {
  opacity: 0.2;
}

.gradient-form:focus-within {
  border-color: rgba(59, 130, 246, 0.6);
  background: rgba(255, 255, 255, 0.98);
  box-shadow: 0 8px 25px rgba(59, 130, 246, 0.25);
}

.gradient-form:focus-within::before {
  opacity: 0.3;
}

/* Blue accent button styling */
.blue-accent-button {
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%) !important;
  border: none !important;
  color: white !important;
  font-weight: 600 !important;
  transition: all 0.3s ease !important;
  position: relative !important;
  overflow: hidden !important;
}

.blue-accent-button::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.2), transparent);
  transition: left 0.5s ease;
}

.blue-accent-button:hover {
  background: linear-gradient(135deg, #2563eb 0%, #1d4ed8 100%) !important;
  transform: translateY(-1px) !important;
  box-shadow: 0 8px 25px rgba(59, 130, 246, 0.4) !important;
}

.blue-accent-button:hover::before {
  left: 100%;
}

.blue-accent-button:active {
  transform: translateY(0) !important;
  box-shadow: 0 4px 15px rgba(59, 130, 246, 0.3) !important;
}

/* Blue accent input styling */
.blue-accent-input {
  background: rgba(255, 255, 255, 0.8) !important;
  border: 2px solid rgba(59, 130, 246, 0.2) !important;
  border-radius: 8px !important;
  transition: all 0.3s ease !important;
}

.blue-accent-input:focus {
  background: rgba(255, 255, 255, 0.95) !important;
  border-color: rgba(59, 130, 246, 0.5) !important;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1) !important;
  outline: none !important;
}

.blue-accent-input::placeholder {
  color: rgba(59, 130, 246, 0.6) !important;
  font-weight: 500 !important;
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

/* Responsive adjustments */
@media (max-width: 768px) {
  .gradient-text {
    font-size: 3rem;
  }
  
  .gradient-card {
    padding: 1rem;
  }
  
  .blob {
    filter: blur(30px);
  }
  
  .blob-1, .blob-3 {
    width: 200px;
    height: 200px;
  }
  
  .blob-2, .blob-4, .blob-5 {
    width: 150px;
    height: 150px;
  }
  
  .blob-6 {
    width: 120px;
    height: 120px;
  }
}
</style>
