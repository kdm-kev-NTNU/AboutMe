<script setup lang="ts">
import { ref } from 'vue'

const open = ref(false)

function toggle() {
  open.value = !open.value
}
</script>

<template>
  <button class="fab" @click="toggle" aria-label="Open info">
    ℹ️
  </button>

  <div v-if="open" class="overlay" @click.self="toggle">
    <div class="sheet">
      <div class="sheet-header">
        <h2 class="title">Kevin's AI</h2>
        <button class="close" @click="toggle" aria-label="Close">✕</button>
      </div>

      <p class="sub">Welcome to my AI portfolio!</p>

      <h3 class="section">Links</h3>
      <ul class="links">
        <li><a href="https://github.com/kdm-kev-NTNU/AboutMe" target="_blank" rel="noopener">Project Repo</a></li>
        <li><a href="https://github.com/kdm-kev-NTNU" target="_blank" rel="noopener">GitHub Profile</a></li>
      </ul>

      <h3 class="section">Privacy Concerns</h3>
      <p class="text">Please be aware that all conversations may be saved and visible to anyone. (Don't ask crazy questions, we see you)</p>

      <h3 class="section">How Kevin's AI Knows About Me</h3>
      <p class="text">I provide an AI model context about my background  and education and how it should answer your questions. This can still hallucinate, so don't trust everything blindly.</p>

      <!-- Mode controls removed per request -->

      <button class="warn" disabled>⚠️ Do not click on this</button>
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
.warn { margin-top: 14px; width: 100%; padding: 10px; border-radius: 10px; border: 1px solid #e5e7eb; background: #f9fafb; color: #111827; }
</style>


