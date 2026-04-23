<script setup lang="ts">
import { ref, watch, nextTick } from 'vue'
import { Brain, UserRound, MessageSquare } from 'lucide-vue-next'
import TypewriterAnimation from '@/components/TypewriterAnimation.vue'
import SafeMarkdown from '@/components/SafeMarkdown.vue'

type Message = { role: 'user' | 'assistant'; text: string; isNew?: boolean }

interface Props {
  messages: Message[]
  isLoading?: boolean
  isReadOnly?: boolean
  showHeader?: boolean
  headerText?: string
}

const props = withDefaults(defineProps<Props>(), {
  isLoading: false,
  isReadOnly: false,
  showHeader: false,
  headerText: 'Chat Messages'
})

const scrollableContainer = ref<HTMLElement>()

// Scroll to bottom function
const scrollToBottom = () => {
  nextTick(() => {
    if (scrollableContainer.value) {
      scrollableContainer.value.scrollTop = scrollableContainer.value.scrollHeight
    }
  })
}

// Watch for new messages and scroll to bottom
watch(() => props.messages.length, () => {
  scrollToBottom()
})

// Watch for changes in messages to scroll to bottom
watch(() => props.messages, () => {
  scrollToBottom()
}, { deep: true })
</script>

<template>
  <div class="flex flex-col h-full">
    <!-- Optional Header -->
    <div v-if="showHeader" class="flex-shrink-0 mb-4">
      <h3 class="text-lg font-semibold text-gray-800">{{ headerText }}</h3>
    </div>

    <!-- Scrollable Container -->
    <div ref="scrollableContainer" class="flex-1 overflow-y-auto">
      <!-- Messages Area -->
      <div 
        class="space-y-5 rounded-3xl border border-blue-100/70 bg-white/85 p-5 shadow-lg shadow-blue-900/10 backdrop-blur-xl transition-all duration-300 sm:p-6"
        :class="{ 'border-gray-200/50 bg-white/90': isReadOnly }"
      >
      <!-- Chat Messages -->
      <div v-for="(m, idx) in messages" :key="idx" class="flex" :class="m.role === 'user' ? 'justify-end' : 'justify-start'">
        <div class="max-w-[85%] sm:max-w-[78%]">
          <div class="flex items-start gap-3" :class="m.role === 'user' ? 'flex-row-reverse' : 'flex-row'">
            <!-- Avatar -->
            <div
              class="flex h-9 w-9 items-center justify-center rounded-full border"
              :class="
                m.role === 'user'
                  ? 'border-blue-600/60 bg-blue-600 text-white'
                  : 'border-blue-100/80 bg-blue-50 text-blue-700'
              "
            >
              <UserRound v-if="m.role === 'user'" class="w-4 h-4" />
              <Brain v-else class="w-4 h-4" />
            </div>

            <!-- Message Bubble -->
            <div class="flex-1">
              <div class="mb-1 text-xs font-medium text-slate-500" :class="m.role === 'user' ? 'text-right' : 'text-left'">
                {{ m.role === 'user' ? 'You' : 'Kevin\'s AI' }}
              </div>
              <div class="relative rounded-2xl px-4 py-3.5 transition-all duration-300"
                   :class="m.role === 'user'
                     ? 'border border-blue-500/70 bg-gradient-to-r from-blue-600 to-blue-700 text-white shadow-lg shadow-blue-500/35 hover:-translate-y-0.5 hover:from-blue-700 hover:to-blue-800 hover:shadow-xl hover:shadow-blue-500/45'
                     : 'border border-blue-100/80 bg-white/95 text-slate-700 shadow-sm hover:border-blue-200/80 hover:bg-white hover:shadow-md hover:shadow-blue-500/15'">
                <p v-if="m.role === 'user'" class="whitespace-pre-wrap text-sm leading-relaxed text-white">{{ m.text }}</p>
                <TypewriterAnimation v-else-if="m.isNew && !isReadOnly" :text="m.text" :text-class="'text-slate-700'" :speed="25" @finished="m.isNew = false" @scroll="scrollToBottom"/>
                <SafeMarkdown v-else :source="m.text" />
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Loading Indicator -->
      <div v-if="isLoading" class="flex justify-start">
        <div class="max-w-[85%] sm:max-w-[78%]">
          <div class="flex items-start gap-3">
            <div class="flex h-9 w-9 items-center justify-center rounded-full border border-blue-100/80 bg-blue-50 text-blue-700">
              <Brain class="w-4 h-4" />
            </div>
            <div class="flex-1">
              <div class="mb-1 text-xs font-medium text-blue-700">Kevin's AI</div>
              <div class="rounded-2xl border border-blue-100/80 bg-white/95 px-4 py-3.5 shadow-sm">
                <div class="flex items-center gap-1">
                  <div class="w-2 h-2 bg-blue-400 rounded-full animate-bounce"></div>
                  <div class="w-2 h-2 bg-blue-400 rounded-full animate-bounce" style="animation-delay: 0.1s"></div>
                  <div class="w-2 h-2 bg-blue-400 rounded-full animate-bounce" style="animation-delay: 0.2s"></div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Empty State -->
      <div v-if="messages.length === 0 && !isLoading" class="flex h-32 items-center justify-center text-slate-500">
        <div class="text-center">
          <MessageSquare class="mx-auto mb-2 h-8 w-8 opacity-50" />
          <p class="text-sm">No messages yet</p>
        </div>
      </div>
    </div>
    </div>
  </div>
</template>
