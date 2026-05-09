<script setup lang="ts">
import { onMounted, reactive, ref, computed, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { useLangStore } from '../stores/lang'
import { useChatModelStore } from '../stores/model'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Alert, AlertDescription } from '@/components/ui/alert'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import MessagesArea from '@/views/MessagesArea.vue'
import {
  askQuestion,
  ChatModelOptionProvider,
  ModelTag,
  type ChatModelOption,
} from '@/api/generated/portfolio'
import { captureProductAnalyticsEvent } from '@/lib/analytics'
import {
  POSTHOG_CHAT_EVENTS,
  POSTHOG_FEATURE_FLAGS,
  getFeatureFlag,
  registerAnalyticsProperties,
} from '@/lib/posthog-sdk'
import { hasFeatureFlagConsent, hasPageviewConsent } from '@/lib/posthog-consent'
import {
  getOrCreateChatConversationId,
  resetChatConversationId,
} from '@/lib/chat-telemetry'
import AudioWaveform from '@/components/AudioWaveform.vue'
import { useSpeechTranscription, MAX_SPEECH_PROMPT_CHARS } from '@/composables/useSpeechTranscription'
import { apiErrorMessage } from '@/lib/api-error'
import { Loader2, Mic, Square, Headphones } from 'lucide-vue-next'

// RAG chat: sessionStorage transcript, optional ?conversationId= REST hydrate, POST /ask with optional model id; clear stays on /chat.
type Message = { role: 'user' | 'assistant'; text: string; isNew?: boolean }

// --- Route + local UI state ---
const route = useRoute()
const router = useRouter()
const input = ref('')
const isLoading = ref(false)
const errorText = ref('')
const showInfoPopup = ref(false)
const state = reactive<{ messages: Message[] }>({ messages: [] })
const MAX_PROMPT_CHARS = MAX_SPEECH_PROMPT_CHARS
const CHAT_INFO_DISMISSED_KEY = 'chatInfoPopupDismissed.v2'
const langStore = useLangStore()
const chatModelStore = useChatModelStore()
const language = computed(() => langStore.language)

const speechBlocked = computed(() => isLoading.value)

const {
  supportsSpeechInput,
  isRecording,
  isTranscribing,
  recordingMediaStream,
  voiceError,
  toggleVoiceInput,
} = useSpeechTranscription({
  language,
  maxChars: MAX_PROMPT_CHARS,
  isBlocked: speechBlocked,
  onTranscript: (t) => {
    const next = (input.value ? `${input.value.trim()} ${t}` : t).trim()
    input.value = next.slice(0, MAX_PROMPT_CHARS)
  },
})

const chatBannerError = computed(() => errorText.value || voiceError.value)

const providerLabels = computed(() =>
  language.value === 'no'
    ? { heading: 'AI-leverandør', openai: 'OpenAI', anthropic: 'Anthropic' }
    : { heading: 'AI provider', openai: 'OpenAI', anthropic: 'Anthropic' },
)

const modelsForActiveProvider = computed(() => {
  const p = chatModelStore.activeProvider
  if (!p) return chatModelStore.models
  return chatModelStore.modelsForProvider(p)
})

/** Human-readable tag suffix for the model dropdown (FAST / REASONING). */
const formatModelTags = computed(() => (m: ChatModelOption) => {
  const tags = m.tags ?? []
  if (tags.length === 0) return ''
  const no = language.value === 'no'
  const parts = tags.map((t) => {
    if (t === ModelTag.FAST) return no ? 'Rask' : 'Fast'
    if (t === ModelTag.REASONING) return no ? 'Resonnering' : 'Reasoning'
    return String(t)
  })
  return ` (${parts.join(', ')})`
})

const selectedModelId = computed({
  get: () => chatModelStore.selectedModelId,
  set: (id: string) => chatModelStore.setSelectedModelId(id),
})

const showProviderToggle = computed(
  () => chatModelStore.hasOpenAI && chatModelStore.hasAnthropic,
)

