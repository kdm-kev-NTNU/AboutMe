<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, type Ref } from 'vue'
import { useIntersectionObserver } from '@vueuse/core'
import { useLangStore } from '../stores/lang'
import { Badge } from '@/components/ui/badge'
import {
  Cpu,
  Server,
  Globe,
  Database,
  Eye,
  Container,
  BrainCircuit,
  Layers,
  Workflow,
  MonitorSmartphone,
  BookOpen,
} from 'lucide-vue-next'

const langStore = useLangStore()
const isNo = computed(() => langStore.language === 'no')

const title = computed(() => (isNo.value ? 'Teknologistakk' : 'Tech stack'))
const lastUpdated = computed(() =>
  isNo.value ? 'Sist oppdatert: april 2026' : 'Last updated: April 2026',
)

const headerBadge = computed(() =>
  isNo.value ? 'IDATT2901 · Piscada AS' : 'IDATT2901 · Piscada AS',
)

const headerSubtitle = computed(() =>
  isNo.value
    ? 'Sporbarhet fra bacheloroppgave og produktkontekst til konkrete teknologivalg i denne porteføljen, uten å erstatte full dokumentasjon i rapporten.'
    : 'Traceability from the bachelor thesis and product context to the concrete technology choices in this portfolio, without replacing full documentation in the report.',
)

const contextParagraph = computed(() =>
  isNo.value
    ? 'Denne porteføljen utvikles aktivt som grunnlag for bacheloroppgave (IDATT2901) ved NTNU Trondheim, i samarbeid med Piscada AS. Prosjektet retter seg mot en AI-assistent som kan forklare energidata på naturlig språk og gi målrettede råd om energisparing i næringsbygg.'
    : 'This portfolio is being actively developed as the foundation for a bachelor thesis (IDATT2901) at NTNU Trondheim, in collaboration with Piscada AS. The project focuses on an AI assistant that explains energy data in natural language and provides targeted energy-saving advice for commercial buildings.',
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
          body: 'Spring AI, språkmodeller og RAG med PostgreSQL/pgvector for kontekstuelle svar.',
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
          body: 'Spring Boot, sikkerhet, persistens og API mot klienten.',
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
          body: 'Vue 3, Vite og komponentbibliotek i shadcn-stil.',
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
          body: 'Spring AI, language models, and RAG with PostgreSQL/pgvector for contextual answers.',
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
          body: 'Spring Boot, security, persistence, and APIs for the client.',
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
          body: 'Vue 3, Vite, and shadcn-style UI components.',
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
        'Vue 3, TypeScript, Vite 7',
        'Pinia, Vue Router',
        'Tailwind CSS 4, Reka UI (shadcn-stil), Lucide',
        'VueUse (Core, Motion), markdown-it, DOMPurify',
        'Orval, OpenAPI-generert fetch-klient',
        'PostHog (analyse + LLM-observabilitet, samtykkestyrt på nettsiden)',
      ] as const)
    : ([
        'Vue 3, TypeScript, Vite 7',
        'Pinia, Vue Router',
        'Tailwind CSS 4, Reka UI (shadcn-style), Lucide',
        'VueUse (Core, Motion), markdown-it, DOMPurify',
        'Orval, OpenAPI-generated fetch client',
        'PostHog (analytics + LLM observability, consent-gated in the browser)',
      ] as const),
)

const stackBack = computed(() =>
  isNo.value
    ? ([
        'Java 21, Spring Boot 3.5, Spring Security, Spring Data JPA',
        'PostgreSQL 17 med pgvector, Spring AI, Tika-dokumentlesing',
        'Apache OpenNLP (NER m.m. i saniterings-/tekstflyt)',
        'Actuator (helse, metrics, Prometheus-scrape), Micrometer, PostHog (LLM-genereringer + analyse)',
        'Docker Compose, nginx (produksjonsbygg)',
        'Bucket4j (rate limiting)',
      ] as const)
    : ([
        'Java 21, Spring Boot 3.5, Spring Security, Spring Data JPA',
        'PostgreSQL 17 with pgvector, Spring AI, Tika document reading',
        'Apache OpenNLP (NER etc. in sanitization / text flow)',
        'Actuator (health, metrics, Prometheus scrape), Micrometer, PostHog (LLM generations + analytics)',
        'Docker Compose, nginx (production build)',
        'Bucket4j (rate limiting)',
      ] as const),
)

const stackHeading = computed(() => (isNo.value ? 'Teknologistakk (kort)' : 'Tech stack (summary)'))

const sectionsHeading = computed(() =>
  isNo.value ? 'Valg og begrunnelse' : 'Choices and rationale',
)

const sectionsIntro = computed(() =>
  isNo.value
    ? 'Hver blokk beskriver hvordan teknologien brukes i porteføljen.'
    : 'Each block describes how the technology is used in the portfolio.',
)

