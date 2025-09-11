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
  <main class="flex flex-col h-screen pt-20">
    <!-- Chat Container -->
    <div class="flex-1 flex flex-col max-w-4xl mx-auto w-full px-8 py-8 overflow-hidden">
      <!-- Error Alert -->
      <Alert v-if="errorText" variant="destructive" class="mb-6 flex-shrink-0">
        <AlertDescription>{{ errorText }}</AlertDescription>
      </Alert>

      <!-- Chat Header with Clear Button -->
      <div v-if="state.messages.length > 0" class="flex justify-between items-center mb-4 flex-shrink-0">
        <h2 class="text-lg font-semibold text-gray-800">Chat with Kevin's AI</h2>
        <Button
          @click="clearChat"
          variant="outline"
          size="sm"
          class="text-gray-600 hover:text-red-600 hover:border-red-300 cursor-pointer"
        >
          Clear Chat
        </Button>
      </div>

      <!-- Messages Area -->
      <div class="flex-1 overflow-y-auto space-y-4 mb-8 pr-2">
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
                <div class="bg-white border border-gray-200 rounded-xl px-4 py-3 shadow-sm hover:shadow-lg transition-all duration-300"
                     :class="m.role === 'user' ? 'bg-blue-500 border-blue-500' : ''">
                  <p class="text-sm leading-relaxed whitespace-pre-wrap" :class="m.role === 'user' ? 'text-black' : ''">{{ m.text }}</p>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Loading Indicator -->
        <div v-if="isLoading" class="flex justify-start">
          <div class="max-w-[80%]">
            <div class="flex items-start gap-3">
              <div class="w-8 h-8 rounded-full bg-gray-200 text-gray-600 flex items-center justify-center">
                <Brain class="w-4 h-4" />
              </div>
              <div class="flex-1">
                <div class="text-xs text-gray-500 mb-1">Kevin's AI</div>
                <div class="bg-white border border-gray-200 rounded-xl px-4 py-3 shadow-sm">
                  <div class="flex items-center gap-1">
                    <div class="w-2 h-2 bg-gray-400 rounded-full animate-bounce"></div>
                    <div class="w-2 h-2 bg-gray-400 rounded-full animate-bounce" style="animation-delay: 0.1s"></div>
                    <div class="w-2 h-2 bg-gray-400 rounded-full animate-bounce" style="animation-delay: 0.2s"></div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Form at Bottom -->
      <div class="pb-8 flex-shrink-0">
        <form class="flex gap-3" @submit.prevent="send(input)">
          <Input
            v-model="input"
            :disabled="isLoading"
            type="text"
            class="flex-1"
            :placeholder="language === 'en' ? 'Ask Kevin\'s AI anything...' : 'Spør Kevin\'s AI om noe...'"
          />
          <Button type="submit" :disabled="isLoading || !input.trim()">
            {{ isLoading ? 'Sending...' : 'Send →' }}
          </Button>
        </form>
      </div>
    </div>
  </main>
</template>

