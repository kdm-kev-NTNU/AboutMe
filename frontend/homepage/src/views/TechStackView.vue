<script setup lang="ts">
import { ref, computed } from 'vue'
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
} from 'lucide-vue-next'

const langStore = useLangStore()
const isNo = computed(() => langStore.language === 'no')

const title = computed(() => (isNo.value ? 'Teknologistakk' : 'Tech stack'))
const lastUpdated = computed(() =>
  isNo.value ? 'Sist oppdatert: april 2026' : 'Last updated: April 2026',
)

type Category = 'all' | 'ai' | 'backend' | 'frontend' | 'integration' | 'devops'
type CardSize = 'large' | 'medium' | 'small' | 'full'

interface Section {
  id: string
  heading: string
  paragraphs: string[]
  icon: typeof Cpu
  category: Category
  badges: string[]
  size: CardSize
  accentFrom: string
  accentTo: string
}

const activeFilter = ref<Category>('all')

const filterTabs = computed<{ id: Category; label: string }[]>(() =>
  isNo.value
    ? [
        { id: 'all', label: 'Alle' },
        { id: 'ai', label: 'AI & RAG' },
        { id: 'backend', label: 'Backend' },
        { id: 'frontend', label: 'Frontend' },
        { id: 'integration', label: 'Integrasjon' },
        { id: 'devops', label: 'Drift' },
      ]
    : [
        { id: 'all', label: 'All' },
        { id: 'ai', label: 'AI & RAG' },
        { id: 'backend', label: 'Backend' },
        { id: 'frontend', label: 'Frontend' },
        { id: 'integration', label: 'Integration' },
        { id: 'devops', label: 'DevOps' },
      ],
)

