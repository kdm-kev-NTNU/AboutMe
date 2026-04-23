<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed, watch } from 'vue'
import { RouterLink } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()

type EvalDatasetSummary = { id: string; name: string; exampleCount: number }
type ChatModelOption = { id: string; label: string; provider: string }
type RunSummary = {
  id: number
  name: string
  datasetName: string
  generatorModel: string
  evaluatorModel: string
  status: string
  totalExamples: number
  meanFaithfulness: number | null
  meanRelevance: number | null
  meanCorrectness: number | null
  meanConciseness: number | null
  errorMessage: string | null
  createdAt: string
  completedAt: string | null
}
type ExperimentResultRow = {
  id: number
  question: string
  referenceAnswer: string
  ragResponse: string
  documentsPreview: string | null
  faithfulness: number | null
  relevance: number | null
  correctness: number | null
  conciseness: number | null
  faithfulnessExplanation: string | null
  relevanceExplanation: string | null
  correctnessExplanation: string | null
  concisenessExplanation: string | null
}
type RunDetail = RunSummary & {
  evalDatasetId: number | null
  posthogHost: string
  results: ExperimentResultRow[]
}

type DocumentEntry = { documentId: string; filename: string; chunkCount: number; lastIngestedAt: string }

type GenerationStatus = {
  id: number
  status: string
  questionsGenerated: number | null
  resultDatasetId: string | null
  errorMessage: string | null
  createdAt: string | null
  completedAt: string | null
}

const API = '/api/admin/tools/experiments'
const DOC_API = '/api/admin/tools/documents'

function authHeaders(): HeadersInit {
  const h: Record<string, string> = { 'Content-Type': 'application/json' }
  if (auth.basicToken) h.Authorization = `Basic ${auth.basicToken}`
  return h
}

async function fetchJson<T>(url: string, init?: RequestInit): Promise<{ ok: boolean; status: number; data: T }> {
  const r = await fetch(url, { ...init, headers: { ...authHeaders(), ...(init?.headers as Record<string, string>) } })
  const data = (await r.json().catch(() => ({}))) as T
  return { ok: r.ok, status: r.status, data }
}

/** Backend LLM capture to PostHog ($ai_generation); optional. Eval datasets live in PostgreSQL. */
const posthogCaptureConfigured = ref(false)
const posthogIngestHost = ref('')
const datasets = ref<EvalDatasetSummary[]>([])
const datasetsLoading = ref(false)
const datasetsError = ref('')

const documents = ref<DocumentEntry[]>([])
const documentsLoading = ref(false)
const documentsError = ref('')

const genName = ref('')
const genModel = ref('')
const genDocumentId = ref('') /** empty = alle dokumenter */
const genQuestionsPerChunk = ref('1')
const genMaxQuestions = ref('')
const genSeed = ref('')
const genBusy = ref(false)
const genMessage = ref('')
const genError = ref('')
const genPollTimer = ref<ReturnType<typeof setInterval> | null>(null)

const models = ref<ChatModelOption[]>([])
const modelsLoading = ref(false)

const selectedDatasetId = ref('')
const generatorModel = ref('')
const evaluatorModel = ref('')
/** Optional cap; empty string = use full dataset */
const maxExamplesInput = ref('')

const runBusy = ref(false)
const runMessage = ref('')
const runError = ref('')
const lastRunId = ref<number | null>(null)
const pollTimer = ref<ReturnType<typeof setInterval> | null>(null)

const runs = ref<RunSummary[]>([])
const runsLoading = ref(false)
const selectedRunDetail = ref<RunDetail | null>(null)
const detailLoading = ref(false)

const baselineLabel = computed(() => {
  const d = datasets.value.find((x) => x.id === selectedDatasetId.value)
  return d ? `${d.name} (${d.exampleCount})` : 'Velg datasett'
})

const posthogAppBase = computed(() => posthogAppBaseUrl(posthogIngestHost.value))

/** Map ingest host (e.g. https://eu.i.posthog.com) to browser app origin (https://eu.posthog.com). */
function posthogAppBaseUrl(ingestHost: string): string {
  const t = ingestHost.trim()
  if (!t) return ''
  try {
    const u = new URL(t)
    const host = u.hostname.replace(/\.i\.posthog\.com$/i, '.posthog.com')
    return `${u.protocol}//${host}`
  } catch {
    return ''
  }
}

