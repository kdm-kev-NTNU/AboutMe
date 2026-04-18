<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import {
  adminDocumentsCollections,
  adminDocumentsDelete,
  adminDocumentsList,
  adminDocumentsUpload,
  type ChromaCollectionsResponse,
  type DocumentListEntry,
} from '@/api/generated/portfolio'

interface ChunkItem {
  id: string
  documentTitle: string
  chunkIndex: number | null
  text: string
  metadata: Record<string, unknown>
}

interface ChunkListResponse {
  collectionName: string
  total: number
  totalMatching: number
  limit: number
  offset: number
  chunks: ChunkItem[]
}

const auth = useAuthStore()
const title = ref('')
const force = ref(false)
const busy = ref(false)
const status = ref('')
const error = ref('')
const documents = ref<DocumentListEntry[]>([])
const chromaInfo = ref<ChromaCollectionsResponse | null>(null)

const chunksData = ref<ChunkListResponse | null>(null)
const chunksBusy = ref(false)
const chunkError = ref('')
const chunkDocumentFilter = ref('')
const chunkLimit = ref(25)
const chunkOffset = ref(0)
const expandedChunkId = ref<string | null>(null)

function authHeaders(): HeadersInit {
  const h: Record<string, string> = {}
  if (auth.basicToken) {
    h['Authorization'] = `Basic ${auth.basicToken}`
  }
  return h
}

async function loadData() {
  error.value = ''
  try {
    const [dRes, cRes] = await Promise.all([adminDocumentsList(), adminDocumentsCollections()])
    if (dRes.status !== 200) {
      throw new Error(dRes.status === 401 ? 'Ikke autorisert (logg inn som admin)' : `Liste feilet (${dRes.status})`)
    }
    if (cRes.status !== 200) {
      throw new Error(`Chroma status feilet (${cRes.status})`)
    }
    documents.value = dRes.data
    chromaInfo.value = cRes.data
  } catch (e: unknown) {
    error.value = e instanceof Error ? e.message : 'Ukjent feil'
  }
}

async function upload() {
  const input = document.getElementById('ingest-file') as HTMLInputElement | null
  const file = input?.files?.[0]
  if (!file) {
    status.value = 'Velg en fil først.'
    return
  }
  busy.value = true
  status.value = ''
  error.value = ''
  try {
    const r = await adminDocumentsUpload({
      file,
      title: title.value.trim() || undefined,
      force: force.value,
    })
    if (r.status !== 200) {
      const errBody = r.data
      throw new Error(errBody?.message || `Opplasting feilet (${r.status})`)
    }
    const body = r.data
    const docIdPreview = (body.documentId ?? '').slice(0, 12)
    status.value = body.skipped
      ? `Hoppet over: ${body.message}`
      : `OK: ${body.chunksIngested ?? 0} chunks for ${body.filename ?? ''}${docIdPreview ? ` (${docIdPreview}…)` : ''}`
    if (input) input.value = ''
    await loadData()
  } catch (e: unknown) {
    error.value = e instanceof Error ? e.message : 'Ukjent feil'
  } finally {
    busy.value = false
  }
}

async function removeDoc(documentId: string) {
  if (!confirm('Slette alle chunks for dette dokumentet?')) return
  busy.value = true
  error.value = ''
  try {
    const r = await adminDocumentsDelete(documentId)
    const delStatus = r.status
    if (delStatus !== 204 && delStatus !== 400) {
      throw new Error(`Sletting feilet (${delStatus})`)
    }
    await loadData()
  } catch (e: unknown) {
    error.value = e instanceof Error ? e.message : 'Ukjent feil'
  } finally {
    busy.value = false
  }
}

function truncatePreview(text: string, max = 160) {
  if (!text) return '—'
  if (text.length <= max) return text
  return text.slice(0, max) + '…'
}

function onChunkFilterOrLimitChange() {
  chunkOffset.value = 0
  void loadChunks()
}

async function loadChunks() {
  chunksBusy.value = true
  chunkError.value = ''
  expandedChunkId.value = null
  try {
    const params = new URLSearchParams()
    params.set('limit', String(chunkLimit.value))
    params.set('offset', String(chunkOffset.value))
    if (chunkDocumentFilter.value.trim()) {
      params.set('documentId', chunkDocumentFilter.value.trim())
    }
    const res = await fetch(`/api/admin/tools/documents/chunks?${params.toString()}`, {
      headers: authHeaders(),
    })
    if (!res.ok) {
      throw new Error(res.status === 401 ? 'Ikke autorisert' : `Chunks feilet (${res.status})`)
    }
    chunksData.value = (await res.json()) as ChunkListResponse
  } catch (e: unknown) {
    chunkError.value = e instanceof Error ? e.message : 'Ukjent feil'
    chunksData.value = null
  } finally {
    chunksBusy.value = false
  }
}

function chunkPrev() {
  chunkOffset.value = Math.max(0, chunkOffset.value - chunkLimit.value)
  void loadChunks()
}

function chunkNext() {
  const d = chunksData.value
  if (!d) return
  if (chunkOffset.value + d.chunks.length >= d.totalMatching) return
  chunkOffset.value += chunkLimit.value
  void loadChunks()
}

