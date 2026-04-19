<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import {
  adminDocumentsChunks,
  adminDocumentsCollections,
  adminDocumentsDelete,
  adminDocumentsFiles,
  adminDocumentsIngestByPath,
  adminDocumentsList,
  adminDocumentsReseed,
  adminDocumentsUpload,
  adminDocumentsUploadBatch,
  type ChromaCollectionsResponse,
  type ChunkListResponse,
  type DocumentListEntry,
  type IngestionResult,
} from '@/api/generated/portfolio'

const auth = useAuthStore()
const title = ref('')
const force = ref(false)
const busy = ref(false)
const status = ref('')
const error = ref('')
const documents = ref<DocumentListEntry[]>([])
const chromaInfo = ref<ChromaCollectionsResponse | null>(null)

const uploadProgress = ref('')
const batchResults = ref<IngestionResult[]>([])

const serverFileList = ref<string[]>([])
const serverFilesLoading = ref(false)
const serverFilesLoadError = ref('')
const selectedForIngest = ref<Set<string>>(new Set())

const pathIngestForce = ref(false)
const pathIngestBusy = ref(false)
const pathIngestError = ref('')
const pathIngestResults = ref<IngestionResult[]>([])

const reseedBusy = ref(false)
const reseedMessage = ref('')

const chunksData = ref<ChunkListResponse | null>(null)
const chunksBusy = ref(false)
const chunkError = ref('')
const chunkDocumentFilter = ref('')
const chunkLimit = ref(25)
const chunkOffset = ref(0)
const expandedChunkId = ref<string | null>(null)

function formatHttpError(status: number, data: unknown): string {
  if (status === 401) return 'Ikke autorisert (logg inn som admin)'
  if (data && typeof data === 'object') {
    const o = data as Record<string, unknown>
    if (typeof o.error === 'string' && o.error) return o.error
    if (typeof o.message === 'string' && o.message) return o.message
  }
  return `Feilet (${status})`
}