/** Evaluator must be from a different LLM vendor than the generator (avoid model-family bias). */
const evaluatorModels = computed(() => {
  const gen = models.value.find((m) => m.id === generatorModel.value)
  if (!gen) return models.value
  return models.value.filter((m) => m.provider !== gen.provider)
})

const crossFamilyPairAvailable = computed(() => {
  if (!models.value.length) return true
  return evaluatorModels.value.length > 0
})

function pickCrossFamilyEvaluatorId(all: ChatModelOption[], genId: string): string | null {
  const gen = all.find((m) => m.id === genId) ?? all[0]
  if (!gen) return null
  const other = all.find((m) => m.provider !== gen.provider)
  return other?.id ?? null
}

function syncEvaluatorToCrossFamily() {
  const all = models.value
  if (!all.length) return
  const gen = all.find((m) => m.id === generatorModel.value)
  const ev = all.find((m) => m.id === evaluatorModel.value)
  if (!gen || !ev || gen.provider === ev.provider) {
    const nextId = pickCrossFamilyEvaluatorId(all, generatorModel.value)
    if (nextId) evaluatorModel.value = nextId
    else evaluatorModel.value = ''
  }
}

watch(generatorModel, () => {
  syncEvaluatorToCrossFamily()
})

function formatScore(v: number | null | undefined) {
  if (v == null || Number.isNaN(v)) return '–'
  return v.toFixed(3)
}

async function loadConfig() {
  const { ok, data } = await fetchJson<{ posthogConfigured: boolean; posthogHost: string }>(`${API}/config`)
  if (ok && data) {
    posthogCaptureConfigured.value = !!data.posthogConfigured
    posthogIngestHost.value = data.posthogHost || ''
  }
}

async function loadDatasets() {
  datasetsLoading.value = true
  datasetsError.value = ''
  try {
    const { ok, status, data } = await fetchJson<EvalDatasetSummary[] | { error?: string }>(`${API}/datasets`)
    if (!ok) {
      datasetsError.value =
        typeof (data as { error?: string }).error === 'string'
          ? (data as { error: string }).error
          : `Kunne ikke hente datasett (${status})`
      datasets.value = []
      return
    }
    datasets.value = Array.isArray(data) ? data : []
  } finally {
    datasetsLoading.value = false
  }
}

async function loadModels() {
  modelsLoading.value = true
  try {
    const { ok, data } = await fetchJson<ChatModelOption[]>(`${API}/models`)
    if (ok && Array.isArray(data)) {
      models.value = data
      if (!generatorModel.value && data.length) generatorModel.value = data[0].id
      if (!genModel.value && data.length) genModel.value = data[0].id
      syncEvaluatorToCrossFamily()
    }
  } finally {
    modelsLoading.value = false
  }
}

async function loadDocuments() {
  documentsLoading.value = true
  documentsError.value = ''
  try {
    const { ok, status, data } = await fetchJson<DocumentEntry[] | { error?: string }>(DOC_API)
    if (!ok) {
      documentsError.value =
        typeof (data as { error?: string }).error === 'string'
          ? (data as { error: string }).error
          : `Kunne ikke hente dokumenter (${status})`
      documents.value = []
      return
    }
    documents.value = Array.isArray(data) ? data : []
  } finally {
    documentsLoading.value = false
  }
}

function stopGenPoll() {
  if (genPollTimer.value) clearInterval(genPollTimer.value)
  genPollTimer.value = null
}

function startGenPoll(generationId: number) {
  stopGenPoll()
  genPollTimer.value = setInterval(async () => {
    const { ok, data } = await fetchJson<GenerationStatus>(`${API}/datasets/generate/${generationId}/status`)
    if (!ok || !data) return
    if (data.questionsGenerated != null) {
      genMessage.value = `Genererer… (${data.questionsGenerated} spørsmål hittil)`
    }
    if (data.status === 'COMPLETED') {
      stopGenPoll()
      genBusy.value = false
      genMessage.value = `Datasett opprettet (id ${data.resultDatasetId ?? ''}).`
      await loadDatasets()
      if (data.resultDatasetId) selectedDatasetId.value = data.resultDatasetId
    } else if (data.status === 'FAILED') {
      stopGenPoll()
      genBusy.value = false
      genError.value = data.errorMessage || 'Generering feilet.'
    }
  }, 2000)
}