const heroNodes = computed(() =>
  isNo.value
    ? [
        { label: 'Frontend', icon: MonitorSmartphone },
        { label: 'Backend (REST)', icon: Server },
        { label: 'Database & Vektorer', icon: Database },
        { label: 'AI-modeller', icon: BrainCircuit },
      ]
    : [
        { label: 'Frontend', icon: MonitorSmartphone },
        { label: 'Backend (REST)', icon: Server },
        { label: 'Database & Vectors', icon: Database },
        { label: 'AI Models', icon: BrainCircuit },
      ],
)

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
        size: 'full',
        accentFrom: 'from-slate-400',
        accentTo: 'to-slate-500',
      },
      {
        id: 'spring-ai',
        heading: 'Spring AI',
        paragraphs: [
          'Spring og Java brukes mye i enterprise-løsninger. Spring AI bygger videre på det økosystemet og gjør det naturlig å koble språkmodeller, dokumentflyt og vektorlagring inn i en Spring Boot-tjeneste.',
          'Spring AI er et relativt nytt prosjekt, og jeg valgte det fordi jeg ønsket å følge med på hvordan integrasjonen mellom JVM-verdenen og AI utvikler seg, og for å lære det som sannsynligvis blir en vanlig sti for AI i Spring-baserte applikasjoner.',
          'Chat går mot OpenAI eller Anthropic (Anthropic er tilgjengelig når API-nøkkel er konfigurert). Embeddings til Chroma kommer fra OpenAI; standard chat-modell og modellvalg styres i konfigurasjon og kan velges fra klienten innenfor et trygt sett støttede modeller.',
        ],
        icon: BrainCircuit,
        category: 'ai',
        badges: ['Spring AI', 'OpenAI', 'Anthropic', 'Java 21'],
        size: 'large',
        accentFrom: 'from-violet-500',
        accentTo: 'to-purple-600',
      },
      {
        id: 'backend',
        heading: 'Backend',
        paragraphs: [
          'Backend kjører på Spring Boot med Java 21. Jeg bruker Spring Security og Spring Data JPA mot MySQL for persistens, og Bucket4j for enkel rate limiting der det trengs.',
          'Det gir en kjent «batteries included»-opplevelse: god dokumentasjon, sterk typing og verktøy som passer godt når man vil at koden skal være forutsigbar over tid.',
        ],
        icon: Server,
        category: 'backend',
        badges: ['Spring Boot', 'Java 21', 'Spring Security', 'JPA', 'MySQL', 'Bucket4j'],
        size: 'medium',
        accentFrom: 'from-green-500',
        accentTo: 'to-emerald-600',
      },
      {
        id: 'rag',
        heading: 'RAG og vektorlagring',
        paragraphs: [
          'For RAG-lignende funksjonalitet bruker jeg Chroma som vektorbase, sammen med Spring AIs Chroma-integrasjon. Dokumenter kan leses inn via Tika-basert dokumentlesing i Spring AI.',
          'RAG-prompten er leverandørspesifikk og hentes fra versjonerte maler, slik at OpenAI og Anthropic kan tones litt ulikt uten å duplisere hele flyten.',
          'Ved henting utvides brukerens spørsmål til både norsk og engelsk, slik at treff i dokumenter på begge språk blir lettere å få med.',
          'Chroma kjører i Docker sammen med resten av stakken, som gjør det enkelt å få opp et konsistent miljø lokalt og i deploy.',
        ],
        icon: Database,
        category: 'ai',
        badges: ['Chroma', 'Tika', 'Embeddings', 'Docker'],
        size: 'medium',
        accentFrom: 'from-amber-500',
        accentTo: 'to-orange-600',
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
        size: 'medium',
        accentFrom: 'from-sky-500',
        accentTo: 'to-blue-600',
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
        size: 'small',
        accentFrom: 'from-teal-500',
        accentTo: 'to-cyan-600',
      },
      {
        id: 'observability',
        heading: 'Observabilitet',
        paragraphs: [
          'Backend eksponerer Spring Boot Actuator, og Micrometer med OpenTelemetry sender spor til Arize Phoenix over OTLP (gRPC). Det gir et sted å se kjeder av kall og latens uten å spre sporingslogikk overalt i koden.',
          'Phoenix kjører som egen tjeneste i Docker Compose (sammen med OTLP-endepunktet), slik at samme oppsett fungerer lokalt som i et typisk compose-basert miljø.',
        ],
        icon: Eye,
        category: 'devops',
        badges: ['Actuator', 'Micrometer', 'OpenTelemetry', 'Phoenix'],
        size: 'small',
        accentFrom: 'from-rose-500',
        accentTo: 'to-pink-600',
      },
      {
        id: 'runtime',
        heading: 'Kjøring og drift',
        paragraphs: [
          'Applikasjonen orkestreres med Docker Compose: MySQL, Chroma, Phoenix (sporings-UI og OTLP), Spring-backend og en nginx-basert frontend-container. Det gir et helhetlig bilde av hvordan tjenestene snakker sammen uten å måtte sette opp alt manuelt hver gang.',
        ],
        icon: Container,
        category: 'devops',
        badges: ['Docker Compose', 'MySQL', 'Chroma', 'Phoenix', 'nginx'],
        size: 'full',
        accentFrom: 'from-blue-500',
        accentTo: 'to-indigo-600',
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
      size: 'full',
      accentFrom: 'from-slate-400',
      accentTo: 'to-slate-500',
    },
    {
      id: 'spring-ai',
      heading: 'Spring AI',
      paragraphs: [
        'Spring and Java are widely used in enterprise systems. Spring AI extends that ecosystem and makes it natural to wire language models, document flows, and vector storage into a Spring Boot service.',
        'Spring AI is a relatively new project, and I chose it because I want to follow how AI integration on the JVM evolves, and to learn what is likely to become a common path for AI in Spring-based applications.',
        'Chat targets OpenAI or Anthropic (Anthropic is available when an API key is configured). Embeddings for Chroma come from OpenAI; the default chat model and model selection are configuration-driven, with the client choosing from a small allow-listed set of supported models.',
      ],
      icon: BrainCircuit,
      category: 'ai',
      badges: ['Spring AI', 'OpenAI', 'Anthropic', 'Java 21'],
      size: 'large',
      accentFrom: 'from-violet-500',
      accentTo: 'to-purple-600',
    },
    {
      id: 'backend',
      heading: 'Backend',
      paragraphs: [
        'The backend runs on Spring Boot with Java 21. I use Spring Security and Spring Data JPA with MySQL for persistence, and Bucket4j for straightforward rate limiting where needed.',
        'That combination provides a familiar batteries-included experience: solid documentation, strong typing, and tooling that fits well when you want the codebase to stay predictable over time.',
      ],
      icon: Server,
      category: 'backend',
      badges: ['Spring Boot', 'Java 21', 'Spring Security', 'JPA', 'MySQL', 'Bucket4j'],
      size: 'medium',
      accentFrom: 'from-green-500',
      accentTo: 'to-emerald-600',
    },
    {
      id: 'rag',
      heading: 'RAG and vector storage',
      paragraphs: [
        'For RAG-style features I use Chroma as the vector database, together with Spring AI\'s Chroma integration. Documents can be ingested using Spring AI\'s Tika-based document reader.',
        'The RAG prompt is provider-specific and loaded from versioned templates, so OpenAI and Anthropic can be tuned slightly differently without duplicating the whole flow.',
        'At retrieval time the user question is expanded into Norwegian and English so documents in either language are easier to surface.',
        'Chroma runs in Docker alongside the rest of the stack, which makes it easy to spin up a consistent environment locally and in deployment.',
      ],
      icon: Database,
      category: 'ai',
      badges: ['Chroma', 'Tika', 'Embeddings', 'Docker'],
      size: 'medium',
      accentFrom: 'from-amber-500',
      accentTo: 'to-orange-600',
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
      size: 'medium',
      accentFrom: 'from-sky-500',
      accentTo: 'to-blue-600',
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
      size: 'small',
      accentFrom: 'from-teal-500',
      accentTo: 'to-cyan-600',
    },
    {
      id: 'observability',
      heading: 'Observability',
      paragraphs: [
        'The backend exposes Spring Boot Actuator, and Micrometer with OpenTelemetry exports traces to Arize Phoenix over OTLP (gRPC). That gives one place to inspect call chains and latency without scattering tracing logic across the codebase.',
        'Phoenix runs as its own Docker Compose service (alongside the OTLP endpoint), so the same layout works locally as in a typical compose-based environment.',
      ],
      icon: Eye,
      category: 'devops',
      badges: ['Actuator', 'Micrometer', 'OpenTelemetry', 'Phoenix'],
      size: 'small',
      accentFrom: 'from-rose-500',
      accentTo: 'to-pink-600',
    },
    {
      id: 'runtime',
      heading: 'Runtime and operations',
      paragraphs: [
        'The application is orchestrated with Docker Compose: MySQL, Chroma, Phoenix (trace UI and OTLP), the Spring backend, and an nginx-served frontend container. That gives a coherent picture of how services talk to each other without manual setup every time.',
      ],
      icon: Container,
      category: 'devops',
      badges: ['Docker Compose', 'MySQL', 'Chroma', 'Phoenix', 'nginx'],
      size: 'full',
      accentFrom: 'from-blue-500',
      accentTo: 'to-indigo-600',
    },
  ]
})

