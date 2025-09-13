<script setup lang="ts">
import { computed } from 'vue'
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Button } from '@/components/ui/button'
import { ArrowRight } from 'lucide-vue-next'

const props = defineProps<{
  title: string,
  desc: string,
  descOpt?: string,
  descBlue?: string,
  index: number,
  open: boolean,
  class?: string,
  isEnd?: boolean
}>()

const emit = defineEmits<{
  'update:open': [value: boolean]
  'start-guided-tour': []
}>()

const showWelcomeDialog = computed({
  get: () => props.open,
  set: (value: boolean) => emit('update:open', value)
})

function startGuidedTour() {
  emit('start-guided-tour')
}
</script>

<template>
  <Dialog v-model:open="showWelcomeDialog">
    <DialogContent :class="['max-w-md', props.class]">
      <DialogHeader>
        <DialogTitle>{{title}}</DialogTitle>
        <DialogDescription class="space-y-3">
          <p>{{desc}}</p>
          <p v-if="descOpt">
            {{descOpt}}
          </p>
          <p v-if="descBlue" class="flex items-center gap-2 text-blue-600">
            {{descBlue}}
          </p>
        </DialogDescription>
      </DialogHeader>

      <!-- Dialog Footer with Next Button -->
      <div class="flex justify-between mt-6">
        <p class="text-blue-600 border-b-2">{{index}} of 4</p>
        <!-- Next Button -->
        <Button
          @click="startGuidedTour"
          class="flex items-center gap-2 bg-blue-600 hover:bg-blue-700"
        >
         {{ isEnd?  "Done" : "Next" }}
          <ArrowRight class="w-4 h-4" />
        </Button>
      </div>
    </DialogContent>
  </Dialog>
</template>
