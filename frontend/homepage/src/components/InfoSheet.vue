<script setup lang="ts">
import { ref, computed } from 'vue'
import { useLangStore } from '../stores/lang'
import { Info, Github, Linkedin } from 'lucide-vue-next'

const open = ref(false)
const langStore = useLangStore()
const language = computed(() => langStore.language)

function toggle() {
  open.value = !open.value
}
</script>

<template>
  <button class="fab" @click="toggle" :aria-label="language === 'en' ? 'Open info' : 'Åpne info'">
    <Info class="w-5 h-5" />
  </button>

  <div v-if="open" class="overlay" @click.self="toggle">
    <div class="sheet">
      <div class="sheet-header">
        <h2 class="title">About Me</h2>
        <button class="close" @click="toggle" aria-label="Close">✕</button>
      </div>

      <p class="sub">{{ language === 'en' ? 'Welcome to my portfolio!' : 'Velkommen til mitt portfolio!' }}</p>

      <h3 class="section">{{ language === 'en' ? 'Social Links' : 'Sosiale lenker' }}</h3>
      <div class="social-links">
        <a href="https://github.com/kaamyashinde" target="_blank" rel="noopener" class="social-link github-link">
          <Github class="social-icon" />
          <span>{{ language === 'en' ? 'GitHub' : 'GitHub' }}</span>
        </a>
        <a href="https://linkedin.com/in/kaamyashinde" target="_blank" rel="noopener" class="social-link linkedin-link">
          <Linkedin class="social-icon" />
          <span>{{ language === 'en' ? 'LinkedIn' : 'LinkedIn' }}</span>
        </a>
      </div>
      
      <h3 class="section">{{ language === 'en' ? "How Kevin's AI Knows About Me" : 'Hvordan Kevin sin AI vet om meg' }}</h3>
      <p class="text">{{ language === 'en'
        ? "I provide an AI model context about my background and education and how it should answer your questions. This can still hallucinate, so don't trust everything blindly."
        : 'Jeg gir en AI-modell kontekst om min bakgrunn og utdanning og hvordan den skal svare på spørsmål. Den kan fortsatt hallusinere, så ikke stol blindt på alt.'
      }}</p>
      
      <div class="button-section">
        <button class="dont-click-button" disabled>
          {{ language === 'en' ? "Don't click here" : 'Ikke klikk her' }}
        </button>
      </div>
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
  z-index: 50;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
  color: #374151;
}

.fab:hover {
  background: #f9fafb;
  border-color: #9ca3af;
  transform: scale(1.05);
  color: #1f2937;
}

.fab:active {
  transform: scale(0.95);
}
.overlay {
  position: fixed;
  inset: 0;
  background: rgba(0,0,0,.18);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px;
  z-index: 100;
}
.sheet {
  width: min(540px, 100%);
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 24px 60px rgba(0,0,0,.15);
  padding: 18px 18px 22px;
}
.sheet-header { display: flex; align-items: center; justify-content: center; position: relative; }
.title { margin: 0; font-size: 20px; }
.close { 
  position: absolute; 
  right: 0; 
  border: none; 
  background: transparent; 
  cursor: pointer; 
  font-size: 18px; 
}
.sub { 
  color: #6b7280; 
  margin: 6px 0 16px; 
  text-align: center;
}
.section { margin: 18px 0 10px; font-size: 16px; }
.social-links { display: flex; flex-direction: row; gap: 12px; margin-bottom: 20px; }
.social-link { 
  display: flex; 
  align-items: center; 
  gap: 12px; 
  padding: 12px 16px; 
  border-radius: 8px; 
  text-decoration: none; 
  color: #111827; 
  border: 1px solid #e5e7eb; 
  background: #f9fafb; 
  transition: all 0.2s ease;
  flex: 1;
}
.social-link:hover { 
  background: #f3f4f6; 
  border-color: #d1d5db; 
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
}
.github-link:hover { 
  border-color: #333; 
  background: #f6f8fa; 
}
.github-link:hover .social-icon { 
  color: #333; 
}
.linkedin-link:hover { 
  border-color: #0077b5; 
  background: #f0f8ff; 
}
.linkedin-link:hover .social-icon { 
  color: #0077b5; 
}
.social-icon { 
  width: 18px; 
  height: 18px; 
  color: #3b82f6;
  stroke-width: 1.5;
}
.text { 
  color: #374151; 
  line-height: 1.7;
  margin: 0 0 20px 0;
  padding: 16px;
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
  border-radius: 8px;
  border-left: 4px solid #3b82f6;
  font-size: 14px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
}
.button-section { margin-top: 20px; }
.dont-click-button { 
  width: 100%; 
  padding: 12px 16px; 
  border-radius: 8px; 
  border: 1px solid #e5e7eb; 
  background: #f9fafb; 
  color: #6b7280; 
  font-size: 14px; 
  cursor: not-allowed; 
  opacity: 0.7;
}
</style>


