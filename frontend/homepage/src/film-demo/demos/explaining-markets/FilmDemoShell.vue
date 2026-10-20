<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { FILM_META } from './film-script'
import FilmPrimitives from './FilmPrimitives.vue'
import FilmPanels from './FilmPanels.vue'
import { useFilmDemo } from '../../shared/useFilmDemo'
import { applyReducedMotionFallback, buildFilmTimeline } from './film-demo'

const props = withDefaults(
  defineProps<{
    embedded?: boolean
  }>(),
  { embedded: false },
)

const viewportRef = ref<HTMLElement | null>(null)
const primitivesRef = ref<InstanceType<typeof FilmPrimitives> | null>(null)
const scale = ref(1)

const demoRootRef = computed(() => primitivesRef.value?.demoRootRef ?? null)

const { canControl, isPlaying, isReducedMotion, play, pause, restart } = useFilmDemo(
  demoRootRef,
  buildFilmTimeline,
  applyReducedMotionFallback,
)

function togglePlayback(): void {
  if (isPlaying.value) pause()
  else play()
}

defineExpose({
  togglePlayback,
  restart,
  canControl,
  isPlaying,
  isReducedMotion,
})

function updateScale() {
  const el = viewportRef.value
  if (!el) return

  const padding = props.embedded ? 0 : 32
  const availableWidth = el.clientWidth - padding
  const availableHeight = el.clientHeight - padding
  const scaleX = availableWidth / FILM_META.frameWidth
  const scaleY = availableHeight / FILM_META.frameHeight
  scale.value = Math.min(scaleX, scaleY, props.embedded ? Infinity : 1)
}

let resizeObserver: ResizeObserver | null = null

onMounted(() => {
  updateScale()
  resizeObserver = new ResizeObserver(updateScale)
  if (viewportRef.value) {
    resizeObserver.observe(viewportRef.value)
  }
})

onUnmounted(() => {
  resizeObserver?.disconnect()
})
</script>

<template>
  <div
    ref="viewportRef"
    :class="
      embedded
        ? 'relative h-full w-full overflow-hidden'
        : 'flex min-h-screen items-center justify-center bg-slate-100 p-4'
    "
  >
    <div
      :class="embedded ? 'absolute left-0 top-0 origin-top-left' : 'origin-center'"
      :style="{
        width: `${FILM_META.frameWidth}px`,
        height: `${FILM_META.frameHeight}px`,
        transform: `scale(${scale})`,
      }"
    >
      <FilmPrimitives
        ref="primitivesRef"
        :can-control="canControl"
        :is-playing="isPlaying"
        :is-reduced-motion="isReducedMotion"
        :show-controls="!embedded"
        :toggle-playback="togglePlayback"
        :restart="restart"
      >
        <FilmPanels />
      </FilmPrimitives>
    </div>
  </div>
</template>
