<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, type Ref } from 'vue'
import { useRouter } from 'vue-router'
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
  Mic,
  Rocket,
} from 'lucide-vue-next'

const router = useRouter()

function onOpenChat() {
  void router.push({ name: 'chat' })
}

const langStore = useLangStore()
const isNo = computed(() => langStore.language === 'no')

const title = computed(() => (isNo.value ? 'Teknologistakk' : 'Tech stack'))
const lastUpdated = computed(() =>
  isNo.value ? 'Sist oppdatert: mai 2026' : 'Last updated: May 2026',
)

const headerBadge = computed(() => 'AboutMe · AI portfolio')

const headerSubtitle = computed(() =>
  isNo.value
    ? 'Kort oversikt over arkitektur, RAG, live stemme, adminverktøy og drift.'
    : 'A short overview of architecture, RAG, live voice, admin tools, and operations.',
)

const contextParagraph = computed(() =>
  isNo.value
    ? 'AboutMe kombinerer en offentlig portfolio med dokumentbasert chat, OpenAI Realtime-stemme og adminflater for å holde kunnskapsbasen, promptene og RAG-evalueringene oppdatert.'
    : 'AboutMe combines a public portfolio with document-grounded chat, OpenAI Realtime voice, and admin surfaces for keeping the knowledge base, prompts, and RAG evaluations current.',
)

const contextLinkLabel = computed(() => (isNo.value ? 'Prøv chatten' : 'Open the chat'))

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
          body: 'Spring AI mot OpenAI/Anthropic, dokumenter i PostgreSQL/pgvector og valgfri ONNX-rerank.',
          cta: 'Åpne chat',
          icon: BrainCircuit,
          accentBorder: 'hover:border-blue-200',
          accentRing: 'focus-visible:ring-blue-600',
          iconBg: 'bg-blue-50 text-blue-700 group-hover:bg-blue-100',
          ctaClass: 'text-blue-700',
        },
        {
          to: '/reason#projects' as const,
          title: 'Backend',
          body: 'Spring Boot 4 med sikkerhet, SpringDoc-skjema for klientgenerering, AI-budsjett, Realtime og adminflate.',
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
          body: 'Vue 3, Vite 8, Tailwind 4, Reka UI, Orval, PostHog og Cypress/Vitest.',
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
          body: 'Spring AI to OpenAI/Anthropic, documents in PostgreSQL/pgvector, and optional ONNX rerank.',
          cta: 'Open chat',
          icon: BrainCircuit,
          accentBorder: 'hover:border-blue-200',
          accentRing: 'focus-visible:ring-blue-600',
          iconBg: 'bg-blue-50 text-blue-700 group-hover:bg-blue-100',
          ctaClass: 'text-blue-700',
        },
        {
          to: '/reason#projects' as const,
          title: 'Backend',
          body: 'Spring Boot 4 with security, SpringDoc-backed schema for codegen, AI budgets, Realtime, and admin surfaces.',
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
          body: 'Vue 3, Vite 8, Tailwind 4, Reka UI, Orval, PostHog, and Cypress/Vitest.',
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
        'Vue 3, TypeScript, Vite 8, Pinia, Vue Router, Tailwind 4, Reka UI og VueUse',
        'Orval-generert klient fra SpringDoc-skjema; markdown-it + DOMPurify for trygg chat-rendering; PostHog i nettleseren etter samtykke',
        'Vitest for enhetstester og Cypress for E2E, inkludert Realtime voice-smoke',
      ] as const)
    : ([
        'Vue 3, TypeScript, Vite 8, Pinia, Vue Router, Tailwind 4, Reka UI, and VueUse',
        'Orval-generated client from the SpringDoc schema; markdown-it + DOMPurify for safe chat rendering; PostHog in the browser after consent',
        'Vitest for unit tests and Cypress for E2E, including Realtime voice smoke',
      ] as const),
)

