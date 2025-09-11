<script setup lang="ts">
import { onMounted, reactive, ref, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useLangStore } from '../stores/lang'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { Card, CardContent } from '@/components/ui/card'
import { Brain, UserRound } from 'lucide-vue-next'

type Message = { role: 'user' | 'assistant'; text: string }

const route = useRoute()
const router = useRouter()
const input = ref('')
const isLoading = ref(false)
const errorText = ref('')
const state = reactive<{ messages: Message[] }>({ messages: [] })
const MAX_PROMPT_CHARS = 3000
const langStore = useLangStore()
const language = computed(() => langStore.language)

// Session storage functions
const saveMessagesToStorage = () => {
  try {
    sessionStorage.setItem('chatMessages', JSON.stringify(state.messages))
    // Dispatch custom event to notify other components
    window.dispatchEvent(new CustomEvent('chatMessagesUpdated'))
  } catch (error) {
    console.warn('Failed to save messages to session storage:', error)
  }
}

const loadMessagesFromStorage = () => {
  try {
    const stored = sessionStorage.getItem('chatMessages')
    if (stored) {
      const messages = JSON.parse(stored)
      if (Array.isArray(messages)) {
        state.messages = messages
      }
    }
  } catch (error) {
    console.warn('Failed to load messages from session storage:', error)
  }
}

// Watch for changes in messages and save to storage
watch(() => state.messages, saveMessagesToStorage, { deep: true })

// Clear chat function - redirects to home page
const clearChat = () => {
  // Clear session storage first
  sessionStorage.removeItem('chatMessages')
  window.dispatchEvent(new CustomEvent('chatMessagesUpdated'))

  // Redirect to home page
  router.push({ name: 'home' })
}

async function send(text: string) {
  if (!text.trim() || isLoading.value) return
  // client-side validation to mirror backend
  if (text.length > MAX_PROMPT_CHARS) {
    errorText.value = `Prompten er for lang (${text.length}/${MAX_PROMPT_CHARS}).`;
    return
  }
  errorText.value = ''
  state.messages.push({ role: 'user', text })
  input.value = ''
  try {
    isLoading.value = true
    const res = await fetch('/api/ask', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ question: text }),
    })
    if (!res.ok) {
      // Try parse JSON error from backend
      let msg = 'Noe gikk galt. Prøv igjen.'
      try {
        const data = await res.json() as any
        if (data && typeof data.error === 'string') {
          msg = data.error
        }
      } catch (_) {
        // ignore parse errors
      }
      if (res.status === 429) {
        msg = 'For mange forespørsler. Vent litt før du prøver igjen.'
      } else if (res.status === 400 && !msg) {
        msg = 'Ugyldig forespørsel.'
      }
      errorText.value = msg
      return
    }
    const data: { answer: string } = await res.json()
    state.messages.push({ role: 'assistant', text: data.answer })
  } catch (e: any) {
    errorText.value = 'Nettverksfeil. Prøv igjen.'
  } finally {
    isLoading.value = false
  }
}

onMounted(() => {
  // Load messages from session storage first
  loadMessagesFromStorage()

  const q = (route.query.q as string) || ''
  if (q) {
    input.value = q
    // auto-send the initial question from the homepage button
    send(q)
  }
})
</script>