const filteredSections = computed(() =>
  activeFilter.value === 'all'
    ? sections.value
    : sections.value.filter(
        (s) => s.category === activeFilter.value || s.category === 'all',
      ),
)

function gridClass(size: CardSize): string {
  switch (size) {
    case 'large':
      return 'md:col-span-2'
    case 'full':
      return 'md:col-span-3'
    case 'small':
    case 'medium':
    default:
      return 'md:col-span-1'
  }
}

function cardDelay(index: number): number {
  return 100 + index * 80
}
</script>

<template>
  <main class="min-h-screen pt-20 pb-16 bg-gradient-to-br from-slate-50 to-slate-100 relative overflow-hidden">
    <!-- Background blobs -->
    <div class="blob-container">
      <div class="blob blob-1"></div>
      <div class="blob blob-2"></div>
      <div class="blob blob-3"></div>
      <div class="blob blob-4"></div>
    </div>

    <!-- Gradient overlay -->
    <div class="absolute inset-0 pointer-events-none">
      <div
        class="absolute top-0 left-0 w-full h-full"
        style="
          background: radial-gradient(
              circle at 20% 80%,
              rgba(59, 130, 246, 0.08) 0%,
              transparent 50%
            ),
            radial-gradient(
              circle at 80% 20%,
              rgba(37, 99, 235, 0.08) 0%,
              transparent 50%
            );
        "
      ></div>
    </div>

    <div class="relative z-10 mx-auto max-w-6xl px-4 sm:px-6 lg:px-8">
      <!-- Header -->
      <div
        v-motion
        :initial="{ opacity: 0, y: 30 }"
        :visible-once="{ opacity: 1, y: 0, transition: { duration: 600 } }"
        class="text-center mb-12"
      >
        <h1
          class="text-4xl sm:text-5xl font-bold tracking-tight bg-gradient-to-r from-blue-600 via-blue-700 to-blue-800 bg-clip-text text-transparent animate-gradient-x"
        >
          {{ title }}
        </h1>
        <p class="mt-3 text-sm text-gray-500">{{ lastUpdated }}</p>
      </div>

      <!-- Architecture Hero Diagram -->
      <div
        v-motion
        :initial="{ opacity: 0, y: 20 }"
        :visible-once="{ opacity: 1, y: 0, transition: { duration: 600, delay: 200 } }"
        class="mb-14"
      >
        <div class="hidden md:flex items-center justify-center gap-0">
          <template v-for="(node, i) in heroNodes" :key="node.label">
            <div
              v-motion
              :initial="{ opacity: 0, scale: 0.8 }"
              :visible-once="{
                opacity: 1,
                scale: 1,
                transition: { duration: 400, delay: 300 + i * 150 },
              }"
              class="hero-node group relative flex flex-col items-center gap-2"
            >
              <div
                class="w-16 h-16 rounded-2xl bg-white shadow-lg border border-gray-100 flex items-center justify-center transition-all duration-300 group-hover:shadow-xl group-hover:-translate-y-1 group-hover:border-blue-200"
              >
                <component
                  :is="node.icon"
                  class="w-7 h-7 text-blue-600 transition-colors duration-300 group-hover:text-blue-700"
                />
              </div>
              <span
                class="text-xs font-medium text-gray-600 whitespace-nowrap group-hover:text-blue-700 transition-colors duration-300"
              >
                {{ node.label }}
              </span>
            </div>
            <div
              v-if="i < heroNodes.length - 1"
              class="connector-line mx-2 flex-shrink-0"
            >
              <svg width="60" height="20" viewBox="0 0 60 20" fill="none">
                <line
                  x1="0"
                  y1="10"
                  x2="48"
                  y2="10"
                  stroke="#93C5FD"
                  stroke-width="2"
                  stroke-dasharray="6 4"
                  class="animate-dash"
                />
                <polygon points="48,5 58,10 48,15" fill="#3B82F6" />
              </svg>
            </div>
          </template>
        </div>

        <!-- Mobile hero: vertical -->
        <div class="flex md:hidden flex-col items-center gap-3">
          <template v-for="(node, i) in heroNodes" :key="'m-' + node.label">
            <div class="flex items-center gap-3">
              <div
                class="w-12 h-12 rounded-xl bg-white shadow-md border border-gray-100 flex items-center justify-center"
              >
                <component :is="node.icon" class="w-5 h-5 text-blue-600" />
              </div>
              <span class="text-sm font-medium text-gray-700">{{ node.label }}</span>
            </div>
            <div v-if="i < heroNodes.length - 1">
              <svg width="20" height="28" viewBox="0 0 20 28" fill="none">
                <line
                  x1="10"
                  y1="0"
                  x2="10"
                  y2="18"
                  stroke="#93C5FD"
                  stroke-width="2"
                  stroke-dasharray="4 3"
                  class="animate-dash-v"
                />
                <polygon points="5,18 10,27 15,18" fill="#3B82F6" />
              </svg>
            </div>
          </template>
        </div>
      </div>

      <!-- Filter Tabs -->
      <div
        v-motion
        :initial="{ opacity: 0, y: 15 }"
        :visible-once="{ opacity: 1, y: 0, transition: { duration: 500, delay: 400 } }"
        class="flex flex-wrap justify-center gap-2 mb-10"
      >
        <button
          v-for="tab in filterTabs"
          :key="tab.id"
          class="px-4 py-2 rounded-full text-sm font-medium transition-all duration-300 cursor-pointer"
          :class="
            activeFilter === tab.id
              ? 'bg-gradient-to-r from-blue-600 to-blue-700 text-white shadow-md shadow-blue-500/25'
              : 'bg-white text-gray-600 border border-gray-200 hover:border-blue-300 hover:text-blue-700 hover:shadow-sm'
          "
          @click="activeFilter = tab.id"
        >
          {{ tab.label }}
        </button>
      </div>

      <!-- Bento Grid -->
      <div class="grid grid-cols-1 md:grid-cols-3 gap-5">
        <section
          v-for="(section, idx) in filteredSections"
          :key="section.id"
          v-motion
          :initial="{ opacity: 0, y: 30 }"
          :visible-once="{
            opacity: 1,
            y: 0,
            transition: { duration: 500, delay: cardDelay(idx) },
          }"
          :class="gridClass(section.size)"
          class="bento-card group rounded-2xl bg-white border border-gray-100 shadow-sm overflow-hidden transition-all duration-300 hover:-translate-y-1 hover:shadow-xl hover:border-blue-200/60"
        >
          <!-- Accent gradient bar -->
          <div
            class="h-1 bg-gradient-to-r"
            :class="[section.accentFrom, section.accentTo]"
          ></div>

          <div class="p-6">
            <!-- Card header -->
            <div class="flex items-center gap-3 mb-4">
              <div
                class="w-10 h-10 rounded-xl bg-gradient-to-br flex items-center justify-center flex-shrink-0 transition-transform duration-300 group-hover:scale-110"
                :class="[section.accentFrom, section.accentTo]"
              >
                <component :is="section.icon" class="w-5 h-5 text-white" />
              </div>
              <h2 class="text-lg font-semibold text-gray-900">
                {{ section.heading }}
              </h2>
            </div>

            <!-- Card body -->
            <div class="space-y-3 text-sm leading-relaxed text-gray-600 mb-5">
              <p v-for="(p, pIdx) in section.paragraphs" :key="pIdx">{{ p }}</p>
            </div>

            <!-- Badges -->
            <div v-if="section.badges.length" class="flex flex-wrap gap-2">
              <Badge
                v-for="badge in section.badges"
                :key="badge"
                variant="secondary"
                class="text-xs bg-gradient-to-r from-blue-50 to-slate-50 text-blue-700 border border-blue-100 hover:border-blue-300 transition-colors duration-200"
              >
                {{ badge }}
              </Badge>
            </div>
          </div>
        </section>
      </div>
    </div>
  </main>
