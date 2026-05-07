<script setup lang="ts">
import { computed, inject, onMounted, onUnmounted, ref, type Ref } from 'vue'
import { useRouter } from 'vue-router'

type ProjectSectionNav = {
	openBachelor: () => void
}
const sectionNav = inject<ProjectSectionNav | null>('projectSectionNav', null)
const router = useRouter()

function onReadMoreBachelor() {
	if (sectionNav) {
		sectionNav.openBachelor()
		return
	}
	void router.push({ path: '/project', hash: '#section-bachelor' })
}
import { useIntersectionObserver } from '@vueuse/core'
import { useLangStore } from '@/stores/lang'
import { Badge } from '@/components/ui/badge'
import {
  Cpu,
  Server,
  Globe,
  BrainCircuit,
  Layers,
  MonitorSmartphone,
  BookOpen,
} from 'lucide-vue-next'

const langStore = useLangStore()
const isNo = computed(() => langStore.language === 'no')

const title = computed(() => (isNo.value ? 'Teknologistakk' : 'Tech stack'))
const lastUpdated = computed(() =>
  isNo.value ? 'Sist oppdatert: mai 2026' : 'Last updated: May 2026',
)

const headerBadge = computed(() =>
  isNo.value ? 'IDATT2901 · Piscada AS' : 'IDATT2901 · Piscada AS',
)

const headerSubtitle = computed(() =>
  isNo.value
    ? 'Kort oversikt over arkitektur og hovedvalg. Detaljer og kilder finner du i README og bachelor-seksjonen.'
    : 'A short overview of architecture and main choices. Details and sources live in the README and the Bachelor section.',
)

const contextParagraph = computed(() =>
  isNo.value
    ? 'Utviklet som del av bacheloroppgave (IDATT2901, NTNU) med Piscada AS: en AI-assistent for energidata og sparing i næringsbygg.'
    : 'Developed as part of a bachelor thesis (IDATT2901, NTNU) with Piscada AS: an AI assistant for energy data and savings in commercial buildings.',
)

const contextLinkLabel = computed(() =>
  isNo.value ? 'Les mer om bacheloroppgaven' : 'Read more about the bachelor thesis',
)

type Category = 'all' | 'ai' | 'backend' | 'frontend' | 'integration' | 'devops'

interface Section {
  id: string
  heading: string
  paragraphs: string[]
  icon: typeof Cpu
  category: Category
  badges: string[]
}

function useReducedMotion(): Ref<boolean> {
  const reduce = ref(false)
  let mq: MediaQueryList | null = null
  const update = () => {
    reduce.value = window.matchMedia('(prefers-reduced-motion: reduce)').matches
  }
  onMounted(() => {
    if (typeof window.matchMedia !== 'function') {
      reduce.value = false
      return
    }
    mq = window.matchMedia('(prefers-reduced-motion: reduce)')
    update()
    mq.addEventListener('change', update)
  })
  onUnmounted(() => mq?.removeEventListener('change', update))
  return reduce
}

function useRevealSection(reduceMotion: Ref<boolean>) {
  const target = ref<HTMLElement | null>(null)
  const visible = ref(false)
  useIntersectionObserver(
    target,
    ([entry]) => {
      if (entry?.isIntersecting) visible.value = true
    },
    { threshold: 0.08, rootMargin: '0px 0px -32px 0px' },
  )

  const motionClass = computed(() => {
    if (reduceMotion.value) {
      return 'opacity-100'
    }
    return [
      'transition-[opacity,transform] duration-700 ease-out motion-reduce:transition-none motion-reduce:opacity-100 motion-reduce:translate-y-0',
      visible.value ? 'opacity-100 translate-y-0' : 'opacity-0 translate-y-8',
    ].join(' ')
  })

  return { target, motionClass }
}

