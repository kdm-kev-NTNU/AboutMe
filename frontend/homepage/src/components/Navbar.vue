<script setup lang="ts">
import { useRouter, useRoute } from 'vue-router'
import { computed } from 'vue'
import { useLangStore } from '../stores/lang'

const router = useRouter()
const route = useRoute()
const langStore = useLangStore()

const navigateTo = (routeName: string) => {
  router.push({ name: routeName })
}

const isActive = (routeName: string) => {
  return route.name === routeName
}

const getButtonText = (key: string) => {
  const texts = {
    home: { en: 'Home', no: 'Hjem' },
    about: { en: 'About', no: 'Om' },
    work: { en: 'Work', no: 'Arbeid' },
    education: { en: 'Education', no: 'Utdanning' }
  }
  return texts[key][langStore.language]
}

const getIndicatorPosition = () => {
  if (isActive('home')) return 'translate-x-0'
  if (isActive('about')) return 'translate-x-16'
  if (isActive('work-experience')) return 'translate-x-32'
  if (isActive('education')) return 'translate-x-48'
  return 'translate-x-0 opacity-0'
}
</script>

<template>
  <nav class="center-nav">
    <div class="nav-container">
      <div class="relative bg-gray-200 rounded-full p-1 flex">
        <div
          class="absolute top-1 bottom-1 w-16 bg-white rounded-full shadow-md transition-transform duration-300 ease-in-out"
          :class="getIndicatorPosition()"
        ></div>
        <button
          class="relative z-10 w-16 py-2 text-sm font-medium transition-colors duration-300 cursor-pointer"
          :class="isActive('home') ? 'text-gray-700' : 'text-gray-500'"
          @click="navigateTo('home')"
        >
          {{ getButtonText('home') }}
        </button>
        <button
          class="relative z-10 w-16 py-2 text-sm font-medium transition-colors duration-300 cursor-pointer"
          :class="isActive('about') ? 'text-gray-700' : 'text-gray-500'"
          @click="navigateTo('about')"
        >
          {{ getButtonText('about') }}
        </button>
        <button
          class="relative z-10 w-16 py-2 text-sm font-medium transition-colors duration-300 cursor-pointer"
          :class="isActive('work-experience') ? 'text-gray-700' : 'text-gray-500'"
          @click="navigateTo('work-experience')"
        >
          {{ getButtonText('work') }}
        </button>
        <button
          class="relative z-10 w-16 py-2 text-sm font-medium transition-colors duration-300 cursor-pointer"
          :class="isActive('education') ? 'text-gray-700' : 'text-gray-500'"
          @click="navigateTo('education')"
        >
          {{ getButtonText('education') }}
        </button>
      </div>
    </div>
  </nav>
</template>

<style scoped>
.center-nav {
  position: fixed;
  top: 2rem;
  left: 50%;
  transform: translateX(-50%);
  z-index: 100;
}

.nav-container {
  display: flex;
  justify-content: center;
}

/* Mobile adjustments */
@media (max-width: 768px) {
  .center-nav {
    top: 1rem;
  }
}
</style>
