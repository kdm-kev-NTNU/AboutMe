<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { RouterLink } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { healthChroma, type VectorStoreHealthResponse } from '@/api/generated/portfolio'

/** Admin hub: links to pipeline / chunks / prompts plus GET /health/chroma (alias, no Basic auth required). */
const auth = useAuthStore()
const chromaHealth = ref<VectorStoreHealthResponse | null>(null)
const chromaHealthLoading = ref(false)

onMounted(() => {
  auth.restore()
  chromaHealthLoading.value = true
  healthChroma()
    .then((r) => {
      if (r.status === 200 || r.status === 503) {
        chromaHealth.value = r.data
      }
    })
    .catch(() => {
      chromaHealth.value = { healthy: false, message: 'Kunne ikke hente status' }
    })
    .finally(() => {
      chromaHealthLoading.value = false
    })
})
</script>

<template>
  <div class="min-h-screen bg-[hsl(220_20%_97%)] text-[hsl(220_25%_10%)] font-sans antialiased pb-12">
    <nav
      class="border-b border-gray-200/80 bg-white/90 backdrop-blur-sm px-4 py-3 text-sm flex flex-wrap gap-x-4 gap-y-1 items-center max-w-3xl mx-auto"
    >
      <RouterLink to="/" class="text-blue-600 hover:underline">Hjem</RouterLink>
      <span class="text-gray-500">/</span>
      <strong class="text-gray-900">Internal tools</strong>
    </nav>

    <main class="mx-auto max-w-3xl px-4 pt-8">
      <h1 class="text-2xl font-semibold tracking-tight text-gray-900 mb-2">Internal tools</h1>
      <p class="text-sm text-gray-600 mb-8 leading-relaxed">
        Verktøy for dokumentpipeline og pgvector. Krever admin-innlogging. Velg en side under. Samme idé som
        Piscada tools-hub.
      </p>

      <!-- Vector store quick health -->
      <section
        class="mb-8 rounded-xl border border-gray-200 bg-white p-4 shadow-[0_1px_3px_rgb(0_0_0/0.06)]"
      >
        <h2 class="text-sm font-semibold text-gray-900 mb-2">Vektorlagring (hurtigstatus)</h2>
        <p v-if="chromaHealthLoading" class="text-sm text-gray-500">Sjekker…</p>
        <template v-else-if="chromaHealth">
          <p class="text-sm" :class="chromaHealth.healthy ? 'text-green-700' : 'text-amber-800'">
            <span class="font-medium">{{ chromaHealth.healthy ? 'Tilgjengelig' : 'Problem' }}</span>
            <span v-if="chromaHealth.collectionName" class="text-gray-700">
              : collection <code class="font-mono text-xs bg-gray-100 px-1 rounded">{{
                chromaHealth.collectionName
              }}</code>
            </span>
            <span v-if="chromaHealth.embeddingCount != null" class="text-gray-600">
              ({{ chromaHealth.embeddingCount }} embeddings)
            </span>
          </p>
          <p v-if="chromaHealth.message" class="text-xs text-gray-500 mt-1">{{ chromaHealth.message }}</p>
        </template>
      </section>

      <section>
        <h2 class="text-lg font-semibold text-gray-900 mb-4">Verktøy</h2>
        <ul class="space-y-4">
          <li
            class="rounded-xl border border-gray-200 bg-white p-5 shadow-[0_1px_3px_rgb(0_0_0/0.06)] hover:border-gray-300 transition-colors"
          >
            <RouterLink to="/admin/pipeline" class="block group">
              <span class="text-base font-semibold text-blue-600 group-hover:underline">Document pipeline</span>
              <p class="text-sm text-gray-600 mt-2 leading-relaxed">
                Last opp og indekser dokumenter til PostgreSQL/pgvector: enkeltfil, batch fra data-mappe, re-seed og liste over
                indekserte dokumenter.
              </p>
            </RouterLink>
          </li>
          <li
            class="rounded-xl border border-gray-200 bg-white p-5 shadow-[0_1px_3px_rgb(0_0_0/0.06)] hover:border-gray-300 transition-colors"
          >
            <RouterLink to="/admin/chunks" class="block group">
              <span class="text-base font-semibold text-blue-600 group-hover:underline">Chunk viewer</span>
              <p class="text-sm text-gray-600 mt-2 leading-relaxed">
                Se chunks i aktiv collection: tabell med dokument, chunk #, content hash, tekst (forkortet), utvidbar
                rad med full tekst og metadata, inspirert av Piscada chunk-visning.
              </p>
            </RouterLink>
          </li>
          <li
            class="rounded-xl border border-gray-200 bg-white p-5 shadow-[0_1px_3px_rgb(0_0_0/0.06)] hover:border-gray-300 transition-colors"
          >
            <RouterLink to="/admin/prompts" class="block group">
              <span class="text-base font-semibold text-blue-600 group-hover:underline">Prompt versions</span>
              <p class="text-sm text-gray-600 mt-2 leading-relaxed">
                Administrer versjonerte prompt-maler: opprett, aktiver, sammenlign mot classpath, og seed fra
                .st-filer, inspirert av Piscadas prompt_versions-arkitektur.
              </p>
            </RouterLink>
          </li>
          <li
            class="rounded-xl border border-gray-200 bg-white p-5 shadow-[0_1px_3px_rgb(0_0_0/0.06)] hover:border-gray-300 transition-colors"
          >
            <RouterLink to="/admin/experiments" class="block group">
              <span class="text-base font-semibold text-blue-600 group-hover:underline">Experiments (Phoenix)</span>
              <p class="text-sm text-gray-600 mt-2 leading-relaxed">
                Kjør RAG-eval mot datasett i Arize Phoenix (Railway/lokal), LLM-as-judge (faithfulness, relevance,
                correctness, conciseness), og se aggregerte scorer lagret i MySQL, inspirert av Piscada eval-steg 2.
              </p>
            </RouterLink>
          </li>
        </ul>
      </section>
    </main>
  </div>
</template>