</template>

<style scoped>
@keyframes gradient-x {
  0%,
  100% {
    background-position: 0% 50%;
  }
  50% {
    background-position: 100% 50%;
  }
}

.animate-gradient-x {
  background-size: 200% 200%;
  animation: gradient-x 3s ease-in-out infinite;
}

/* Animated dashes flowing along the connector */
@keyframes dash-flow {
  to {
    stroke-dashoffset: -20;
  }
}

@keyframes dash-flow-v {
  to {
    stroke-dashoffset: -14;
  }
}

.animate-dash {
  animation: dash-flow 1.2s linear infinite;
}

.animate-dash-v {
  animation: dash-flow-v 1.2s linear infinite;
}

/* Background blobs */
.blob-container {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  pointer-events: none;
  overflow: hidden;
  z-index: 1;
}

.blob {
  position: absolute;
  border-radius: 50%;
  filter: blur(50px);
  animation: float 8s ease-in-out infinite;
}

@keyframes float {
  0%,
  100% {
    transform: translate(0, 0) scale(1);
  }
  25% {
    transform: translate(20px, -30px) scale(1.1);
  }
  50% {
    transform: translate(-15px, -20px) scale(0.9);
  }
  75% {
    transform: translate(-25px, 10px) scale(1.05);
  }
}