const footerText = computed(() =>
  isNo.value
    ? 'Mer om oppgaven og retningen finner du under bachelor. Full brukerhistorier og krav ligger i avtale og veiledning med oppdragsgiver.'
    : 'More about the thesis and direction is on the bachelor page. Full user stories and requirements live in agreements and supervision with the industry partner.',
)

const footerHome = computed(() => (isNo.value ? 'Til forsiden' : 'Back to home'))
const footerBachelor = computed(() => (isNo.value ? 'Bacheloroppgaven' : 'Bachelor thesis'))

const sections = computed<Section[]>(() => {
  if (isNo.value) {
    return [
      {
        id: 'why',
        heading: 'Hvorfor denne stakken?',
        paragraphs: [
          'Jeg prøver gjennomgående i prosjektet å finne en balanse mellom lettvinthet og funksjonalitet: løsninger som er enkle å jobbe med dag til dag, men som fortsatt gir nok struktur og kraft til å bygge noe som tåler litt vekst og endring.',
          'Valgene under speiler det: modne rammeverk der det gir mening, og pragmatiske byggeklosser der det sparer tid uten å ofre det viktigste.',
        ],
        icon: Layers,
        category: 'all',
        badges: [],
      },
      {
        id: 'spring-ai',
        heading: 'Spring AI',
        paragraphs: [
          'Spring og Java brukes mye i enterprise-løsninger. Spring AI bygger videre på det økosystemet og gjør det naturlig å koble språkmodeller, dokumentflyt og vektorlagring inn i en Spring Boot-tjeneste.',
          'Spring AI er et relativt nytt prosjekt, og jeg valgte det fordi jeg ønsket å følge med på hvordan integrasjonen mellom JVM-verdenen og AI utvikler seg, og for å lære det som sannsynligvis blir en vanlig sti for AI i Spring-baserte applikasjoner.',
          'Chat går mot OpenAI eller Anthropic (Anthropic er tilgjengelig når API-nøkkel er konfigurert). Embeddings lagres i PostgreSQL med pgvector (OpenAI); standard chat-modell og modellvalg styres i konfigurasjon og kan velges fra klienten innenfor et trygt sett støttede modeller.',
        ],
        icon: BrainCircuit,
        category: 'ai',
        badges: ['Spring AI', 'OpenAI', 'Anthropic', 'Java 21'],
      },
      {
        id: 'backend',
        heading: 'Backend',
        paragraphs: [
          'Backend kjører på Spring Boot 3.5 med Java 21. Jeg bruker Spring Security og Spring Data JPA mot PostgreSQL for persistens, og Bucket4j for enkel rate limiting der det trengs.',
          'Apache OpenNLP brukes der det trengs i tekstbehandling og sanitering (for eksempel navngitte entiteter).',
          'Det gir en kjent «batteries included»-opplevelse: god dokumentasjon, sterk typing og verktøy som passer godt når man vil at koden skal være forutsigbar over tid.',
        ],
        icon: Server,
        category: 'backend',
        badges: ['Spring Boot 3.5', 'Java 21', 'Spring Security', 'JPA', 'PostgreSQL', 'OpenNLP', 'Bucket4j'],
      },
      {
        id: 'rag',
        heading: 'RAG og vektorlagring',
        paragraphs: [
          'For RAG-lignende funksjonalitet bruker jeg PostgreSQL med pgvector som vektorbase, sammen med Spring AIs pgvector-integrasjon. Dokumenter kan leses inn via Tika-basert dokumentlesing i Spring AI.',
          'RAG-prompten er leverandørspesifikk og hentes fra versjonerte maler, slik at OpenAI og Anthropic kan tones litt ulikt uten å duplisere hele flyten.',
          'Ved henting utvides brukerens spørsmål til både norsk og engelsk, slik at treff i dokumenter på begge språk blir lettere å få med.',
          'PostgreSQL med pgvector kjører i Docker sammen med resten av stakken, som gjør det enkelt å få opp et konsistent miljø lokalt og i deploy.',
          'Fangst → kuratér → ingest → verifiser: valgfritt samtaleinnslag (f.eks. stemmeleverandør) er et produktvalg for rikere råmateriale; det som faktisk indekseres, er kuraterte dokumenter via admin-pipelinen. Faglitteratur om RAG dekker indeksering, chunking, retrieval og evaluering — ikke valg av stemme-UI.',
          'Primærkilder (arXiv): https://arxiv.org/abs/2407.01219 (Wang m.fl., EMNLP 2024), https://arxiv.org/abs/2312.10997 (Gao m.fl., oversikt, des. 2023). Menneskeforankret eval: https://arxiv.org/abs/2503.09902, https://arxiv.org/abs/2504.14891. Utdypet flyt og praksislenker står i rot-README og på siden «Videre arbeid».',
        ],
        icon: Database,
        category: 'ai',
        badges: ['PostgreSQL', 'pgvector', 'Tika', 'Embeddings', 'Docker'],
      },
      {
        id: 'frontend',
        heading: 'Frontend',
        paragraphs: [
          'Frontend er bygget med Vue 3, Vite og TypeScript. Tilstand og ruter håndteres med Pinia og Vue Router, og UI er satt sammen med Tailwind CSS 4 (Vite-plugin) og komponenter i samme ånd som shadcn (Reka UI).',
          'Målet er god utvikleropplevelse og raske iterasjoner, samtidig som resultatet holder seg ryddig og tilgjengelig.',
        ],
        icon: Globe,
        category: 'frontend',
        badges: ['Vue 3', 'Vite', 'TypeScript', 'Pinia', 'Tailwind 4', 'Reka UI'],
      },
      {
        id: 'api',
        heading: 'API og integrasjon',
        paragraphs: [
          'REST-grensesnittet er dokumentert med OpenAPI via SpringDoc, med Swagger UI på backend. I lokal utvikling proxier Vite forespørsler under `/api` til Spring Boot, slik at SPA og API føles som én opprinnelse.',
          'OpenAPI-skjemaet kan brukes til å generere en typesikker HTTP-klient i frontend med Orval, som holder klient og kontrakt i takt når API-et endrer seg.',
        ],
        icon: Workflow,
        category: 'integration',
        badges: ['OpenAPI', 'SpringDoc', 'Swagger', 'Orval'],
      },
      {
        id: 'observability',
        heading: 'Observabilitet',
        paragraphs: [
          'Backend eksponerer Spring Boot Actuator med blant annet health, info og Prometheus-scrape for metrics via Micrometer.',
          'LLM-kall (tokens, estimert kostnad, latens, kontekst) kan sendes som PostHog $ai_generation-hendelser fra serveren når det er slått på, slik at produktanalyse og modellbruk kan sees i samme verktøy som samtykkestyrt frontend-analyse.',
          'Admin RAG-eval bruker eval-datasett i PostgreSQL og LLM-as-judge i egen kode; PostHogs LLM-observabilitet kompletterer med hendelser fra faktisk trafikk og evalueringer i PostHog-prosjektet.',
        ],
        icon: Eye,
        category: 'devops',
        badges: ['Actuator', 'Prometheus', 'Micrometer', 'PostHog'],
      },
      {
        id: 'runtime',
        heading: 'Kjøring og drift',
        paragraphs: [
          'Applikasjonen orkestreres med Docker Compose: PostgreSQL (pgvector), Spring-backend og en nginx-basert frontend-container. Det gir et helhetlig bilde av hvordan tjenestene snakker sammen uten å måtte sette opp alt manuelt hver gang.',
        ],
        icon: Container,
        category: 'devops',
        badges: ['Docker Compose', 'PostgreSQL', 'pgvector', 'nginx'],
      },
    ]
  }
  return [
    {
      id: 'why',
      heading: 'Why this stack?',
      paragraphs: [
        'Across this project I try to balance ease of use with functionality: choices that stay pleasant to work with day to day, while still providing enough structure and power to build something that can grow and evolve.',
        'The items below reflect that: mature frameworks where they earn their place, and pragmatic building blocks where they save time without giving up what matters most.',
      ],
      icon: Layers,
      category: 'all',
      badges: [],
    },
    {
      id: 'spring-ai',
      heading: 'Spring AI',
      paragraphs: [
        'Spring and Java are widely used in enterprise systems. Spring AI extends that ecosystem and makes it natural to wire language models, document flows, and vector storage into a Spring Boot service.',
        'Spring AI is a relatively new project, and I chose it because I want to follow how AI integration on the JVM evolves, and to learn what is likely to become a common path for AI in Spring-based applications.',
        'Chat targets OpenAI or Anthropic (Anthropic is available when an API key is configured). Embeddings are stored in PostgreSQL with pgvector (OpenAI); the default chat model and model selection are configuration-driven, with the client choosing from a small allow-listed set of supported models.',
      ],
      icon: BrainCircuit,
      category: 'ai',
      badges: ['Spring AI', 'OpenAI', 'Anthropic', 'Java 21'],
    },
    {
      id: 'backend',
      heading: 'Backend',
      paragraphs: [
        'The backend runs on Spring Boot 3.5 with Java 21. I use Spring Security and Spring Data JPA with PostgreSQL for persistence, and Bucket4j for straightforward rate limiting where needed.',
        'Apache OpenNLP is used where needed for text processing and sanitization (for example named-entity recognition).',
        'That combination provides a familiar batteries-included experience: solid documentation, strong typing, and tooling that fits well when you want the codebase to stay predictable over time.',
      ],
      icon: Server,
      category: 'backend',
      badges: ['Spring Boot 3.5', 'Java 21', 'Spring Security', 'JPA', 'PostgreSQL', 'OpenNLP', 'Bucket4j'],
    },
    {
      id: 'rag',
      heading: 'RAG and vector storage',
      paragraphs: [
        "For RAG-style features I use PostgreSQL with pgvector as the vector database, together with Spring AI's pgvector integration. Documents can be ingested using Spring AI's Tika-based document reader.",
        'The RAG prompt is provider-specific and loaded from versioned templates, so OpenAI and Anthropic can be tuned slightly differently without duplicating the whole flow.',
        'At retrieval time the user question is expanded into Norwegian and English so documents in either language are easier to surface.',
        'PostgreSQL with pgvector runs in Docker alongside the rest of the stack, which makes it easy to spin up a consistent environment locally and in deployment.',
        'Capture → curate → ingest → verify: optional conversational capture (for example a voice vendor) is a product choice for richer raw material; what gets indexed is curated content through the admin pipeline. RAG literature covers indexing, chunking, retrieval, and evaluation—not the voice UI choice.',
        'Primary arXiv sources: https://arxiv.org/abs/2407.01219 (Wang et al., EMNLP 2024), https://arxiv.org/abs/2312.10997 (Gao et al., survey, Dec 2023). Human-grounded evaluation: https://arxiv.org/abs/2503.09902, https://arxiv.org/abs/2504.14891. The root README and the Future work page spell out the flow and practice links.',
      ],
      icon: Database,
      category: 'ai',
      badges: ['PostgreSQL', 'pgvector', 'Tika', 'Embeddings', 'Docker'],
    },
    {
      id: 'frontend',
      heading: 'Frontend',
      paragraphs: [
        'The frontend is built with Vue 3, Vite, and TypeScript. State and routing are handled with Pinia and Vue Router, and the UI is composed with Tailwind CSS 4 (Vite plugin) and components in the same spirit as shadcn (Reka UI).',
        'The goal is a strong developer experience and fast iteration, while keeping the end result clean and accessible.',
      ],
      icon: Globe,
      category: 'frontend',
      badges: ['Vue 3', 'Vite', 'TypeScript', 'Pinia', 'Tailwind 4', 'Reka UI'],
    },
    {
      id: 'api',
      heading: 'API and integration',
      paragraphs: [
        'The REST API is documented with OpenAPI through SpringDoc, with Swagger UI served from the backend. In local development Vite proxies `/api` to Spring Boot so the SPA and API feel like a single origin.',
        'The same OpenAPI schema can generate a typed HTTP client in the frontend with Orval, keeping the client and contract aligned as the API evolves.',
      ],
      icon: Workflow,
      category: 'integration',
      badges: ['OpenAPI', 'SpringDoc', 'Swagger', 'Orval'],
    },
    {
      id: 'observability',
      heading: 'Observability',
      paragraphs: [
        'The backend exposes Spring Boot Actuator including health, info, and a Prometheus scrape endpoint for metrics via Micrometer.',
        'LLM calls (tokens, estimated cost, latency, context) can be sent as PostHog `$ai_generation` events from the server when enabled, so product analytics and model usage can live alongside consent-gated frontend analytics in one tool.',
        'Admin RAG evaluation uses eval datasets in PostgreSQL and an in-app LLM-as-judge pipeline; PostHog LLM observability complements that with events from live traffic and evaluations in the PostHog project.',
      ],
      icon: Eye,
      category: 'devops',
      badges: ['Actuator', 'Prometheus', 'Micrometer', 'PostHog'],
    },
    {
      id: 'runtime',
      heading: 'Runtime and operations',
      paragraphs: [
        'The application is orchestrated with Docker Compose: PostgreSQL (pgvector), the Spring backend, and an nginx-served frontend container. That gives a coherent picture of how services talk to each other without manual setup every time.',
      ],
      icon: Container,
      category: 'devops',
      badges: ['Docker Compose', 'PostgreSQL', 'pgvector', 'nginx'],
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
  <div class="min-h-screen bg-gradient-to-b from-slate-50 via-white to-slate-50 pt-20">
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
                <RouterLink
                  to="/bachelor"
                  class="text-sm font-medium text-blue-700 underline underline-offset-2 hover:text-blue-900 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-600 focus-visible:ring-offset-2"
                >
                  {{ contextLinkLabel }}
                </RouterLink>
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
          <RouterLink
            to="/bachelor"
            class="inline-flex items-center justify-center rounded-lg border border-blue-200 bg-blue-50 px-5 py-2.5 text-sm font-semibold text-blue-900 shadow-sm transition hover:bg-blue-100 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-600 focus-visible:ring-offset-2"
          >
            {{ footerBachelor }}
          </RouterLink>
        </div>
      </footer>
    </div>
  </div>
</template>
