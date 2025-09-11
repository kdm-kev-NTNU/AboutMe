<script setup lang="ts">
import { useRouter } from 'vue-router'
import { ref, computed, onMounted } from 'vue'
import { useLangStore } from '../stores/lang'
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from '@/components/ui/dialog'

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
  <main class="home">
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

    <section class="brand">
      <h1 class="logo">Kevin's <span>AI</span>.</h1>
    </section>

    <section class="lang">
      <div class="lang-toggle">
        <button class="lang-btn" :class="{ active: language === 'en' }" @click="language = 'en'">EN</button>
        <button class="lang-btn" :class="{ active: language === 'no' }" @click="language = 'no'">NO</button>
      </div>
    </section>

    <section class="quick">
      <div class="grid">
        <button v-for="q in visibleQuestions" :key="q" class="btn-pill" @click="ask(q)">
          {{ q }}
        </button>
      </div>
    </section>

    <form class="home-composer" @submit.prevent="submitQuick">
      <input
        v-model="quickQuestion"
        type="text"
        class="home-input"
        :placeholder="language === 'en' ? `Curious? Kevin's AI is here to answer!` : `Nysgjerrig? Kevin sin AI svarer gjerne!`"
      />
      <button type="submit" class="home-send">Send →</button>
    </form>
  </main>
</template>


