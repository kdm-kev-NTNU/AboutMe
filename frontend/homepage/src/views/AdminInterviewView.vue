<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { RouterLink } from 'vue-router'
import { Loader2, Mic, MicOff, Square } from 'lucide-vue-next'
import { useAuthStore } from '@/stores/auth'
import { useVoiceModelStore } from '@/stores/voice-model'
import { useInterviewVoice } from '@/composables/useInterviewVoice'
import {
  fetchRealtimeVoiceStatus,
  type RealtimeReasoningEffort,
  type RealtimeVoiceChoice,
} from '@/lib/realtime-voice'
import {
  cleanInterviewTranscript,
  createInterviewSession,
  createInterviewTextDocument,
  finalizeInterviewSession,
  ingestInterviewTranscript,
  uploadInterviewDocument,
  type InterviewDocument,
  type InterviewTranscript,
} from '@/lib/interview-voice'

type WizardStep = 'source' | 'interview' | 'transcript' | 'clean'

const auth = useAuthStore()
const voiceModelStore = useVoiceModelStore()

const step = ref<WizardStep>('source')
const uiLang = ref<'en' | 'no'>('no')
const pastedText = ref('')
const pasteFilename = ref('interview-questions.md')
const busy = ref(false)
const error = ref('')
const status = ref('')

const document = ref<InterviewDocument | null>(null)
const sessionId = ref<string | null>(null)
const transcript = ref<InterviewTranscript | null>(null)

const liveAvailable = ref<boolean | null>(null)
const selectedVoice = ref<RealtimeVoiceChoice>('marin')
const selectedReasoning = ref<RealtimeReasoningEffort>('low')

const sessionOptions = computed(() => ({
  voice: selectedVoice.value,
  reasoningEffort: selectedReasoning.value,
  vadEagerness: 'low' as const,
}))
const selectedVoiceModel = computed(() => voiceModelStore.selectedModel)

const {
  connectionState,
  errorMessage,
  sessionNotice,
  assistantTranscript,
  userTranscript,
  isModelSpeaking,
  committedTurns,
  connect,
  disconnectAndFlush,
  stopResponse,
  resetTurns,
  flushTurns,
} = useInterviewVoice(sessionId, uiLang, sessionOptions, selectedVoiceModel)

function countQuestions(text: string): number {
  return text.split('\n').filter((line) => line.trim() !== '').length
}

const canStartInterview = computed(() => !!document.value || pastedText.value.trim() !== '')

const activeQuestionCount = computed(() => {
  if (pastedText.value.trim()) return countQuestions(pastedText.value)
  return document.value ? null : 0
})

const copy = computed(() => {
  const en = uiLang.value === 'en'
  return {
    title: en ? 'Voice interview (about you)' : 'Stemmeintervju (om deg)',
    intro: en
      ? 'Write or upload questions you want to be asked, practice with voice, save the transcript, clean it, and add it to RAG.'
      : 'Skriv eller last opp spørsmål du vil bli stilt, øv med stemme, lagre transkriptet, rens det og legg det til i RAG.',
    source: en ? '1. Questions' : '1. Spørsmål',
    interview: en ? '2. Live interview' : '2. Live intervju',
    transcript: en ? '3. Transcript' : '3. Transkript',
    clean: en ? '4. Clean & ingest' : '4. Rens og ingest',
    upload: en ? 'Upload file with questions' : 'Last opp fil med spørsmål',
    paste: en ? 'Or paste questions' : 'Eller lim inn spørsmål',
    pastePlaceholder: en
      ? 'One question per line, e.g.:\nTell me about a project you are proud of.\nWhat is your biggest technical strength?'
      : 'Ett spørsmål per linje, f.eks.:\nFortell om et prosjekt du er stolt av.\nHva er din største tekniske styrke?',
    saveText: en ? 'Save questions' : 'Lagre spørsmål',
    activeQuestions: en ? 'Active questions' : 'Aktive spørsmål',
    activeFile: en ? 'Active file' : 'Aktiv fil',
    lines: en ? 'lines' : 'linjer',
    lang: en ? 'Interview language' : 'Intervjuspråk',
    start: en ? 'Start interview' : 'Start intervju',
    connect: en ? 'Connect microphone' : 'Koble til mikrofon',
    disconnect: en ? 'End voice session' : 'Avslutt stemme',
    finalize: en ? 'Save transcript' : 'Lagre transkript',
    cleanBtn: en ? 'Clean transcript' : 'Rens transkript',
    ingest: en ? 'Add to knowledge base' : 'Legg til i kontekstbase',
    raw: en ? 'Raw transcript' : 'Rått transkript',
    cleaned: en ? 'Cleaned document' : 'Renset dokument',
  }
})

