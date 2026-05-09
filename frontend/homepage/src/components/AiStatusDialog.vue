<script setup lang="ts">
import { computed } from 'vue'
import { AlertTriangle } from 'lucide-vue-next'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { Button } from '@/components/ui/button'

const props = withDefaults(
  defineProps<{
    open: boolean
    title: string
    message: string
    description?: string
    dismissLabel?: string
    retryLabel?: string
    showRetry?: boolean
  }>(),
  {
    description: '',
    dismissLabel: 'OK',
    retryLabel: 'Try again',
    showRetry: false,
  },
)

const emit = defineEmits<{
  'update:open': [value: boolean]
  retry: []
}>()

const isOpen = computed({
  get: () => props.open,
  set: (value: boolean) => emit('update:open', value),
})

function retry() {
  emit('retry')
}
</script>

<template>
  <Dialog v-model:open="isOpen">
    <DialogContent class="max-w-[calc(100%-2rem)] border-red-100 bg-white p-0 shadow-2xl sm:max-w-md">
      <div class="rounded-lg bg-gradient-to-br from-white via-red-50/70 to-blue-50/70 p-6">
        <DialogHeader class="space-y-3 text-left">
          <div
            class="flex h-11 w-11 items-center justify-center rounded-2xl border border-red-100 bg-white text-red-600 shadow-sm"
            aria-hidden="true"
          >
            <AlertTriangle class="size-5" />
          </div>
          <DialogTitle class="text-xl font-bold tracking-tight text-slate-900">
            {{ title }}
          </DialogTitle>
          <DialogDescription class="text-sm leading-6 text-slate-600">
            {{ description }}
          </DialogDescription>
        </DialogHeader>

        <p class="mt-4 rounded-2xl border border-red-100 bg-white/85 px-4 py-3 text-sm font-medium text-red-700">
          {{ message }}
        </p>

        <DialogFooter class="mt-6 flex-col-reverse gap-2 sm:flex-row sm:justify-end">
          <Button type="button" variant="outline" @click="isOpen = false">
            {{ dismissLabel }}
          </Button>
          <Button
            v-if="showRetry"
            type="button"
            class="bg-blue-600 text-white hover:bg-blue-700"
            @click="retry"
          >
            {{ retryLabel }}
          </Button>
        </DialogFooter>
      </div>
    </DialogContent>
  </Dialog>
</template>