const popupCopy = computed(() =>
  language.value === 'no'
    ? {
        title: 'Porteføljen oppdateres fortløpende',
        body: 'Chatten og kunnskapsgrunnlaget endrer seg etter hvert som jeg lærer mer i bacheloroppgaven. Svarene blir gjerne mer treffsikre over tid.',
        recommendation: 'Lurer du på hva som skjer akkurat nå? Ta en titt på prosjektsiden.',
        bachelorCta: 'Åpne prosjektsiden',
        dismiss: 'Forstått',
      }
    : {
        title: 'This portfolio keeps evolving',
        body: "I'm updating this chat and its knowledge base as I learn more in my bachelor's thesis. Answers should get sharper over time.",
        recommendation: 'Curious what I am working on? Check the project page.',
        bachelorCta: 'Open the project page',
        dismiss: 'Got it',
      },
)

// --- Ephemeral transcript (same-tab only; not a substitute for server-side conversation storage) ---
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

watch(() => state.messages, saveMessagesToStorage, { deep: true })

const GENERIC_ASK_ERROR = 'Noe gikk galt. Prøv igjen.'

// Drops the in-memory transcript and stays on /chat (strip deep-link query params).
const clearChat = () => {
  sessionStorage.removeItem('chatMessages')
  window.dispatchEvent(new CustomEvent('chatMessagesUpdated'))
  state.messages = []
  errorText.value = ''
  voiceError.value = ''
  input.value = ''
  resetChatConversationId()
  void router.replace({ name: 'chat', query: {} })
}

const shouldShowInfoPopup = () => {
  try {
    return !localStorage.getItem(CHAT_INFO_DISMISSED_KEY)
  } catch {
    return true
  }
}

const markInfoPopupDismissed = () => {
  try {
    localStorage.setItem(CHAT_INFO_DISMISSED_KEY, 'true')
  } catch {}
}

const dismissInfoPopup = () => {
  showInfoPopup.value = false
  markInfoPopupDismissed()
}

watch(showInfoPopup, (isOpen, wasOpen) => {
  if (wasOpen && !isOpen) {
    markInfoPopupDismissed()
  }
})

// Calls the portfolio backend; auth store is restored so optional future authenticated /ask works the same way.
async function send(text: string) {
  if (!text.trim() || isLoading.value || isTranscribing.value) return
  // client-side validation to mirror backend
  if (text.length > MAX_PROMPT_CHARS) {
    errorText.value =
      language.value === 'en'
        ? `Prompt is too long (${text.length}/${MAX_PROMPT_CHARS}).`
        : `Prompten er for lang (${text.length}/${MAX_PROMPT_CHARS}).`
    return
  }
  errorText.value = ''
  voiceError.value = ''
  state.messages.push({ role: 'user', text })
  input.value = ''
  const modelId = chatModelStore.selectedModelId ?? null
  const ffKey = POSTHOG_FEATURE_FLAGS.CHAT_REPLY_EXPERIMENT
  const ffVariant = hasFeatureFlagConsent() ? getFeatureFlag(ffKey) : undefined
  const ffProp =
    ffVariant !== undefined ? { [`$feature/${ffKey}`]: ffVariant } : {}
  const conversationId = getOrCreateChatConversationId()
  captureProductAnalyticsEvent(POSTHOG_CHAT_EVENTS.ASK_SUBMITTED, {
    prompt_length: text.trim().length,
    model_id: modelId,
    conversation_id: conversationId,
    ...ffProp,
  })
  try {
    isLoading.value = true
    const auth = (await import('@/stores/auth')).useAuthStore()
    auth.restore()
    const payload: { question: string; model?: string } = { question: text }
    if (chatModelStore.selectedModelId) {
      payload.model = chatModelStore.selectedModelId
    }
    // Refresh registered property in case the user cleared chat.
    if (hasPageviewConsent()) {
      registerAnalyticsProperties({ conversation_id: conversationId })
    }

    const r = await askQuestion(payload, {
      headers: { 'X-Conversation-Id': conversationId },
    })
    if (r.status === 200) {
      state.messages.push({ role: 'assistant', text: r.data.answer, isNew: true })
      captureProductAnalyticsEvent(POSTHOG_CHAT_EVENTS.ANSWER_RECEIVED, {
        http_status: 200,
        model_id: modelId,
        conversation_id: conversationId,
        ...ffProp,
      })
      return
    }
    const err = r as { status: number; data: unknown }
    const sc = err.status
    const errData = err.data
    if (sc === 429) {
      captureProductAnalyticsEvent(POSTHOG_CHAT_EVENTS.ANSWER_ERROR, {
        http_status: sc,
        model_id: modelId,
        conversation_id: conversationId,
        ...ffProp,
      })
      errorText.value = 'For mange forespørsler. Vent litt før du prøver igjen.'
      return
    }
    if (sc === 403) {
      captureProductAnalyticsEvent(POSTHOG_CHAT_EVENTS.ANSWER_ERROR, {
        http_status: sc,
        model_id: modelId,
        conversation_id: conversationId,
        ...ffProp,
      })
      errorText.value = apiErrorMessage(errData) ?? GENERIC_ASK_ERROR
      return
    }
    if (sc === 400 || sc === 503) {
      captureProductAnalyticsEvent(POSTHOG_CHAT_EVENTS.ANSWER_ERROR, {
        http_status: sc,
        model_id: modelId,
        conversation_id: conversationId,
        ...ffProp,
      })
      errorText.value = apiErrorMessage(errData) ?? GENERIC_ASK_ERROR
      return
    }
    captureProductAnalyticsEvent(POSTHOG_CHAT_EVENTS.ANSWER_ERROR, {
      http_status: sc,
      model_id: modelId,
      conversation_id: conversationId,
      ...ffProp,
    })
    errorText.value = apiErrorMessage(errData) ?? GENERIC_ASK_ERROR
  } catch {
    captureProductAnalyticsEvent(POSTHOG_CHAT_EVENTS.ANSWER_ERROR, {
      http_status: 0,
      model_id: modelId,
      conversation_id: conversationId,
      ...ffProp,
    })
    errorText.value = 'Nettverksfeil. Prøv igjen.'
  } finally {
    isLoading.value = false
  }
}

