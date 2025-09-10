<script setup lang="ts">
import { useRouter } from 'vue-router'
import { ref, computed } from 'vue'
import { useLangStore } from '../stores/lang'

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

function ask(q: string) {
  router.push({ name: 'chat', query: { q } })
}

function submitQuick() {
  const q = quickQuestion.value.trim()
  if (!q) return
  ask(q)
}
</script>

<template>
  <main class="home">
    <section class="brand">
      <div class="sparkles">✦✦</div>
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

<style scoped>
.home { max-width: 1100px; margin: 0 auto; padding: 40px 24px 110px; position: relative; min-height: 100vh; box-sizing: border-box; }

/* Brand */
.brand { text-align: center; margin: 40px 0 24px; }
.sparkles { color: #5b6bff; opacity: .9; font-size: 22px; margin-bottom: 8px; }
.logo { font-size: 64px; line-height: 1; margin: 0; letter-spacing: .5px; color: #2d2e35; text-shadow: 0 2px 0 rgba(0,0,0,.05); }
.logo span { color: #3b5bff; text-shadow: 0 2px 0 rgba(0,0,0,.06); }

/* Language toggle */
.lang { display: flex; justify-content: center; margin: 10px 0 0; }
.lang-toggle { display: inline-flex; gap: 8px; background: #fff; border: 1px solid #e5e7eb; border-radius: 999px; padding: 4px; }
.lang-btn { border: none; background: transparent; padding: 8px 12px; border-radius: 999px; font-weight: 700; color: #1f2937; cursor: pointer; }
.lang-btn.active { background: #304ffe; color: #fff; }

/* Quick buttons */
.grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(360px, 1fr)); gap: 22px; max-width: 1000px; margin: 30px auto; }
.btn-pill { background: #fff; border: 2px solid #e5e7eb; border-radius: 16px; padding: 18px 20px; font-weight: 700; color: #1f2937; cursor: pointer; text-align: center; box-shadow: 0 6px 0 rgba(17, 24, 39, 0.25); transition: transform .12s ease, box-shadow .12s ease, background .12s ease; }
.btn-pill:hover { transform: translateY(-1px); box-shadow: 0 7px 0 rgba(17, 24, 39, 0.28); background: #fafafa; }
.btn-pill:active { transform: translateY(2px); box-shadow: 0 3px 0 rgba(17, 24, 39, 0.22); }

.copyright { position: fixed; left: 12px; bottom: 18px; color: #6b7280; font-size: 12px; }

/* Bottom composer */
.home-composer { position: fixed; left: 50%; bottom: 18px; transform: translateX(-50%); display: flex; gap: 8px; align-items: center; width: min(820px, calc(100% - 28px)); background: #fff; border: 1px solid #d1d5db; border-radius: 999px; padding: 8px; box-shadow: 0 8px 24px rgba(31,41,55,.08); }
.home-input { flex: 1; border: none; outline: none; padding: 12px 14px 12px 16px; font-size: 15px; border-radius: 999px; }
.home-send { border: none; background: #304ffe; color: #fff; border-radius: 999px; padding: 10px 16px; font-weight: 700; cursor: pointer; }
.home-send:hover { background: #2746ff; }

@media (max-width: 480px) {
  .logo { font-size: 42px; }
  .grid { grid-template-columns: 1fr; gap: 14px; }
}
</style>