onMounted(async () => {
  auth.restore()
  const statusRes = await fetchRealtimeVoiceStatus()
  liveAvailable.value = statusRes.liveEnabled && (await voiceModelStore.ensureModelsLoaded(), voiceModelStore.hasModels)
  selectedVoice.value = statusRes.voice
  selectedReasoning.value = statusRes.reasoningEffort
})

watch(errorMessage, (msg) => {
  if (msg.trim() !== '') error.value = msg
})

async function handleFileUpload(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  busy.value = true
  error.value = ''
  try {
    document.value = await uploadInterviewDocument(file)
    pastedText.value = ''
    status.value =
      uiLang.value === 'en'
        ? `Questions file saved (${document.value.charCount} chars)`
        : `Spørsmålsfil lagret (${document.value.charCount} tegn)`
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Upload failed'
  } finally {
    busy.value = false
    input.value = ''
  }
}

async function handlePasteDocument() {
  if (!pastedText.value.trim()) return
  busy.value = true
  error.value = ''
  try {
    document.value = await createInterviewTextDocument(pastedText.value.trim(), pasteFilename.value)
    const n = countQuestions(pastedText.value)
    status.value =
      uiLang.value === 'en'
        ? `Questions saved (${n} lines)`
        : `Spørsmål lagret (${n} linjer)`
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Create failed'
  } finally {
    busy.value = false
  }
}

async function ensureQuestionsDocument(): Promise<InterviewDocument | null> {
  if (document.value) return document.value
  if (!pastedText.value.trim()) return null
  document.value = await createInterviewTextDocument(pastedText.value.trim(), pasteFilename.value)
  return document.value
}

async function startInterview() {
  if (!canStartInterview.value) return
  busy.value = true
  error.value = ''
  try {
    const doc = await ensureQuestionsDocument()
    if (!doc) return
    const session = await createInterviewSession(doc.id, uiLang.value, selectedVoice.value)
    sessionId.value = session.id
    resetTurns()
    step.value = 'interview'
    status.value = 'Sesjon opprettet'
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Session failed'
  } finally {
    busy.value = false
  }
}

async function goToTranscriptStep() {
  await disconnectAndFlush()
  step.value = 'transcript'
}

async function saveTranscript() {
  if (!sessionId.value) return
  busy.value = true
  error.value = ''
  try {
    await flushTurns()
    transcript.value = await finalizeInterviewSession(sessionId.value)
    step.value = 'clean'
    status.value = 'Transkript lagret'
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Finalize failed'
  } finally {
    busy.value = false
  }
}

async function runClean() {
  if (!transcript.value) return
  busy.value = true
  error.value = ''
  try {
    transcript.value = await cleanInterviewTranscript(transcript.value.id)
    status.value = 'Transkript renset'
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Clean failed'
  } finally {
    busy.value = false
  }
}

async function runIngest() {
  if (!transcript.value) return
  if (!confirm(uiLang.value === 'en' ? 'Add cleaned transcript to the knowledge base?' : 'Legge renset transkript til i kontekstbasen?')) {
    return
  }
  busy.value = true
  error.value = ''
  try {
    const result = await ingestInterviewTranscript(transcript.value.id, false)
    status.value = `Ingest fullført: ${JSON.stringify(result)}`
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Ingest failed'
  } finally {
    busy.value = false
  }
}
</script>