async function startDatasetGeneration() {
  genError.value = ''
  genMessage.value = ''
  if (!genName.value.trim()) {
    genError.value = 'Angi et navn på datasettet.'
    return
  }
  if (!genModel.value) {
    genError.value = 'Velg en modell for generering.'
    return
  }
  const qpc = Number.parseInt(genQuestionsPerChunk.value.trim(), 10)
  const body: Record<string, unknown> = {
    name: genName.value.trim(),
    description: '',
    documentId: genDocumentId.value.trim() || null,
    model: genModel.value,
    questionsPerChunk: Number.isFinite(qpc) && qpc > 0 ? qpc : 1,
  }
  const maxQ = genMaxQuestions.value.trim()
  if (maxQ !== '') {
    const n = Number.parseInt(maxQ, 10)
    if (Number.isFinite(n) && n > 0) body.maxQuestions = n
  }
  const seedStr = genSeed.value.trim()
  if (seedStr !== '') {
    const s = Number.parseInt(seedStr, 10)
    if (Number.isFinite(s)) body.seed = s
  }
  genBusy.value = true
  try {
    const { ok, status, data } = await fetchJson<{ generationId?: number; status?: string; error?: string }>(
      `${API}/datasets/generate`,
      { method: 'POST', body: JSON.stringify(body) },
    )
    if (!ok) {
      genError.value = (data as { error?: string })?.error || `Start feilet (${status})`
      return
    }
    const id = (data as { generationId: number }).generationId
    genMessage.value = `QRA-generering startet (jobb #${id}). Poller status…`
    startGenPoll(id)
  } finally {
    if (!genPollTimer.value) genBusy.value = false
  }
}

async function loadRuns() {
  runsLoading.value = true
  try {
    const { ok, data } = await fetchJson<RunSummary[]>(`${API}/runs`)
    if (ok && Array.isArray(data)) runs.value = data
  } finally {
    runsLoading.value = false
  }
}

async function deleteDataset() {
  if (!selectedDatasetId.value) return
  if (!confirm('Slette datasettet permanent fra databasen? Kan ikke angres.')) return
  const { ok, status, data } = await fetchJson<{ error?: string }>(`${API}/datasets/${encodeURIComponent(selectedDatasetId.value)}`, {
    method: 'DELETE',
  })
  if (!ok) {
    runError.value = (data as { error?: string })?.error || `Sletting feilet (${status})`
    return
  }
  selectedDatasetId.value = ''
  await loadDatasets()
}

async function startRun() {
  runError.value = ''
  runMessage.value = ''
  if (!selectedDatasetId.value) {
    runError.value = 'Velg et datasett.'
    return
  }
  if (!generatorModel.value || !evaluatorModel.value) {
    runError.value = 'Velg generator- og evaluator-modell.'
    return
  }
  const genOpt = models.value.find((m) => m.id === generatorModel.value)
  const evOpt = models.value.find((m) => m.id === evaluatorModel.value)
  if (genOpt && evOpt && genOpt.provider === evOpt.provider) {
    runError.value =
      'Generator og evaluator må være fra ulike leverandører (OpenAI vs Anthropic) for å unngå modellfamilie-bias.'
    return
  }
  if (!crossFamilyPairAvailable.value) {
    runError.value =
      'Krever minst én modell fra hver leverandør (OpenAI og Anthropic). Sjekk at begge API-nøkler er satt i backend.'
    return
  }
  const d = datasets.value.find((x) => x.id === selectedDatasetId.value)
  const body = {
    datasetId: selectedDatasetId.value,
    datasetName: d?.name ?? '',
    generatorModel: generatorModel.value,
    evaluatorModel: evaluatorModel.value,
    maxExamples:
      maxExamplesInput.value.trim() === '' ? null : Number.parseInt(maxExamplesInput.value.trim(), 10) || null,
  }
  runBusy.value = true
  try {
    const { ok, status, data } = await fetchJson<{ runId?: number; error?: string }>(`${API}/run`, {
      method: 'POST',
      body: JSON.stringify(body),
    })
    if (!ok) {
      runError.value = (data as { error?: string })?.error || `Start feilet (${status})`
      return
    }
    const id = (data as { runId: number }).runId
    lastRunId.value = id
    runMessage.value = `Kjøring startet (run id ${id}). Poller status…`
    startPoll(id)
    await loadRuns()
  } finally {
    runBusy.value = false
  }
}

