<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { RouterLink } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import {
  promptVersionsNames,
  promptVersionsHistory,
  promptVersionsCreate,
  promptVersionsActivate,
  promptVersionsSeed,
  promptVersionsDeleteVariant,
  promptVersionsDiff,
  type PromptNameEntry,
  type PromptVersionResponse,
  type PromptDiffResponse,
} from '@/api/generated/portfolio'
import { formatAdminHttpError as formatHttpError } from '@/lib/api-error'

// Versioned RAG prompts: list, history, create, activate, diff vs classpath, seed, delete variant.
const auth = useAuthStore()
const error = ref('')
const busy = ref(false)
const successMsg = ref('')

const activeNames = ref<PromptNameEntry[]>([])

const selectedVariant = ref<{ name: string; language?: string; provider?: string } | null>(null)
const history = ref<PromptVersionResponse[]>([])
const historyLoading = ref(false)
const expandedVersionId = ref<number | null>(null)

const createOpen = ref(false)
const createName = ref('')
const createLanguage = ref('')
const createProvider = ref('')
const createContent = ref('')
const createDescription = ref('')
const createBusy = ref(false)

const diffData = ref<PromptDiffResponse | null>(null)
const diffLoading = ref(false)

function clearMessages() {
  error.value = ''
  successMsg.value = ''
}

async function loadNames() {
  clearMessages()
  try {
    const r = await promptVersionsNames()
    if (r.status !== 200) throw new Error(formatHttpError(r.status, r.data))
    activeNames.value = r.data
  } catch (e: unknown) {
    error.value = e instanceof Error ? e.message : 'Ukjent feil'
  }
}

async function loadHistory(name: string, language?: string, provider?: string) {
  selectedVariant.value = { name, language, provider }
  historyLoading.value = true
  history.value = []
  expandedVersionId.value = null
  diffData.value = null
  try {
    const r = await promptVersionsHistory({ name, language, provider })
    if (r.status !== 200) throw new Error(formatHttpError(r.status, r.data))
    history.value = r.data
  } catch (e: unknown) {
    error.value = e instanceof Error ? e.message : 'Ukjent feil'
  } finally {
    historyLoading.value = false
  }
}

async function activateVersion(id: number) {
  clearMessages()
  busy.value = true
  try {
    const r = await promptVersionsActivate({ id })
    if (r.status !== 200) throw new Error(formatHttpError(r.status, r.data))
    successMsg.value = `Versjon ${id} aktivert.`
    await loadNames()
    if (selectedVariant.value) {
      await loadHistory(
        selectedVariant.value.name,
        selectedVariant.value.language,
        selectedVariant.value.provider,
      )
    }
  } catch (e: unknown) {
    error.value = e instanceof Error ? e.message : 'Ukjent feil'
  } finally {
    busy.value = false
  }
}

async function handleCreate() {
  if (!createName.value.trim() || !createContent.value.trim()) {
    error.value = 'Navn og innhold er påkrevd.'
    return
  }
  clearMessages()
  createBusy.value = true
  try {
    const r = await promptVersionsCreate({
      name: createName.value.trim(),
      content: createContent.value,
      language: createLanguage.value.trim() || undefined,
      provider: createProvider.value.trim() || undefined,
      description: createDescription.value.trim() || undefined,
    })
    if (r.status !== 200) throw new Error(formatHttpError(r.status, r.data))
    successMsg.value = `Versjon ${(r.data as PromptVersionResponse).version} opprettet for "${createName.value}".`
    createOpen.value = false
    createName.value = ''
    createContent.value = ''
    createLanguage.value = ''
    createProvider.value = ''
    createDescription.value = ''
    await loadNames()
    if (selectedVariant.value) {
      await loadHistory(
        selectedVariant.value.name,
        selectedVariant.value.language,
        selectedVariant.value.provider,
      )
    }
  } catch (e: unknown) {
    error.value = e instanceof Error ? e.message : 'Ukjent feil'
  } finally {
    createBusy.value = false
  }
}