function toggleChunkExpand(id: string) {
  expandedChunkId.value = expandedChunkId.value === id ? null : id
}

onMounted(() => {
  auth.restore()
  loadData()
})
</script>

<template>
  <div class="mx-auto max-w-5xl px-4 py-8">
    <h1 class="text-2xl font-bold text-gray-900 mb-2">Internal tools</h1>
    <p class="text-gray-600 mb-8">
      Dokumentingest til ChromaDB (Spring AI). Krever admin-innlogging.
    </p>

    <section class="mb-10 rounded-lg border border-gray-200 bg-white p-6 shadow-sm">
      <h2 class="text-lg font-semibold mb-4">ChromaDB</h2>
      <div v-if="chromaInfo" class="text-sm text-gray-700 space-y-1">
        <p>
          <span class="font-medium">Aktiv collection:</span>
          {{ chromaInfo.activeCollectionName }}
          ({{ chromaInfo.activeCollectionEmbeddingCount }} embeddings)
        </p>
        <p class="font-medium mt-3">Collections i databasen:</p>
        <ul class="list-disc list-inside text-gray-600">
          <li v-for="c in chromaInfo.collections" :key="c.id">{{ c.name }} <code class="text-xs">({{ c.id }})</code></li>
        </ul>
      </div>
      <p v-else class="text-gray-500 text-sm">Laster…</p>
      <button
        type="button"
        class="mt-4 px-3 py-2 text-sm rounded-md bg-gray-100 hover:bg-gray-200 cursor-pointer"
        :disabled="busy"
        @click="loadData"
      >
        Oppdater
      </button>
    </section>

    <section class="mb-10 rounded-lg border border-gray-200 bg-white p-6 shadow-sm">
      <h2 class="text-lg font-semibold mb-4">Last opp dokument</h2>
      <div class="space-y-4 max-w-xl">
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">Fil</label>
          <input
            id="ingest-file"
            type="file"
            accept=".pdf,.docx,.doc,.txt,.md,.png,.jpg,.jpeg,.gif,.bmp,.tiff,.webp,.svg"
            class="block w-full text-sm text-gray-600"
          />
        </div>
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">Tittel (valgfritt)</label>
          <input v-model="title" type="text" class="w-full border rounded px-3 py-2 text-sm" placeholder="Brukes som visningsnavn" />
        </div>
        <label class="flex items-center gap-2 text-sm text-gray-700">
          <input v-model="force" type="checkbox" />
          Tving re-indeks (slett eksisterende chunks for samme innhold først)
        </label>
        <button
          type="button"
          class="px-4 py-2 rounded-md text-white bg-blue-600 hover:bg-blue-700 disabled:opacity-50 cursor-pointer"
          :disabled="busy"
          @click="upload"
        >
          {{ busy ? 'Jobber…' : 'Kjør ingest' }}
        </button>
        <p v-if="status" class="text-sm text-green-700">{{ status }}</p>
      </div>
    </section>

    <section class="rounded-lg border border-gray-200 bg-white p-6 shadow-sm">
      <h2 class="text-lg font-semibold mb-4">Indekserte dokumenter</h2>
      <p v-if="error" class="text-sm text-red-600 mb-4">{{ error }}</p>
      <div v-if="documents.length === 0" class="text-sm text-gray-500">Ingen dokumenter funnet.</div>
      <div v-else class="overflow-x-auto">
        <table class="min-w-full text-sm text-left">
          <thead>
            <tr class="border-b text-gray-600">
              <th class="py-2 pr-4">Filnavn</th>
              <th class="py-2 pr-4">document_id</th>
              <th class="py-2 pr-4">Chunks</th>
              <th class="py-2 pr-4">Ingested</th>
              <th class="py-2">Handling</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(d, docIdx) in documents" :key="d.documentId ?? docIdx" class="border-b border-gray-100">
              <td class="py-2 pr-4">{{ d.filename }}</td>
              <td class="py-2 pr-4 font-mono text-xs">{{ (d.documentId ?? '').slice(0, 16) }}…</td>
              <td class="py-2 pr-4">{{ d.chunkCount }}</td>
              <td class="py-2 pr-4 text-gray-600">{{ d.lastIngestedAt || '—' }}</td>
              <td class="py-2">
                <button
                  type="button"
                  class="text-red-600 hover:underline cursor-pointer disabled:opacity-50"
                  :disabled="busy || !d.documentId"
                  @click="d.documentId && removeDoc(d.documentId)"
                >
                  Slett
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>

    <section class="mt-10 rounded-lg border border-gray-200 bg-white p-6 shadow-sm">
      <h2 class="text-lg font-semibold mb-2">Se chunks</h2>
      <p class="text-sm text-gray-600 mb-4">
        Hent råtekst og metadata fra Chroma for valgt dokument (sortert på chunk_index) eller alle embeddings med offset-paginering.
      </p>
      <div class="flex flex-wrap gap-4 items-end mb-4">
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">Dokument</label>
          <select
            v-model="chunkDocumentFilter"
            class="min-w-[14rem] border rounded px-3 py-2 text-sm bg-white"
            @change="onChunkFilterOrLimitChange"
          >
            <option value="">Alle (paginering i Chroma-rekkefølge)</option>
            <option v-for="d in documents" :key="d.documentId" :value="d.documentId">
              {{ d.filename }} ({{ d.chunkCount }} chunks)
            </option>
          </select>
        </div>
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">Per side</label>
          <select
            v-model.number="chunkLimit"
            class="border rounded px-3 py-2 text-sm bg-white"
            @change="onChunkFilterOrLimitChange"
          >
            <option :value="10">10</option>
            <option :value="25">25</option>
            <option :value="50">50</option>
            <option :value="100">100</option>
          </select>
        </div>
        <button
          type="button"
          class="px-4 py-2 rounded-md text-white bg-indigo-600 hover:bg-indigo-700 disabled:opacity-50 text-sm cursor-pointer"
          :disabled="chunksBusy"
          @click="loadChunks"
        >
          {{ chunksBusy ? 'Laster…' : 'Hent chunks' }}
        </button>
      </div>
      <p v-if="chunkError" class="text-sm text-red-600 mb-3">{{ chunkError }}</p>
      <div v-if="chunksData" class="text-xs text-gray-600 mb-3 space-y-1">
        <p>
          <span class="font-medium">Collection:</span> {{ chunksData.collectionName }} ·
          <span class="font-medium">Treff:</span> {{ chunksData.totalMatching }} av {{ chunksData.total }} embeddings i collection
        </p>
        <p v-if="chunksData.chunks.length > 0">
          Viser {{ chunksData.offset + 1 }}–{{ chunksData.offset + chunksData.chunks.length }} (offset {{ chunksData.offset }}, limit {{ chunksData.limit }})
        </p>
      </div>
      <div v-if="chunksData && chunksData.chunks.length === 0" class="text-sm text-gray-500">Ingen chunks i dette vinduet.</div>
      <div v-else-if="chunksData && chunksData.chunks.length > 0" class="overflow-x-auto border border-gray-100 rounded-md">
        <table class="min-w-full text-sm text-left">
          <thead>
            <tr class="border-b bg-gray-50 text-gray-600">
              <th class="py-2 px-3 w-10"></th>
              <th class="py-2 px-3">Fil / tittel</th>
              <th class="py-2 px-3 w-24">Chunk #</th>
              <th class="py-2 px-3">Tekst (forkortet)</th>
            </tr>
          </thead>
          <tbody>
            <template v-for="c in chunksData.chunks" :key="c.id">
              <tr
                class="border-b border-gray-100 hover:bg-gray-50/80 cursor-pointer align-top"
                @click="toggleChunkExpand(c.id)"
              >
                <td class="py-2 px-3 text-gray-400">{{ expandedChunkId === c.id ? '▼' : '▶' }}</td>
                <td class="py-2 px-3">{{ c.documentTitle || '—' }}</td>
                <td class="py-2 px-3 font-mono text-xs">{{ c.chunkIndex ?? '—' }}</td>
                <td class="py-2 px-3 text-gray-800 whitespace-pre-wrap break-words max-w-md">
                  {{ truncatePreview(c.text) }}
                </td>
              </tr>
              <tr v-if="expandedChunkId === c.id" class="bg-gray-50 border-b border-gray-100">
                <td colspan="4" class="py-3 px-4 text-xs">
                  <p class="font-medium text-gray-700 mb-1">Chunk-ID</p>
                  <code class="block mb-3 break-all text-gray-800">{{ c.id }}</code>
                  <p class="font-medium text-gray-700 mb-1">Full tekst</p>
                  <pre class="whitespace-pre-wrap break-words text-gray-800 mb-3 max-h-64 overflow-y-auto bg-white border rounded p-2">{{ c.text || '(tom)' }}</pre>
                  <details class="mt-2">
                    <summary class="cursor-pointer font-medium text-gray-700">Metadata (JSON)</summary>
                    <pre class="mt-2 max-h-48 overflow-auto bg-white border rounded p-2 text-gray-800">{{ JSON.stringify(c.metadata, null, 2) }}</pre>
                  </details>
                </td>
              </tr>
            </template>
          </tbody>
        </table>
      </div>
      <div
        v-if="chunksData && chunksData.totalMatching > 0"
        class="mt-4 flex flex-wrap items-center gap-3"
      >
        <button
          type="button"
          class="px-3 py-2 text-sm rounded-md bg-gray-100 hover:bg-gray-200 cursor-pointer disabled:opacity-40"
          :disabled="chunksBusy || chunkOffset <= 0"
          @click="chunkPrev"
        >
          Forrige
        </button>
        <button
          type="button"
          class="px-3 py-2 text-sm rounded-md bg-gray-100 hover:bg-gray-200 cursor-pointer disabled:opacity-40"
          :disabled="chunksBusy || !chunksData || chunkOffset + chunksData.chunks.length >= chunksData.totalMatching"
          @click="chunkNext"
        >
          Neste
        </button>
      </div>
    </section>
  </div>
</template>
