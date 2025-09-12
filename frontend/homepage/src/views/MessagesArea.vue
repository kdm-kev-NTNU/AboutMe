<script setup lang="ts">
import { ref, computed, watch, nextTick } from 'vue'
import { Brain, UserRound, MessageSquare } from 'lucide-vue-next'
import VueMarkdown from 'vue-markdown-render'
import TypewriterAnimation from '@/components/TypewriterAnimation.vue'

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

const messagesContainer = ref<HTMLElement>()

// Scroll to bottom function
const scrollToBottom = () => {
  nextTick(() => {
    if (messagesContainer.value) {
      messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
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
    <div class="flex-1 overflow-y-auto">
      <!-- Messages Area -->
      <div 
        ref="messagesContainer" 
        class="space-y-4 pr-2 border-2 border-blue-100/20 rounded-lg p-4 bg-white/90 backdrop-blur-sm hover:border-blue-200/30 hover:shadow-lg hover:shadow-blue-500/10 transition-all duration-300"
        :class="{ 'border-gray-200/20 hover:border-gray-300/30': isReadOnly }"
      >
      <!-- Chat Messages -->
      <div v-for="(m, idx) in messages" :key="idx" class="flex" :class="m.role === 'user' ? 'justify-end' : 'justify-start'">
        <div class="max-w-[80%]">
          <div class="flex items-start gap-3" :class="m.role === 'user' ? 'flex-row-reverse' : 'flex-row'">
            <!-- Avatar -->
            <div class="w-8 h-8 rounded-full flex items-center justify-center"
                 :class="m.role === 'user' ? 'bg-black text-white' : 'bg-gray-200 text-gray-600'">
              <UserRound v-if="m.role === 'user'" class="w-4 h-4" />
              <Brain v-else class="w-4 h-4" />
            </div>

            <!-- Message Bubble -->
            <div class="flex-1">
              <div class="text-xs text-gray-500 mb-1" :class="m.role === 'user' ? 'text-right' : 'text-left'">
                {{ m.role === 'user' ? 'You' : 'Kevin\'s AI' }}
              </div>
              <div class="relative transition-all duration-300 rounded-xl px-4 py-3 shadow-sm hover:shadow-lg"
                   :class="m.role === 'user'
                     ? 'bg-gradient-to-r from-blue-600 to-blue-700 border-2 border-blue-600 hover:from-blue-700 hover:to-blue-800 hover:-translate-y-0.5 hover:shadow-xl hover:shadow-blue-500/40'
                     : 'bg-white/95 border-2 border-blue-100/20 hover:border-blue-200/30 hover:bg-white hover:shadow-lg hover:shadow-blue-500/10'">
                <p v-if="m.role === 'user'" class="text-sm leading-relaxed whitespace-pre-wrap text-white">{{ m.text }}</p>
                <TypewriterAnimation v-else-if="m.isNew && !isReadOnly" :text="m.text" :text-class="'text-gray-700'" :speed="25" @finished="m.isNew = false" @scroll="scrollToBottom"/>
                <vue-markdown v-else class="text-sm leading-relaxed whitespace-pre-wrap text-gray-700" :source="m.text"/>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Loading Indicator -->
      <div v-if="isLoading" class="flex justify-start">
        <div class="max-w-[80%]">
          <div class="flex items-start gap-3">
            <div class="w-8 h-8 rounded-full bg-blue-100 text-blue-600 flex items-center justify-center">
              <Brain class="w-4 h-4" />
            </div>
            <div class="flex-1">
              <div class="text-xs text-blue-600 mb-1 font-medium">Kevin's AI</div>
              <div class="bg-white/95 border-2 border-blue-200/30 rounded-xl px-4 py-3 shadow-sm">
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
      <div v-if="messages.length === 0 && !isLoading" class="flex items-center justify-center h-32 text-gray-500">
        <div class="text-center">
          <MessageSquare class="w-8 h-8 mx-auto mb-2 opacity-50" />
          <p class="text-sm">No messages yet</p>
        </div>
      </div>
    </div>
    </div>
  </div>
</template>

<style scoped>
/* Avatar Styling */
.w-8.h-8.rounded-full {
  transition: all 0.3s ease;
}

.w-8.h-8.rounded-full:hover {
  transform: scale(1.1);
}

/* Responsive adjustments */
@media (max-width: 768px) {
  .hover\:-translate-y-0\.5:hover {
    transform: translateY(-1px);
  }
}
</style>