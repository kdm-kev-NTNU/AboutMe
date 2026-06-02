<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { RouterLink } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import {
  adminDocumentsChunks,
  adminDocumentsChunksExport,
  adminDocumentsCollections,
  adminDocumentsList,
  type VectorStoreInfoResponse,
  type ChunkListResponse,
  type DocumentListEntry,
} from '@/api/generated/portfolio'
import { formatAdminHttpError as formatHttpError } from '@/lib/api-error'

// Chunk browser: paginated embeddings with optional documentId (content hash) filter.
const auth = useAuthStore()
const error = ref('')
const documents = ref<DocumentListEntry[]>([])
const chromaInfo = ref<VectorStoreInfoResponse | null>(null)

const chunksData = ref<ChunkListResponse | null>(null)
const chunksBusy = ref(false)
const chunkError = ref('')
const exportBusy = ref(false)
const exportError = ref('')
const chunkDocumentFilter = ref('')
const chunkLimit = ref(25)
const chunkOffset = ref(0)
const expandedChunkId = ref<string | null>(null)
const expandedTextChunkId = ref<string | null>(null)

const TEXT_PREVIEW_LEN = 200

async function loadData() {
  error.value = ''
  try {
    const [dRes, cRes] = await Promise.all([adminDocumentsList(), adminDocumentsCollections()])
    if (dRes.status !== 200) {
      throw new Error(formatHttpError(dRes.status, dRes.data))
    }
    if (cRes.status !== 200) {
      throw new Error(`Vektorlagring status feilet (${cRes.status})`)
    }
    documents.value = dRes.data
    chromaInfo.value = cRes.data
  } catch (e: unknown) {
    error.value = e instanceof Error ? e.message : 'Ukjent feil'
  }
}

function contentHashFromMetadata(meta: Record<string, unknown> | undefined): string {
  if (!meta) return ''
  const v = meta.content_hash ?? meta.contentHash
  if (typeof v === 'string') return v
  return ''
}

function hashShort(hash: string): string {
  if (!hash) return '–'
  return hash.length > 12 ? hash.slice(0, 12) + '…' : hash
}

function textPreview(text: string | undefined): string {
  const t = text ?? ''
  if (t.length <= TEXT_PREVIEW_LEN) return t
  return t.slice(0, TEXT_PREVIEW_LEN) + '…'
}

function needsTextExpand(text: string | undefined): boolean {
  return (text?.length ?? 0) > TEXT_PREVIEW_LEN
}

function onChunkFilterOrLimitChange() {
  chunkOffset.value = 0
  void loadChunks()
}

