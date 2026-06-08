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
    class="relative overflow-hidden bg-gradient-to-br from-slate-50 via-blue-50 to-slate-100"
    :style="{ width: `${FILM_META.frameWidth}px`, height: `${FILM_META.frameHeight}px` }"
  >
    <div aria-hidden="true" class="absolute inset-0 overflow-hidden">
    <!-- Gradient overlay -->
    <div class="pointer-events-none absolute inset-0" aria-hidden="true">
      <div
        class="absolute inset-0"
        style="
          background:
            radial-gradient(circle at 20% 80%, rgba(59, 130, 246, 0.1) 0%, transparent 50%),
            radial-gradient(circle at 80% 20%, rgba(37, 99, 235, 0.1) 0%, transparent 50%);
        "
      />
    </div>

    <!-- Static blob shapes -->
    <div class="pointer-events-none absolute inset-0 overflow-hidden" aria-hidden="true">
      <div
        class="absolute -left-16 top-24 h-64 w-64 rounded-full bg-blue-400/10 blur-3xl"
      />
      <div
        class="absolute -right-20 bottom-32 h-72 w-72 rounded-full bg-indigo-400/10 blur-3xl"
      />
      <div
        class="absolute left-1/3 top-1/2 h-48 w-48 rounded-full bg-cyan-400/8 blur-2xl"
      />
    </div>

    <!-- Admin pill -->
    <div class="absolute right-6 top-5 z-20">
      <span
        v-bind="daAttr('admin')"
        class="inline-flex items-center rounded-full border border-slate-200 bg-white/90 px-4 py-1.5 text-sm font-medium text-slate-600 shadow-sm"
      >
        {{ COPY.admin }}
      </span>
    </div>

    <!-- Nav -->
    <nav
      v-bind="daAttr('nav')"
      class="absolute left-1/2 top-5 z-20 -translate-x-1/2"
      aria-label="Main navigation"
    >
      <div
        class="relative flex rounded-full border-2 border-transparent bg-gradient-to-r from-slate-200 to-slate-300 p-1 shadow-md"
      >
        <div
          class="absolute bottom-1 top-1 w-44 rounded-full border border-blue-200 bg-gradient-to-r from-white to-slate-50 shadow-lg"
          style="transform: translateX(0)"
          aria-hidden="true"
        />
        <span
          v-bind="daAttr('navHome')"
          class="relative z-10 flex w-44 items-center justify-center rounded-full border border-blue-200 bg-blue-50 py-2 text-sm font-semibold text-blue-700"
        >
          {{ COPY.nav.home }}
        </span>
        <span
          v-bind="daAttr('navReason')"
          class="relative z-10 flex w-44 items-center justify-center rounded-full py-2 text-sm font-medium text-gray-500"
        >
          {{ COPY.nav.reason }}
        </span>
        <span
          v-bind="daAttr('navHow')"
          class="relative z-10 flex w-44 items-center justify-center rounded-full py-2 text-sm font-medium text-gray-500"
        >
          {{ COPY.nav.how }}
        </span>
      </div>
    </nav>

    <!-- Panel content slot -->
    <div class="relative z-10 h-full">
      <slot />
    </div>

    <!-- Lang toggle -->
    <div class="absolute bottom-28 left-0 right-0 z-10 flex justify-center">
      <div
        v-bind="daAttr('langToggle')"
        class="relative flex rounded-full bg-gradient-to-r from-slate-200 to-slate-300 p-1 shadow-md"
      >
        <div
          class="absolute bottom-1 top-1 w-16 rounded-full border border-blue-200 bg-gradient-to-r from-white to-slate-50 shadow-lg"
          style="transform: translateX(0)"
          aria-hidden="true"
        />
        <span
          class="relative z-10 flex w-16 items-center justify-center rounded-full py-2 text-sm font-semibold text-blue-700"
        >
          {{ COPY.lang.en }}
        </span>
        <span
          class="relative z-10 flex w-16 items-center justify-center rounded-full py-2 text-sm font-medium text-gray-500"
        >
          {{ COPY.lang.no }}
        </span>
      </div>
    </div>

    <!-- Social links -->
    <div
      v-bind="daAttr('social')"
      class="absolute bottom-16 left-0 right-0 z-10 flex justify-center gap-3"
    >
      <span
        class="inline-flex h-10 w-10 items-center justify-center rounded-full border border-gray-200 bg-white text-gray-700 shadow-sm"
        aria-hidden="true"
      >
        <svg class="size-5" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
          <path
            d="M12 0c-6.626 0-12 5.373-12 12 0 5.302 3.438 9.8 8.207 11.387.599.111.793-.261.793-.577v-2.234c-3.338.726-4.033-1.416-4.033-1.416-.546-1.387-1.333-1.756-1.333-1.756-1.089-.745.083-.729.083-.729 1.205.084 1.839 1.237 1.839 1.237 1.07 1.834 2.807 1.304 3.492.997.107-.775.418-1.305.762-1.604-2.665-.305-5.467-1.334-5.467-5.931 0-1.311.469-2.381 1.236-3.221-.124-.303-.535-1.524.117-3.176 0 0 1.008-.322 3.301 1.23.957-.266 1.983-.399 3.003-.404 1.02.005 2.047.138 3.006.404 2.291-1.552 3.297-1.23 3.297-1.23.653 1.653.242 2.874.118 3.176.77.84 1.235 1.911 1.235 3.221 0 4.609-2.807 5.624-5.479 5.921.43.372.823 1.102.823 2.222v3.293c0 .319.192.694.801.576 4.765-1.589 8.199-6.086 8.199-11.386 0-6.627-5.373-12-12-12z"
          />
        </svg>
      </span>
      <span
        class="inline-flex h-10 w-10 items-center justify-center rounded-full border border-gray-200 bg-white text-blue-700 shadow-sm"
        aria-hidden="true"
      >
        <svg class="size-5" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
          <path
            d="M20.447 20.452h-3.554v-5.569c0-1.328-.027-3.037-1.852-3.037-1.853 0-2.136 1.445-2.136 2.939v5.667H9.351V9h3.414v1.561h.046c.477-.9 1.637-1.85 3.37-1.85 3.601 0 4.267 2.37 4.267 5.455v6.286zM5.337 7.433c-1.144 0-2.063-.926-2.063-2.065 0-1.138.92-2.063 2.063-2.063 1.14 0 2.064.925 2.064 2.063 0 1.139-.925 2.065-2.064 2.065zm1.782 13.019H3.555V9h3.564v11.452zM22.225 0H1.771C.792 0 0 .774 0 1.729v20.542C0 23.227.792 24 1.771 24h20.451C23.2 24 24 23.227 24 22.271V1.729C24 .774 23.2 0 22.222 0h.003z"
          />
        </svg>
      </span>
    </div>

    <!-- Footer -->
    <footer
      v-bind="daAttr('footer')"
      class="absolute bottom-4 left-0 right-0 z-10 flex justify-center gap-4 text-xs text-slate-500"
    >
      <span>{{ COPY.footer.privacy }}</span>
      <span aria-hidden="true">·</span>
      <span>{{ COPY.footer.cookies }}</span>
      <span aria-hidden="true">·</span>
      <span>{{ COPY.footer.accessibility }}</span>
    </footer>

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
    <div
      v-if="showControls"
      class="absolute bottom-4 right-4 z-[60]"
    >
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
