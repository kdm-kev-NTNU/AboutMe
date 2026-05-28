<script setup lang="ts">
import { computed, nextTick, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ChevronDown, FolderKanban } from 'lucide-vue-next'
import { useLangStore } from '@/stores/lang'
import ProjectBachelorSection from '@/components/project/ProjectBachelorSection.vue'
import ProjectFutureWorkSection from '@/components/project/ProjectFutureWorkSection.vue'

const langStore = useLangStore()
const route = useRoute()
const isNo = computed(() => langStore.language === 'no')

const hero = computed(() =>
  isNo.value
    ? {
        title: 'Hvordan',
        lead: 'Hvordan portefoljen er bygget: bachelorkontekst og videre arbeid.',
      }
    : {
        title: 'How',
        lead: 'How the portfolio is built: bachelor context and future work.',
      },
)

const labels = computed(() =>
  isNo.value
    ? { bachelor: 'Bacheloroppgaven', future: 'Videre arbeid' }
    : { bachelor: "Bachelor's thesis", future: 'Future work' },
)

const bachelorOpen = ref(false)
const futureOpen = ref(false)

function scrollToSection(elementId: string) {
  nextTick(() => {
    document.getElementById(elementId)?.scrollIntoView({ behavior: 'smooth', block: 'nearest' })
  })
}

function applyRouteHash(hash: string | undefined) {
  if (!hash) return
  const h = hash.replace(/^#/, '').toLowerCase()
  if (h === 'bachelor') {
    bachelorOpen.value = true
    scrollToSection('accordion-bachelor')
  } else if (h === 'future-work') {
    futureOpen.value = true
    scrollToSection('accordion-future-work')
  }
}

watch(
  () => route.hash,
  (h) => applyRouteHash(h),
  { immediate: true },
)
</script>

<template>
  <main class="min-h-screen bg-gradient-to-br from-slate-50 to-slate-100 relative pb-20 pt-20">
    <div class="relative z-10 mx-auto max-w-6xl px-4 sm:px-6 lg:px-8">
      <div class="mb-10 text-center">
        <div class="inline-flex items-center justify-center w-14 h-14 rounded-2xl bg-gradient-to-br from-blue-600 to-indigo-700 text-white shadow-lg shadow-blue-500/25 mb-5">
          <FolderKanban class="w-8 h-8" aria-hidden="true" />
        </div>
        <h1 class="text-3xl sm:text-4xl font-bold mb-3 bg-gradient-to-r from-blue-600 via-blue-700 to-blue-800 bg-clip-text text-transparent">
          {{ hero.title }}
        </h1>
        <p class="text-gray-600 max-w-3xl mx-auto leading-relaxed text-sm sm:text-base">
          {{ hero.lead }}
        </p>
      </div>

      <div class="space-y-4">
        <section id="accordion-bachelor" class="rounded-2xl border border-slate-200/80 bg-white/90 shadow-lg shadow-slate-900/5 overflow-hidden">
          <button
            type="button"
            class="flex w-full items-center justify-between gap-3 px-5 py-4 text-left transition hover:bg-slate-50/80"
            :aria-expanded="bachelorOpen"
            @click="bachelorOpen = !bachelorOpen"
          >
            <span class="text-lg font-semibold text-slate-900">{{ labels.bachelor }}</span>
            <ChevronDown class="h-5 w-5 shrink-0 text-slate-500 transition-transform duration-200" :class="{ 'rotate-180': bachelorOpen }" />
          </button>
          <div v-show="bachelorOpen" class="border-t border-slate-200/80">
            <ProjectBachelorSection />
          </div>
        </section>

        <section id="accordion-future-work" class="rounded-2xl border border-slate-200/80 bg-white/90 shadow-lg shadow-slate-900/5 overflow-hidden">
          <button
            type="button"
            class="flex w-full items-center justify-between gap-3 px-5 py-4 text-left transition hover:bg-slate-50/80"
            :aria-expanded="futureOpen"
            @click="futureOpen = !futureOpen"
          >
            <span class="text-lg font-semibold text-slate-900">{{ labels.future }}</span>
            <ChevronDown class="h-5 w-5 shrink-0 text-slate-500 transition-transform duration-200" :class="{ 'rotate-180': futureOpen }" />
          </button>
          <div v-show="futureOpen" class="border-t border-slate-200/80">
            <ProjectFutureWorkSection />
          </div>
        </section>
      </div>
    </div>
  </main>
</template>