function startPoll(runId: number) {
  if (pollTimer.value) clearInterval(pollTimer.value)
  pollTimer.value = setInterval(async () => {
    const { ok, data } = await fetchJson<RunSummary>(`${API}/runs/${runId}/status`)
    if (!ok || !data) return
    if (data.status === 'COMPLETED' || data.status === 'FAILED') {
      if (pollTimer.value) clearInterval(pollTimer.value)
      pollTimer.value = null
      const tail =
        data.status === 'COMPLETED' && posthogCaptureConfigured.value && posthogAppBase.value
          ? ' Se også LLM-hendelser i PostHog.'
          : ''
      runMessage.value =
        data.status === 'COMPLETED' ? `Ferdig. Se resultater under.${tail}` : `Feilet: ${data.errorMessage || ''}`
      await loadRuns()
      await openRunDetail(runId)
    }
  }, 2000)
}

async function openRunDetail(id: number) {
  detailLoading.value = true
  selectedRunDetail.value = null
  try {
    const { ok, data } = await fetchJson<RunDetail>(`${API}/runs/${id}`)
    if (ok) selectedRunDetail.value = data as RunDetail
  } finally {
    detailLoading.value = false
  }
}

onMounted(() => {
  auth.restore()
  loadConfig()
  loadDatasets()
  loadDocuments()
  loadModels()
  loadRuns()
})

onUnmounted(() => {
  if (pollTimer.value) clearInterval(pollTimer.value)
  stopGenPoll()
})
</script>