<template>
  <main class="chat-page flex flex-col h-screen pt-20">
    <!-- Chat Container -->
    <div class="flex-1 flex flex-col max-w-4xl mx-auto w-full px-8 py-8 overflow-hidden relative z-10">
      <!-- Error Alert -->
      <Alert v-if="errorText" variant="destructive" class="mb-6 flex-shrink-0">
        <AlertDescription>{{ errorText }}</AlertDescription>
      </Alert>

      <!-- Chat Header with Clear Button -->
      <div v-if="state.messages.length > 0" class="flex justify-center items-center mb-4 flex-shrink-0">
        <Button
          @click="clearChat"
          variant="outline"
          size="sm"
          class="blue-clear-button text-gray-600 hover:text-blue-600 hover:border-blue-300 cursor-pointer"
        >
          Clear Chat
        </Button>
      </div>

      <!-- Messages Area -->
      <div class="chat-messages-area flex-1 overflow-y-auto space-y-4 mb-8 pr-2 border border-gray-200 rounded-lg p-4 bg-white">
        <!-- Chat Messages -->
        <div v-for="(m, idx) in state.messages" :key="idx" class="flex" :class="m.role === 'user' ? 'justify-end' : 'justify-start'">
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
                <div class="message-bubble bg-white border border-gray-200 rounded-xl px-4 py-3 shadow-sm hover:shadow-lg transition-all duration-300"
                     :class="m.role === 'user' ? 'user-message-bubble bg-blue-500 border-blue-500' : 'ai-message-bubble'">
                  <p class="text-sm leading-relaxed whitespace-pre-wrap" :class="m.role === 'user' ? 'text-white' : 'text-gray-700'">{{ m.text }}</p>
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
                <div class="ai-message-bubble bg-white border border-blue-200 rounded-xl px-4 py-3 shadow-sm">
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
      </div>

      <!-- Form at Bottom -->
      <div class="pb-8 flex-shrink-0">
        <form class="chat-form flex gap-3" @submit.prevent="send(input)">
          <Input
            v-model="input"
            :disabled="isLoading"
            type="text"
            class="flex-1 bg-white blue-chat-input"
            :placeholder="language === 'en' ? 'Ask Kevin\'s AI anything...' : 'Spør Kevin\'s AI om noe...'"
          />
          <Button type="submit" :disabled="isLoading || !input.trim()" class="blue-send-button">
            {{ isLoading ? 'Sending...' : 'Send →' }}
          </Button>
        </form>
      </div>
    </div>
  </main>
</template>

<style scoped>
.chat-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
  position: relative;
}

.chat-page::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: radial-gradient(circle at 20% 80%, rgba(59, 130, 246, 0.08) 0%, transparent 50%),
              radial-gradient(circle at 80% 20%, rgba(37, 99, 235, 0.08) 0%, transparent 50%),
              radial-gradient(circle at 50% 50%, rgba(96, 165, 250, 0.05) 0%, transparent 70%);
  pointer-events: none;
}

/* Chat Messages Area */
.chat-messages-area {
  background: rgba(255, 255, 255, 0.9) !important;
  backdrop-filter: blur(10px);
  border: 2px solid rgba(59, 130, 246, 0.1) !important;
  transition: all 0.3s ease;
}

.chat-messages-area:hover {
  border-color: rgba(59, 130, 246, 0.2) !important;
  box-shadow: 0 8px 25px rgba(59, 130, 246, 0.1);
}

/* Message Bubbles */
.message-bubble {
  position: relative;
  transition: all 0.3s ease;
}

.user-message-bubble {
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%) !important;
  border: 2px solid #2563eb !important;
  box-shadow: 0 4px 15px rgba(59, 130, 246, 0.3);
}