const reduceMotion = useReducedMotion()
const { target: refContext, motionClass: clsContext } = useRevealSection(reduceMotion)
const { target: refPillars, motionClass: clsPillars } = useRevealSection(reduceMotion)
const { target: refSections, motionClass: clsSections } = useRevealSection(reduceMotion)
const { target: refStack, motionClass: clsStack } = useRevealSection(reduceMotion)
const { target: refFooter, motionClass: clsFooter } = useRevealSection(reduceMotion)

const pillarCards = computed(() =>
  isNo.value
    ? [
        {
          to: '/chat' as const,
          title: 'AI & RAG',
          body: 'Spring AI mot OpenAI/Anthropic, dokumenter i PostgreSQL/pgvector (RAG).',
          cta: 'Åpne chat',
          icon: BrainCircuit,
          accentBorder: 'hover:border-blue-200',
          accentRing: 'focus-visible:ring-blue-600',
          iconBg: 'bg-blue-50 text-blue-700 group-hover:bg-blue-100',
          ctaClass: 'text-blue-700',
        },
        {
          to: '/projects' as const,
          title: 'Backend',
          body: 'Spring Boot 4, sikkerhet, JPA og REST mot SPA-en.',
          cta: 'Se prosjekter',
          icon: Server,
          accentBorder: 'hover:border-emerald-200',
          accentRing: 'focus-visible:ring-emerald-600',
          iconBg: 'bg-emerald-50 text-emerald-700 group-hover:bg-emerald-100',
          ctaClass: 'text-emerald-800',
        },
        {
          to: '/' as const,
          title: 'Frontend',
          body: 'Vue 3, Vite 8, Tailwind og Reka UI (shadcn-stil).',
          cta: 'Til forsiden',
          icon: Globe,
          accentBorder: 'hover:border-sky-200',
          accentRing: 'focus-visible:ring-sky-600',
          iconBg: 'bg-sky-50 text-sky-700 group-hover:bg-sky-100',
          ctaClass: 'text-sky-800',
        },
      ]
    : [
        {
          to: '/chat' as const,
          title: 'AI & RAG',
          body: 'Spring AI to OpenAI/Anthropic, documents in PostgreSQL/pgvector (RAG).',
          cta: 'Open chat',
          icon: BrainCircuit,
          accentBorder: 'hover:border-blue-200',
          accentRing: 'focus-visible:ring-blue-600',
          iconBg: 'bg-blue-50 text-blue-700 group-hover:bg-blue-100',
          ctaClass: 'text-blue-700',
        },
        {
          to: '/projects' as const,
          title: 'Backend',
          body: 'Spring Boot 4, security, JPA, and REST for the SPA.',
          cta: 'View projects',
          icon: Server,
          accentBorder: 'hover:border-emerald-200',
          accentRing: 'focus-visible:ring-emerald-600',
          iconBg: 'bg-emerald-50 text-emerald-700 group-hover:bg-emerald-100',
          ctaClass: 'text-emerald-800',
        },
        {
          to: '/' as const,
          title: 'Frontend',
          body: 'Vue 3, Vite 8, Tailwind, and Reka UI (shadcn-style).',
          cta: 'Back to home',
          icon: Globe,
          accentBorder: 'hover:border-sky-200',
          accentRing: 'focus-visible:ring-sky-600',
          iconBg: 'bg-sky-50 text-sky-700 group-hover:bg-sky-100',
          ctaClass: 'text-sky-800',
        },
      ],
)

const stackFront = computed(() =>
  isNo.value
    ? ([
        'Vue 3, TypeScript, Vite 8 — Pinia, Vue Router, Tailwind 4, Reka UI',
        'Orval-generert klient mot OpenAPI; PostHog i nettleseren når det er slått på',
      ] as const)
    : ([
        'Vue 3, TypeScript, Vite 8 — Pinia, Vue Router, Tailwind 4, Reka UI',
        'Orval-generated client from OpenAPI; PostHog in the browser when enabled',
      ] as const),
)

