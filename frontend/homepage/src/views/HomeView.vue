<script setup lang="ts">
import { useRouter } from 'vue-router'
import { ref, computed, onMounted } from 'vue'
import { useLangStore } from '../stores/lang'
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { Button } from '@/components/ui/button'
import { ArrowRight, MoveUpRight } from 'lucide-vue-next'
import TutorialDialog from '@/components/TutorialDialog.vue'

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
const showHomeDialog = ref(false)
const showEductionDialog = ref(false)

function ask(q: string) {
  router.push({ name: 'chat', query: { q } })
}

function submitQuick() {
  const q = quickQuestion.value.trim()
  if (!q) return
  ask(q)
}

function startGuidedTour() {
  showWelcomeDialog.value = false
  showHomeDialog.value = true
}

function showEducationInfo() {
  showHomeDialog.value = false
  showEductionDialog.value = true
}

function showLinksInfo() {
  showEductionDialog.value = false
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
  <main class="flex flex-col h-screen pt-20 relative min-h-screen bg-gradient-to-br from-slate-50 to-slate-100">
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

    <!-- Welcome Dialog -->
    <TutorialDialog
      v-model:open="showWelcomeDialog"
      title="Welcome to Kevin's Portfolio!"
      desc="Please be mindful that this website is still under development."
      descOpt="AI Disclaimer: Please be aware that AI can hallucinate and may provide inaccurate information."
      descBlue="Please do not share private or sensitive information in the chat."
      :index="1"
      @start-guided-tour="startGuidedTour"
    />

    <!-- Guided Tour Dialog -->
    <TutorialDialog
      v-model:open="showHomeDialog"
      title="Welcome to the Tour!"
      desc="This is the navigation bar where you can explore different sections of my portfolio."
      descBlue="You can ask questions about me using the AI chat feature in Home!"
      :index="2"
      @start-guided-tour="showEducationInfo"
      class="[fixed top-56 left-1/5 transform -translate-x-1/2 z-50]"
    />

    <TutorialDialog
      v-model:open="showEductionDialog"
      title="Get an overview about me!"
      desc="This is where you can explore both my academic and professional background."
      descBlue="Do check out the other projects that I have worked on."
      :index="3"
      @start-guided-tour="showEducationInfo"
      class="[fixed top-56 left-4/5 transform -translate-x-1/2 z-50]"
    />

    <!-- Main Content - Centered -->
    <div class="flex-1 flex flex-col items-center justify-center py-8 overflow-y-auto relative z-10">
      <div class="flex flex-col items-center space-y-8">
        <section class="brand">
          <h1 class="text-7xl font-bold text-center mb-4">
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
        </section>

        <section class="quick">
          <div class="grid grid-cols-2 gap-4 max-w-2xl">
            <button
              v-for="q in visibleQuestions"
              :key="q"
              class="relative overflow-hidden bg-white border border-gray-200 rounded-xl p-6 text-left hover:border-blue-300 hover:shadow-xl transition-all duration-300 group hover:bg-gradient-to-br hover:from-white/90 hover:to-slate-50/90 hover:backdrop-blur-sm"
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
      <form class="flex gap-3 max-w-md mx-auto relative bg-white/90 backdrop-blur-sm border-2 border-blue-200/20 rounded-xl p-2 transition-all duration-300 hover:border-blue-300/40 hover:bg-white/95 hover:shadow-lg hover:shadow-blue-500/15 focus-within:border-blue-300/60 focus-within:bg-white/98 focus-within:shadow-lg focus-within:shadow-blue-500/25" @submit.prevent="submitQuick">
        <Input
          v-model="quickQuestion"
          type="text"
          class="flex-1 bg-white/80 border-2 border-blue-200/20 rounded-lg transition-all duration-300 focus:bg-white/95 focus:border-blue-300/50 focus:shadow-sm focus:shadow-blue-500/10 focus:outline-none placeholder:text-blue-600/60 placeholder:font-medium"
          :placeholder="language === 'en' ? `Curious? Kevin's AI is here to answer!` : `Nysgjerrig? Kevin sin AI svarer gjerne!`"
        />
        <Button type="submit" class="cursor-pointer bg-gradient-to-r from-blue-600 to-blue-700 hover:from-blue-700 hover:to-blue-800 text-white font-semibold hover:-translate-y-0.5 hover:shadow-lg hover:shadow-blue-500/40 transition-all duration-300 relative overflow-hidden">Send →</Button>
      </form>
    </div>
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

/* Responsive adjustments */
@media (max-width: 768px) {
  .text-7xl {
    font-size: 3rem;
  }

  .p-6 {
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