.blob-1 {
  width: 280px;
  height: 280px;
  background: radial-gradient(
    circle,
    rgba(59, 130, 246, 0.15) 0%,
    rgba(37, 99, 235, 0.1) 50%,
    transparent 70%
  );
  top: 5%;
  left: 5%;
  animation-duration: 9s;
}

.blob-2 {
  width: 220px;
  height: 220px;
  background: radial-gradient(
    circle,
    rgba(96, 165, 250, 0.12) 0%,
    rgba(59, 130, 246, 0.08) 50%,
    transparent 70%
  );
  top: 50%;
  right: 8%;
  animation-delay: 2s;
  animation-duration: 11s;
}

.blob-3 {
  width: 200px;
  height: 200px;
  background: radial-gradient(
    circle,
    rgba(147, 197, 253, 0.14) 0%,
    rgba(96, 165, 250, 0.1) 50%,
    transparent 70%
  );
  bottom: 15%;
  left: 20%;
  animation-delay: 4s;
  animation-duration: 10s;
}

.blob-4 {
  width: 180px;
  height: 180px;
  background: radial-gradient(
    circle,
    rgba(37, 99, 235, 0.12) 0%,
    rgba(30, 64, 175, 0.08) 50%,
    transparent 70%
  );
  top: 25%;
  right: 25%;
  animation-delay: 3s;
  animation-duration: 7s;
}

/* Responsive adjustments */
@media (max-width: 768px) {
  .blob {
    filter: blur(35px);
  }

  .blob-1,
  .blob-3 {
    width: 180px;
    height: 180px;
  }

  .blob-2,
  .blob-4 {
    width: 140px;
    height: 140px;
  }
}
</style>