<template>
  <div class="min-h-screen bg-[hsl(220_20%_97%)] text-[hsl(220_25%_10%)] font-sans antialiased pb-12">
    <nav
      class="border-b border-gray-200/80 bg-white/90 backdrop-blur-sm px-4 py-3 text-sm flex flex-wrap gap-x-4 gap-y-1 items-center max-w-5xl mx-auto"
    >
      <RouterLink to="/" class="text-blue-600 hover:underline">Hjem</RouterLink>
      <span class="text-gray-500">/</span>
      <RouterLink to="/admin/tools" class="text-blue-600 hover:underline">Internal tools</RouterLink>
      <span class="text-gray-500">/</span>
      <strong class="text-gray-900">Experiments</strong>
    </nav>

    <main class="mx-auto max-w-5xl px-4 pt-8 space-y-8">
      <div>
        <h1 class="text-2xl font-semibold tracking-tight text-gray-900 mb-2">RAG-experiments (datasett + dommer)</h1>
        <p class="text-sm text-gray-600 leading-relaxed max-w-3xl">
          Velg et eval-datasett lagret i PostgreSQL, kjør AboutMe-RAG per spørsmål med valgt modell, og få LLM-as-judge-scorer
          (faithfulness, relevance, correctness, conciseness) lagret i databasen. Valgfritt: samme LLM-kall kan også sendes som
          <code class="text-xs bg-gray-100 px-1 rounded">$ai_generation</code>
          til PostHog når
          <code class="text-xs bg-gray-100 px-1 rounded">POSTHOG_*</code>
          er satt på backend.
        </p>
      </div>

      <section
        class="rounded-xl border border-gray-200 bg-white p-4 shadow-[0_1px_3px_rgb(0_0_0/0.06)]"
        v-if="!posthogCaptureConfigured && !datasetsLoading"
      >
        <p class="text-sm text-amber-800">
          PostHog LLM-hendelser fra backend er ikke aktivert. Eksperiment-flyten og datasett fungerer likevel. For spor i PostHog,
          sett <code class="text-xs bg-gray-100 px-1 rounded">POSTHOG_ENABLED=true</code>,
          <code class="text-xs bg-gray-100 px-1 rounded">POSTHOG_API_KEY</code> og
          <code class="text-xs bg-gray-100 px-1 rounded">POSTHOG_HOST</code> (se
          <code class="text-xs">.env.example</code>).
        </p>
      </section>

      <section class="rounded-xl border border-gray-200 bg-white p-5 shadow-[0_1px_3px_rgb(0_0_0/0.06)]">
        <h2 class="text-lg font-semibold text-gray-900 mb-3">1. Eval-datasett (PostgreSQL)</h2>
        <div class="flex flex-wrap gap-2 items-center mb-2">
          <select
            v-model="selectedDatasetId"
            data-testid="exp-dataset-select"
            class="border border-gray-300 rounded-md px-2 py-1.5 text-sm min-w-[14rem] bg-white"
            :disabled="datasetsLoading"
          >
            <option value="">Velg datasett</option>
            <option v-for="d in datasets" :key="d.id" :value="d.id">{{ d.name }} ({{ d.exampleCount }})</option>
          </select>
          <button
            type="button"
            class="text-sm px-3 py-1.5 rounded-md border border-gray-300 bg-white hover:bg-gray-50"
            @click="loadDatasets"
            :disabled="datasetsLoading"
          >
            Oppdater liste
          </button>
          <button
            type="button"
            class="text-sm px-3 py-1.5 rounded-md border border-red-200 text-red-800 hover:bg-red-50"
            @click="deleteDataset"
            :disabled="!selectedDatasetId"
          >
            Slett valgt
          </button>
        </div>
        <p v-if="datasetsLoading" class="text-sm text-gray-500">Laster datasett…</p>
        <p v-if="datasetsError" class="text-sm text-red-700">{{ datasetsError }}</p>
        <p class="text-sm text-gray-600 mt-2">
          <span class="font-medium">Baseline:</span> {{ baselineLabel }}
        </p>
        <p v-if="posthogCaptureConfigured && posthogAppBase" class="text-sm mt-2">
          <a :href="posthogAppBase" target="_blank" rel="noopener noreferrer" class="text-blue-600 hover:underline"
            >Åpne PostHog (LLM-observabilitet og produktanalyse)</a
          >
        </p>
      </section>

      <section class="rounded-xl border border-gray-200 bg-white p-5 shadow-[0_1px_3px_rgb(0_0_0/0.06)]">
        <h2 class="text-lg font-semibold text-gray-900 mb-2">1b. Generer eval-datasett (QRA)</h2>
        <p class="text-sm text-gray-600 mb-4 max-w-3xl leading-relaxed">
          Opprett syntetiske spørsmål og referansesvar fra tekst-chunks i pgvector (samme idé som Piscada QRA-pipeline): velg modell,
          filtrer valgfritt på ett dokument, og kjør asynkron generering. Når jobben er ferdig, dukker datasettet opp i listen over.
        </p>
        <div class="grid gap-3 sm:grid-cols-2 max-w-2xl">
          <div class="sm:col-span-2">
            <label class="block text-xs font-medium text-gray-700 mb-1">Datasett-navn</label>
            <input
              v-model="genName"
              type="text"
              class="w-full border border-gray-300 rounded-md px-2 py-1.5 text-sm"
              placeholder="f.eks. portfolio-eval-v1"
            />
          </div>
          <div>
            <label class="block text-xs font-medium text-gray-700 mb-1">Modell (generering)</label>
            <select
              v-model="genModel"
              data-testid="gen-model-select"
              class="w-full border border-gray-300 rounded-md px-2 py-1.5 text-sm bg-white"
              :disabled="modelsLoading"
            >
              <option v-for="m in models" :key="'gen-' + m.id" :value="m.id">{{ m.label }} ({{ m.id }})</option>
            </select>
          </div>
          <div>
            <label class="block text-xs font-medium text-gray-700 mb-1">Dokument (valgfritt)</label>
            <select
              v-model="genDocumentId"
              data-testid="gen-doc-select"
              class="w-full border border-gray-300 rounded-md px-2 py-1.5 text-sm bg-white"
              :disabled="documentsLoading"
            >
              <option value="">Alle dokumenter</option>
              <option v-for="d in documents" :key="d.documentId" :value="d.documentId">
                {{ d.filename }} ({{ d.chunkCount }} chunks)
              </option>
            </select>
          </div>
          <div>
            <label class="block text-xs font-medium text-gray-700 mb-1">Spørsmål per chunk</label>
            <input v-model="genQuestionsPerChunk" type="number" min="1" class="w-full border border-gray-300 rounded-md px-2 py-1.5 text-sm" />
          </div>
          <div>
            <label class="block text-xs font-medium text-gray-700 mb-1">Maks antall spørsmål totalt (valgfritt)</label>
            <input
              v-model="genMaxQuestions"
              type="number"
              min="1"
              placeholder="Standard: alle mulige fra chunks"
              class="w-full border border-gray-300 rounded-md px-2 py-1.5 text-sm"
            />
          </div>
          <div class="sm:col-span-2">
            <label class="block text-xs font-medium text-gray-700 mb-1">Seed (valgfritt, reproduserbar shuffle)</label>
            <input v-model="genSeed" type="number" class="w-full max-w-xs border border-gray-300 rounded-md px-2 py-1.5 text-sm" />
          </div>
        </div>
        <div class="flex flex-wrap gap-2 mt-3">
          <button
            type="button"
            class="text-sm font-medium px-4 py-2 rounded-md bg-indigo-600 text-white hover:bg-indigo-700 disabled:opacity-50"
            @click="startDatasetGeneration"
            :disabled="genBusy || modelsLoading"
          >
            Generer datasett
          </button>
          <button
            type="button"
            class="text-sm px-3 py-2 rounded-md border border-gray-300 bg-white hover:bg-gray-50"
            @click="loadDocuments"
            :disabled="documentsLoading"
          >
            Oppdater dokumenter
          </button>
        </div>
        <p v-if="documentsLoading" class="text-sm text-gray-500 mt-2">Laster dokumentliste…</p>
        <p v-if="documentsError" class="text-sm text-red-700 mt-2">{{ documentsError }}</p>
        <p v-if="genMessage" class="text-sm text-green-800 mt-3">{{ genMessage }}</p>
        <p v-if="genError" class="text-sm text-red-700 mt-3">{{ genError }}</p>
      </section>

      <section class="rounded-xl border border-gray-200 bg-white p-5 shadow-[0_1px_3px_rgb(0_0_0/0.06)]">
        <h2 class="text-lg font-semibold text-gray-900 mb-3">2. Kjør experiment</h2>
        <div class="grid gap-4 sm:grid-cols-2 max-w-xl">
          <div>
            <label class="block text-xs font-medium text-gray-700 mb-1">Generator (RAG)</label>
            <select
              v-model="generatorModel"
              data-testid="exp-generator-select"
              class="w-full border border-gray-300 rounded-md px-2 py-1.5 text-sm bg-white"
              :disabled="modelsLoading"
            >
              <option v-for="m in models" :key="'g-' + m.id" :value="m.id">{{ m.label }} ({{ m.id }})</option>
            </select>
          </div>
          <div>
            <label class="block text-xs font-medium text-gray-700 mb-1">Evaluator (dommer)</label>
            <select
              v-model="evaluatorModel"
              data-testid="exp-evaluator-select"
              class="w-full border border-gray-300 rounded-md px-2 py-1.5 text-sm bg-white"
              :disabled="modelsLoading || !evaluatorModels.length"
            >
              <option v-for="m in evaluatorModels" :key="'e-' + m.id" :value="m.id">{{ m.label }} ({{ m.id }})</option>
            </select>
          </div>
          <p class="sm:col-span-2 text-xs text-gray-600 leading-relaxed">
            For å unngå modellfamilie-bias må evaluator være fra en annen leverandør enn generator (OpenAI ↔
            Anthropic).
          </p>
          <p
            v-if="models.length && !crossFamilyPairAvailable"
            class="sm:col-span-2 text-xs text-amber-800 bg-amber-50 border border-amber-200 rounded-md px-2 py-1.5"
          >
            Krever minst én konfigurert modell fra hver leverandør. Sett både
            <code class="text-[11px] bg-white px-0.5 rounded">OPENAI_API_KEY</code> og
            <code class="text-[11px] bg-white px-0.5 rounded">ANTHROPIC_API_KEY</code> for backend.
          </p>
          <div class="sm:col-span-2">
            <label class="block text-xs font-medium text-gray-700 mb-1">Maks antall eksempler (valgfritt)</label>
            <input
              v-model="maxExamplesInput"
              type="number"
              min="1"
              placeholder="Alle i datasettet"
              class="w-full max-w-xs border border-gray-300 rounded-md px-2 py-1.5 text-sm"
            />
          </div>
        </div>
        <button
          type="button"
          class="mt-4 text-sm font-medium px-4 py-2 rounded-md bg-blue-600 text-white hover:bg-blue-700 disabled:opacity-50"
          @click="startRun"
          :disabled="runBusy || !crossFamilyPairAvailable || !evaluatorModel"
        >
          Start experiment
        </button>
        <p v-if="runMessage" class="text-sm text-green-800 mt-3">{{ runMessage }}</p>
        <p v-if="runError" class="text-sm text-red-700 mt-3">{{ runError }}</p>
      </section>

      <section class="rounded-xl border border-gray-200 bg-white p-5 shadow-[0_1px_3px_rgb(0_0_0/0.06)]">
        <div class="flex justify-between items-center mb-3">
          <h2 class="text-lg font-semibold text-gray-900">Tidligere kjøringer</h2>
          <button
            type="button"
            class="text-sm text-blue-600 hover:underline"
            @click="loadRuns"
            :disabled="runsLoading"
          >
            Oppdater
          </button>
        </div>
        <p v-if="runsLoading" class="text-sm text-gray-500">Laster…</p>
        <ul v-else class="divide-y divide-gray-100 border border-gray-100 rounded-lg overflow-hidden">
          <li v-for="r in runs" :key="r.id" class="px-3 py-2 hover:bg-gray-50 flex flex-wrap gap-2 justify-between">
            <button type="button" class="text-left text-sm text-blue-700 hover:underline font-mono" @click="openRunDetail(r.id)">
              #{{ r.id }} · {{ r.name }} · {{ r.status }}
            </button>
            <span class="text-xs text-gray-500"
              >F: {{ formatScore(r.meanFaithfulness) }} · R: {{ formatScore(r.meanRelevance) }} · C:
              {{ formatScore(r.meanCorrectness) }} · K: {{ formatScore(r.meanConciseness) }}</span
            >
          </li>
          <li v-if="!runs.length" class="px-3 py-4 text-sm text-gray-500">Ingen kjøringer ennå.</li>
        </ul>
      </section>

      <section
        v-if="selectedRunDetail || detailLoading"
        class="rounded-xl border border-gray-200 bg-white p-5 shadow-[0_1px_3px_rgb(0_0_0/0.06)]"
      >
        <h2 class="text-lg font-semibold text-gray-900 mb-2">Resultatdetalj</h2>
        <p v-if="detailLoading" class="text-sm text-gray-500">Laster…</p>
        <template v-else-if="selectedRunDetail">
          <p class="text-sm text-gray-600 mb-2">
            Status: <strong>{{ selectedRunDetail.status }}</strong> · modeller: {{ selectedRunDetail.generatorModel }} /
            {{ selectedRunDetail.evaluatorModel }}
          </p>
          <p v-if="selectedRunDetail.errorMessage" class="text-sm text-red-700 mb-2">{{ selectedRunDetail.errorMessage }}</p>
          <p v-if="selectedRunDetail.evalDatasetId != null" class="text-sm text-gray-600 mb-2">
            Eval-datasett-ID: <span class="font-mono">{{ selectedRunDetail.evalDatasetId }}</span>
          </p>
          <p v-if="posthogCaptureConfigured && posthogAppBase" class="text-sm mb-4">
            <a :href="posthogAppBase" target="_blank" rel="noopener noreferrer" class="text-blue-600 hover:underline"
              >PostHog (LLM-observabilitet)</a
            >
          </p>
          <div class="overflow-x-auto">
            <table class="min-w-full text-xs border-collapse">
              <thead>
                <tr class="bg-gray-50 text-left">
                  <th class="p-2 border border-gray-200">#</th>
                  <th class="p-2 border border-gray-200">Spørsmål</th>
                  <th class="p-2 border border-gray-200">F</th>
                  <th class="p-2 border border-gray-200">R</th>
                  <th class="p-2 border border-gray-200">C</th>
                  <th class="p-2 border border-gray-200">K</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="row in selectedRunDetail.results" :key="row.id">
                  <td class="p-2 border border-gray-100 font-mono">{{ row.id }}</td>
                  <td class="p-2 border border-gray-100 max-w-xs truncate" :title="row.question">{{ row.question }}</td>
                  <td class="p-2 border border-gray-100">{{ formatScore(row.faithfulness) }}</td>
                  <td class="p-2 border border-gray-100">{{ formatScore(row.relevance) }}</td>
                  <td class="p-2 border border-gray-100">{{ formatScore(row.correctness) }}</td>
                  <td class="p-2 border border-gray-100">{{ formatScore(row.conciseness) }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </template>
      </section>
    </main>
  </div>
</template>