async function seed() {
  if (!confirm('Seed prompt-versjoner fra classpath (.st-filer)? Eksisterende varianter hoppes over.')) return
  clearMessages()
  busy.value = true
  try {
    const r = await promptVersionsSeed()
    if (r.status !== 200) throw new Error(formatHttpError(r.status, r.data))
    const d = r.data as Record<string, number>
    successMsg.value = `Seed ferdig: ${d.created ?? 0} opprettet, ${d.skipped ?? 0} hoppet over.`
    await loadNames()
  } catch (e: unknown) {
    error.value = e instanceof Error ? e.message : 'Ukjent feil'
  } finally {
    busy.value = false
  }
}

async function deleteVariant(name: string, language?: string, provider?: string) {
  if (!confirm(`Slette ALLE versjoner for "${name}" (lang=${language ?? 'null'}, prov=${provider ?? 'null'})?`))
    return
  clearMessages()
  busy.value = true
  try {
    const r = await promptVersionsDeleteVariant({ name, language, provider })
    if (r.status !== 200) throw new Error(formatHttpError(r.status, r.data))
    const d = r.data as Record<string, unknown>
    successMsg.value = `Slettet ${d.deleted ?? 0} versjon(er).`
    selectedVariant.value = null
    history.value = []
    await loadNames()
  } catch (e: unknown) {
    error.value = e instanceof Error ? e.message : 'Ukjent feil'
  } finally {
    busy.value = false
  }
}

async function loadDiff(name: string, language?: string, provider?: string) {
  diffLoading.value = true
  diffData.value = null
  try {
    const r = await promptVersionsDiff({ name, language, provider })
    if (r.status !== 200) throw new Error(formatHttpError(r.status, r.data))
    diffData.value = r.data
  } catch (e: unknown) {
    error.value = e instanceof Error ? e.message : 'Ukjent feil'
  } finally {
    diffLoading.value = false
  }
}

function prefillCreate(name: string, language?: string, provider?: string, content?: string) {
  createName.value = name
  createLanguage.value = language ?? ''
  createProvider.value = provider ?? ''
  createContent.value = content ?? ''
  createDescription.value = ''
  createOpen.value = true
}

const historyTitle = computed(() => {
  if (!selectedVariant.value) return ''
  const v = selectedVariant.value
  let s = v.name
  if (v.language) s += ` / ${v.language}`
  if (v.provider) s += ` / ${v.provider}`
  return s
})