<template>
  <div class="min-h-screen bg-[hsl(220_20%_97%)] text-[hsl(220_25%_10%)] font-sans antialiased pb-12">
    <nav
      class="border-b border-gray-200/80 bg-white/90 backdrop-blur-sm px-4 py-3 text-sm flex flex-wrap gap-x-4 gap-y-1 items-center max-w-3xl mx-auto"
    >
      <RouterLink to="/" class="text-blue-600 hover:underline">Hjem</RouterLink>
      <span class="text-gray-500">/</span>
      <RouterLink to="/admin/tools" class="text-blue-600 hover:underline">Internal tools</RouterLink>
      <span class="text-gray-500">/</span>
      <strong class="text-gray-900">{{ copy.title }}</strong>
    </nav>

    <main id="main-content" class="mx-auto max-w-3xl px-4 pt-8">
      <h1 class="text-2xl font-semibold tracking-tight text-gray-900 mb-2">{{ copy.title }}</h1>
      <p class="text-sm text-gray-600 mb-6">{{ copy.intro }}</p>

      <p v-if="status" class="text-sm text-green-700 mb-4">{{ status }}</p>
      <p v-if="error" class="text-sm text-red-700 mb-4">{{ error }}</p>

      <section v-if="step === 'source'" class="space-y-6 rounded-xl border border-gray-200 bg-white p-5 shadow-sm">
        <h2 class="text-lg font-semibold">{{ copy.source }}</h2>

        <div>
          <label class="block text-sm font-medium mb-1">{{ copy.lang }}</label>
          <select v-model="uiLang" class="border rounded px-2 py-1 text-sm">
            <option value="no">Norsk</option>
            <option value="en">English</option>
          </select>
        </div>

        <div>
          <label class="block text-sm font-medium mb-1">{{ copy.upload }}</label>
          <input type="file" accept=".pdf,.txt,.md,.doc,.docx" class="text-sm" @change="handleFileUpload" />
        </div>

        <div>
          <label class="block text-sm font-medium mb-1">{{ copy.paste }}</label>
          <input v-model="pasteFilename" class="border rounded px-2 py-1 text-sm mb-2 w-full max-w-xs" />
          <textarea
            v-model="pastedText"
            rows="8"
            class="w-full border rounded p-2 text-sm font-mono"
            :placeholder="copy.pastePlaceholder"
          />
          <button
            type="button"
            class="mt-2 rounded bg-gray-800 text-white px-3 py-1.5 text-sm disabled:opacity-50"
            :disabled="busy || !pastedText.trim()"
            @click="handlePasteDocument"
          >
            {{ copy.saveText }}
          </button>
        </div>

        <p v-if="document && activeQuestionCount != null && activeQuestionCount > 0" class="text-sm text-gray-700">
          {{ copy.activeQuestions }}: {{ activeQuestionCount }} {{ copy.lines }}
        </p>
        <p v-else-if="document" class="text-sm text-gray-700">
          {{ copy.activeFile }}: <code class="bg-gray-100 px-1 rounded">{{ document.originalFilename }}</code>
          ({{ document.charCount }} {{ uiLang === 'en' ? 'chars' : 'tegn' }})
        </p>

        <button
          type="button"
          class="rounded bg-blue-600 text-white px-4 py-2 text-sm font-medium disabled:opacity-50"
          :disabled="busy || !canStartInterview"
          @click="startInterview"
        >
          <Loader2 v-if="busy" class="inline size-4 animate-spin mr-1" />
          {{ copy.start }}
        </button>
      </section>

      <section v-else-if="step === 'interview'" class="space-y-4 rounded-xl border border-gray-200 bg-white p-5 shadow-sm">
        <h2 class="text-lg font-semibold">{{ copy.interview }}</h2>

        <p v-if="liveAvailable === false" class="text-sm text-amber-800">
          Realtime voice er ikke tilgjengelig. Sjekk PORTFOLIO_REALTIME_ENABLED og OPENAI_API_KEY.
        </p>

        <div class="flex flex-wrap gap-2 items-center">
          <label class="text-sm">Stemme</label>
          <select v-model="selectedVoice" class="border rounded px-2 py-1 text-sm" :disabled="connectionState !== 'idle'">
            <option value="marin">Marin</option>
            <option value="cedar">Cedar</option>
          </select>
        </div>

        <div class="flex flex-wrap gap-2">
          <button
            v-if="connectionState === 'idle' || connectionState === 'error'"
            type="button"
            class="inline-flex items-center gap-1 rounded bg-blue-600 text-white px-3 py-2 text-sm"
            :disabled="liveAvailable === false"
            @click="connect"
          >
            <Mic class="size-4" /> {{ copy.connect }}
          </button>
          <button
            v-else
            type="button"
            class="inline-flex items-center gap-1 rounded bg-gray-700 text-white px-3 py-2 text-sm"
            @click="disconnectAndFlush"
          >
            <MicOff class="size-4" /> {{ copy.disconnect }}
          </button>
          <button
            v-if="isModelSpeaking"
            type="button"
            class="inline-flex items-center gap-1 rounded border px-3 py-2 text-sm"
            @click="stopResponse"
          >
            <Square class="size-4" /> Stopp
          </button>
        </div>

        <p v-if="connectionState === 'connecting'" class="text-sm text-gray-600">Kobler til…</p>
        <p v-if="sessionNotice" class="text-sm text-amber-800">{{ sessionNotice }}</p>

        <div class="grid gap-3 sm:grid-cols-2">
          <div class="rounded border p-3 text-sm">
            <p class="font-medium text-gray-700 mb-1">Du</p>
            <p class="text-gray-900 whitespace-pre-wrap">{{ userTranscript || '—' }}</p>
          </div>
          <div class="rounded border p-3 text-sm">
            <p class="font-medium text-gray-700 mb-1">Intervjuer</p>
            <p class="text-gray-900 whitespace-pre-wrap">{{ assistantTranscript || '—' }}</p>
          </div>
        </div>

        <button
          type="button"
          class="rounded border border-blue-600 text-blue-700 px-4 py-2 text-sm"
          @click="goToTranscriptStep"
        >
          Gå videre til transkript →
        </button>
      </section>

      <section v-else-if="step === 'transcript'" class="space-y-4 rounded-xl border border-gray-200 bg-white p-5 shadow-sm">
        <h2 class="text-lg font-semibold">{{ copy.transcript }}</h2>
        <ul class="space-y-2 text-sm max-h-96 overflow-y-auto">
          <li v-for="(t, i) in committedTurns" :key="i" class="rounded border p-2">
            <span class="font-medium">{{ t.role === 'user' ? 'Kevin' : 'Intervjuer' }}:</span>
            {{ t.text }}
          </li>
        </ul>
        <button
          type="button"
          class="rounded bg-blue-600 text-white px-4 py-2 text-sm disabled:opacity-50"
          :disabled="busy || committedTurns.length === 0"
          @click="saveTranscript"
        >
          {{ copy.finalize }}
        </button>
      </section>

      <section v-else class="space-y-4 rounded-xl border border-gray-200 bg-white p-5 shadow-sm">
        <h2 class="text-lg font-semibold">{{ copy.clean }}</h2>
        <div class="grid gap-4 sm:grid-cols-2">
          <div>
            <h3 class="text-sm font-medium mb-1">{{ copy.raw }}</h3>
            <pre class="text-xs bg-gray-50 border rounded p-2 max-h-64 overflow-auto whitespace-pre-wrap">{{ transcript?.rawText }}</pre>
          </div>
          <div>
            <h3 class="text-sm font-medium mb-1">{{ copy.cleaned }}</h3>
            <pre class="text-xs bg-gray-50 border rounded p-2 max-h-64 overflow-auto whitespace-pre-wrap">{{ transcript?.cleanedText || '—' }}</pre>
          </div>
        </div>
        <div class="flex flex-wrap gap-2">
          <button
            type="button"
            class="rounded bg-gray-800 text-white px-4 py-2 text-sm disabled:opacity-50"
            :disabled="busy || !transcript"
            @click="runClean"
          >
            {{ copy.cleanBtn }}
          </button>
          <button
            type="button"
            class="rounded bg-green-700 text-white px-4 py-2 text-sm disabled:opacity-50"
            :disabled="busy || !transcript?.cleanedText"
            @click="runIngest"
          >
            {{ copy.ingest }}
          </button>
        </div>
      </section>
    </main>
  </div>
</template>
