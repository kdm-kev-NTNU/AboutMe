<script setup lang="ts">
import { onMounted, reactive, ref, computed, watch, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useLangStore } from '../stores/lang'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { Card, CardContent } from '@/components/ui/card'
import { Brain, UserRound } from 'lucide-vue-next'
import MessagesArea from '@/views/MessagesArea.vue'


type Message = { role: 'user' | 'assistant'; text: string; isNew?: boolean }

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
        // Ensure loaded messages are not marked as new
        state.messages = messages.map(msg => ({ ...msg, isNew: false }))
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
    state.messages.push({ role: 'assistant', text: data.answer, isNew: true })
  } catch (e: any) {
    errorText.value = 'Nettverksfeil. Prøv igjen.'
  } finally {
    isLoading.value = false
  }
}

// Load conversation from backend
const loadConversation = async (conversationId: string) => {
  try {
    const res = await fetch(`/api/conversations/${conversationId}`, {
      method: 'GET',
      headers: { 'Content-Type': 'application/json' },
    })

    if (res.ok) {
      const conversation: { id: number, startedAt: string, endedAt: string, messages: Array<{ id: number, role: string, text: string, createdAt: string }> } = await res.json()

      // Convert backend messages to frontend format
      state.messages = conversation.messages.map(msg => ({
        role: msg.role as 'user' | 'assistant',
        text: msg.text,
        isNew: false
      }))
    }
  } catch (error) {
    console.warn('Failed to load conversation:', error)
  }
}

onMounted(() => {
  const conversationId = route.query.conversationId as string

  if (conversationId) {
    // Load specific conversation from backend
    loadConversation(conversationId)
  } else {
    // Load messages from session storage for new conversations
    loadMessagesFromStorage()
  }

  const q = (route.query.q as string) || ''
  if (q && !conversationId) {
    input.value = q
    // Only auto-send the initial question if there are no existing messages
    // This prevents re-sending the question on page refresh
    if (state.messages.length === 0) {
      send(q)
    }
  }
})
</script>

<template>
  <main class="flex flex-col h-screen pt-20 min-h-screen bg-gradient-to-br from-slate-50 to-slate-100 relative">
    <!-- Background overlay -->
    <div class="absolute inset-0 pointer-events-none">
      <div class="absolute top-0 left-0 w-full h-full" style="background: radial-gradient(circle at 20% 80%, rgba(59, 130, 246, 0.08) 0%, transparent 50%), radial-gradient(circle at 80% 20%, rgba(37, 99, 235, 0.08) 0%, transparent 50%), radial-gradient(circle at 50% 50%, rgba(96, 165, 250, 0.05) 0%, transparent 70%);"></div>
    </div>
    <!-- Chat Container -->
    <div class="flex-1 flex flex-col max-w-4xl mx-auto w-full px-8 py-8 overflow-hidden relative z-10 min-h-0">
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
          class="border-2 border-blue-300/30 text-blue-600 bg-blue-50/50 hover:border-blue-300/60 hover:bg-blue-50 hover:text-blue-700 hover:-translate-y-0.5 hover:shadow-lg hover:shadow-blue-500/20 transition-all duration-300 relative overflow-hidden cursor-pointer"
        >
          Clear Chat
        </Button>
      </div>

      <!-- Messages Area -->
      <div class="flex-1 mb-8 min-h-0">
        <MessagesArea 
          :messages="state.messages" 
          :is-loading="isLoading"
          :is-read-only="false"
        />
      </div>

      <!-- Form at Bottom -->
      <div class="pb-8 flex-shrink-0">
        <form class="flex gap-3 relative bg-white/90 backdrop-blur-sm border-2 border-blue-200/20 rounded-xl p-2 transition-all duration-300 hover:border-blue-300/40 hover:bg-white/95 hover:shadow-lg hover:shadow-blue-500/15 focus-within:border-blue-300/60 focus-within:bg-white/98 focus-within:shadow-lg focus-within:shadow-blue-500/25" @submit.prevent="send(input)">
          <Input
            v-model="input"
            :disabled="isLoading"
            type="text"
            class="flex-1 bg-white/80 border-2 border-blue-200/20 rounded-lg transition-all duration-300 focus:bg-white/95 focus:border-blue-300/50 focus:shadow-sm focus:shadow-blue-500/10 focus:outline-none placeholder:text-blue-600/60 placeholder:font-medium"
            :placeholder="language === 'en' ? 'Ask Kevin\'s AI anything...' : 'Spør Kevin\'s AI om noe...'"
          />
          <Button type="submit" :disabled="isLoading || !input.trim()" class="bg-gradient-to-r from-blue-600 to-blue-700 hover:from-blue-700 hover:to-blue-800 text-white font-semibold hover:-translate-y-0.5 hover:shadow-lg hover:shadow-blue-500/40 transition-all duration-300 relative overflow-hidden disabled:bg-blue-300/30 disabled:cursor-not-allowed disabled:hover:transform-none disabled:hover:shadow-none">
            {{ isLoading ? 'Sending...' : 'Send →' }}
          </Button>
        </form>
      </div>
    </div>
  </main>
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