async function loadChunks() {
  chunksBusy.value = true
  chunkError.value = ''
  expandedChunkId.value = null
  expandedTextChunkId.value = null
  try {
    const res = await adminDocumentsChunks({
      documentId: chunkDocumentFilter.value.trim() || undefined,
      limit: chunkLimit.value,
      offset: chunkOffset.value,
    })
    if (res.status !== 200) {
      throw new Error(formatHttpError(res.status, res.data))
    }
    chunksData.value = res.data
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
  const totalMatching = d.totalMatching ?? 0
  const len = d.chunks?.length ?? 0
  if (chunkOffset.value + len >= totalMatching) return
  chunkOffset.value += chunkLimit.value
  void loadChunks()
}

function triggerJsonDownload(data: unknown, filename: string) {
  const blob = new Blob([JSON.stringify(data, null, 2)], {
    type: 'application/json;charset=utf-8',
  })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  a.click()
  URL.revokeObjectURL(url)
}

async function downloadChunksExport() {
  exportBusy.value = true
  exportError.value = ''
  try {
    const res = await adminDocumentsChunksExport({
      documentId: chunkDocumentFilter.value.trim() || undefined,
    })
    if (res.status !== 200 || !res.data) {
      throw new Error(formatHttpError(res.status, res.data))
    }
    const trimmed = chunkDocumentFilter.value.trim()
    const safe =
      trimmed.length > 0 ? trimmed.replace(/[^a-zA-Z0-9._-]+/g, '_').slice(0, 48) : 'all'
    triggerJsonDownload(res.data, `portfolio-chunks-${safe}.json`)
  } catch (e: unknown) {
    exportError.value = e instanceof Error ? e.message : 'Ukjent feil'
  } finally {
    exportBusy.value = false
  }
}

function toggleChunkExpand(id: string | undefined) {
  if (!id) return
  expandedChunkId.value = expandedChunkId.value === id ? null : id
}

function toggleTextExpand(id: string | undefined, ev: MouseEvent) {
  ev.stopPropagation()
  if (!id) return
  expandedTextChunkId.value = expandedTextChunkId.value === id ? null : id
}

const pageInfo = computed(() => {
  const d = chunksData.value
  if (!d || (d.totalMatching ?? 0) === 0) return 'Ingen chunks'
  const chunks = d.chunks ?? []
  const total = d.totalMatching ?? 0
  if (chunks.length === 0) return 'Ingen chunks i dette vinduet'
  const from = (d.offset ?? 0) + 1
  const to = (d.offset ?? 0) + chunks.length
  return `${from}–${to} av ${total}`
})

onMounted(() => {
  auth.restore()
  void loadData()
})
</script>

<template>
  <div class="min-h-screen bg-[hsl(220_20%_97%)] text-[hsl(220_25%_10%)] font-sans antialiased pb-12">
    <nav
      class="border-b border-gray-200/80 bg-white/90 backdrop-blur-sm px-4 py-3 text-sm flex flex-wrap gap-x-4 gap-y-1 items-center max-w-5xl mx-auto"
    >
      <RouterLink to="/" class="text-blue-600 hover:underline">Hjem</RouterLink>
      <RouterLink to="/admin/tools" class="text-blue-600 hover:underline">Internal tools</RouterLink>
      <span class="text-gray-500">/</span>
      <RouterLink to="/admin/question-suggestions" class="text-blue-600 hover:underline">
        Spørsmålsforslag
      </RouterLink>
      <span class="text-gray-500">/</span>
      <strong class="text-gray-900">Chunk viewer</strong>
    </nav>

    <main id="main-content" class="mx-auto max-w-5xl px-4 pt-8">
      <h1 class="text-2xl font-semibold tracking-tight text-gray-900 mb-2">PostgreSQL / pgvector chunks</h1>
      <p class="text-sm text-gray-600 mb-6 leading-relaxed">
        Se råtekst og metadata fra den aktive collection. Paginering som i Piscada-tools.
        <RouterLink to="/admin/pipeline" class="text-blue-600 hover:underline">Document pipeline</RouterLink>
        for opplasting.
      </p>

      <p v-if="error" class="mb-4 text-sm text-red-700 rounded-md border border-red-200 bg-red-50 px-3 py-2">
        {{ error }}
      </p>

      <!-- Summary card (vector store table) -->
      <section
        class="mb-6 rounded-xl border border-gray-200 bg-white p-4 shadow-[0_1px_3px_rgb(0_0_0/0.06)]"
      >
        <div class="flex flex-wrap gap-x-8 gap-y-3 items-center text-sm">
          <div>
            <strong class="text-gray-900">Aktiv collection:</strong>
            <span class="text-gray-700 ml-1">{{ chromaInfo?.activeCollectionName ?? '–' }}</span>
          </div>
          <div>
            <strong class="text-gray-900">Embeddings:</strong>
            <span class="text-gray-700 ml-1">{{ chromaInfo?.activeCollectionEmbeddingCount ?? '–' }}</span>
          </div>
          <div>
            <strong class="text-gray-900">Dokumenter (liste):</strong>
            <span class="text-gray-700 ml-1">{{ documents.length }}</span>
          </div>
        </div>
        <div class="mt-4 flex flex-wrap gap-2">
          <button
            type="button"
            class="px-3 py-2 text-sm rounded-md border border-gray-200 bg-white hover:bg-gray-50 cursor-pointer"
            @click="loadData"
          >
            Oppdater oversikt
          </button>
        </div>
        <div v-if="chromaInfo?.collections?.length" class="mt-4 text-xs text-gray-600">
          <span class="font-medium text-gray-800">Collections i DB:</span>
          <span v-for="(c, i) in chromaInfo.collections" :key="c.id" class="ml-1">
            {{ c.name }}<span v-if="i < (chromaInfo.collections?.length ?? 0) - 1">,</span>
          </span>
        </div>
      </section>

      <!-- Controls -->
      <section class="mb-4">
        <h2 class="text-lg font-semibold text-gray-900 mb-2">Chunks i collection</h2>
        <p class="text-xs text-gray-600 mb-4">Filtrer på dokument eller hent alle med offset-paginering.</p>
        <div class="flex flex-wrap gap-4 items-end">
          <div class="space-y-1">
            <label class="block text-sm font-medium text-gray-800">Dokument</label>
            <select
              v-model="chunkDocumentFilter"
              class="min-w-[14rem] border border-gray-200 rounded-lg px-3 py-2 text-sm bg-white"
              @change="onChunkFilterOrLimitChange"
            >
              <option value="">Alle (paginering i tabell-rekkefølge)</option>
              <option v-for="d in documents" :key="d.documentId" :value="d.documentId">
                {{ d.filename }} ({{ d.chunkCount }} chunks)
              </option>
            </select>
          </div>
          <div class="space-y-1">
            <label class="block text-sm font-medium text-gray-800">Per side</label>
            <select
              v-model.number="chunkLimit"
              class="border border-gray-200 rounded-lg px-3 py-2 text-sm bg-white"
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
            class="px-4 py-2 rounded-lg text-sm font-medium text-white bg-[hsl(221_83%_53%)] hover:bg-[hsl(221_83%_48%)] disabled:opacity-50 cursor-pointer"
            :disabled="chunksBusy"
            @click="loadChunks"
          >
            {{ chunksBusy ? 'Laster…' : 'Hent chunks' }}
          </button>
          <button
            type="button"
            class="px-4 py-2 rounded-lg text-sm font-medium border border-gray-300 bg-white hover:bg-gray-50 disabled:opacity-50 cursor-pointer"
            :disabled="exportBusy || chunksBusy"
            @click="downloadChunksExport"
          >
            {{ exportBusy ? 'Eksporterer…' : 'Last ned JSON (chunks)' }}
          </button>
        </div>
        <p v-if="exportError" class="mt-3 text-sm text-red-700">{{ exportError }}</p>
      </section>

      <p
        v-if="chunkError"
        class="mb-3 text-sm text-red-700 rounded-md border border-red-200 bg-red-50 px-3 py-2"
      >
        {{ chunkError }}
      </p>

      <div v-if="chunksData" class="text-xs text-gray-600 mb-3 space-y-1">
        <p>
          <span class="font-medium">Collection:</span> {{ chunksData.collectionName }} ·
          <span class="font-medium">Treff:</span> {{ chunksData.totalMatching }} av
          {{ chunksData.total }} embeddings
        </p>
      </div>

      <div
        v-if="chunksData && (chunksData.chunks?.length ?? 0) === 0"
        class="text-sm text-gray-500 mb-4"
      >
        Ingen chunks i dette vinduet.
      </div>

      <div
        v-else-if="chunksData && (chunksData.chunks?.length ?? 0) > 0"
        class="overflow-x-auto rounded-xl border border-gray-200 bg-white shadow-[0_1px_3px_rgb(0_0_0/0.06)]"
      >
        <table class="w-full text-sm border-collapse min-w-[48rem]">
          <thead>
            <tr class="bg-gray-100 text-gray-800">
              <th class="border border-gray-200 px-2 py-2 text-left font-semibold w-8"></th>
              <th class="border border-gray-200 px-2 py-2 text-left font-semibold">Document</th>
              <th class="border border-gray-200 px-2 py-2 text-left font-semibold w-24">Chunk #</th>
              <th class="border border-gray-200 px-2 py-2 text-left font-semibold min-w-[10rem]">
                Content hash
              </th>
              <th class="border border-gray-200 px-2 py-2 text-left font-semibold">Text</th>
            </tr>
          </thead>
          <tbody>
            <template v-for="c in chunksData.chunks" :key="c.id ?? ''">
              <tr
                class="bg-white hover:bg-gray-50/80 cursor-pointer align-top"
                @click="toggleChunkExpand(c.id)"
              >
                <td class="border border-gray-200 px-2 py-2 text-gray-400">
                  {{ expandedChunkId === c.id ? '▼' : '▶' }}
                </td>
                <td class="border border-gray-200 px-2 py-2">{{ c.documentTitle || '–' }}</td>
                <td class="border border-gray-200 px-2 py-2 font-mono text-xs">
                  {{ c.chunkIndex ?? '–' }}
                </td>
                <td
                  class="border border-gray-200 px-2 py-2 font-mono text-[11px]"
                  :title="contentHashFromMetadata(c.metadata as Record<string, unknown> | undefined) || undefined"
                >
                  {{
                    hashShort(
                      contentHashFromMetadata(c.metadata as Record<string, unknown> | undefined),
                    )
                  }}
                </td>
                <td class="border border-gray-200 px-2 py-2 max-w-md">
                  <span class="text-gray-800 whitespace-pre-wrap break-words align-top">{{
                    expandedTextChunkId === c.id ? c.text : textPreview(c.text)
                  }}</span>
                  <button
                    v-if="needsTextExpand(c.text) && expandedTextChunkId !== c.id"
                    type="button"
                    class="ml-1 text-[11px] text-blue-600 hover:underline align-baseline cursor-pointer"
                    @click="toggleTextExpand(c.id, $event)"
                  >
                    Vis mer
                  </button>
                  <button
                    v-if="needsTextExpand(c.text) && expandedTextChunkId === c.id"
                    type="button"
                    class="ml-1 text-[11px] text-blue-600 hover:underline cursor-pointer"
                    @click="toggleTextExpand(c.id, $event)"
                  >
                    Vis mindre
                  </button>
                </td>
              </tr>
              <tr v-if="expandedChunkId === c.id" class="bg-gray-50">
                <td colspan="5" class="border border-gray-200 px-4 py-3 text-xs">
                  <p class="font-medium text-gray-800 mb-1">Chunk-ID</p>
                  <code class="block mb-3 break-all text-gray-900 font-mono text-[11px]">{{ c.id }}</code>
                  <p class="font-medium text-gray-800 mb-1">Full tekst</p>
                  <pre
                    class="whitespace-pre-wrap break-words text-gray-900 mb-3 max-h-64 overflow-y-auto bg-white border border-gray-200 rounded-lg p-2 font-mono text-[11px]"
                    >{{ c.text || '(tom)' }}</pre
                  >
                  <details class="mt-2">
                    <summary class="cursor-pointer font-medium text-gray-800">Metadata (JSON)</summary>
                    <pre
                      class="mt-2 max-h-48 overflow-auto bg-white border border-gray-200 rounded-lg p-2 text-gray-900 font-mono text-[11px]"
                      >{{ JSON.stringify(c.metadata ?? {}, null, 2) }}</pre
                    >
                  </details>
                </td>
              </tr>
            </template>
          </tbody>
        </table>
      </div>

      <div
        v-if="chunksData && (chunksData.totalMatching ?? 0) > 0"
        class="mt-4 flex flex-wrap items-center gap-3"
      >
        <button
          type="button"
          class="px-3 py-2 text-sm rounded-md border border-gray-200 bg-white hover:bg-gray-50 cursor-pointer disabled:opacity-40"
          :disabled="chunksBusy || chunkOffset <= 0"
          @click="chunkPrev"
        >
          Forrige
        </button>
        <span class="text-sm text-gray-600">{{ pageInfo }}</span>
        <button
          type="button"
          class="px-3 py-2 text-sm rounded-md border border-gray-200 bg-white hover:bg-gray-50 cursor-pointer disabled:opacity-40"
          :disabled="
            chunksBusy ||
            !chunksData ||
            chunkOffset + (chunksData.chunks?.length ?? 0) >= (chunksData.totalMatching ?? 0)
          "
          @click="chunkNext"
        >
          Neste
        </button>
      </div>
    </main>
  </div>
</template>
