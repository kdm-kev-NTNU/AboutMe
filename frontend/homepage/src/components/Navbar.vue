<script setup lang="ts">
import { ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { Menu, X } from 'lucide-vue-next'
import { useLangStore } from '../stores/lang'

// Pill nav + sliding indicator; button width follows the longest label for the active language.
const route = useRoute()
const langStore = useLangStore()

const isMenuOpen = ref(false)

watch(
  () => route.fullPath,
  () => {
    isMenuOpen.value = false
  },
)

function toggleMenu() {
  isMenuOpen.value = !isMenuOpen.value
}

function closeMenu() {
  isMenuOpen.value = false
}

const isActive = (routeName: string) => route.name === routeName

const getButtonText = (key: string) => {
  const texts: Record<string, { en: string; no: string }> = {
    home: { en: 'Home', no: 'Hjem' },
    projects: { en: 'Projects', no: 'Prosjekter' },
    work: { en: 'Work', no: 'Arbeid' },
    education: { en: 'Education', no: 'Utdanning' },
    techStack: { en: 'Tech stack', no: 'Teknologistakk' },
    futureWork: { en: 'Future work', no: 'Videre arbeid' },
  }
  return texts[key][langStore.language]
}

const getButtonWidth = () => {
  // Calculate the width needed for the longest button text
  const buttonTexts = [
    getButtonText('home'),
    getButtonText('projects'),
    getButtonText('work'),
    getButtonText('education'),
    getButtonText('techStack'),
    getButtonText('futureWork'),
  ]

  // Estimate width based on character count (roughly 8px per character for this font size)
  const maxChars = Math.max(...buttonTexts.map((text) => text.length))
  return Math.max(64, maxChars * 8 + 16) // Minimum 64px, or character-based width + padding
}

const getIndicatorPosition = () => {
  const buttonWidth = getButtonWidth()

  if (isActive('home')) return { transform: 'translateX(0px)', opacity: '1' }
  if (isActive('projects')) return { transform: `translateX(${buttonWidth}px)`, opacity: '1' }
  if (isActive('work-experience')) return { transform: `translateX(${buttonWidth * 2}px)`, opacity: '1' }
  if (isActive('education')) return { transform: `translateX(${buttonWidth * 3}px)`, opacity: '1' }
  if (isActive('tech-stack')) return { transform: `translateX(${buttonWidth * 4}px)`, opacity: '1' }
  if (isActive('future-work')) return { transform: `translateX(${buttonWidth * 5}px)`, opacity: '1' }
  return { transform: 'translateX(0px)', opacity: '0' }
}

const getButtonClasses = (routeName: string) => {
  const baseClasses =
    'gradient-navbar-button relative z-10 py-2 text-sm font-medium transition-all duration-300 cursor-pointer flex items-center justify-center rounded-full'

  if (isActive(routeName)) {
    return `${baseClasses} text-blue-700 font-semibold bg-blue-50 border border-blue-200`
  }

  return `${baseClasses} text-gray-500`
}

const mobileNavLabel = () => (langStore.language === 'no' ? 'Hovedmeny' : 'Main menu')

const closeOverlayLabel = () => (langStore.language === 'no' ? 'Lukk meny' : 'Close menu')

const mobileLinkBase =
  'block rounded-xl px-4 py-3 text-sm font-medium transition-colors border border-transparent'
const mobileLinkActive = 'bg-blue-50 text-blue-700 border-blue-200'
const mobileLinkInactive = 'text-gray-700 hover:bg-slate-50 hover:text-gray-900'
</script>

<template>
  <nav class="center-nav fixed z-[100]" :aria-label="mobileNavLabel()">
    <!-- Mobile: hamburger + drawer (centered bar) -->
    <div class="relative flex justify-center md:hidden">
      <button
        type="button"
        class="flex h-11 w-11 items-center justify-center rounded-full border-2 border-slate-200 bg-gradient-to-br from-slate-100 to-slate-200 text-gray-700 shadow-md transition hover:border-blue-300 hover:text-blue-700 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-blue-600"
        :aria-expanded="isMenuOpen"
        aria-controls="mobile-nav-drawer"
        @click="toggleMenu"
      >
        <span class="sr-only">{{ mobileNavLabel() }}</span>
        <Menu v-if="!isMenuOpen" class="size-5" aria-hidden="true" />
        <X v-else class="size-5" aria-hidden="true" />
      </button>

      <Transition
        enter-active-class="transition-opacity duration-200"
        leave-active-class="transition-opacity duration-150"
        enter-from-class="opacity-0"
        leave-to-class="opacity-0"
      >
        <button
          v-if="isMenuOpen"
          type="button"
          class="fixed inset-0 z-[90] bg-slate-900/40 backdrop-blur-[2px] md:hidden"
          :aria-label="closeOverlayLabel()"
          @click="closeMenu"
        />
      </Transition>

      <Transition
        enter-active-class="transition duration-200 ease-out"
        leave-active-class="transition duration-150 ease-in"
        enter-from-class="-translate-y-2 opacity-0"
        leave-to-class="-translate-y-2 opacity-0"
      >
        <div
          v-if="isMenuOpen"
          id="mobile-nav-drawer"
          class="absolute left-1/2 top-[calc(100%+0.5rem)] z-[95] min-w-[14rem] -translate-x-1/2 rounded-2xl border border-slate-200/80 bg-white/95 p-2 shadow-xl shadow-slate-900/10 backdrop-blur-md md:hidden"
          role="menu"
        >
          <RouterLink
            to="/"
            role="menuitem"
            :class="[mobileLinkBase, isActive('home') ? mobileLinkActive : mobileLinkInactive]"
            @click="closeMenu"
          >
            {{ getButtonText('home') }}
          </RouterLink>
          <RouterLink
            to="/projects"
            role="menuitem"
            :class="[mobileLinkBase, isActive('projects') ? mobileLinkActive : mobileLinkInactive]"
            @click="closeMenu"
          >
            {{ getButtonText('projects') }}
          </RouterLink>
          <RouterLink
            to="/work-experience"
            role="menuitem"
            :class="[
              mobileLinkBase,
              isActive('work-experience') ? mobileLinkActive : mobileLinkInactive,
            ]"
            @click="closeMenu"
          >
            {{ getButtonText('work') }}
          </RouterLink>
          <RouterLink
            to="/education"
            role="menuitem"
            :class="[mobileLinkBase, isActive('education') ? mobileLinkActive : mobileLinkInactive]"
            @click="closeMenu"
          >
            {{ getButtonText('education') }}
          </RouterLink>
          <RouterLink
            to="/tech-stack"
            role="menuitem"
            :class="[mobileLinkBase, isActive('tech-stack') ? mobileLinkActive : mobileLinkInactive]"
            @click="closeMenu"
          >
            {{ getButtonText('techStack') }}
          </RouterLink>
          <RouterLink
            to="/future-work"
            role="menuitem"
            :class="[mobileLinkBase, isActive('future-work') ? mobileLinkActive : mobileLinkInactive]"
            @click="closeMenu"
          >
            {{ getButtonText('futureWork') }}
          </RouterLink>
        </div>
      </Transition>
    </div>

    <!-- Desktop: pill nav (Tailwind handles display; scoped .nav-container must not set display or it overrides `hidden` on small screens) -->
    <div class="hidden justify-center md:flex">
      <div class="gradient-navbar-container relative flex rounded-full p-1">
        <div
          class="gradient-navbar-slider absolute top-1 bottom-1 rounded-full shadow-lg transition-all duration-300 ease-in-out"
          :style="{
            width: getButtonWidth() + 'px',
            ...getIndicatorPosition(),
          }"
        ></div>
        <RouterLink to="/" :class="getButtonClasses('home')" :style="{ width: getButtonWidth() + 'px' }">
          {{ getButtonText('home') }}
        </RouterLink>
        <RouterLink
          to="/projects"
          :class="getButtonClasses('projects')"
          :style="{ width: getButtonWidth() + 'px' }"
        >
          {{ getButtonText('projects') }}
        </RouterLink>
        <RouterLink
          to="/work-experience"
          :class="getButtonClasses('work-experience')"
          :style="{ width: getButtonWidth() + 'px' }"
        >
          {{ getButtonText('work') }}
        </RouterLink>
        <RouterLink
          to="/education"
          :class="getButtonClasses('education')"
          :style="{ width: getButtonWidth() + 'px' }"
        >
          {{ getButtonText('education') }}
        </RouterLink>
        <RouterLink
          to="/tech-stack"
          :class="getButtonClasses('tech-stack')"
          :style="{ width: getButtonWidth() + 'px' }"
        >
          {{ getButtonText('techStack') }}
        </RouterLink>
        <RouterLink
          to="/future-work"
          :class="getButtonClasses('future-work')"
          :style="{ width: getButtonWidth() + 'px' }"
        >
          {{ getButtonText('futureWork') }}
        </RouterLink>
      </div>
    </div>
  </nav>
</template>

<style scoped>
.center-nav {
  top: 1rem;
  left: 50%;
  right: auto;
  transform: translateX(-50%);
}

@media (min-width: 768px) {
  .center-nav {
    top: 2rem;
    right: auto;
    left: 50%;
    transform: translateX(-50%);
  }
}

/* Enhanced navbar with gradient styling */
.gradient-navbar-container {
  background: linear-gradient(135deg, #e2e8f0 0%, #cbd5e1 100%);
  box-shadow:
    0 4px 6px -1px rgba(0, 0, 0, 0.1),
    0 2px 4px -1px rgba(0, 0, 0, 0.06);
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
  box-shadow:
    0 4px 6px -1px rgba(0, 0, 0, 0.1),
    0 2px 4px -1px rgba(0, 0, 0, 0.06);
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
</style>
