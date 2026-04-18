<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'

interface IngestionResult {
  documentId: string
  filename: string
  chunksIngested: number
  skipped: boolean
  message: string
}

interface DocumentListEntry {
  documentId: string
  filename: string
  chunkCount: number
  lastIngestedAt: string
}

interface ChromaCollectionSummary {
  id: string
  name: string
}

interface ChromaCollectionsResponse {
  activeCollectionName: string
  activeCollectionEmbeddingCount: number
  collections: ChromaCollectionSummary[]
}

const auth = useAuthStore()
const title = ref('')
const force = ref(false)
const busy = ref(false)
const status = ref('')
const error = ref('')
const documents = ref<DocumentListEntry[]>([])
const chromaInfo = ref<ChromaCollectionsResponse | null>(null)

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
    const [dRes, cRes] = await Promise.all([
      fetch('/api/admin/tools/documents', { headers: authHeaders() }),
      fetch('/api/admin/tools/documents/collections', { headers: authHeaders() }),
    ])
    if (!dRes.ok) {
      throw new Error(dRes.status === 401 ? 'Ikke autorisert (logg inn som admin)' : `Liste feilet (${dRes.status})`)
    }
    if (!cRes.ok) {
      throw new Error(`Chroma status feilet (${cRes.status})`)
    }
    documents.value = (await dRes.json()) as DocumentListEntry[]
    chromaInfo.value = (await cRes.json()) as ChromaCollectionsResponse
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
    const form = new FormData()
    form.append('file', file)
    if (title.value.trim()) {
      form.append('title', title.value.trim())
    }
    form.append('force', force.value ? 'true' : 'false')
    const res = await fetch('/api/admin/tools/documents/upload', {
      method: 'POST',
      headers: authHeaders(),
      body: form,
    })
    let body: IngestionResult | null = null
    try {
      body = (await res.json()) as IngestionResult
    } catch {
      /* non-JSON error body */
    }
    if (!res.ok) {
      throw new Error(body?.message || `Opplasting feilet (${res.status})`)
    }
    if (!body) {
      throw new Error('Uventet svar fra server')
    }
    status.value = body.skipped
      ? `Hoppet over: ${body.message}`
      : `OK: ${body.chunksIngested} chunks for ${body.filename} (${body.documentId.slice(0, 12)}…)`
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
    const res = await fetch(`/api/admin/tools/documents/${encodeURIComponent(documentId)}`, {
      method: 'DELETE',
      headers: authHeaders(),
    })
    if (!res.ok && res.status !== 204) {
      throw new Error(`Sletting feilet (${res.status})`)
    }
    await loadData()
  } catch (e: unknown) {
    error.value = e instanceof Error ? e.message : 'Ukjent feil'
  } finally {
    busy.value = false
  }
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
            <tr v-for="d in documents" :key="d.documentId" class="border-b border-gray-100">
              <td class="py-2 pr-4">{{ d.filename }}</td>
              <td class="py-2 pr-4 font-mono text-xs">{{ d.documentId.slice(0, 16) }}…</td>
              <td class="py-2 pr-4">{{ d.chunkCount }}</td>
              <td class="py-2 pr-4 text-gray-600">{{ d.lastIngestedAt || '—' }}</td>
              <td class="py-2">
                <button
                  type="button"
                  class="text-red-600 hover:underline cursor-pointer disabled:opacity-50"
                  :disabled="busy"
                  @click="removeDoc(d.documentId)"
                >
                  Slett
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>
  </div>
</template>