const stackBack = computed(() =>
  isNo.value
    ? ([
        'Java 21, Spring Boot 4, Spring AI 2 (BOM), Spring Security, JPA → PostgreSQL 17/pgvector',
        'Innlesing og RAG-flyt i Spring AI; valgfri ONNX-rerank og OpenNLP der det trengs',
        'Docker Compose (db + API + nginx), Actuator/Prometheus; Bucket4j på offentlige endepunkter',
      ] as const)
    : ([
        'Java 21, Spring Boot 4, Spring AI 2 (BOM), Spring Security, JPA → PostgreSQL 17/pgvector',
        'Ingestion and RAG in Spring AI; optional ONNX rerank and OpenNLP where needed',
        'Docker Compose (db + API + nginx), Actuator/Prometheus; Bucket4j on public endpoints',
      ] as const),
)

const stackHeading = computed(() => (isNo.value ? 'Teknologistakk (kort)' : 'Tech stack (summary)'))

const sectionsHeading = computed(() =>
  isNo.value ? 'Hovedgrep' : 'Main ideas',
)

const sectionsIntro = computed(() =>
  isNo.value
    ? 'Fokus på det som faktisk bærer løsningen — resten ligger i kode og README.'
    : 'Focus on what actually carries the solution; the rest lives in code and the README.',
)

const footerText = computed(() =>
  isNo.value
    ? 'Mer om oppgaven og retningen finner du i bachelor-seksjonen på denne siden. Full brukerhistorier og krav ligger i avtale og veiledning med oppdragsgiver.'
    : 'More about the thesis and direction is in the Bachelor section on this page. Full user stories and requirements live in agreements and supervision with the industry partner.',
)

const footerHome = computed(() => (isNo.value ? 'Til forsiden' : 'Back to home'))

