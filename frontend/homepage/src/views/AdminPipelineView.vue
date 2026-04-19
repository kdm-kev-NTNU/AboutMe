<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { RouterLink } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import {
  adminDocumentsCollections,
  adminDocumentsDelete,
  adminDocumentsFiles,
  adminDocumentsIngestByPath,
  adminDocumentsList,
  adminDocumentsReseed,
  adminDocumentsUpload,
  adminDocumentsUploadBatch,
  type ChromaCollectionsResponse,
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
  if (
    !confirm(
      'Re-seede alle seed-dokumenter fra server (documentsToLoadDir / classpath)? Eksisterende chunks med samme innhold erstattes.',
    )
  ) {
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
    selectedForIngest.value = new Set()
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
  <div class="min-h-screen bg-[hsl(220_20%_97%)] text-[hsl(220_25%_10%)] font-sans antialiased pb-12">
    <nav
      class="border-b border-gray-200/80 bg-white/90 backdrop-blur-sm px-4 py-3 text-sm flex flex-wrap gap-x-4 gap-y-1 items-center max-w-3xl mx-auto"
    >
      <RouterLink to="/" class="text-blue-600 hover:underline">Hjem</RouterLink>
      <RouterLink to="/admin/tools" class="text-blue-600 hover:underline">Internal tools</RouterLink>
      <span class="text-gray-500">/</span>
      <strong class="text-gray-900">Document pipeline</strong>
    </nav>

    <main class="mx-auto max-w-xl px-4 pt-8">
      <h1 class="text-2xl font-semibold tracking-tight text-gray-900 mb-2">Document pipeline</h1>
      <p class="text-sm text-gray-600 mb-6 leading-relaxed">
        Last opp og indekser dokumenter til ChromaDB (Spring AI). Krever admin-innlogging. For å inspisere
        chunks og metadata, gå til
        <RouterLink to="/admin/chunks" class="text-blue-600 hover:underline">Chunk viewer</RouterLink>.
      </p>

      <!-- Chroma summary card -->
      <section
        class="mb-6 rounded-xl border border-gray-200 bg-white p-4 shadow-[0_1px_3px_rgb(0_0_0/0.06)]"
      >
        <h2 class="text-sm font-semibold text-gray-900 mb-3">ChromaDB</h2>
        <div v-if="chromaInfo" class="text-sm text-gray-700 space-y-1">
          <p>
            <span class="font-medium">Aktiv collection:</span>
            {{ chromaInfo.activeCollectionName }}
            <span class="text-gray-500"
              >({{ chromaInfo.activeCollectionEmbeddingCount ?? 0 }} embeddings)</span
            >
          </p>
          <p class="font-medium text-gray-800 pt-2">Collections:</p>
          <ul class="list-disc list-inside text-gray-600 text-xs">
            <li v-for="c in chromaInfo.collections" :key="c.id">
              {{ c.name }} <code class="font-mono">({{ c.id }})</code>
            </li>
          </ul>
        </div>
        <p v-else class="text-gray-500 text-sm">Laster…</p>
        <div class="mt-4 flex flex-wrap gap-2">
          <button
            type="button"
            class="px-3 py-2 text-sm rounded-md border border-gray-200 bg-white hover:bg-gray-50 cursor-pointer"
            :disabled="busy || reseedBusy"
            @click="loadData"
          >
            Oppdater
          </button>
          <button
            type="button"
            class="px-3 py-2 text-sm rounded-md border border-amber-200 bg-amber-50 hover:bg-amber-100 cursor-pointer disabled:opacity-50"
            :disabled="busy || reseedBusy"
            @click="reseedClasspath"
          >
            {{ reseedBusy ? 'Re-seeder…' : 'Re-seed seed-dokumenter' }}
          </button>
        </div>
        <p v-if="reseedMessage" class="mt-2 text-sm text-green-700 rounded-md bg-green-50/80 px-2 py-1.5">
          {{ reseedMessage }}
        </p>
      </section>

      <p v-if="error" class="mb-4 text-sm text-red-700 rounded-md border border-red-200 bg-red-50 px-3 py-2">
        {{ error }}
      </p>

      <!-- Single / multi upload disclosure -->
      <details
        class="group mb-4 rounded-xl border border-gray-200 bg-gray-50/50 open:bg-white open:shadow-[0_1px_3px_rgb(0_0_0/0.06)] transition-colors"
      >
        <summary
          class="cursor-pointer list-none px-4 py-3 font-semibold text-gray-900 flex items-center gap-2 select-none rounded-xl"
        >
          <span
            class="inline-block text-gray-500 transition-transform duration-200 group-open:rotate-90"
            aria-hidden="true"
            >▶</span
          >
          Last opp fil(er)
        </summary>
        <div class="px-4 pb-4 pt-0 border-t border-gray-200">
          <p class="text-xs text-gray-600 mb-4 pt-3">
            Velg én fil for valgfri tittel. Flere filer sendes i én batch til serveren.
          </p>
          <div class="space-y-4">
            <div class="space-y-1">
              <label class="block text-sm font-medium text-gray-800">Filer</label>
              <input
                id="ingest-file"
                type="file"
                multiple
                accept=".pdf,.docx,.doc,.txt,.md,.png,.jpg,.jpeg,.gif,.bmp,.tiff,.webp,.svg"
                class="block w-full text-sm text-gray-600"
              />
              <p class="text-xs text-gray-500">Støttede formater fra serveren.</p>
            </div>
            <div class="space-y-1">
              <label class="block text-sm font-medium text-gray-800">Tittel (valgfritt, kun første fil)</label>
              <input
                v-model="title"
                type="text"
                class="w-full border border-gray-200 rounded-lg px-3 py-2 text-sm bg-white"
                placeholder="Brukes som visningsnavn"
              />
            </div>
            <label class="flex items-center gap-2 text-sm text-gray-700">
              <input v-model="force" type="checkbox" class="rounded border-gray-300" />
              Tving re-indeks (slett eksisterende chunks for samme innhold først)
            </label>
            <button
              type="button"
              class="px-4 py-2 rounded-lg text-sm font-medium text-white bg-[hsl(221_83%_53%)] hover:bg-[hsl(221_83%_48%)] disabled:opacity-50 cursor-pointer"
              :disabled="busy"
              @click="upload"
            >
              {{ busy ? 'Jobber…' : 'Kjør ingest' }}
            </button>
            <p v-if="uploadProgress" class="text-sm text-gray-600">{{ uploadProgress }}</p>
            <p
              v-if="status"
              class="text-sm text-green-800 rounded-md border border-green-200 bg-green-50/80 px-3 py-2"
            >
              {{ status }}
            </p>
          </div>

          <div v-if="batchResults.length > 0" class="mt-6 overflow-x-auto">
            <h3 class="text-xs font-semibold text-gray-700 uppercase tracking-wide mb-2">Resultat per fil</h3>
            <table class="w-full text-sm border-collapse border border-gray-200 rounded-lg overflow-hidden">
              <thead>
                <tr class="bg-gray-100 text-gray-700">
                  <th class="border border-gray-200 px-2 py-2 text-left font-semibold">Fil</th>
                  <th class="border border-gray-200 px-2 py-2 text-left font-semibold">Chunks</th>
                  <th class="border border-gray-200 px-2 py-2 text-left font-semibold">Status</th>
                  <th class="border border-gray-200 px-2 py-2 text-left font-semibold">Melding</th>
                </tr>
              </thead>
              <tbody>
                <tr
                  v-for="(row, i) in batchResults"
                  :key="i"
                  class="bg-white"
                  :class="resultRowClass(row)"
                >
                  <td class="border border-gray-200 px-2 py-2 align-top">{{ row.filename || '—' }}</td>
                  <td class="border border-gray-200 px-2 py-2 align-top">{{ row.chunksIngested ?? 0 }}</td>
                  <td class="border border-gray-200 px-2 py-2 align-top">
                    <span v-if="row.skipped">Hoppet over</span>
                    <span v-else-if="(row.chunksIngested ?? 0) > 0">OK</span>
                    <span v-else>—</span>
                  </td>
                  <td class="border border-gray-200 px-2 py-2 align-top text-xs">{{ row.message || '—' }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </details>

      <!-- Batch from data dir disclosure -->
      <details
        class="group mb-8 rounded-xl border border-gray-200 bg-gray-50/50 open:bg-white open:shadow-[0_1px_3px_rgb(0_0_0/0.06)] transition-colors"
      >
        <summary
          class="cursor-pointer list-none px-4 py-3 font-semibold text-gray-900 flex items-center gap-2 select-none rounded-xl"
        >
          <span
            class="inline-block text-gray-500 transition-transform duration-200 group-open:rotate-90"
            aria-hidden="true"
            >▶</span
          >
          Batch-ingest fra data-mappe
        </summary>
        <div class="px-4 pb-4 pt-0 border-t border-gray-200">
          <p class="text-xs text-gray-600 mb-4 pt-3 leading-relaxed">
            Filer under
            <code class="font-mono text-[11px] bg-gray-100 px-1 rounded">sfg.aiapp.documentsToLoadDir</code>
            (typisk <code class="font-mono text-[11px] bg-gray-100 px-1 rounded">backend/data/docs/</code>).
          </p>
          <div class="flex flex-wrap gap-2 mb-4">
            <button
              type="button"
              class="px-3 py-2 text-sm rounded-md border border-gray-200 bg-white hover:bg-gray-50 cursor-pointer disabled:opacity-50"
              :disabled="serverFilesLoading"
              @click="loadServerFiles"
            >
              {{ serverFilesLoading ? 'Laster liste…' : 'Oppdater filliste' }}
            </button>
            <button
              type="button"
              class="px-3 py-2 text-sm rounded-md border border-gray-200 bg-white hover:bg-gray-50 cursor-pointer disabled:opacity-50"
              :disabled="serverFileList.length === 0"
              @click="selectAllServerFiles"
            >
              Velg alle
            </button>
            <button
              type="button"
              class="px-3 py-2 text-sm rounded-md border border-gray-200 bg-white hover:bg-gray-50 cursor-pointer disabled:opacity-50"
              :disabled="selectedForIngest.size === 0"
              @click="clearServerSelection"
            >
              Fjern alle valg
            </button>
          </div>
          <p v-if="serverFilesLoadError" class="text-sm text-red-600 mb-3">{{ serverFilesLoadError }}</p>
          <div
            v-else-if="!serverFilesLoading && serverFileList.length === 0"
            class="text-sm text-gray-500 mb-4 border border-dashed border-gray-200 rounded-lg p-4 bg-gray-50/50"
          >
            Ingen filer funnet. Legg filer i <code class="font-mono text-xs">backend/data/docs/</code> og klikk
            Oppdater.
          </div>
          <ul
            v-else-if="serverFileList.length > 0"
            class="max-h-56 overflow-y-auto border border-gray-200 rounded-lg divide-y divide-gray-100 mb-4 bg-white"
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
              <label :for="'srv-file-' + index" class="cursor-pointer font-mono text-gray-800 flex-1 truncate">{{
                f
              }}</label>
            </li>
          </ul>
          <label class="flex items-center gap-2 text-sm text-gray-700 mb-3">
            <input v-model="pathIngestForce" type="checkbox" class="rounded border-gray-300" />
            Tving re-indeks for valgte filer
          </label>
          <button
            type="button"
            class="px-4 py-2 rounded-lg text-sm font-medium text-white bg-teal-600 hover:bg-teal-700 disabled:opacity-50 cursor-pointer"
            :disabled="pathIngestBusy || selectedForIngest.size === 0"
            @click="runPathIngest"
          >
            {{ pathIngestBusy ? 'Jobber…' : 'Kjør batch-ingest (valgte filer)' }}
          </button>
          <p v-if="pathIngestError" class="mt-2 text-sm text-red-600">{{ pathIngestError }}</p>
          <div v-if="pathIngestResults.length > 0" class="mt-4 overflow-x-auto">
            <table class="w-full text-sm border-collapse border border-gray-200 rounded-lg overflow-hidden">
              <thead>
                <tr class="bg-gray-100 text-gray-700">
                  <th class="border border-gray-200 px-2 py-2 text-left font-semibold">Sti / fil</th>
                  <th class="border border-gray-200 px-2 py-2 text-left font-semibold">Chunks</th>
                  <th class="border border-gray-200 px-2 py-2 text-left font-semibold">Melding</th>
                </tr>
              </thead>
              <tbody>
                <tr
                  v-for="(row, i) in pathIngestResults"
                  :key="i"
                  class="bg-white"
                  :class="resultRowClass(row)"
                >
                  <td class="border border-gray-200 px-2 py-2 align-top">{{ row.filename || '—' }}</td>
                  <td class="border border-gray-200 px-2 py-2 align-top">{{ row.chunksIngested ?? 0 }}</td>
                  <td class="border border-gray-200 px-2 py-2 align-top text-xs">{{ row.message || '—' }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </details>

      <!-- Indexed documents -->
      <section class="rounded-xl border border-gray-200 bg-white p-4 shadow-[0_1px_3px_rgb(0_0_0/0.06)]">
        <h2 class="text-sm font-semibold text-gray-900 mb-3">Indekserte dokumenter</h2>
        <div v-if="documents.length === 0" class="text-sm text-gray-500">Ingen dokumenter funnet.</div>
        <div v-else class="overflow-x-auto -mx-1">
          <table class="w-full text-sm border-collapse border border-gray-200 min-w-[32rem]">
            <thead>
              <tr class="bg-gray-100 text-gray-700">
                <th class="border border-gray-200 px-2 py-2 text-left font-semibold">Filnavn</th>
                <th class="border border-gray-200 px-2 py-2 text-left font-semibold">document_id</th>
                <th class="border border-gray-200 px-2 py-2 text-left font-semibold">Chunks</th>
                <th class="border border-gray-200 px-2 py-2 text-left font-semibold">Ingested</th>
                <th class="border border-gray-200 px-2 py-2 text-left font-semibold">Handling</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(d, docIdx) in documents" :key="d.documentId ?? docIdx" class="bg-white">
                <td class="border border-gray-200 px-2 py-2 align-top">{{ d.filename }}</td>
                <td class="border border-gray-200 px-2 py-2 align-top font-mono text-xs">
                  {{ (d.documentId ?? '').slice(0, 16) }}…
                </td>
                <td class="border border-gray-200 px-2 py-2 align-top">{{ d.chunkCount }}</td>
                <td class="border border-gray-200 px-2 py-2 align-top text-gray-600">
                  {{ d.lastIngestedAt || '—' }}
                </td>
                <td class="border border-gray-200 px-2 py-2 align-top">
                  <button
                    type="button"
                    class="text-red-600 hover:underline text-sm cursor-pointer disabled:opacity-50"
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
    </main>
  </div>
</template>

<style scoped>
details > summary::-webkit-details-marker {
  display: none;
}
</style>
