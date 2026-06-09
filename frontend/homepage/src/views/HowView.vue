<script setup lang="ts">
import { computed, nextTick, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ChevronDown, FolderKanban } from 'lucide-vue-next'
import { useLangStore } from '@/stores/lang'
import ProjectBachelorSection from '@/components/project/ProjectBachelorSection.vue'
import ProjectHowAiWorkflowSection from '@/components/project/ProjectHowAiWorkflowSection.vue'
import ProjectFutureWorkSection from '@/components/project/ProjectFutureWorkSection.vue'

const langStore = useLangStore()
const route = useRoute()
const isNo = computed(() => langStore.language === 'no')

const hero = computed(() =>
  isNo.value
    ? {
        title: 'Hvordan',
        lead: 'Hvordan portefoljen er bygget: bachelorkontekst, AI-støttet utviklingsflyt og videre arbeid.',
      }
    : {
        title: 'How',
        lead: 'How the portfolio is built: bachelor context, AI-supported development workflow, and future work.',
      },
)

const labels = computed(() =>
  isNo.value
    ? { bachelor: 'Bacheloroppgaven', workflow: 'AI i utviklingsflyten', future: 'Videre arbeid' }
    : { bachelor: "Bachelor's thesis", workflow: 'AI in the development workflow', future: 'Future work' },
)

const bachelorOpen = ref(false)
const workflowOpen = ref(false)
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
  } else if (h === 'ai-workflow') {
    workflowOpen.value = true
    scrollToSection('accordion-ai-workflow')
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
  <main id="main-content" class="min-h-screen bg-gradient-to-br from-slate-50 to-slate-100 relative pb-20 pt-20">
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
            class="flex w-full items-center justify-between gap-3 px-5 py-4 text-left transition hover:bg-slate-50/80 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-inset focus-visible:ring-blue-600"
            :aria-expanded="bachelorOpen"
            aria-controls="panel-bachelor"
            id="section-bachelor"
            @click="bachelorOpen = !bachelorOpen"
          >
            <span class="text-lg font-semibold text-slate-900">{{ labels.bachelor }}</span>
            <ChevronDown class="h-5 w-5 shrink-0 text-slate-500 transition-transform duration-200" :class="{ 'rotate-180': bachelorOpen }" aria-hidden="true" />
          </button>
          <div
            v-show="bachelorOpen"
            id="panel-bachelor"
            role="region"
            aria-labelledby="section-bachelor"
            class="border-t border-slate-200/80"
          >
            <ProjectBachelorSection />
          </div>
        </section>

        <section id="accordion-ai-workflow" class="rounded-2xl border border-slate-200/80 bg-white/90 shadow-lg shadow-slate-900/5 overflow-hidden">
          <button
            type="button"
            class="flex w-full items-center justify-between gap-3 px-5 py-4 text-left transition hover:bg-slate-50/80 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-inset focus-visible:ring-blue-600"
            :aria-expanded="workflowOpen"
            aria-controls="panel-ai-workflow"
            id="section-ai-workflow"
            @click="workflowOpen = !workflowOpen"
          >
            <span class="text-lg font-semibold text-slate-900">{{ labels.workflow }}</span>
            <ChevronDown class="h-5 w-5 shrink-0 text-slate-500 transition-transform duration-200" :class="{ 'rotate-180': workflowOpen }" aria-hidden="true" />
          </button>
          <div
            v-show="workflowOpen"
            id="panel-ai-workflow"
            role="region"
            aria-labelledby="section-ai-workflow"
            class="border-t border-slate-200/80"
          >
            <ProjectHowAiWorkflowSection />
          </div>
        </section>

        <section id="accordion-future-work" class="rounded-2xl border border-slate-200/80 bg-white/90 shadow-lg shadow-slate-900/5 overflow-hidden">
          <button
            type="button"
            class="flex w-full items-center justify-between gap-3 px-5 py-4 text-left transition hover:bg-slate-50/80 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-inset focus-visible:ring-blue-600"
            :aria-expanded="futureOpen"
            aria-controls="panel-future-work"
            id="section-future-work"
            @click="futureOpen = !futureOpen"
          >
            <span class="text-lg font-semibold text-slate-900">{{ labels.future }}</span>
            <ChevronDown class="h-5 w-5 shrink-0 text-slate-500 transition-transform duration-200" :class="{ 'rotate-180': futureOpen }" aria-hidden="true" />
          </button>
          <div
            v-show="futureOpen"
            id="panel-future-work"
            role="region"
            aria-labelledby="section-future-work"
            class="border-t border-slate-200/80"
          >
            <ProjectFutureWorkSection />
          </div>
        </section>
      </div>
    </div>
  </main>
</template>