const stackBack = computed(() =>
  isNo.value
    ? ([
        'Java 21, Spring Boot 4, Spring AI 2 (BOM), Spring Security og JPA mot PostgreSQL 17/pgvector',
        'OpenAI/Anthropic-chat, OpenAI embeddings, Realtime WebRTC + Whisper, valgfri ONNX-rerank og OpenNLP-sanitizer',
        'Bucket4j, AI-budsjett og kill switch; Actuator/Prometheus + Micrometer/OpenTelemetry-tracing',
        'Docker (multi-arch via Build Cloud) til Docker Hub; Railway + Postgres/pgvector i prod',
      ] as const)
    : ([
        'Java 21, Spring Boot 4, Spring AI 2 (BOM), Spring Security, and JPA against PostgreSQL 17/pgvector',
        'OpenAI/Anthropic chat, OpenAI embeddings, Realtime WebRTC + Whisper, optional ONNX rerank, and OpenNLP sanitizer',
        'Bucket4j, AI budgets, and a kill switch; Actuator/Prometheus + Micrometer/OpenTelemetry tracing',
        'Docker (multi-arch via Build Cloud) to Docker Hub; Railway + Postgres/pgvector in production',
      ] as const),
)

const stackHeading = computed(() => (isNo.value ? 'Teknologistakk (kort)' : 'Tech stack (summary)'))

const sectionsHeading = computed(() => (isNo.value ? 'Hovedgrep' : 'Main ideas'))

const sectionsIntro = computed(() =>
  isNo.value
    ? 'Fokus på det som faktisk bærer løsningen; resten ligger i kode og README.'
    : 'Focus on what actually carries the solution; the rest lives in code and the README.',
)

const footerText = computed(() =>
  isNo.value
    ? 'Denne oversikten speiler funksjonene som er i appen nå. README-en har mer om kjøring, konfigurasjon, integrasjon og tester.'
    : 'This overview mirrors the functionality currently in the app. The README has more on running, configuration, integration, and tests.',
)

const footerHome = computed(() => (isNo.value ? 'Til forsiden' : 'Back to home'))

