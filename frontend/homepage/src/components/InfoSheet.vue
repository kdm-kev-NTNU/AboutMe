<script setup lang="ts">
import { ref, computed } from 'vue'
import { useLangStore } from '../stores/lang'

const open = ref(false)
const langStore = useLangStore()
const language = computed(() => langStore.language)

function toggle() {
  open.value = !open.value
}
</script>

<template>
  <button class="fab" @click="toggle" :aria-label="language === 'en' ? 'Open info' : 'Åpne info'">ℹ️</button>

  <div v-if="open" class="overlay" @click.self="toggle">
    <div class="sheet">
      <div class="sheet-header">
        <h2 class="title">Kevin's AI</h2>
        <button class="close" @click="toggle" aria-label="Close">✕</button>
      </div>

      <p class="sub">{{ language === 'en' ? 'Welcome to my AI portfolio!' : 'Velkommen til mitt AI-portfolio!' }}</p>

      <h3 class="section">{{ language === 'en' ? 'Links' : 'Lenker' }}</h3>
      <ul class="links">
        <li><a href="https://github.com/kdm-kev-NTNU/AboutMe" target="_blank" rel="noopener">{{ language === 'en' ? 'Project Repo' : 'Prosjekt-repo' }}</a></li>
        <li><a href="https://github.com/kdm-kev-NTNU" target="_blank" rel="noopener">{{ language === 'en' ? 'GitHub Profile' : 'GitHub-profil' }}</a></li>
      </ul>

      <h3 class="section">{{ language === 'en' ? 'Privacy Concerns' : 'Personvern' }}</h3>
      <p class="text">{{ language === 'en'
        ? 'Please be aware that all conversations may be saved and visible to anyone. (Don\'t ask crazy questions, we see you)'
        : 'Vær oppmerksom på at alle samtaler kan lagres og være synlige for andre. (Ikke still ville spørsmål, vi ser deg)'
      }}</p>

      <h3 class="section">{{ language === 'en' ? "How Kevin's AI Knows About Me" : 'Hvordan Kevin sin AI vet om meg' }}</h3>
      <p class="text">{{ language === 'en'
        ? "I provide an AI model context about my background and education and how it should answer your questions. This can still hallucinate, so don't trust everything blindly."
        : 'Jeg gir en AI-modell kontekst om min bakgrunn og utdanning og hvordan den skal svare på spørsmål. Den kan fortsatt hallusinere, så ikke stol blindt på alt.'
      }}</p>

      <!-- <button class="warn" disabled>⚠️ {{ language === 'en' ? 'Do not click on this' : 'Ikke klikk på denne' }}</button> -->
    </div>
  </div>
</template>

<style scoped>
.fab {
  position: fixed;
  left: 16px;
  bottom: 16px;
  width: 44px;
  height: 44px;
  border-radius: 50%;
  border: 1px solid #d1d5db;
  background: #fff;
  cursor: pointer;
  box-shadow: 0 8px 20px rgba(0,0,0,.08);
}
.overlay {
  position: fixed;
  inset: 0;
  background: rgba(0,0,0,.18);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px;
}
.sheet {
  width: min(540px, 100%);
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 24px 60px rgba(0,0,0,.15);
  padding: 18px 18px 22px;
}
.sheet-header { display: flex; align-items: center; justify-content: space-between; }
.title { margin: 0; font-size: 20px; }
.close { border: none; background: transparent; cursor: pointer; font-size: 18px; }
.sub { color: #6b7280; margin: 6px 0 16px; }
.section { margin: 18px 0 10px; font-size: 16px; }
.links { list-style: none; padding: 0; display: grid; grid-template-columns: 1fr 1fr; gap: 10px 16px; }
.links a { color: #111827; text-decoration: none; }
.links a:hover { text-decoration: underline; }
.text { color: #374151; }
.mode { display: flex; gap: 10px; }
.mode-btn { padding: 8px 12px; border-radius: 8px; border: 1px solid #e5e7eb; background: #fff; cursor: pointer; }
.mode-btn.active { border-color: #111827; }
.warn { margin-top: 14px; width: 100%; padding: 10px; border-radius: 10px; border: 1px solid #e5e7eb; background: #f9fafb; color: #111827; }
</style>


