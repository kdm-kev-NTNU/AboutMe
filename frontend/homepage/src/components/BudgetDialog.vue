<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useLangStore } from '@/stores/lang'
import { Button } from '@/components/ui/button'
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
  DialogFooter,
} from '@/components/ui/dialog'

const props = defineProps<{
  open: boolean
}>()

const emit = defineEmits<{
  'update:open': [value: boolean]
  retry: []
}>()

const router = useRouter()
const langStore = useLangStore()

const copy = computed(() => {
  if (langStore.language === 'no') {
    return {
      title: 'AI-budsjettgrense nådd',
      body: 'AI-budsjettet er midlertidig brukt opp. Prøv igjen senere, eller bruk tekstchat mens stemme ikke er tilgjengelig.',
      retry: 'Prøv igjen',
      ok: 'OK',
    }
  }
  return {
    title: 'AI budget limit reached',
    body: 'The live AI budget is temporarily exhausted. Please try again later or use text chat while voice is unavailable.',
    retry: 'Try again',
    ok: 'OK',
  }
})

function handleRetry() {
  emit('update:open', false)
  emit('retry')
}

function handleOk() {
  emit('update:open', false)
}
</script>

<template>
  <Dialog :open="props.open" @update:open="emit('update:open', $event)">
    <DialogContent class="max-w-md rounded-[22px] border-red-500/20 shadow-[0px_22px_38px_rgba(5,8,20,0.18)]">
      <DialogHeader>
        <DialogTitle class="text-xl font-bold text-[#0f1729]">
          {{ copy.title }}
        </DialogTitle>
        <DialogDescription class="text-sm leading-relaxed text-[#47546b]">
          {{ copy.body }}
        </DialogDescription>
      </DialogHeader>
      <DialogFooter class="flex-row gap-3 sm:justify-start">
        <Button
          class="rounded-2xl bg-[#2663eb] px-6 font-semibold text-white shadow-[0px_14px_24px_rgba(38,99,235,0.25)] hover:bg-blue-700"
          @click="handleRetry"
        >
          {{ copy.retry }}
        </Button>
        <Button
          variant="outline"
          class="rounded-2xl border-[#c7d6ed] px-6 font-semibold text-[#1f293b] hover:bg-slate-50"
          @click="handleOk"
        >
          {{ copy.ok }}
        </Button>
      </DialogFooter>
    </DialogContent>
  </Dialog>
</template>
