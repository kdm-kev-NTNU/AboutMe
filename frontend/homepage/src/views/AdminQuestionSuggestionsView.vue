<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { RouterLink } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import {
  adminDocumentsList,
  adminDocumentsQuestionSuggestions,
  listChatModels,
  type ChatModelOption,
  type DocumentListEntry,
} from '@/api/generated/portfolio'
import { formatAdminHttpError as formatHttpError } from '@/lib/api-error'

const auth = useAuthStore()
const error = ref('')
const busy = ref(false)

const documents = ref<DocumentListEntry[]>([])
const models = ref<ChatModelOption[]>([])

/** currentChunks | uploadedJson */
const source = ref<'currentChunks' | 'uploadedJson'>('currentChunks')
const documentFilter = ref('')
const chunksJsonInput = ref('')
const selectedModelId = ref('')
const maxQuestions = ref(12)
const language = ref('Norwegian')

const suggestions = ref<string[]>([])
const modelUsed = ref('')

const pasteHint =
  'Lim inn eller last opp JSON fra «Last ned JSON (chunks)». Må inneholde et «chunks»-felt.'

async function loadDocumentsAndModels() {
  error.value = ''
  try {
    const [dRes, mRes] = await Promise.all([adminDocumentsList(), listChatModels()])
    if (dRes.status !== 200) {
      throw new Error(formatHttpError(dRes.status, dRes.data))
    }
    if (mRes.status !== 200) {
      throw new Error(`Kunne ikke hente modeller (${mRes.status})`)
    }
    documents.value = dRes.data ?? []
    models.value = mRes.data ?? []
    if (!selectedModelId.value && models.value.length > 0) {
      selectedModelId.value = models.value[0]?.id ?? ''
    }
  } catch (e: unknown) {
    error.value = e instanceof Error ? e.message : 'Ukjent feil'
  }
}

function onJsonFile(ev: Event) {
  const input = ev.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  const reader = new FileReader()
  reader.onload = () => {
    chunksJsonInput.value =
      typeof reader.result === 'string' ? reader.result : String(reader.result ?? '')
  }
  reader.readAsText(file, 'UTF-8')
  input.value = ''
}

async function generateSuggestions() {
  error.value = ''
  suggestions.value = []
  modelUsed.value = ''
  busy.value = true
  try {
    if (!selectedModelId.value.trim()) {
      throw new Error('Velg en modell')
    }
    const body = {
      source: source.value,
      model: selectedModelId.value.trim(),
      maxQuestions: maxQuestions.value,
      language: language.value.trim() || 'Norwegian',
      documentId:
        source.value === 'currentChunks' && documentFilter.value.trim()
          ? documentFilter.value.trim()
          : undefined,
      chunksJson:
        source.value === 'uploadedJson' && chunksJsonInput.value.trim()
          ? chunksJsonInput.value.trim()
          : undefined,
    }
    if (body.source === 'uploadedJson' && !body.chunksJson) {
      throw new Error('Lim inn eller last opp chunk-JSON')
    }
    const res = await adminDocumentsQuestionSuggestions(body)
    if (res.status === 400) {
      throw new Error(formatHttpError(res.status, res.data))
    }
    if (res.status !== 200 || !res.data) {
      throw new Error(formatHttpError(res.status, res.data))
    }
    suggestions.value = res.data.suggestions ?? []
    modelUsed.value = res.data.modelUsed ?? ''
  } catch (e: unknown) {
    error.value = e instanceof Error ? e.message : 'Ukjent feil'
  } finally {
    busy.value = false
  }
}

async function copySuggestionsJson() {
  try {
    await navigator.clipboard.writeText(JSON.stringify(suggestions.value, null, 2))
  } catch {
    error.value = 'Klarte ikke kopiere til utklippstavle'
  }
}

async function copySuggestionsLines() {
  try {
    await navigator.clipboard.writeText(suggestions.value.join('\n'))
  } catch {
    error.value = 'Klarte ikke kopiere til utklippstavle'
  }
}

/** Only gate on model so missing uploaded JSON still runs submit → inline error (not a silent no-op). */
const canSubmit = computed(() => !!selectedModelId.value.trim())

onMounted(() => {
  auth.restore()
  void loadDocumentsAndModels()
})
</script>