const sections = computed<Section[]>(() => {
  if (isNo.value) {
    return [
      {
        id: 'principle',
        heading: 'Prinsipp',
        paragraphs: [
          'Modne rammer på server (Spring) der det lønner seg, og et moderne SPA-skall (Vue) som er raskt å endre. Målet er forutsigbar kode og en stakk som er enkel å kjøre lokalt og i produksjon.',
        ],
        icon: Layers,
        category: 'all',
        badges: [],
      },
      {
        id: 'ai-rag',
        heading: 'KI og RAG',
        paragraphs: [
          'Spring AI 2 (BOM) kobler chat mot OpenAI og valgfritt Anthropic; embeddings og chunk-lagring ligger i PostgreSQL med pgvector. Dokumenter kommer inn via Spring AI (Tika der det trengs), og det som indekseres er kuratert via admin-flyten.',
          'Henting støtter norsk og engelsk i samme spørsmål; valgfri ONNX-rerank og leverandørspesifikke prompt-maler finnes når man trenger finere kontroll.',
        ],
        icon: BrainCircuit,
        category: 'ai',
        badges: ['Spring AI 2', 'pgvector', 'OpenAI', 'Anthropic'],
      },
      {
        id: 'server-api',
        heading: 'Backend og API',
        paragraphs: [
          'Spring Boot 4 på Java 21 med sikkerhet, JPA og Bucket4j på åpne endepunkter. OpenNLP inngår i sanitering der det er aktuelt.',
          'REST er dokumentert med OpenAPI (SpringDoc); Vite proxier `/api` i utvikling, og Orval holder frontend-klienten i tråd med skjemaet.',
        ],
        icon: Server,
        category: 'backend',
        badges: ['Spring Boot 4', 'Java 21', 'OpenAPI', 'Orval'],
      },
      {
        id: 'client-ops',
        heading: 'Klient og kjøring',
        paragraphs: [
          'Vue 3, Vite 8, TypeScript, Pinia, Tailwind 4 og Reka UI for UI og tilstand.',
          'Docker Compose binder sammen Postgres, API og nginx-bygget frontend. Actuator og Prometheus gir grunnleggende innsyn; PostHog (nettleser og valgfritt serversporing) kan skrus på etter behov.',
        ],
        icon: Globe,
        category: 'devops',
        badges: ['Vue 3', 'Docker Compose', 'nginx', 'Actuator'],
      },
    ]
  }
  return [
    {
      id: 'principle',
      heading: 'Principle',
      paragraphs: [
        'Mature server-side frameworks (Spring) where they pay off, and a modern Vue SPA that is quick to change. The goal is predictable code and a stack that is easy to run locally and in production.',
      ],
      icon: Layers,
      category: 'all',
      badges: [],
    },
    {
      id: 'ai-rag',
      heading: 'AI and RAG',
      paragraphs: [
        'Spring AI 2 (BOM) wires chat to OpenAI and optionally Anthropic; embeddings and chunk storage live in PostgreSQL with pgvector. Documents enter through Spring AI (Tika where needed), and what gets indexed is curated through the admin pipeline.',
        'Retrieval spans Norwegian and English in one question; optional ONNX rerank and provider-specific prompt templates are there when you need finer control.',
      ],
      icon: BrainCircuit,
      category: 'ai',
      badges: ['Spring AI 2', 'pgvector', 'OpenAI', 'Anthropic'],
    },
    {
      id: 'server-api',
      heading: 'Backend and API',
      paragraphs: [
        'Spring Boot 4 on Java 21 with security, JPA, and Bucket4j on public endpoints. OpenNLP participates in sanitization where relevant.',
        'REST is documented with OpenAPI (SpringDoc); Vite proxies `/api` in development, and Orval keeps the frontend client aligned with the schema.',
      ],
      icon: Server,
      category: 'backend',
      badges: ['Spring Boot 4', 'Java 21', 'OpenAPI', 'Orval'],
    },
    {
      id: 'client-ops',
      heading: 'Client and runtime',
      paragraphs: [
        'Vue 3, Vite 8, TypeScript, Pinia, Tailwind 4, and Reka UI for UI and state.',
        'Docker Compose ties together Postgres, the API, and the nginx-built frontend. Actuator and Prometheus give basic insight; PostHog (browser and optional server-side capture) can be enabled as needed.',
      ],
      icon: Globe,
      category: 'devops',
      badges: ['Vue 3', 'Docker Compose', 'nginx', 'Actuator'],
    },
  ]
})

function sectionIconWrapClass(cat: Category): string {
  switch (cat) {
    case 'ai':
      return 'bg-blue-600 text-white shadow-md'
    case 'backend':
      return 'bg-emerald-600 text-white shadow-md'
    case 'frontend':
      return 'bg-sky-600 text-white shadow-md'
    case 'integration':
      return 'bg-teal-600 text-white shadow-md'
    case 'devops':
      return 'bg-rose-600 text-white shadow-md'
    default:
      return 'bg-slate-700 text-white shadow-md'
  }
}
</script>

