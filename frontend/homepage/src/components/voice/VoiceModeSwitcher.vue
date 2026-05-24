<script setup lang="ts">
import { ShieldCheck, TriangleAlert } from 'lucide-vue-next'
import type { VoiceMode } from '@/stores/voice-mode'

defineProps<{
  modelValue: VoiceMode
  language: 'en' | 'no'
}>()

const emit = defineEmits<{
  'update:modelValue': [value: VoiceMode]
}>()
</script>

<template>
  <div class="mb-6 grid grid-cols-1 gap-2 rounded-2xl border border-blue-100 bg-white/85 p-2 shadow-sm sm:grid-cols-2">
    <button
      type="button"
      class="flex items-center justify-between rounded-xl px-3 py-3 text-left transition"
      :class="
        modelValue === 'standard'
          ? 'bg-emerald-50 text-emerald-900 ring-1 ring-emerald-200'
          : 'bg-white text-slate-700 hover:bg-slate-50'
      "
      @click="emit('update:modelValue', 'standard')"
    >
      <span class="inline-flex items-center gap-2 text-sm font-semibold">
        <ShieldCheck class="size-4" aria-hidden="true" />
        {{ language === 'no' ? 'Standard' : 'Standard' }}
      </span>
      <span class="text-xs">
        {{ language === 'no' ? 'Anbefalt' : 'Recommended' }}
      </span>
    </button>
    <button
      type="button"
      class="flex items-center justify-between rounded-xl px-3 py-3 text-left transition"
      :class="
        modelValue === 'live'
          ? 'bg-amber-50 text-amber-900 ring-1 ring-amber-200'
          : 'bg-white text-slate-700 hover:bg-slate-50'
      "
      @click="emit('update:modelValue', 'live')"
    >
      <span class="inline-flex items-center gap-2 text-sm font-semibold">
        <TriangleAlert class="size-4" aria-hidden="true" />
        {{ language === 'no' ? 'Live' : 'Live' }}
      </span>
      <span class="text-xs">
        {{ language === 'no' ? 'Mindre stabil' : 'Less stable' }}
      </span>
    </button>
  </div>
</template>