.user-message-bubble:hover {
  background: linear-gradient(135deg, #2563eb 0%, #1d4ed8 100%) !important;
  transform: translateY(-1px);
  box-shadow: 0 6px 20px rgba(59, 130, 246, 0.4);
}

.ai-message-bubble {
  background: rgba(255, 255, 255, 0.95) !important;
  border: 2px solid rgba(59, 130, 246, 0.1) !important;
}

.ai-message-bubble:hover {
  border-color: rgba(59, 130, 246, 0.2) !important;
  background: rgba(255, 255, 255, 1) !important;
  box-shadow: 0 4px 15px rgba(59, 130, 246, 0.1);
}

/* Chat Form Styling */
.chat-form {
  position: relative;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(10px);
  border: 2px solid rgba(59, 130, 246, 0.2);
  border-radius: 12px;
  padding: 8px;
  transition: all 0.3s ease;
}

.chat-form::before {
  content: '';
  position: absolute;
  top: -2px;
  left: -2px;
  right: -2px;
  bottom: -2px;
  background: linear-gradient(135deg, #3b82f6, #2563eb, #1d4ed8);
  border-radius: 14px;
  opacity: 0;
  transition: opacity 0.3s ease;
  z-index: -1;
}

.chat-form:hover {
  border-color: rgba(59, 130, 246, 0.4);
  background: rgba(255, 255, 255, 0.95);
  box-shadow: 0 8px 25px rgba(59, 130, 246, 0.15);
}

.chat-form:hover::before {
  opacity: 0.2;
}

.chat-form:focus-within {
  border-color: rgba(59, 130, 246, 0.6);
  background: rgba(255, 255, 255, 0.98);
  box-shadow: 0 8px 25px rgba(59, 130, 246, 0.25);
}

.chat-form:focus-within::before {
  opacity: 0.3;
}

/* Blue Chat Input Styling */
.blue-chat-input {
  background: rgba(255, 255, 255, 0.8) !important;
  border: 2px solid rgba(59, 130, 246, 0.2) !important;
  border-radius: 8px !important;
  transition: all 0.3s ease !important;
}

.blue-chat-input:focus {
  background: rgba(255, 255, 255, 0.95) !important;
  border-color: rgba(59, 130, 246, 0.5) !important;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1) !important;
  outline: none !important;
}

.blue-chat-input::placeholder {
  color: rgba(59, 130, 246, 0.6) !important;
  font-weight: 500 !important;
}

/* Blue Send Button Styling */
.blue-send-button {
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%) !important;
  border: none !important;
  color: white !important;
  font-weight: 600 !important;
  transition: all 0.3s ease !important;
  position: relative !important;
  overflow: hidden !important;
}

.blue-send-button::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.2), transparent);
  transition: left 0.5s ease;
}

.blue-send-button:hover:not(:disabled) {
  background: linear-gradient(135deg, #2563eb 0%, #1d4ed8 100%) !important;
  transform: translateY(-1px) !important;
  box-shadow: 0 8px 25px rgba(59, 130, 246, 0.4) !important;
}

.blue-send-button:hover:not(:disabled)::before {
  left: 100%;
}

.blue-send-button:active:not(:disabled) {
  transform: translateY(0) !important;
  box-shadow: 0 4px 15px rgba(59, 130, 246, 0.3) !important;
}

.blue-send-button:disabled {
  background: rgba(59, 130, 246, 0.3) !important;
  cursor: not-allowed !important;
}

/* Blue Clear Button Styling */
.blue-clear-button {
  border: 2px solid rgba(59, 130, 246, 0.3) !important;
  color: #3b82f6 !important;
  background: rgba(59, 130, 246, 0.05) !important;
  transition: all 0.3s ease !important;
  position: relative !important;
  overflow: hidden !important;
}

.blue-clear-button::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(59, 130, 246, 0.1), transparent);
  transition: left 0.5s ease;
}

.blue-clear-button:hover {
  border-color: rgba(59, 130, 246, 0.6) !important;
  background: rgba(59, 130, 246, 0.1) !important;
  color: #2563eb !important;
  transform: translateY(-1px) !important;
  box-shadow: 0 4px 15px rgba(59, 130, 246, 0.2) !important;
}

.blue-clear-button:hover::before {
  left: 100%;
}

/* Avatar Styling */
.w-8.h-8.rounded-full {
  transition: all 0.3s ease;
}

.w-8.h-8.rounded-full:hover {
  transform: scale(1.1);
}

/* Responsive adjustments */
@media (max-width: 768px) {
  .chat-form:hover {
    transform: none;
  }
  
  .blue-send-button:hover:not(:disabled),
  .blue-clear-button:hover {
    transform: none !important;
  }
  
  .user-message-bubble:hover,
  .ai-message-bubble:hover {
    transform: none;
  }
}
</style>