onMounted(() => {
  auth.restore()
  void loadNames()
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
      <strong class="text-gray-900">Prompt versions</strong>
    </nav>

    <main id="main-content" class="mx-auto max-w-5xl px-4 pt-8">
      <h1 class="text-2xl font-semibold tracking-tight text-gray-900 mb-2">Prompt versions</h1>
      <p class="text-sm text-gray-600 mb-6 leading-relaxed">
        Versjoner, redigering og aktivering av prompt-maler. Speilet fra Piscadas prompt_versions-arkitektur,
        tilpasset MySQL + Spring AI.
      </p>

      <p
        v-if="error"
        class="mb-4 text-sm text-red-700 rounded-md border border-red-200 bg-red-50 px-3 py-2"
      >
        {{ error }}
      </p>
      <p
        v-if="successMsg"
        class="mb-4 text-sm text-green-800 rounded-md border border-green-200 bg-green-50/80 px-3 py-2"
      >
        {{ successMsg }}
      </p>

      <!-- Actions bar -->
      <section class="mb-6 flex flex-wrap gap-2">
        <button
          type="button"
          class="px-3 py-2 text-sm rounded-md border border-gray-200 bg-white hover:bg-gray-50 cursor-pointer"
          @click="loadNames"
        >
          Oppdater
        </button>
        <button
          type="button"
          class="px-3 py-2 text-sm rounded-md border border-amber-200 bg-amber-50 hover:bg-amber-100 cursor-pointer disabled:opacity-50"
          :disabled="busy"
          @click="seed"
        >
          {{ busy ? 'Seeder…' : 'Seed fra classpath' }}
        </button>
        <button
          type="button"
          class="px-3 py-2 text-sm rounded-md border border-blue-200 bg-blue-50 hover:bg-blue-100 cursor-pointer text-blue-800"
          @click="createOpen = !createOpen"
        >
          {{ createOpen ? 'Lukk opprettelse' : 'Ny versjon' }}
        </button>
      </section>

      <!-- Create form disclosure -->
      <section
        v-if="createOpen"
        class="mb-6 rounded-xl border border-blue-200 bg-white p-4 shadow-[0_1px_3px_rgb(0_0_0/0.06)]"
      >
        <h2 class="text-sm font-semibold text-gray-900 mb-3">Opprett ny prompt-versjon</h2>
        <div class="grid grid-cols-1 sm:grid-cols-2 gap-4 max-w-2xl">
          <div class="space-y-1">
            <label class="block text-sm font-medium text-gray-800">Navn</label>
            <input
              v-model="createName"
              type="text"
              class="w-full border border-gray-200 rounded-lg px-3 py-2 text-sm bg-white"
              placeholder="f.eks. rag_portfolio"
            />
          </div>
          <div class="space-y-1">
            <label class="block text-sm font-medium text-gray-800">Provider (valgfritt)</label>
            <input
              v-model="createProvider"
              type="text"
              class="w-full border border-gray-200 rounded-lg px-3 py-2 text-sm bg-white"
              placeholder="openai / anthropic"
            />
          </div>
          <div class="space-y-1">
            <label class="block text-sm font-medium text-gray-800">Language (valgfritt)</label>
            <input
              v-model="createLanguage"
              type="text"
              class="w-full border border-gray-200 rounded-lg px-3 py-2 text-sm bg-white"
              placeholder="en / no"
            />
          </div>
          <div class="space-y-1">
            <label class="block text-sm font-medium text-gray-800">Beskrivelse (valgfritt)</label>
            <input
              v-model="createDescription"
              type="text"
              class="w-full border border-gray-200 rounded-lg px-3 py-2 text-sm bg-white"
              placeholder="Endring av tone / regler"
            />
          </div>
        </div>
        <div class="mt-4 space-y-1 max-w-2xl">
          <label class="block text-sm font-medium text-gray-800">Innhold (prompt template)</label>
          <textarea
            v-model="createContent"
            rows="12"
            class="w-full border border-gray-200 rounded-lg px-3 py-2 text-sm bg-white font-mono text-[12px] leading-relaxed"
            placeholder="Skriv prompt-malen her…"
          ></textarea>
        </div>
        <button
          type="button"
          class="mt-4 px-4 py-2 rounded-lg text-sm font-medium text-white bg-[hsl(221_83%_53%)] hover:bg-[hsl(221_83%_48%)] disabled:opacity-50 cursor-pointer"
          :disabled="createBusy"
          @click="handleCreate"
        >
          {{ createBusy ? 'Oppretter…' : 'Opprett versjon' }}
        </button>
      </section>

      <!-- Active variants table -->
      <section class="mb-8 rounded-xl border border-gray-200 bg-white shadow-[0_1px_3px_rgb(0_0_0/0.06)]">
        <div class="px-4 pt-4 pb-2">
          <h2 class="text-sm font-semibold text-gray-900">Aktive varianter</h2>
          <p class="text-xs text-gray-600 mt-1">Klikk på en rad for å se historikk og diff.</p>
        </div>
        <div v-if="activeNames.length === 0" class="px-4 pb-4 text-sm text-gray-500">
          Ingen prompt-versjoner funnet. Klikk «Seed fra classpath» for å opprette initielle versjoner.
        </div>
        <div v-else class="overflow-x-auto">
          <table class="w-full text-sm border-collapse">
            <thead>
              <tr class="bg-gray-100 text-gray-800">
                <th class="border border-gray-200 px-3 py-2 text-left font-semibold">Navn</th>
                <th class="border border-gray-200 px-3 py-2 text-left font-semibold">Language</th>
                <th class="border border-gray-200 px-3 py-2 text-left font-semibold">Provider</th>
                <th class="border border-gray-200 px-3 py-2 text-left font-semibold">Aktiv v.</th>
                <th class="border border-gray-200 px-3 py-2 text-left font-semibold">Opprettet</th>
                <th class="border border-gray-200 px-3 py-2 text-left font-semibold">Handlinger</th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="entry in activeNames"
                :key="`${entry.name}-${entry.language}-${entry.provider}`"
                class="bg-white hover:bg-gray-50/80 cursor-pointer"
                @click="loadHistory(entry.name ?? '', entry.language ?? undefined, entry.provider ?? undefined)"
              >
                <td class="border border-gray-200 px-3 py-2 font-medium">{{ entry.name }}</td>
                <td class="border border-gray-200 px-3 py-2 text-gray-600">{{ entry.language ?? '–' }}</td>
                <td class="border border-gray-200 px-3 py-2 text-gray-600">{{ entry.provider ?? '–' }}</td>
                <td class="border border-gray-200 px-3 py-2 font-mono text-xs">v{{ entry.activeVersion }}</td>
                <td class="border border-gray-200 px-3 py-2 text-xs text-gray-600">
                  {{ entry.createdAt?.slice(0, 19) ?? '–' }}
                </td>
                <td class="border border-gray-200 px-3 py-2">
                  <div class="flex gap-2">
                    <button
                      type="button"
                      class="text-blue-600 hover:underline text-xs cursor-pointer"
                      @click.stop="
                        loadDiff(entry.name ?? '', entry.language ?? undefined, entry.provider ?? undefined)
                      "
                    >
                      Diff
                    </button>
                    <button
                      type="button"
                      class="text-red-600 hover:underline text-xs cursor-pointer"
                      @click.stop="
                        deleteVariant(entry.name ?? '', entry.language ?? undefined, entry.provider ?? undefined)
                      "
                    >
                      Slett
                    </button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>

      <!-- Diff panel -->
      <section
        v-if="diffData || diffLoading"
        class="mb-8 rounded-xl border border-gray-200 bg-white p-4 shadow-[0_1px_3px_rgb(0_0_0/0.06)]"
      >
        <h2 class="text-sm font-semibold text-gray-900 mb-2">
          Diff: {{ diffData?.name ?? '…' }}
          <span v-if="diffData?.provider" class="text-gray-500 font-normal"> / {{ diffData.provider }}</span>
        </h2>
        <p v-if="diffLoading" class="text-sm text-gray-500">Laster diff…</p>
        <template v-else-if="diffData">
          <div class="flex flex-wrap gap-4 text-xs text-gray-700 mb-3">
            <span>
              DB aktiv: <strong :class="diffData.hasDbActive ? 'text-green-700' : 'text-amber-700'">{{
                diffData.hasDbActive ? 'Ja' : 'Nei'
              }}</strong>
            </span>
            <span>
              Classpath fallback:
              <strong :class="diffData.hasCodeFallback ? 'text-green-700' : 'text-amber-700'">{{
                diffData.hasCodeFallback ? 'Ja' : 'Nei'
              }}</strong>
            </span>
            <span>
              Identisk:
              <strong :class="diffData.isEqual ? 'text-green-700' : 'text-amber-700'">{{
                diffData.isEqual ? 'Ja' : 'Nei'
              }}</strong>
            </span>
          </div>
          <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <p class="text-xs font-medium text-gray-800 mb-1">DB (aktiv)</p>
              <pre
                class="text-[11px] font-mono whitespace-pre-wrap break-words bg-gray-50 border border-gray-200 rounded-lg p-2 max-h-64 overflow-y-auto"
                >{{ diffData.dbContent ?? '(ingen)' }}</pre
              >
            </div>
            <div>
              <p class="text-xs font-medium text-gray-800 mb-1">Classpath fallback</p>
              <pre
                class="text-[11px] font-mono whitespace-pre-wrap break-words bg-gray-50 border border-gray-200 rounded-lg p-2 max-h-64 overflow-y-auto"
                >{{ diffData.fallbackContent ?? '(ingen)' }}</pre
              >
            </div>
          </div>
        </template>
      </section>

      <!-- History panel -->
      <section
        v-if="selectedVariant"
        class="rounded-xl border border-gray-200 bg-white shadow-[0_1px_3px_rgb(0_0_0/0.06)]"
      >
        <div class="px-4 pt-4 pb-2 flex flex-wrap items-center justify-between gap-2">
          <h2 class="text-sm font-semibold text-gray-900">
            Historikk: {{ historyTitle }}
          </h2>
          <button
            type="button"
            class="text-xs text-blue-600 hover:underline cursor-pointer"
            @click="
              prefillCreate(
                selectedVariant!.name,
                selectedVariant!.language,
                selectedVariant!.provider,
                history[0]?.content,
              )
            "
          >
            Ny versjon basert på aktiv
          </button>
        </div>
        <p v-if="historyLoading" class="px-4 pb-4 text-sm text-gray-500">Laster historikk…</p>
        <div v-else-if="history.length === 0" class="px-4 pb-4 text-sm text-gray-500">
          Ingen versjoner funnet.
        </div>
        <div v-else class="overflow-x-auto">
          <table class="w-full text-sm border-collapse">
            <thead>
              <tr class="bg-gray-100 text-gray-800">
                <th class="border border-gray-200 px-3 py-2 text-left font-semibold w-8"></th>
                <th class="border border-gray-200 px-3 py-2 text-left font-semibold">Versjon</th>
                <th class="border border-gray-200 px-3 py-2 text-left font-semibold">Hash</th>
                <th class="border border-gray-200 px-3 py-2 text-left font-semibold">Status</th>
                <th class="border border-gray-200 px-3 py-2 text-left font-semibold">Beskrivelse</th>
                <th class="border border-gray-200 px-3 py-2 text-left font-semibold">Opprettet</th>
                <th class="border border-gray-200 px-3 py-2 text-left font-semibold">Handling</th>
              </tr>
            </thead>
            <tbody>
              <template v-for="v in history" :key="v.id">
                <tr
                  class="bg-white hover:bg-gray-50/80 cursor-pointer align-top"
                  :class="{ 'bg-green-50/30': v.isActive }"
                  @click="expandedVersionId = expandedVersionId === (v.id ?? 0) ? null : (v.id ?? 0)"
                >
                  <td class="border border-gray-200 px-3 py-2 text-gray-400">
                    {{ expandedVersionId === v.id ? '▼' : '▶' }}
                  </td>
                  <td class="border border-gray-200 px-3 py-2 font-mono">v{{ v.version }}</td>
                  <td
                    class="border border-gray-200 px-3 py-2 font-mono text-[11px]"
                    :title="v.contentHash"
                  >
                    {{ v.contentHash?.slice(0, 12) }}…
                  </td>
                  <td class="border border-gray-200 px-3 py-2">
                    <span
                      v-if="v.isActive"
                      class="inline-block px-1.5 py-0.5 text-[11px] font-semibold rounded bg-green-100 text-green-800"
                      >AKTIV</span
                    >
                    <span v-else class="text-xs text-gray-500">inaktiv</span>
                  </td>
                  <td class="border border-gray-200 px-3 py-2 text-xs text-gray-700 max-w-xs truncate">
                    {{ v.description || '–' }}
                  </td>
                  <td class="border border-gray-200 px-3 py-2 text-xs text-gray-600">
                    {{ v.createdAt?.slice(0, 19) ?? '–' }}
                  </td>
                  <td class="border border-gray-200 px-3 py-2">
                    <button
                      v-if="!v.isActive"
                      type="button"
                      class="text-blue-600 hover:underline text-xs cursor-pointer disabled:opacity-50"
                      :disabled="busy"
                      @click.stop="activateVersion(v.id ?? 0)"
                    >
                      Aktiver
                    </button>
                    <span v-else class="text-xs text-gray-400">–</span>
                  </td>
                </tr>
                <tr v-if="expandedVersionId === v.id" class="bg-gray-50">
                  <td colspan="7" class="border border-gray-200 px-4 py-3 text-xs">
                    <p class="font-medium text-gray-800 mb-1">Full innhold</p>
                    <pre
                      class="whitespace-pre-wrap break-words text-gray-900 max-h-72 overflow-y-auto bg-white border border-gray-200 rounded-lg p-2 font-mono text-[11px]"
                      >{{ v.content }}</pre
                    >
                    <p class="font-medium text-gray-800 mt-3 mb-1">Content hash</p>
                    <code class="text-[11px] font-mono text-gray-900 break-all">{{ v.contentHash }}</code>
                  </td>
                </tr>
              </template>
            </tbody>
          </table>
        </div>
      </section>
    </main>
  </div>
</template>