<template>
  <div class="project-tech-stack-root">
    <div class="max-w-5xl mx-auto px-4 py-10 lg:py-14">
      <header
        class="relative overflow-hidden rounded-2xl border border-slate-200/80 bg-white/90 p-8 shadow-lg shadow-slate-900/5 backdrop-blur-sm lg:p-10"
      >
        <div
          class="pointer-events-none absolute -right-16 -top-16 h-48 w-48 rounded-full bg-blue-100/60 blur-3xl"
          aria-hidden="true"
        />
        <div
          class="pointer-events-none absolute -bottom-20 -left-10 h-56 w-56 rounded-full bg-slate-200/40 blur-3xl"
          aria-hidden="true"
        />
        <div class="relative flex flex-col gap-3">
          <p
            class="mb-1 inline-flex w-fit items-center gap-2 rounded-full bg-blue-50 px-3 py-1 text-sm font-medium text-blue-800"
          >
            <BookOpen class="h-4 w-4 shrink-0" aria-hidden="true" />
            {{ headerBadge }}
          </p>
          <h1 class="text-3xl font-bold tracking-tight text-slate-900 lg:text-4xl">
            {{ title }}
          </h1>
          <p class="max-w-2xl text-lg text-slate-600">
            {{ headerSubtitle }}
          </p>
          <p class="text-sm text-slate-500">{{ lastUpdated }}</p>
        </div>
      </header>

      <section ref="refContext" :class="['mt-10', clsContext]" aria-labelledby="ctx-heading">
        <h2 id="ctx-heading" class="sr-only">{{ isNo ? 'Kontekst' : 'Context' }}</h2>
        <div
          class="rounded-2xl border border-slate-200 bg-white p-6 shadow-md transition-shadow hover:shadow-lg lg:p-8"
        >
          <div class="flex items-start gap-4">
            <div
              class="flex h-12 w-12 shrink-0 items-center justify-center rounded-xl bg-blue-600 text-white shadow-md"
            >
              <BrainCircuit class="h-6 w-6" aria-hidden="true" />
            </div>
            <div class="space-y-3 text-slate-700">
              <p class="text-base leading-relaxed lg:text-lg">
                {{ contextParagraph }}
              </p>
              <p>
                <button
                  type="button"
                  class="text-sm font-medium text-blue-700 underline underline-offset-2 hover:text-blue-900 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-600 focus-visible:ring-offset-2"
                  @click="onReadMoreBachelor"
                >
                  {{ contextLinkLabel }}
                </button>
              </p>
            </div>
          </div>
        </div>
      </section>

      <section ref="refPillars" :class="['mt-12', clsPillars]" aria-labelledby="pillars-heading">
        <div class="mb-6 text-center lg:text-left">
          <h2 id="pillars-heading" class="text-2xl font-bold text-slate-900">
            {{ isNo ? 'Hovedspor i løsningen' : 'Main tracks in the solution' }}
          </h2>
          <p class="mt-1 text-slate-600">
            {{ isNo ? 'Tre innganger som speiler kjernen i arkitekturen.' : 'Three entry points that mirror the core architecture.' }}
          </p>
        </div>
        <div class="grid gap-5 md:grid-cols-3">
          <RouterLink
            v-for="card in pillarCards"
            :key="card.title"
            :to="card.to"
            :class="[
              'group relative flex flex-col rounded-2xl border border-slate-200 bg-white p-6 text-left shadow-md transition-all duration-300 hover:-translate-y-1 hover:shadow-xl focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-offset-2 motion-reduce:hover:translate-y-0',
              card.accentBorder,
              card.accentRing,
            ]"
          >
            <div
              :class="[
                'mb-4 inline-flex h-11 w-11 items-center justify-center rounded-xl transition-colors',
                card.iconBg,
              ]"
            >
              <component :is="card.icon" class="h-6 w-6" aria-hidden="true" />
            </div>
            <h3 class="text-lg font-semibold text-slate-900">{{ card.title }}</h3>
            <p class="mt-2 flex-1 text-sm leading-relaxed text-slate-600">
              {{ card.body }}
            </p>
            <span
              :class="[
                'mt-4 inline-flex items-center text-sm font-semibold group-hover:gap-1',
                card.ctaClass,
              ]"
            >
              {{ card.cta }}
              <span class="transition-transform group-hover:translate-x-0.5" aria-hidden="true">→</span>
            </span>
          </RouterLink>
        </div>
      </section>

      <section ref="refSections" :class="['mt-14', clsSections]" aria-labelledby="sections-heading">
        <h2 id="sections-heading" class="text-2xl font-bold text-slate-900">
          {{ sectionsHeading }}
        </h2>
        <p class="mt-1 text-slate-600">{{ sectionsIntro }}</p>
        <div class="mt-6 flex flex-col gap-5">
          <article
            v-for="section in sections"
            :key="section.id"
            class="rounded-xl border border-slate-200 bg-white p-5 shadow-sm transition-shadow hover:shadow-md lg:p-6"
          >
            <div class="flex flex-col gap-4 sm:flex-row sm:items-start">
              <div
                :class="[
                  'flex h-11 w-11 shrink-0 items-center justify-center rounded-xl',
                  sectionIconWrapClass(section.category),
                ]"
              >
                <component :is="section.icon" class="h-5 w-5" aria-hidden="true" />
              </div>
              <div class="min-w-0 flex-1">
                <h3 class="text-lg font-semibold text-slate-900">{{ section.heading }}</h3>
                <div class="mt-3 space-y-3 text-sm leading-relaxed text-slate-600">
                  <p v-for="(p, pIdx) in section.paragraphs" :key="pIdx">{{ p }}</p>
                </div>
                <div v-if="section.badges.length" class="mt-4 flex flex-wrap gap-2">
                  <Badge
                    v-for="badge in section.badges"
                    :key="badge"
                    variant="secondary"
                    class="text-xs border border-slate-200 bg-slate-50 text-slate-800 hover:border-blue-200 hover:bg-blue-50/80"
                  >
                    {{ badge }}
                  </Badge>
                </div>
              </div>
            </div>
          </article>
        </div>
      </section>

      <section ref="refStack" :class="['mt-14', clsStack]" aria-labelledby="stack-heading">
        <h2 id="stack-heading" class="text-2xl font-bold text-slate-900">{{ stackHeading }}</h2>
        <div class="mt-6 grid gap-5 lg:grid-cols-2">
          <div
            class="rounded-2xl border border-slate-200 bg-gradient-to-br from-white to-slate-50 p-6 shadow-md"
          >
            <div class="mb-4 flex items-center gap-2 text-slate-900">
              <MonitorSmartphone class="h-5 w-5 text-blue-600" aria-hidden="true" />
              <h3 class="text-lg font-semibold">{{ isNo ? 'Frontend' : 'Frontend' }}</h3>
            </div>
            <ul class="space-y-2 text-sm text-slate-700">
              <li v-for="line in stackFront" :key="line" class="flex gap-2">
                <span class="mt-1.5 h-1.5 w-1.5 shrink-0 rounded-full bg-blue-500" aria-hidden="true" />
                <span>{{ line }}</span>
              </li>
            </ul>
          </div>
          <div
            class="rounded-2xl border border-slate-200 bg-gradient-to-br from-white to-slate-50 p-6 shadow-md"
          >
            <div class="mb-4 flex items-center gap-2 text-slate-900">
              <Server class="h-5 w-5 text-emerald-600" aria-hidden="true" />
              <h3 class="text-lg font-semibold">{{ isNo ? 'Backend' : 'Backend' }}</h3>
            </div>
            <ul class="space-y-2 text-sm text-slate-700">
              <li v-for="line in stackBack" :key="line" class="flex gap-2">
                <span
                  class="mt-1.5 h-1.5 w-1.5 shrink-0 rounded-full bg-emerald-500"
                  aria-hidden="true"
                />
                <span>{{ line }}</span>
              </li>
            </ul>
          </div>
        </div>
      </section>

      <footer
        ref="refFooter"
        :class="[
          'mt-14 flex flex-col gap-4 border-t border-slate-200 pt-8 sm:flex-row sm:items-center sm:justify-between',
          clsFooter,
        ]"
      >
        <p class="text-sm text-slate-600">
          {{ footerText }}
        </p>
        <div class="flex flex-wrap gap-2 sm:justify-end">
          <RouterLink
            to="/"
            class="inline-flex items-center justify-center rounded-lg border border-slate-300 bg-white px-5 py-2.5 text-sm font-semibold text-slate-800 shadow-sm transition hover:bg-slate-50 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-600 focus-visible:ring-offset-2"
          >
            {{ footerHome }}
          </RouterLink>
        </div>
      </footer>
    </div>
  </div>
</template>
