<script setup lang="ts">
import { ref } from 'vue'
import FilmDemoControls from '../../shared/FilmDemoControls.vue'
import { COPY, CURSOR_RULES, DATA_ATTRIBUTES, FILM_META } from './film-script'

withDefaults(
  defineProps<{
    canControl: boolean
    isPlaying: boolean
    isReducedMotion: boolean
    showControls?: boolean
    togglePlayback: () => void
    restart: () => void
  }>(),
  { showControls: true },
)

function daAttr(name: keyof typeof DATA_ATTRIBUTES): Record<string, string> {
  return { [DATA_ATTRIBUTES[name]]: '' }
}

const cursorStart = CURSOR_RULES.startPosition
const demoRootRef = ref<HTMLElement | null>(null)

defineExpose({ demoRootRef })
</script>

<template>
  <div
    ref="demoRootRef"
    v-bind="daAttr('root')"
    class="relative overflow-hidden bg-white"
    :style="{ width: `${FILM_META.frameWidth}px`, height: `${FILM_META.frameHeight}px` }"
  >
    <div aria-hidden="true" class="absolute inset-0 overflow-hidden">
      <!-- Navbar -->
      <header
        v-bind="daAttr('nav')"
        class="absolute left-0 right-0 top-0 z-30 flex h-16 items-center justify-between border-b border-slate-100 bg-white px-8"
      >
        <div class="flex shrink-0 items-center gap-2">
          <div
            class="flex size-9 items-center justify-center rounded-lg bg-blue-600 text-white shadow-sm"
            aria-hidden="true"
          >
            <svg class="size-5" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
              <path
                d="M12 2L4 5v6.09c0 5.05 3.41 9.76 8 10.91 4.59-1.15 8-5.86 8-10.91V5l-8-3zm-1 14.5l-3.5-3.5 1.41-1.41L11 13.67l4.59-4.58L17 10.5 11 16.5z"
              />
            </svg>
          </div>
          <span class="text-lg font-bold text-blue-700">{{ COPY.brand }}</span>
        </div>

        <div class="flex items-center gap-6">
          <nav class="flex items-center gap-6" aria-label="Hovednavigasjon">
            <span class="flex items-center gap-1.5 text-sm font-medium text-slate-700">
              <svg class="size-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                <path d="M3 6l6-3 6 3 6-3v15l-6 3-6-3-6 3V6z" />
                <path d="M9 3v15M15 6v15" />
              </svg>
              {{ COPY.nav.kart }}
            </span>
            <span
              v-bind="daAttr('navKriser')"
              class="flex items-center gap-1.5 text-sm font-medium text-slate-700"
            >
              <svg class="size-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                <path d="M8 6h13M8 12h13M8 18h13M3 6h.01M3 12h.01M3 18h.01" />
              </svg>
              {{ COPY.nav.kriser }}
            </span>
            <span
              v-bind="daAttr('navHusstand')"
              class="flex items-center gap-1.5 text-sm font-medium text-slate-700"
            >
              <svg class="size-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                <path d="M3 10.5 12 3l9 7.5V21H3z" />
                <path d="M9 21v-6h6v6" />
              </svg>
              {{ COPY.nav.husstand }}
            </span>
            <span class="text-slate-400" aria-hidden="true">
              <svg class="size-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M12 2L2 7l10 5 10-5-10-5zM2 17l10 5 10-5M2 12l10 5 10-5" />
              </svg>
            </span>
          </nav>

          <button
            v-bind="daAttr('loginBtn')"
            type="button"
            class="rounded-lg bg-blue-600 px-5 py-2 text-sm font-semibold text-white shadow-sm"
          >
            {{ COPY.nav.login }}
          </button>
        </div>
      </header>

      <!-- Panel content -->
      <div class="relative z-10 h-full pt-16">
        <slot />
      </div>

      <!-- Floating audio button -->
      <div
        class="absolute bottom-5 right-5 z-40 flex size-11 items-center justify-center rounded-full bg-slate-800 text-white shadow-lg"
        aria-hidden="true"
      >
        <svg class="size-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M11 5 6 9H2v6h4l5 4V5z" />
          <path d="M15.54 8.46a5 5 0 0 1 0 7.07" />
        </svg>
      </div>

      <!-- Fake cursor -->
      <div
        v-bind="daAttr('cursor')"
        class="pointer-events-none absolute left-0 top-0 z-50"
        :style="{
          transform: `translate(${cursorStart.x}px, ${cursorStart.y}px)`,
          filter: 'drop-shadow(0 2px 8px rgba(0,0,0,0.55))',
        }"
        aria-hidden="true"
      >
        <svg width="40" height="40" viewBox="0 0 28 28" fill="none" aria-hidden="true">
          <path
            d="M4 2L4 22L9.5 16.5L14 26L17 24.5L12.5 15L20 15L4 2Z"
            fill="#e0e7ff"
            stroke="#1e293b"
            stroke-width="1.5"
            stroke-linejoin="round"
          />
        </svg>
      </div>

      <!-- Click ripple -->
      <div
        v-bind="daAttr('cursorRipple')"
        class="pointer-events-none absolute left-0 top-0 z-40 h-10 w-10 -translate-x-1/2 -translate-y-1/2 rounded-full border-2 border-blue-500 opacity-0"
        :style="{
          transform: `translate(${cursorStart.x}px, ${cursorStart.y}px) scale(0.2)`,
        }"
        aria-hidden="true"
      />
    </div>

    <!-- Playback controls -->
    <div v-if="showControls" class="absolute bottom-4 right-4 z-[60]">
      <FilmDemoControls
        :can-control="canControl"
        :is-playing="isPlaying"
        :is-reduced-motion="isReducedMotion"
        @toggle="togglePlayback"
        @restart="restart"
      />
    </div>
  </div>
</template>
