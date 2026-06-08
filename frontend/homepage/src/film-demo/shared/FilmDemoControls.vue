<script setup lang="ts">
defineProps<{
  canControl: boolean
  isPlaying: boolean
  isReducedMotion: boolean
}>()

const emit = defineEmits<{
  toggle: []
  restart: []
}>()
</script>

<template>
  <div
    class="flex items-center gap-1 rounded-lg bg-black/60 p-1 shadow-lg backdrop-blur-sm"
    role="group"
    aria-label="Demo playback controls"
  >
    <button
      type="button"
      class="inline-flex size-9 items-center justify-center rounded-md text-white transition hover:bg-white/15 disabled:cursor-not-allowed disabled:opacity-40"
      :disabled="!canControl"
      :aria-label="isPlaying ? 'Pause demo' : 'Play demo'"
      @click="emit('toggle')"
    >
      <svg
        v-if="isPlaying"
        class="size-4"
        viewBox="0 0 24 24"
        fill="currentColor"
        aria-hidden="true"
      >
        <path d="M6 19h4V5H6v14zm8-14v14h4V5h-4z" />
      </svg>
      <svg
        v-else
        class="size-4"
        viewBox="0 0 24 24"
        fill="currentColor"
        aria-hidden="true"
      >
        <path d="M8 5v14l11-7z" />
      </svg>
    </button>

    <button
      type="button"
      class="inline-flex size-9 items-center justify-center rounded-md text-white transition hover:bg-white/15 disabled:cursor-not-allowed disabled:opacity-40"
      :disabled="!canControl"
      aria-label="Restart demo"
      @click="emit('restart')"
    >
      <svg
        class="size-4"
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        stroke-width="2"
        aria-hidden="true"
      >
        <path
          stroke-linecap="round"
          stroke-linejoin="round"
          d="M4 4v5h5M20 20v-5h-5M4 9a9 9 0 0115.36-6.36M20 15a9 9 0 01-15.36 6.36"
        />
      </svg>
    </button>

    <span
      v-if="isReducedMotion"
      class="px-2 text-xs text-white/80"
    >
      Static preview
    </span>
  </div>
</template>