async function loadData() {
  error.value = ''
  try {
    const [dRes, cRes] = await Promise.all([adminDocumentsList(), adminDocumentsCollections()])
    if (dRes.status !== 200) {
      throw new Error(formatHttpError(dRes.status, dRes.data))
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

async function reseedClasspath() {
  if (!confirm('Re-seede alle seed-dokumenter fra server (documentsToLoadDir / classpath)? Eksisterende chunks med samme innhold erstattes.')) {
    return
  }
  reseedBusy.value = true
  reseedMessage.value = ''
  error.value = ''
  try {
    const r = await adminDocumentsReseed()
    if (r.status !== 200) {
      throw new Error(formatHttpError(r.status, r.data))
    }
    const list = r.data ?? []
    const ok = list.filter((x) => (x.chunksIngested ?? 0) > 0 || x.skipped).length
    reseedMessage.value = `Reseed ferdig: ${list.length} fil(er), ${ok} med innhold.`
    await loadData()
  } catch (e: unknown) {
    error.value = e instanceof Error ? e.message : 'Ukjent feil'
  } finally {
    reseedBusy.value = false
  }
}

async function upload() {
  const input = document.getElementById('ingest-file') as HTMLInputElement | null
  const files = input?.files ? Array.from(input.files) : []
  if (files.length === 0) {
    status.value = 'Velg minst én fil.'
    return
  }
  busy.value = true
  status.value = ''
  uploadProgress.value = ''
  error.value = ''
  batchResults.value = []

  try {
    if (files.length === 1) {
      const r = await adminDocumentsUpload({
        file: files[0],
        title: title.value.trim() || undefined,
        force: force.value,
      })
      if (r.status !== 200) {
        throw new Error(formatHttpError(r.status, r.data))
      }
      const body = r.data
      const docIdPreview = (body.documentId ?? '').slice(0, 12)
      status.value = body.skipped
        ? `Hoppet over: ${body.message}`
        : `OK: ${body.chunksIngested ?? 0} chunks for ${body.filename ?? ''}${docIdPreview ? ` (${docIdPreview}…)` : ''}`
      batchResults.value = [body]
    } else {
      const r = await adminDocumentsUploadBatch({
        files,
        force: force.value,
      })
      const batchRes = r as { status: number; data: unknown }
      if (batchRes.status !== 200 && batchRes.status !== 400) {
        throw new Error(formatHttpError(batchRes.status, batchRes.data))
      }
      const results = Array.isArray(batchRes.data) ? batchRes.data : []
      batchResults.value = results
      const ok = results.filter((x) => (x.chunksIngested ?? 0) > 0 || x.skipped).length
      uploadProgress.value = `${results.length} fil(er) behandlet (${ok} med resultat)`
      status.value = `Batch: ${results.length} fil(er). Se tabellen under for detaljer.`
      if (batchRes.status === 400 && results.length > 0) {
        status.value = results[0]?.message ?? status.value
      }
    }
    if (input) input.value = ''
    await loadData()
  } catch (e: unknown) {
    error.value = e instanceof Error ? e.message : 'Ukjent feil'
  } finally {
    busy.value = false
    uploadProgress.value = ''
  }
}

async function loadServerFiles() {
  serverFilesLoading.value = true
  serverFilesLoadError.value = ''
  try {
    const r = await adminDocumentsFiles()
    if (r.status !== 200) {
      throw new Error(formatHttpError(r.status, r.data))
    }
    const list = Array.isArray(r.data) ? r.data : []
    serverFileList.value = list
    selectedForIngest.value = new Set()
  } catch (e: unknown) {
    serverFilesLoadError.value = e instanceof Error ? e.message : 'Ukjent feil'
    serverFileList.value = []
  } finally {
    serverFilesLoading.value = false
  }
}

function toggleServerFile(path: string, checked: boolean) {
  const next = new Set(selectedForIngest.value)
  if (checked) next.add(path)
  else next.delete(path)
  selectedForIngest.value = next
}

function onServerFileCheckboxChange(path: string, ev: Event) {
  const t = ev.target as HTMLInputElement | null
  if (t) toggleServerFile(path, t.checked)
}

function selectAllServerFiles() {
  selectedForIngest.value = new Set(serverFileList.value)
}

function clearServerSelection() {
  selectedForIngest.value = new Set()
}

async function runPathIngest() {
  const paths = Array.from(selectedForIngest.value)
  if (paths.length === 0) {
    pathIngestError.value =
      'Velg minst én fil i listen, eller klikk «Oppdater filliste» hvis listen er tom.'
    return
  }
  pathIngestBusy.value = true
  pathIngestError.value = ''
  pathIngestResults.value = []
  try {
    const r = await adminDocumentsIngestByPath({
      paths,
      force: pathIngestForce.value,
    })
    const pathRes = r as { status: number; data: unknown }
    if (pathRes.status !== 200 && pathRes.status !== 400) {
      throw new Error(formatHttpError(pathRes.status, pathRes.data))
    }
    pathIngestResults.value = Array.isArray(pathRes.data) ? pathRes.data : []
    if (pathRes.status === 400 && pathIngestResults.value.length > 0) {
      pathIngestError.value = pathIngestResults.value[0]?.message ?? 'Ugyldig forespørsel'
    }
    await loadData()
    await loadServerFiles()
  } catch (e: unknown) {
    pathIngestError.value = e instanceof Error ? e.message : 'Ukjent feil'
  } finally {
    pathIngestBusy.value = false
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

function toggleChunkExpand(id: string) {
  expandedChunkId.value = expandedChunkId.value === id ? null : id
}

function resultRowClass(row: IngestionResult): string {
  const chunks = row.chunksIngested ?? 0
  if (row.skipped) return 'text-amber-800 bg-amber-50/50'
  if (chunks > 0) return 'text-green-800 bg-green-50/30'
  const msg = row.message ?? ''
  if (msg && msg !== 'OK') return 'text-red-700 bg-red-50/30'
  return ''
}

onMounted(() => {
  auth.restore()
  void loadData()
  void loadServerFiles()
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
      <div class="mt-4 flex flex-wrap gap-2">
        <button
          type="button"
          class="px-3 py-2 text-sm rounded-md bg-gray-100 hover:bg-gray-200 cursor-pointer"
          :disabled="busy || reseedBusy"
          @click="loadData"
        >
          Oppdater
        </button>
        <button
          type="button"
          class="px-3 py-2 text-sm rounded-md bg-amber-100 hover:bg-amber-200 cursor-pointer disabled:opacity-50"
          :disabled="busy || reseedBusy"
          @click="reseedClasspath"
        >
          {{ reseedBusy ? 'Re-seeder…' : 'Re-seed seed-dokumenter' }}
        </button>
      </div>
      <p v-if="reseedMessage" class="mt-2 text-sm text-green-700">{{ reseedMessage }}</p>
    </section>

    <section class="mb-10 rounded-lg border border-gray-200 bg-white p-6 shadow-sm">
      <h2 class="text-lg font-semibold mb-4">Last opp dokument(er)</h2>
      <p class="text-sm text-gray-600 mb-4">
        Velg én fil for valgfri tittel. Flere filer sendes i én batch til serveren.
      </p>
      <div class="space-y-4 max-w-xl">
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">Filer</label>
          <input
            id="ingest-file"
            type="file"
            multiple
            accept=".pdf,.docx,.doc,.txt,.md,.png,.jpg,.jpeg,.gif,.bmp,.tiff,.webp,.svg"
            class="block w-full text-sm text-gray-600"
          />
        </div>
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">Tittel (valgfritt, kun første fil)</label>
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
        <p v-if="uploadProgress" class="text-sm text-gray-600">{{ uploadProgress }}</p>
        <p v-if="status" class="text-sm text-green-700">{{ status }}</p>
      </div>

      <div v-if="batchResults.length > 0" class="mt-6 overflow-x-auto">
        <h3 class="text-sm font-medium text-gray-800 mb-2">Resultat per fil</h3>
        <table class="min-w-full text-sm text-left border border-gray-100 rounded-md">
          <thead>
            <tr class="border-b bg-gray-50 text-gray-600">
              <th class="py-2 px-3">Fil</th>
              <th class="py-2 px-3">Chunks</th>
              <th class="py-2 px-3">Status</th>
              <th class="py-2 px-3">Melding</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="(row, i) in batchResults"
              :key="i"
              class="border-b border-gray-100"
              :class="resultRowClass(row)"
            >
              <td class="py-2 px-3">{{ row.filename || '—' }}</td>
              <td class="py-2 px-3">{{ row.chunksIngested ?? 0 }}</td>
              <td class="py-2 px-3">
                <span v-if="row.skipped">Hoppet over</span>
                <span v-else-if="(row.chunksIngested ?? 0) > 0">OK</span>
                <span v-else>—</span>
              </td>
              <td class="py-2 px-3 text-xs">{{ row.message || '—' }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>

    <section class="mb-10 rounded-lg border border-gray-200 bg-white p-6 shadow-sm">
      <h2 class="text-lg font-semibold mb-2">Batch-ingest fra data-mappe</h2>
      <p class="text-sm text-gray-600 mb-4">
        Filer under konfigurert <code class="text-xs bg-gray-100 px-1 rounded">sfg.aiapp.documentsToLoadDir</code>
        (typisk <code class="text-xs bg-gray-100 px-1 rounded">file:./data/docs/</code> →
        <code class="text-xs bg-gray-100 px-1 rounded">backend/data/docs/</code> ved kjøring fra
        <code class="text-xs">backend/</code>). Legg dokumenter i mappen, klikk «Oppdater filliste», velg filer og kjør batch-ingest.
      </p>
      <div class="flex flex-wrap gap-2 mb-4">
        <button
          type="button"
          class="px-3 py-2 text-sm rounded-md bg-gray-100 hover:bg-gray-200 cursor-pointer disabled:opacity-50"
          :disabled="serverFilesLoading"
          @click="loadServerFiles"
        >
          {{ serverFilesLoading ? 'Laster liste…' : 'Oppdater filliste' }}
        </button>
        <button
          type="button"
          class="px-3 py-2 text-sm rounded-md bg-gray-100 hover:bg-gray-200 cursor-pointer disabled:opacity-50"
          :disabled="serverFileList.length === 0"
          @click="selectAllServerFiles"
        >
          Velg alle
        </button>
        <button
          type="button"
          class="px-3 py-2 text-sm rounded-md bg-gray-100 hover:bg-gray-200 cursor-pointer disabled:opacity-50"
          :disabled="selectedForIngest.size === 0"
          @click="clearServerSelection"
        >
          Fjern alle valg
        </button>
      </div>
      <p v-if="serverFilesLoadError" class="text-sm text-red-600 mb-3">{{ serverFilesLoadError }}</p>
      <div
        v-else-if="!serverFilesLoading && serverFileList.length === 0"
        class="text-sm text-gray-500 mb-4 border border-dashed border-gray-200 rounded-md p-4 bg-gray-50/50"
      >
        Ingen filer funnet i data/docs/. Legg filer i <code class="text-xs">backend/data/docs/</code> og klikk Oppdater.
      </div>
      <ul
        v-else-if="serverFileList.length > 0"
        class="max-h-56 overflow-y-auto border border-gray-100 rounded-md divide-y divide-gray-100 mb-4"
      >
        <li
          v-for="(f, index) in serverFileList"
          :key="f"
          class="flex items-center gap-3 px-3 py-2 text-sm hover:bg-gray-50/80"
        >
          <input
            :id="'srv-file-' + index"
            type="checkbox"
            class="rounded border-gray-300"
            :checked="selectedForIngest.has(f)"
            @change="onServerFileCheckboxChange(f, $event)"
          />
          <label :for="'srv-file-' + index" class="cursor-pointer font-mono text-gray-800 flex-1 truncate">{{ f }}</label>
        </li>
      </ul>
      <label class="flex items-center gap-2 text-sm text-gray-700 mb-3">
        <input v-model="pathIngestForce" type="checkbox" />
        Tving re-indeks for valgte filer
      </label>
      <button
        type="button"
        class="mt-1 px-4 py-2 rounded-md text-white bg-teal-600 hover:bg-teal-700 disabled:opacity-50 text-sm cursor-pointer"
        :disabled="pathIngestBusy || selectedForIngest.size === 0"
        @click="runPathIngest"
      >
        {{ pathIngestBusy ? 'Jobber…' : 'Kjør batch-ingest (valgte filer)' }}
      </button>
      <p v-if="pathIngestError" class="mt-2 text-sm text-red-600">{{ pathIngestError }}</p>
      <div v-if="pathIngestResults.length > 0" class="mt-4 overflow-x-auto">
        <table class="min-w-full text-sm text-left border border-gray-100 rounded-md">
          <thead>
            <tr class="border-b bg-gray-50 text-gray-600">
              <th class="py-2 px-3">Sti / fil</th>
              <th class="py-2 px-3">Chunks</th>
              <th class="py-2 px-3">Melding</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="(row, i) in pathIngestResults"
              :key="i"
              class="border-b border-gray-100"
              :class="resultRowClass(row)"
            >
              <td class="py-2 px-3">{{ row.filename || '—' }}</td>
              <td class="py-2 px-3">{{ row.chunksIngested ?? 0 }}</td>
              <td class="py-2 px-3 text-xs">{{ row.message || '—' }}</td>
            </tr>
          </tbody>
        </table>
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
        <p v-if="(chunksData.chunks?.length ?? 0) > 0">
          Viser {{ (chunksData.offset ?? 0) + 1 }}–{{ (chunksData.offset ?? 0) + (chunksData.chunks?.length ?? 0) }} (offset {{ chunksData.offset }}, limit {{ chunksData.limit }})
        </p>
      </div>
      <div v-if="chunksData && (chunksData.chunks?.length ?? 0) === 0" class="text-sm text-gray-500">Ingen chunks i dette vinduet.</div>
      <div v-else-if="chunksData && (chunksData.chunks?.length ?? 0) > 0" class="overflow-x-auto border border-gray-100 rounded-md">
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
            <template v-for="c in chunksData.chunks" :key="c.id ?? ''">
              <tr
                class="border-b border-gray-100 hover:bg-gray-50/80 cursor-pointer align-top"
                @click="c.id && toggleChunkExpand(c.id)"
              >
                <td class="py-2 px-3 text-gray-400">{{ expandedChunkId === c.id ? '▼' : '▶' }}</td>
                <td class="py-2 px-3">{{ c.documentTitle || '—' }}</td>
                <td class="py-2 px-3 font-mono text-xs">{{ c.chunkIndex ?? '—' }}</td>
                <td class="py-2 px-3 text-gray-800 whitespace-pre-wrap break-words max-w-md">
                  {{ truncatePreview(c.text ?? '') }}
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
                    <pre class="mt-2 max-h-48 overflow-auto bg-white border rounded p-2 text-gray-800">{{ JSON.stringify(c.metadata ?? {}, null, 2) }}</pre>
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
          class="px-3 py-2 text-sm rounded-md bg-gray-100 hover:bg-gray-200 cursor-pointer disabled:opacity-40"
          :disabled="chunksBusy || chunkOffset <= 0"
          @click="chunkPrev"
        >
          Forrige
        </button>
        <button
          type="button"
          class="px-3 py-2 text-sm rounded-md bg-gray-100 hover:bg-gray-200 cursor-pointer disabled:opacity-40"
          :disabled="chunksBusy || !chunksData || chunkOffset + (chunksData.chunks?.length ?? 0) >= (chunksData.totalMatching ?? 0)"
          @click="chunkNext"
        >
          Neste
        </button>
      </div>
    </section>
  </div>
</template>
