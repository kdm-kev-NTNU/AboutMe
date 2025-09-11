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
        <h2 class="title">About Me</h2>
        <button class="close" @click="toggle" aria-label="Close">✕</button>
      </div>

      <p class="sub">{{ language === 'en' ? 'Welcome to my portfolio!' : 'Velkommen til mitt portfolio!' }}</p>

      <h3 class="section">{{ language === 'en' ? 'Social Links' : 'Sosiale lenker' }}</h3>
      <div class="social-links">
        <a href="https://github.com/kaamyashinde" target="_blank" rel="noopener" class="social-link github-link">
          <span class="social-icon">🐙</span>
          <span>{{ language === 'en' ? 'GitHub' : 'GitHub' }}</span>
        </a>
        <a href="https://linkedin.com/in/kaamyashinde" target="_blank" rel="noopener" class="social-link linkedin-link">
          <span class="social-icon">💼</span>
          <span>{{ language === 'en' ? 'LinkedIn' : 'LinkedIn' }}</span>
        </a>
      </div>
      
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
  font-size: 18px;
  transition: all 0.2s ease;
}

.fab:hover {
  background: #f9fafb;
  border-color: #9ca3af;
  transform: scale(1.05);
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
.sheet-header { display: flex; align-items: center; justify-content: space-between; }
.title { margin: 0; font-size: 20px; }
.close { border: none; background: transparent; cursor: pointer; font-size: 18px; }
.sub { color: #6b7280; margin: 6px 0 16px; }
.section { margin: 18px 0 10px; font-size: 16px; }
.social-links { display: flex; flex-direction: column; gap: 12px; margin-bottom: 20px; }
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
}
.social-link:hover { 
  background: #f3f4f6; 
  border-color: #d1d5db; 
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
}
.github-link:hover { border-color: #333; background: #f6f8fa; }
.linkedin-link:hover { border-color: #0077b5; background: #f0f8ff; }
.social-icon { font-size: 18px; }
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


