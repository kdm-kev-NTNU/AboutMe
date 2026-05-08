<script setup lang="ts">
import { nextTick, onMounted, onUnmounted, ref, toRef, watch } from 'vue'
import { useAudioWaveform } from '@/composables/useAudioWaveform'

const props = withDefaults(
  defineProps<{
    /** Live microphone stream while recording */
    stream: MediaStream | null
    /** Accessible name for the waveform region */
    ariaLabel?: string
  }>(),
  { ariaLabel: 'Recording level' },
)

const streamRef = toRef(props, 'stream')
const { timeDomainData, tick } = useAudioWaveform(streamRef)

const containerRef = ref<HTMLElement | null>(null)
const canvasRef = ref<HTMLCanvasElement | null>(null)
let resizeObserver: ResizeObserver | null = null

function resizeCanvasToContainer() {
  const canvas = canvasRef.value
  const container = containerRef.value
  if (!canvas || !container) return

  const dpr = typeof window !== 'undefined' ? window.devicePixelRatio || 1 : 1
  const { clientWidth, clientHeight } = container
  const w = Math.max(1, Math.floor(clientWidth * dpr))
  const h = Math.max(1, Math.floor(clientHeight * dpr))
  if (canvas.width !== w || canvas.height !== h) {
    canvas.width = w
    canvas.height = h
  }
}

function canvasStrokeScale(canvas: HTMLCanvasElement): number {
  const rect = canvas.getBoundingClientRect()
  if (!rect.height) return 1
  return canvas.height / rect.height
}

function draw() {
  const canvas = canvasRef.value
  const buf = timeDomainData.value
  if (!canvas || !buf) return

  resizeCanvasToContainer()

  const ctx = canvas.getContext('2d')
  if (!ctx) return

  const w = canvas.width
  const h = canvas.height
  ctx.clearRect(0, 0, w, h)

  const mid = h / 2
  const len = buf.length
  const gain = Math.min(h * 0.42, w * 0.08)

  ctx.lineJoin = 'round'
  ctx.lineCap = 'round'
  ctx.lineWidth = Math.max(1, canvasStrokeScale(canvas) * 1.25)
  ctx.strokeStyle = 'rgb(15 23 42)' /* slate-900 */

  ctx.beginPath()
  for (let i = 0; i < len; i++) {
    const v = buf[i] ?? 0
    const x = (i / Math.max(1, len - 1)) * w
    const y = mid - v * gain
    if (i === 0) ctx.moveTo(x, y)
    else ctx.lineTo(x, y)
  }
  ctx.stroke()

  ctx.globalAlpha = 0.2
  ctx.beginPath()
  ctx.moveTo(0, mid)
  ctx.lineTo(w, mid)
  ctx.strokeStyle = 'rgb(100 116 139)' /* slate-500 */
  ctx.lineWidth = Math.max(1, canvasStrokeScale(canvas))
  ctx.stroke()
  ctx.globalAlpha = 1
}

watch(tick, draw)

watch(
  () => props.stream,
  () => {
    requestAnimationFrame(draw)
  },
)

onMounted(() => {
  resizeObserver = new ResizeObserver(() => {
    requestAnimationFrame(draw)
  })
  void nextTick(() => {
    if (containerRef.value) {
      resizeObserver?.observe(containerRef.value)
    }
    requestAnimationFrame(draw)
  })
})

onUnmounted(() => {
  resizeObserver?.disconnect()
  resizeObserver = null
})
</script>

<template>
  <div
    ref="containerRef"
    class="relative flex min-h-11 min-w-0 flex-1 overflow-hidden rounded-2xl border border-blue-100/70 bg-slate-950/3"
    role="img"
    :aria-label="ariaLabel"
  >
    <canvas ref="canvasRef" class="absolute inset-0 h-full w-full" />
  </div>
</template>
