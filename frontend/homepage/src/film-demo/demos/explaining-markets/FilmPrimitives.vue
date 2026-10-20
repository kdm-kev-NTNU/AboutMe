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
    class="relative overflow-hidden bg-slate-50"
    :style="{ width: `${FILM_META.frameWidth}px`, height: `${FILM_META.frameHeight}px` }"
  >
    <div aria-hidden="true" class="absolute inset-0 flex overflow-hidden">
      <!-- Streamlit-style sidebar -->
      <aside
        v-bind="daAttr('sidebar')"
        class="flex w-64 shrink-0 flex-col border-r border-slate-200 bg-white px-4 py-5"
      >
        <div class="mb-6 flex items-center gap-2 px-2">
          <span class="text-xl" aria-hidden="true">📈</span>
          <div>
            <p class="text-sm font-bold text-slate-900">{{ COPY.brand }}</p>
            <p class="text-[11px] leading-snug text-slate-500">{{ COPY.presenter }}</p>
          </div>
        </div>

        <nav class="flex flex-col gap-1" aria-label="Sidemeny">
          <span
            v-bind="daAttr('navCompetition')"
            class="flex items-center gap-2 rounded-lg bg-sky-50 px-3 py-2.5 text-sm font-semibold text-sky-800"
          >
            <span aria-hidden="true">🏠</span>
            {{ COPY.nav.competition }}
          </span>
          <span
            v-bind="daAttr('navAgent')"
            class="flex items-center gap-2 rounded-lg px-3 py-2.5 text-sm font-medium text-slate-700"
          >
            <span aria-hidden="true">🤖</span>
            {{ COPY.nav.agent }}
          </span>
          <span
            v-bind="daAttr('navResearch')"
            class="flex items-center gap-2 rounded-lg px-3 py-2.5 text-sm font-medium text-slate-700"
          >
            <span aria-hidden="true">🔬</span>
            {{ COPY.nav.research }}
          </span>
        </nav>

        <div class="mt-auto rounded-lg border border-emerald-200 bg-emerald-50 px-3 py-3">
          <p class="text-xs font-semibold text-emerald-800">Live-status</p>
          <p class="mt-1 text-xs text-emerald-700">{{ COPY.liveStatus }} ✅</p>
        </div>
      </aside>

      <!-- Panel content -->
      <div class="relative z-10 min-w-0 flex-1">
        <slot />
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
        class="pointer-events-none absolute left-0 top-0 z-40 h-10 w-10 -translate-x-1/2 -translate-y-1/2 rounded-full border-2 border-sky-500 opacity-0"
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
