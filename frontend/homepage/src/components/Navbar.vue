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
  const texts: Record<string, { en: string; no: string }> = {
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
      <div class="gradient-navbar-container relative rounded-full p-1 flex">
        <div
          class="gradient-navbar-slider absolute top-1 bottom-1 rounded-full shadow-lg transition-all duration-300 ease-in-out"
          :style="{ 
            width: getButtonWidth() + 'px',
            ...getIndicatorPosition()
          }"
        ></div>
        <RouterLink
          to="/"
          class="gradient-navbar-button relative z-10 py-2 text-sm font-medium transition-all duration-300 cursor-pointer flex items-center justify-center rounded-full"
          :class="isActive('home') ? 'text-blue-700 font-semibold' : 'text-gray-500'"
          :style="{ width: getButtonWidth() + 'px' }"
        >
          {{ getButtonText('home') }}
        </RouterLink>
        <RouterLink
          to="/projects"
          class="gradient-navbar-button relative z-10 py-2 text-sm font-medium transition-all duration-300 cursor-pointer flex items-center justify-center rounded-full"
          :class="isActive('projects') ? 'text-blue-700 font-semibold' : 'text-gray-500'"
          :style="{ width: getButtonWidth() + 'px' }"
        >
          {{ getButtonText('projects') }}
        </RouterLink>
        <RouterLink
          to="/work-experience"
          class="gradient-navbar-button relative z-10 py-2 text-sm font-medium transition-all duration-300 cursor-pointer flex items-center justify-center rounded-full"
          :class="isActive('work-experience') ? 'text-blue-700 font-semibold' : 'text-gray-500'"
          :style="{ width: getButtonWidth() + 'px' }"
        >
          {{ getButtonText('work') }}
        </RouterLink>
        <RouterLink
          to="/education"
          class="gradient-navbar-button relative z-10 py-2 text-sm font-medium transition-all duration-300 cursor-pointer flex items-center justify-center rounded-full"
          :class="isActive('education') ? 'text-blue-700 font-semibold' : 'text-gray-500'"
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

/* Enhanced navbar with gradient styling */
.gradient-navbar-container {
  background: linear-gradient(135deg, #e2e8f0 0%, #cbd5e1 100%);
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06);
  border: 2px solid transparent;
  background-clip: padding-box;
  position: relative;
}

.gradient-navbar-container::before {
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

.gradient-navbar-container:hover::before {
  opacity: 0.6;
}

.gradient-navbar-slider {
  background: linear-gradient(135deg, #ffffff 0%, #f8fafc 100%);
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06);
  border: 1px solid rgba(59, 130, 246, 0.2);
}

.gradient-navbar-button {
  position: relative;
  overflow: hidden;
}

.gradient-navbar-button::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(59, 130, 246, 0.1), transparent);
  transition: left 0.3s ease;
}

.gradient-navbar-button:hover::before {
  left: 100%;
}

.gradient-navbar-button:hover {
  background: rgba(59, 130, 246, 0.05);
}

/* Mobile adjustments */
@media (max-width: 768px) {
  .center-nav {
    top: 1rem;
  }
}
</style>