<template>
  <div class="min-h-screen bg-[hsl(220_20%_97%)] text-[hsl(220_25%_10%)] font-sans antialiased pb-12">
    <nav
      class="border-b border-gray-200/80 bg-white/90 backdrop-blur-sm px-4 py-3 text-sm flex flex-wrap gap-x-4 gap-y-1 items-center max-w-3xl mx-auto"
    >
      <RouterLink to="/" class="text-blue-600 hover:underline">Hjem</RouterLink>
      <RouterLink to="/admin/tools" class="text-blue-600 hover:underline">Internal tools</RouterLink>
      <span class="text-gray-500">/</span>
      <strong class="text-gray-900">Spørsmålsforslag (LLM)</strong>
    </nav>

    <main id="main-content" class="mx-auto max-w-3xl px-4 pt-8">
      <h1 class="text-2xl font-semibold tracking-tight text-gray-900 mb-2">Standard spørsmål til chatbot</h1>
      <p class="text-sm text-gray-600 mb-6 leading-relaxed">
        Bruk indekserte chunks direkte fra databasen, eller lim inn JSON eksportert fra
        <RouterLink to="/admin/chunks" class="text-blue-600 hover:underline">Chunk viewer</RouterLink>.
        LLM-et foreslår korte spørsmål besøkende kan bruke som hurtigvalg.
      </p>

      <p v-if="error" class="mb-4 text-sm text-red-700 rounded-md border border-red-200 bg-red-50 px-3 py-2">
        {{ error }}
      </p>

      <section
        class="rounded-xl border border-gray-200 bg-white p-5 shadow-[0_1px_3px_rgb(0_0_0/0.06)] space-y-4"
      >
        <div class="space-y-1">
          <label class="block text-sm font-medium text-gray-800">Datakilde</label>
          <select
            v-model="source"
            data-testid="suggestion-source"
            class="w-full border border-gray-200 rounded-lg px-3 py-2 text-sm bg-white"
          >
            <option value="currentChunks">Aktive chunks i databasen</option>
            <option value="uploadedJson">Eksportert JSON (lim inn / fil)</option>
          </select>
        </div>

        <div v-if="source === 'currentChunks'" class="space-y-1">
          <label class="block text-sm font-medium text-gray-800">Dokument (valgfritt)</label>
          <select
            v-model="documentFilter"
            class="w-full border border-gray-200 rounded-lg px-3 py-2 text-sm bg-white"
          >
            <option value="">Alle dokumenter i collection</option>
            <option v-for="d in documents" :key="d.documentId" :value="d.documentId">
              {{ d.filename }} ({{ d.chunkCount }} chunks)
            </option>
          </select>
        </div>

        <div v-else class="space-y-2">
          <label class="block text-sm font-medium text-gray-800">Chunk-JSON</label>
          <p class="text-xs text-gray-600">{{ pasteHint }}</p>
          <input
            type="file"
            accept="application/json,.json"
            class="block text-sm text-gray-700"
            @change="onJsonFile"
          />
          <textarea
            v-model="chunksJsonInput"
            rows="10"
            class="w-full border border-gray-200 rounded-lg px-3 py-2 text-xs font-mono bg-gray-50"
            placeholder="{ &quot;chunks&quot;: [ ... ] }"
          />
        </div>

        <div class="grid gap-4 sm:grid-cols-2">
          <div class="space-y-1">
            <label class="block text-sm font-medium text-gray-800">Modell</label>
            <select
              v-model="selectedModelId"
              class="w-full border border-gray-200 rounded-lg px-3 py-2 text-sm bg-white"
            >
              <option value="" disabled>Velg modell…</option>
              <option v-for="m in models" :key="m.id" :value="m.id">
                {{ m.label ?? m.id }} ({{ m.provider }})
              </option>
            </select>
          </div>
          <div class="space-y-1">
            <label class="block text-sm font-medium text-gray-800">Maks antall spørsmål</label>
            <input
              v-model.number="maxQuestions"
              type="number"
              min="3"
              max="30"
              class="w-full border border-gray-200 rounded-lg px-3 py-2 text-sm bg-white"
            />
          </div>
        </div>

        <div class="space-y-1">
          <label class="block text-sm font-medium text-gray-800">Språk for spørsmål</label>
          <input
            v-model="language"
            type="text"
            class="w-full border border-gray-200 rounded-lg px-3 py-2 text-sm bg-white"
            placeholder="Norwegian"
          />
        </div>

        <button
          type="button"
          class="px-4 py-2 rounded-lg text-sm font-medium text-white bg-[hsl(221_83%_53%)] hover:bg-[hsl(221_83%_48%)] disabled:opacity-50 cursor-pointer"
          :disabled="busy || !canSubmit"
          @click="generateSuggestions"
        >
          {{ busy ? 'Genererer…' : 'Generer forslag' }}
        </button>
      </section>

      <section v-if="suggestions.length > 0" class="mt-8 rounded-xl border border-gray-200 bg-white p-5 shadow-[0_1px_3px_rgb(0_0_0/0.06)]">
        <div class="flex flex-wrap gap-2 items-center justify-between mb-4">
          <h2 class="text-lg font-semibold text-gray-900">
            Forslag
            <span v-if="modelUsed" class="text-sm font-normal text-gray-500">({{ modelUsed }})</span>
          </h2>
          <div class="flex flex-wrap gap-2">
            <button
              type="button"
              class="px-3 py-1.5 text-xs rounded-md border border-gray-200 bg-white hover:bg-gray-50 cursor-pointer"
              @click="copySuggestionsJson"
            >
              Kopier JSON
            </button>
            <button
              type="button"
              class="px-3 py-1.5 text-xs rounded-md border border-gray-200 bg-white hover:bg-gray-50 cursor-pointer"
              @click="copySuggestionsLines"
            >
              Kopier én per linje
            </button>
          </div>
        </div>
        <ol class="list-decimal list-inside space-y-2 text-sm text-gray-800">
          <li v-for="(s, i) in suggestions" :key="i" class="pl-1">{{ s }}</li>
        </ol>
      </section>
    </main>
  </div>
</template>
