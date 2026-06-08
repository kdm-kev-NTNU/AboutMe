<script setup lang="ts">
import {
  computed,
  defineAsyncComponent,
  onMounted,
  onUnmounted,
  ref,
} from 'vue'
import { DEMO_REGISTRY } from '../registry'
import FilmDemoControls from './FilmDemoControls.vue'

type AsyncDemoComponent = ReturnType<typeof defineAsyncComponent>

type FilmDemoShellExposed = {
  togglePlayback: () => void
  restart: () => void
  canControl: boolean
  isPlaying: boolean
  isReducedMotion: boolean
}

const props = defineProps<{
  demoId: string
}>()

const FRAME_WIDTH = 1280

const containerRef = ref<HTMLElement | null>(null)
const demoShellRef = ref<FilmDemoShellExposed | null>(null)
const isVisible = ref(false)
const DemoComponent = ref<AsyncDemoComponent | null>(null)
const scale = ref(1)

const canControl = computed(() => demoShellRef.value?.canControl ?? false)
const isPlaying = computed(() => demoShellRef.value?.isPlaying ?? false)
const isReducedMotion = computed(() => demoShellRef.value?.isReducedMotion ?? false)

function togglePlayback(): void {
  demoShellRef.value?.togglePlayback()
}

function restartDemo(): void {
  demoShellRef.value?.restart()
}

let observer: IntersectionObserver | null = null
let resizeObserver: ResizeObserver | null = null

function loadDemo(loader: () => Promise<{ default: unknown }>): void {
  isVisible.value = true
  if (!DemoComponent.value) {
    DemoComponent.value = defineAsyncComponent(loader)
  }
}

function updateScale(width: number): void {
  scale.value = width / FRAME_WIDTH
}

onMounted(() => {
  const loader = DEMO_REGISTRY[props.demoId]
  if (!loader || !containerRef.value) return

  if (typeof ResizeObserver !== 'undefined') {
    resizeObserver = new ResizeObserver(([entry]) => {
      updateScale(entry.contentRect.width)
    })
    resizeObserver.observe(containerRef.value)
    updateScale(containerRef.value.getBoundingClientRect().width)
  }

  if (typeof IntersectionObserver === 'undefined') {
    loadDemo(loader)
    return
  }

  observer = new IntersectionObserver(
    (entries) => {
      if (entries.some((entry) => entry.isIntersecting)) {
        loadDemo(loader)
        observer?.disconnect()
        observer = null
      }
    },
    { rootMargin: '200px' },
  )

  observer.observe(containerRef.value)
})

onUnmounted(() => {
  observer?.disconnect()
  resizeObserver?.disconnect()
})
</script>

<template>
  <div
    ref="containerRef"
    class="group relative w-full overflow-hidden bg-slate-100 aspect-[1280/800]"
  >
    <div
      aria-hidden="true"
      :style="{
        transform: `scale(${scale})`,
        transformOrigin: 'top left',
        width: '1280px',
        height: '800px',
      }"
    >
      <component
        :is="DemoComponent"
        v-if="isVisible && DemoComponent"
        ref="demoShellRef"
        embedded
      />
    </div>

    <div
      v-if="isVisible && DemoComponent"
      class="absolute bottom-3 right-3 z-10 opacity-40 transition-opacity duration-300 group-hover:opacity-100 group-focus-within:opacity-100"
    >
      <FilmDemoControls
        :can-control="canControl"
        :is-playing="isPlaying"
        :is-reduced-motion="isReducedMotion"
        @toggle="togglePlayback"
        @restart="restartDemo"
      />
    </div>
  </div>
</template>
