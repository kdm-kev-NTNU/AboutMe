<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useLangStore } from '../stores/lang'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { Brain, UserRound, MessageSquare, Calendar } from 'lucide-vue-next'
import VueMarkdown from 'vue-markdown-render'

type ChatMessage = {
  role: 'user' | 'assistant'
  text: string
  timestamp?: string
}

type ChatSession = {
  id: number
  startedAt: string
  endedAt: string
  messageCount: number
  preview: string
}

const router = useRouter()
const langStore = useLangStore()
const language = computed(() => langStore.language)

const chatSessions = ref<ChatSession[]>([])
const isLoading = ref(false)
const errorText = ref('')

// Fetch chat history from backend
const fetchChatHistory = async () => {
  isLoading.value = true
  errorText.value = ''

  try {
    const res = await fetch('/api/conversations', {
      method: 'GET',
      headers: { 'Content-Type': 'application/json' },
    })

    if (!res.ok) {
      let msg = language.value === 'en' ? 'Something went wrong. Please try again.' : 'Noe gikk galt. Prøv igjen.'
      try {
        const data = await res.json() as any
        if (data && typeof data.error === 'string') {
          msg = data.error
        }
      } catch (_) {
        // ignore parse errors
      }
      errorText.value = msg
      return
    }

    const data: ChatSession[] = await res.json()
    chatSessions.value = data.sort((a, b) =>
      new Date(b.endedAt).getTime() - new Date(a.endedAt).getTime()
    )
  } catch (e: any) {
    errorText.value = language.value === 'en' ? 'Network error. Please try again.' : 'Nettverksfeil. Prøv igjen.'
  } finally {
    isLoading.value = false
  }
}

// Format date for display
const formatDate = (dateString: string) => {
  const date = new Date(dateString)
  return date.toLocaleDateString(language.value === 'en' ? 'en-US' : 'no-NO', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  })
}

// Navigate to specific chat
const openChat = (conversationId: number) => {
  router.push({ name: 'chat', query: { conversationId: conversationId.toString() } })
}

onMounted(() => {
  fetchChatHistory()
})
</script>

<template>
  <main class="flex flex-col h-screen pt-20 min-h-screen bg-gradient-to-br from-slate-50 to-slate-100 relative">
    <!-- Background overlay -->
    <div class="absolute inset-0 pointer-events-none">
      <div class="absolute top-0 left-0 w-full h-full" style="background: radial-gradient(circle at 20% 80%, rgba(59, 130, 246, 0.08) 0%, transparent 50%), radial-gradient(circle at 80% 20%, rgba(37, 99, 235, 0.08) 0%, transparent 50%), radial-gradient(circle at 50% 50%, rgba(96, 165, 250, 0.05) 0%, transparent 70%);"></div>
    </div>

    <!-- Header -->
    <div class="flex-1 flex flex-col max-w-6xl mx-auto w-full px-8 py-8 overflow-hidden relative z-10">
      <!-- Title -->
      <div class="text-center mb-8 flex-shrink-0">
        <h1 class="text-3xl font-bold text-gray-800">
          {{ language === 'en' ? 'Chat History' : 'Chat-historikk' }}
        </h1>
      </div>

      <!-- Error Alert -->
      <Alert v-if="errorText" variant="destructive" class="mb-6 flex-shrink-0">
        <AlertDescription>{{ errorText }}</AlertDescription>
      </Alert>

      <!-- Loading State -->
      <div v-if="isLoading" class="flex-1 flex items-center justify-center">
        <div class="text-center">
          <div class="w-8 h-8 border-4 border-blue-200 border-t-blue-600 rounded-full animate-spin mx-auto mb-4"></div>
          <p class="text-gray-600">{{ language === 'en' ? 'Loading chat history...' : 'Laster chat-historikk...' }}</p>
        </div>
      </div>

      <!-- Empty State -->
      <div v-else-if="chatSessions.length === 0" class="flex-1 flex items-center justify-center">
        <div class="text-center">
          <MessageSquare class="w-16 h-16 text-gray-400 mx-auto mb-4" />
          <h3 class="text-xl font-semibold text-gray-700 mb-2">
            {{ language === 'en' ? 'No chat history yet' : 'Ingen chat-historikk ennå' }}
          </h3>
          <p class="text-gray-500 mb-6">
            {{ language === 'en' ? 'Start a conversation to see your chat history here.' : 'Start en samtale for å se chat-historikken din her.' }}
          </p>
         
        </div>
      </div>

      <!-- Chat Sessions Grid -->
      <div v-else class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6 overflow-y-auto">
        <Card
          v-for="session in chatSessions"
          :key="session.id"
          class="bg-white/90 backdrop-blur-sm border-2 border-blue-100/20 hover:border-blue-200/30 hover:shadow-lg hover:shadow-blue-500/10 transition-all duration-300 cursor-pointer hover:-translate-y-1"
          @click="openChat(session.id)"
        >
          <CardHeader class="pb-3">
            <div class="flex items-center justify-between">
              <CardTitle class="text-lg text-gray-800 flex items-center gap-2">
                <MessageSquare class="w-5 h-5 text-blue-600" />
                  {{ session.preview.length > 120 ? session.preview.substring(0, 120) + '...' : session.preview }}
              </CardTitle>

            </div>
          </CardHeader>
          <CardContent class="pt-0">
            <!-- Show conversation preview -->
            <div class="space-y-3">
              <!-- Preview Text -->
              <div class="min-w-0">
                <div class="flex items-center gap-2 text-sm text-gray-500">
                <Calendar class="w-4 h-4" />
                {{ formatDate(session.endedAt) }}
              </div>
              </div>
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  </main>
</template>

<style scoped>
/* Smooth transitions for interactive elements */
.transition-all {
  transition: all 0.3s ease;
}

/* Hover effects for cards */
.card:hover {
  transform: translateY(-2px);
}
</style>