/** When deep-linking with ?conversationId=, hydrate the thread from the API instead of sessionStorage. */
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

onMounted(async () => {
  await chatModelStore.ensureModelsLoaded()
  if (shouldShowInfoPopup()) {
    showInfoPopup.value = true
  }

  const conversationId = getOrCreateChatConversationId()
  if (hasPageviewConsent()) {
    registerAnalyticsProperties({ conversation_id: conversationId })
  }

  const conversationIdParam = route.query.conversationId as string

  if (conversationIdParam) {
    loadConversation(conversationIdParam)
  } else {
    loadMessagesFromStorage()
  }

  const q = (route.query.q as string) || ''
  if (q && !conversationIdParam) {
    input.value = q
    // Home page passes ?q=: auto-send once so the user does not need a second click on /chat.
    send(q)
  }
})
</script>

<template>
  <main class="relative flex min-h-0 flex-1 flex-col overflow-hidden bg-gradient-to-br from-slate-100 via-blue-50 to-slate-100 pt-20">
    <Dialog v-model:open="showInfoPopup">
      <DialogContent class="sm:max-w-xl">
        <DialogHeader>
          <DialogTitle>{{ popupCopy.title }}</DialogTitle>
          <DialogDescription>{{ popupCopy.body }}</DialogDescription>
        </DialogHeader>
        <p class="text-sm text-slate-700">
          {{ popupCopy.recommendation }}
        </p>
        <DialogFooter class="flex-col gap-2 sm:flex-row sm:justify-end">
          <Button as-child variant="outline">
            <RouterLink :to="{ name: 'project', hash: '#bachelor' }">{{ popupCopy.bachelorCta }}</RouterLink>
          </Button>
          <Button type="button" @click="dismissInfoPopup">{{ popupCopy.dismiss }}</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
    <!-- Background overlay -->
    <div class="absolute inset-0 pointer-events-none">
      <div class="absolute top-0 left-0 w-full h-full" style="background: radial-gradient(circle at 20% 80%, rgba(59, 130, 246, 0.08) 0%, transparent 50%), radial-gradient(circle at 80% 20%, rgba(37, 99, 235, 0.08) 0%, transparent 50%), radial-gradient(circle at 50% 50%, rgba(96, 165, 250, 0.05) 0%, transparent 70%);"></div>
    </div>
    <!-- Chat Container -->
    <div class="relative z-10 mx-auto flex min-h-0 w-full max-w-6xl flex-1 flex-col overflow-hidden px-4 py-8 sm:px-6 lg:px-8">
      <!-- Error Alert -->
      <Alert v-if="chatBannerError" variant="destructive" class="mb-6 flex-shrink-0">
        <AlertDescription>{{ chatBannerError }}</AlertDescription>
      </Alert>

      <section class="mb-5 flex-shrink-0 rounded-3xl border border-blue-100/70 bg-white/85 p-4 shadow-lg shadow-blue-900/10 backdrop-blur-xl sm:p-5">
        <div class="flex items-center justify-between gap-3">
          <div class="flex items-center gap-3">
            <h1 class="text-2xl font-bold tracking-tight text-slate-800 sm:text-3xl">
              Kevin's AI
            </h1>
            <span class="rounded-full border border-blue-200/70 bg-blue-50 px-3 py-1 text-xs font-semibold text-blue-700">
              {{ isLoading ? 'Thinking...' : 'Online' }}
            </span>
          </div>
          <div class="flex flex-wrap items-center gap-2">
            <Button as-child variant="outline" size="sm" class="border border-blue-200/80 bg-white/85 text-blue-700 hover:border-blue-300/80 hover:bg-blue-50/70 hover:text-blue-800">
              <RouterLink to="/voice" class="inline-flex items-center gap-1.5">
                <Headphones class="size-4 shrink-0" aria-hidden="true" />
                {{ language === 'no' ? 'Stemmechat' : 'Voice chat' }}
              </RouterLink>
            </Button>
            <Button
              v-if="state.messages.length > 0"
              @click="clearChat"
              variant="outline"
              size="sm"
              class="border border-blue-200/80 bg-white/85 text-blue-700 hover:border-blue-300/80 hover:bg-blue-50/70 hover:text-blue-800"
            >
              Clear chat
            </Button>
          </div>
        </div>
      </section>

      <!-- Messages Area -->
      <div class="flex-1 mb-6 min-h-0">
        <MessagesArea 
          :messages="state.messages" 
          :is-loading="isLoading"
          :is-read-only="false"
        />
      </div>

      <!-- Form at Bottom -->
      <div class="pb-8 flex-shrink-0 space-y-4">
        <div
          v-if="chatModelStore.models.length > 0"
          class="grid gap-3 text-sm text-slate-700 sm:grid-cols-2"
        >
          <div
            v-if="showProviderToggle"
            class="rounded-2xl border border-blue-100/70 bg-white/85 px-4 py-3 shadow-sm backdrop-blur-md"
          >
            <p class="mb-2 text-xs font-semibold uppercase tracking-wide text-slate-500">
              {{ providerLabels.heading }}
            </p>
            <div class="flex justify-start">
              <div
                class="relative flex rounded-full border border-blue-100/70 bg-slate-100/80 p-1"
              >
                <div
                  class="absolute bottom-1 top-1 w-28 rounded-full border border-blue-100 bg-white shadow-sm transition-transform duration-300 ease-in-out"
                  :class="
                    chatModelStore.activeProvider === ChatModelOptionProvider.OPENAI
                      ? 'translate-x-0'
                      : 'translate-x-28'
                  "
                ></div>
                <button
                  type="button"
                  class="relative z-10 w-28 rounded-full py-2 text-sm font-medium transition-all duration-300 disabled:opacity-40"
                  :class="
                    chatModelStore.activeProvider === ChatModelOptionProvider.OPENAI
                      ? 'font-semibold text-blue-700'
                      : 'text-slate-500'
                  "
                  :disabled="isLoading || !chatModelStore.hasOpenAI"
                  @click="chatModelStore.selectFirstForProvider(ChatModelOptionProvider.OPENAI)"
                >
                  {{ providerLabels.openai }}
                </button>
                <button
                  type="button"
                  class="relative z-10 w-28 rounded-full py-2 text-sm font-medium transition-all duration-300 disabled:opacity-40"
                  :class="
                    chatModelStore.activeProvider === ChatModelOptionProvider.ANTHROPIC
                      ? 'font-semibold text-blue-700'
                      : 'text-slate-500'
                  "
                  :disabled="isLoading || !chatModelStore.hasAnthropic"
                  @click="chatModelStore.selectFirstForProvider(ChatModelOptionProvider.ANTHROPIC)"
                >
                  {{ providerLabels.anthropic }}
                </button>
              </div>
            </div>
          </div>
          <div class="rounded-2xl border border-blue-100/70 bg-white/85 px-4 py-3 shadow-sm backdrop-blur-md">
            <label
              for="chat-model-select"
              class="mb-2 block text-xs font-semibold uppercase tracking-wide text-slate-500"
            >
              {{ language === 'en' ? 'Model' : 'Modell' }}
            </label>
            <select
              id="chat-model-select"
              v-model="selectedModelId"
              :disabled="isLoading"
              class="w-full rounded-xl border border-blue-200/70 bg-white/90 px-3 py-2.5 text-sm text-slate-700 focus:border-blue-300/80 focus:outline-none disabled:opacity-50"
            >
              <option v-for="m in modelsForActiveProvider" :key="m.id" :value="m.id">
                {{ m.label }} ({{ m.provider }}){{ formatModelTags(m) }}
              </option>
            </select>
          </div>
        </div>
        <form
          class="relative flex gap-3 rounded-3xl border border-blue-100/70 bg-white/85 p-3 shadow-lg shadow-blue-900/10 backdrop-blur-xl transition-all duration-300 hover:border-blue-200/80 focus-within:border-blue-300/70 focus-within:shadow-lg focus-within:shadow-blue-500/20"
          @submit.prevent="send(input)"
        >
          <Button
            v-if="supportsSpeechInput"
            type="button"
            variant="outline"
            :disabled="isLoading || isTranscribing"
            :aria-pressed="isRecording"
            :aria-label="language === 'en' ? 'Voice input' : 'Taleinndata'"
            class="relative shrink-0 rounded-2xl border border-blue-200/80 bg-white/90 px-3 text-slate-700 hover:bg-blue-50/80 disabled:opacity-50"
            :class="{ 'animate-pulse ring-2 ring-red-400 ring-offset-1': isRecording }"
            @click="toggleVoiceInput"
          >
            <Loader2 v-if="isTranscribing" class="h-5 w-5 animate-spin text-blue-600" />
            <Square v-else-if="isRecording" class="h-5 w-5 text-red-600" />
            <Mic v-else class="h-5 w-5" />
          </Button>
          <AudioWaveform
            v-if="isRecording"
            :stream="recordingMediaStream"
            :aria-label="language === 'en' ? 'Audio level while recording' : 'Lydnivå under opptak'"
          />
          <Input
            v-else
            v-model="input"
            :disabled="isLoading || isTranscribing"
            type="text"
            class="flex-1 rounded-2xl border border-blue-100/70 bg-white/85 text-slate-700 transition-all duration-300 placeholder:font-medium placeholder:text-slate-400 focus:border-blue-300/70 focus:bg-white focus:shadow-sm focus:shadow-blue-500/15 focus:outline-none"
            :placeholder="language === 'en' ? 'Ask Kevin\'s AI anything...' : 'Spør Kevin\'s AI om noe...'"
          />
          <Button
            type="submit"
            :disabled="isLoading || isTranscribing || isRecording || !input.trim()"
            class="rounded-2xl bg-gradient-to-r from-blue-600 to-blue-700 px-6 text-sm font-semibold text-white shadow-lg shadow-blue-500/25 transition-all duration-300 hover:-translate-y-0.5 hover:from-blue-700 hover:to-blue-800 hover:shadow-xl hover:shadow-blue-500/35 disabled:cursor-not-allowed disabled:opacity-50 disabled:shadow-none disabled:hover:transform-none"
          >
            {{ isLoading ? 'Sending...' : 'Send →' }}
          </Button>
        </form>
      </div>
    </div>
  </main>
</template>