const sections = computed<Section[]>(() => {
  if (isNo.value) {
    return [
      {
        id: 'principle',
        heading: 'Prinsipp',
        paragraphs: [
          'Modne rammer på server der det lønner seg, og et moderne SPA-skall som er raskt å endre. Målet er forutsigbar kode og en stack som er enkel å kjøre lokalt, i Docker og i produksjon.',
        ],
        icon: Layers,
        category: 'all',
        badges: [],
      },
      {
        id: 'ai-rag',
        heading: 'KI og RAG',
        paragraphs: [
          'Spring AI 2 (BOM) kobler chat mot OpenAI og valgfritt Anthropic. Embeddings og chunk-lagring ligger i PostgreSQL med pgvector, slik at relasjonsdata og vector store lever i samme database.',
          'Dokumentpipeline, chunks, promptversjoner og RAG-eksperimenter styres fra adminflatene. Valgfri ONNX-rerank og leverandørspesifikke promptmaler finnes når man trenger finere kontroll.',
        ],
        icon: BrainCircuit,
        category: 'ai',
        badges: ['Spring AI 2', 'pgvector', 'OpenAI', 'Anthropic', 'ONNX'],
      },
      {
        id: 'voice',
        heading: 'Live stemme',
        paragraphs: [
          'OpenAI Realtime (gpt-realtime) kjøres via WebRTC fra stemmesiden, med støtte på serversiden for tilkoblingsstatus, SDP-økter og RAG-oppslag. Whisper-transkripsjon brukes også der det trengs på server.',
          'Funksjonen er av som standard og aktiveres med miljøvariabler; sesjonslengde og budsjett begrenses fra `application.yaml`.',
        ],
        icon: Mic,
        category: 'integration',
        badges: ['OpenAI Realtime', 'WebRTC', 'Whisper', 'RAG lookup'],
      },
      {
        id: 'server-api',
        heading: 'Backend og tjenester',
        paragraphs: [
          'Spring Boot 4 på Java 21 med Spring Security, JPA og Bucket4j for hastighetsbegrensning på utsatte grensesnitt. AI-budsjett (per bruker, daglig/månedlig) og en global kill switch begrenser kostnader; OpenNLP brukes i sanitering der det er aktuelt.',
          'Standardmodellene er gpt-5.4-mini for chat, claude-haiku-4-5 som alternativ og text-embedding-3-large for embeddings, med konfigurasjon i `application.yaml`.',
          'Tjenestekontrakten er dokumentert med SpringDoc; i utvikling proxier Vite kall til backend, og Orval holder frontend-klienten i tråd med skjemaet. Actuator + Micrometer eksponerer Prometheus-metrikker og OpenTelemetry-tracing.',
        ],
        icon: Server,
        category: 'backend',
        badges: [
          'Spring Boot 4',
          'Java 21',
          'Bucket4j',
          'AI budsjett',
          'SpringDoc',
          'Orval',
          'Micrometer',
          'OpenTelemetry',
        ],
      },
      {
        id: 'client-ops',
        heading: 'Klient og kjøring',
        paragraphs: [
          'Vue 3, Vite 8, TypeScript, Pinia, Tailwind 4 og Reka UI for UI og tilstand. VueUse gir komposisjoner og animasjoner, mens markdown-it og DOMPurify renderer chat-svar trygt.',
          'Docker Compose binder sammen Postgres, backend og nginx-bygget frontend. Actuator, Prometheus og Micrometer/OpenTelemetry-tracing gir grunnleggende innsyn; PostHog i nettleser og valgfri serversporing kan skrus på etter behov.',
        ],
        icon: Globe,
        category: 'devops',
        badges: ['Vue 3', 'VueUse', 'Docker Compose', 'nginx', 'Actuator', 'OpenTelemetry'],
      },
      {
        id: 'deploy-ci',
        heading: 'Drift og CI/CD',
        paragraphs: [
          'Backend og frontend bygges som multi-arch (amd64/arm64) Docker-images via GitHub Actions med Docker Build Cloud, og publiseres til Docker Hub med provenance og SBOM.',
          'Tests-workflowen kjører Maven verify med JaCoCo for backend og typecheck, lint, Vitest med dekningsterskler og Cypress E2E for frontend. Semgrep skanner kode i en egen workflow, og Dependabot holder npm-, Maven- og Actions-avhengigheter oppdatert.',
          'Produksjonsmiljøet kjøres på Railway med Postgres + pgvector; en valgfri synkroniseringsjobb kan kopiere vector_store fra Railway til lokal database for utvikling.',
        ],
        icon: Rocket,
        category: 'devops',
        badges: ['Railway', 'GitHub Actions', 'Docker Build Cloud', 'Semgrep', 'Dependabot'],
      },
    ]
  }
  return [
    {
      id: 'principle',
      heading: 'Principle',
      paragraphs: [
        'Mature server-side frameworks where they pay off, and a modern SPA shell that is quick to change. The goal is predictable code and a stack that is easy to run locally, in Docker, and in production.',
      ],
      icon: Layers,
      category: 'all',
      badges: [],
    },
    {
      id: 'ai-rag',
      heading: 'AI and RAG',
      paragraphs: [
        'Spring AI 2 (BOM) wires chat to OpenAI and optionally Anthropic. Embeddings and chunk storage live in PostgreSQL with pgvector, so relational data and the vector store share one database.',
        'Document ingestion, chunks, prompt versions, and RAG experiments are managed from the admin surfaces. Optional ONNX rerank and provider-specific prompt templates are there when finer control is needed.',
      ],
      icon: BrainCircuit,
      category: 'ai',
      badges: ['Spring AI 2', 'pgvector', 'OpenAI', 'Anthropic', 'ONNX'],
    },
    {
      id: 'voice',
      heading: 'Live voice',
      paragraphs: [
        'OpenAI Realtime (gpt-realtime) runs through WebRTC from the voice experience, with server-side support for connection status, SDP sessions, and RAG lookup. Whisper transcription is used on the server where needed.',
        'The feature is off by default and enabled with environment variables; session length and budgets are bounded in `application.yaml`.',
      ],
      icon: Mic,
      category: 'integration',
      badges: ['OpenAI Realtime', 'WebRTC', 'Whisper', 'RAG lookup'],
    },
    {
      id: 'server-api',
      heading: 'Backend and services',
      paragraphs: [
        'Spring Boot 4 on Java 21 with Spring Security, JPA, and Bucket4j rate limiting on exposed interfaces. AI budgets (per-user, daily/monthly) and a global kill switch cap spend; OpenNLP participates in sanitization where relevant.',
        'Default models are gpt-5.4-mini for chat, claude-haiku-4-5 as an alternative, and text-embedding-3-large for embeddings, configured in `application.yaml`.',
        'The service contract is documented with SpringDoc; in development Vite proxies traffic to the backend, and Orval keeps the frontend client aligned with the schema. Actuator + Micrometer expose Prometheus metrics and OpenTelemetry tracing.',
      ],
      icon: Server,
      category: 'backend',
      badges: [
        'Spring Boot 4',
        'Java 21',
        'Bucket4j',
        'AI budgets',
        'SpringDoc',
        'Orval',
        'Micrometer',
        'OpenTelemetry',
      ],
    },
    {
      id: 'client-ops',
      heading: 'Client and runtime',
      paragraphs: [
        'Vue 3, Vite 8, TypeScript, Pinia, Tailwind 4, and Reka UI for UI and state. VueUse provides composables and motion, while markdown-it and DOMPurify render chat answers safely.',
        'Docker Compose ties together Postgres, the backend, and the nginx-built frontend. Actuator, Prometheus, and Micrometer/OpenTelemetry tracing give basic insight; PostHog in the browser and optional server-side capture can be enabled as needed.',
      ],
      icon: Globe,
      category: 'devops',
      badges: ['Vue 3', 'VueUse', 'Docker Compose', 'nginx', 'Actuator', 'OpenTelemetry'],
    },
    {
      id: 'deploy-ci',
      heading: 'Delivery and CI/CD',
      paragraphs: [
        'Backend and frontend ship as multi-arch (amd64/arm64) Docker images built through GitHub Actions with Docker Build Cloud, and are published to Docker Hub with provenance and SBOM.',
        'The tests workflow runs Maven verify with JaCoCo for the backend and typecheck, lint, Vitest with coverage thresholds, and Cypress E2E for the frontend. Semgrep runs in its own workflow, and Dependabot keeps npm, Maven, and Actions dependencies current.',
        'Production runs on Railway with Postgres + pgvector; an optional sync job can copy the vector_store from Railway into a local database during development.',
      ],
      icon: Rocket,
      category: 'devops',
      badges: ['Railway', 'GitHub Actions', 'Docker Build Cloud', 'Semgrep', 'Dependabot'],
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
                  @click="onOpenChat"
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
            {{
              isNo
                ? 'Tre innganger som speiler kjernen i arkitekturen.'
                : 'Three entry points that mirror the core architecture.'
            }}
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
              <h3 class="text-lg font-semibold">Frontend</h3>
            </div>
            <ul class="space-y-2 text-sm text-slate-700">
              <li v-for="line in stackFront" :key="line" class="flex gap-2">
                <span
                  class="mt-1.5 h-1.5 w-1.5 shrink-0 rounded-full bg-blue-500"
                  aria-hidden="true"
                />
                <span>{{ line }}</span>
              </li>
            </ul>
          </div>
          <div
            class="rounded-2xl border border-slate-200 bg-gradient-to-br from-white to-slate-50 p-6 shadow-md"
          >
            <div class="mb-4 flex items-center gap-2 text-slate-900">
              <Server class="h-5 w-5 text-emerald-600" aria-hidden="true" />
              <h3 class="text-lg font-semibold">Backend</h3>
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
