<script setup lang="ts">
import { useRoute } from 'vue-router'
import { computed } from 'vue'
import { useLangStore } from '../stores/lang'

const route = useRoute()
const langStore = useLangStore()

const isActive = (routeName: string) => {
  return route.name === routeName
}

const getButtonText = (key: string) => {
  const texts = {
    home: { en: 'Home', no: 'Hjem' },
    projects: { en: 'Projects', no: 'Prosjekter' },
    work: { en: 'Work', no: 'Arbeid' },
    education: { en: 'Education', no: 'Utdanning' }
  }
  return texts[key][langStore.language]
}

const getButtonWidth = () => {
  // Calculate the width needed for the longest button text
  const buttonTexts = [
    getButtonText('home'),
    getButtonText('projects'),
    getButtonText('work'),
    getButtonText('education')
  ]
  
  // Estimate width based on character count (roughly 8px per character for this font size)
  const maxChars = Math.max(...buttonTexts.map(text => text.length))
  return Math.max(64, maxChars * 8 + 16) // Minimum 64px, or character-based width + padding
}

const getIndicatorPosition = () => {
  const buttonWidth = getButtonWidth()
  
  if (isActive('home')) return { transform: 'translateX(0px)', opacity: '1' }
  if (isActive('projects')) return { transform: `translateX(${buttonWidth}px)`, opacity: '1' }
  if (isActive('work-experience')) return { transform: `translateX(${buttonWidth * 2}px)`, opacity: '1' }
  if (isActive('education')) return { transform: `translateX(${buttonWidth * 3}px)`, opacity: '1' }
  return { transform: 'translateX(0px)', opacity: '0' }
}
</script>

<template>
  <nav class="center-nav">
    <div class="nav-container">
      <div class="relative bg-gray-200 rounded-full p-1 flex">
        <div
          class="absolute top-1 bottom-1 bg-white rounded-full shadow-md transition-all duration-300 ease-in-out"
          :style="{ 
            width: getButtonWidth() + 'px',
            ...getIndicatorPosition()
          }"
        ></div>
        <RouterLink
          to="/"
          class="relative z-10 py-2 text-sm font-medium transition-colors duration-300 cursor-pointer flex items-center justify-center"
          :class="isActive('home') ? 'text-gray-700' : 'text-gray-500'"
          :style="{ width: getButtonWidth() + 'px' }"
        >
          {{ getButtonText('home') }}
        </RouterLink>
        <RouterLink
          to="/projects"
          class="relative z-10 py-2 text-sm font-medium transition-colors duration-300 cursor-pointer flex items-center justify-center"
          :class="isActive('projects') ? 'text-gray-700' : 'text-gray-500'"
          :style="{ width: getButtonWidth() + 'px' }"
        >
          {{ getButtonText('projects') }}
        </RouterLink>
        <RouterLink
          to="/work-experience"
          class="relative z-10 py-2 text-sm font-medium transition-colors duration-300 cursor-pointer flex items-center justify-center"
          :class="isActive('work-experience') ? 'text-gray-700' : 'text-gray-500'"
          :style="{ width: getButtonWidth() + 'px' }"
        >
          {{ getButtonText('work') }}
        </RouterLink>
        <RouterLink
          to="/education"
          class="relative z-10 py-2 text-sm font-medium transition-colors duration-300 cursor-pointer flex items-center justify-center"
          :class="isActive('education') ? 'text-gray-700' : 'text-gray-500'"
          :style="{ width: getButtonWidth() + 'px' }"
        >
          {{ getButtonText('education') }}
        </RouterLink>
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
