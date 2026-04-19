<script setup lang="ts">
import { computed } from 'vue'
import { useLangStore } from '../stores/lang'

const langStore = useLangStore()
const isNo = computed(() => langStore.language === 'no')

const title = computed(() => (isNo.value ? 'Teknologistakk' : 'Tech stack'))
const lastUpdated = computed(() =>
  isNo.value ? 'Sist oppdatert: april 2026' : 'Last updated: April 2026',
)

const sections = computed(() => {
  if (isNo.value) {
    return [
      {
        heading: 'Hvorfor denne stakken?',
        paragraphs: [
          'Jeg prøver gjennomgående i prosjektet å finne en balanse mellom lettvinthet og funksjonalitet: løsninger som er enkle å jobbe med dag til dag, men som fortsatt gir nok struktur og kraft til å bygge noe som tåler litt vekst og endring.',
          'Valgene under speiler det — modne rammeverk der det gir mening, og pragmatiske byggeklosser der det sparer tid uten å ofre det viktigste.',
        ],
      },
      {
        heading: 'Spring AI',
        paragraphs: [
          'Spring og Java brukes mye i enterprise-løsninger. Spring AI bygger videre på det økosystemet og gjør det naturlig å koble språkmodeller, dokumentflyt og vektorlagring inn i en Spring Boot-tjeneste.',
          'Spring AI er et relativt nytt prosjekt, og jeg valgte det fordi jeg ønsket å følge med på hvordan integrasjonen mellom JVM-verdenen og AI utvikler seg — og for å lære det som sannsynligvis blir en vanlig sti for AI i Spring-baserte applikasjoner.',
          'Chat går mot OpenAI eller Anthropic (Anthropic er tilgjengelig når API-nøkkel er konfigurert). Embeddings til Chroma kommer fra OpenAI; standard chat-modell og modellvalg styres i konfigurasjon og kan velges fra klienten innenfor et trygt sett støttede modeller.',
        ],
      },
      {
        heading: 'Backend',
        paragraphs: [
          'Backend kjører på Spring Boot med Java 21. Jeg bruker Spring Security og Spring Data JPA mot MySQL for persistens, og Bucket4j for enkel rate limiting der det trengs.',
          'Det gir en kjent «batteries included»-opplevelse: god dokumentasjon, sterk typing og verktøy som passer godt når man vil at koden skal være forutsigbar over tid.',
        ],
      },
      {
        heading: 'RAG og vektorlagring',
        paragraphs: [
          'For RAG-lignende funksjonalitet bruker jeg Chroma som vektorbase, sammen med Spring AIs Chroma-integrasjon. Dokumenter kan leses inn via Tika-basert dokumentlesing i Spring AI.',
          'RAG-prompten er leverandørspesifikk og hentes fra versjonerte maler, slik at OpenAI og Anthropic kan tones litt ulikt uten å duplisere hele flyten.',
          'Ved henting utvides brukerens spørsmål til både norsk og engelsk, slik at treff i dokumenter på begge språk blir lettere å få med.',
          'Chroma kjører i Docker sammen med resten av stakken, som gjør det enkelt å få opp et konsistent miljø lokalt og i deploy.',
        ],
      },
      {
        heading: 'Frontend',
        paragraphs: [
          'Frontend er bygget med Vue 3, Vite og TypeScript. Tilstand og ruter håndteres med Pinia og Vue Router, og UI er satt sammen med Tailwind CSS 4 (Vite-plugin) og komponenter i samme ånd som shadcn (Reka UI).',
          'Målet er god utvikleropplevelse og raske iterasjoner, samtidig som resultatet holder seg ryddig og tilgjengelig.',
        ],
      },
      {
        heading: 'API og integrasjon',
        paragraphs: [
          'REST-grensesnittet er dokumentert med OpenAPI via SpringDoc, med Swagger UI på backend. I lokal utvikling proxier Vite forespørsler under `/api` til Spring Boot, slik at SPA og API føles som én opprinnelse.',
          'OpenAPI-skjemaet kan brukes til å generere en typesikker HTTP-klient i frontend med Orval, som holder klient og kontrakt i takt når API-et endrer seg.',
        ],
      },
      {
        heading: 'Observabilitet',
        paragraphs: [
          'Backend eksponerer Spring Boot Actuator, og Micrometer med OpenTelemetry sender spor til Arize Phoenix over OTLP (gRPC). Det gir et sted å se kjeder av kall og latens uten å spre sporingslogikk overalt i koden.',
          'Phoenix kjører som egen tjeneste i Docker Compose (sammen med OTLP-endepunktet), slik at samme oppsett fungerer lokalt som i et typisk compose-basert miljø.',
        ],
      },
      {
        heading: 'Kjøring og drift',
        paragraphs: [
          'Applikasjonen orkestreres med Docker Compose: MySQL, Chroma, Phoenix (sporings-UI og OTLP), Spring-backend og en nginx-basert frontend-container. Det gir et helhetlig bilde av hvordan tjenestene snakker sammen uten å måtte sette opp alt manuelt hver gang.',
        ],
      },
    ]
  }
  return [
    {
      heading: 'Why this stack?',
      paragraphs: [
        'Across this project I try to balance ease of use with functionality: choices that stay pleasant to work with day to day, while still providing enough structure and power to build something that can grow and evolve.',
        'The items below reflect that — mature frameworks where they earn their place, and pragmatic building blocks where they save time without giving up what matters most.',
      ],
    },
    {
      heading: 'Spring AI',
      paragraphs: [
        'Spring and Java are widely used in enterprise systems. Spring AI extends that ecosystem and makes it natural to wire language models, document flows, and vector storage into a Spring Boot service.',
        'Spring AI is a relatively new project, and I chose it because I want to follow how AI integration on the JVM evolves — and to learn what is likely to become a common path for AI in Spring-based applications.',
        'Chat targets OpenAI or Anthropic (Anthropic is available when an API key is configured). Embeddings for Chroma come from OpenAI; the default chat model and model selection are configuration-driven, with the client choosing from a small allow-listed set of supported models.',
      ],
    },
    {
      heading: 'Backend',
      paragraphs: [
        'The backend runs on Spring Boot with Java 21. I use Spring Security and Spring Data JPA with MySQL for persistence, and Bucket4j for straightforward rate limiting where needed.',
        'That combination provides a familiar batteries-included experience: solid documentation, strong typing, and tooling that fits well when you want the codebase to stay predictable over time.',
      ],
    },
    {
      heading: 'RAG and vector storage',
      paragraphs: [
        'For RAG-style features I use Chroma as the vector database, together with Spring AI’s Chroma integration. Documents can be ingested using Spring AI’s Tika-based document reader.',
        'The RAG prompt is provider-specific and loaded from versioned templates, so OpenAI and Anthropic can be tuned slightly differently without duplicating the whole flow.',
        'At retrieval time the user question is expanded into Norwegian and English so documents in either language are easier to surface.',
        'Chroma runs in Docker alongside the rest of the stack, which makes it easy to spin up a consistent environment locally and in deployment.',
      ],
    },
    {
      heading: 'Frontend',
      paragraphs: [
        'The frontend is built with Vue 3, Vite, and TypeScript. State and routing are handled with Pinia and Vue Router, and the UI is composed with Tailwind CSS 4 (Vite plugin) and components in the same spirit as shadcn (Reka UI).',
        'The goal is a strong developer experience and fast iteration, while keeping the end result clean and accessible.',
      ],
    },
    {
      heading: 'API and integration',
      paragraphs: [
        'The REST API is documented with OpenAPI through SpringDoc, with Swagger UI served from the backend. In local development Vite proxies `/api` to Spring Boot so the SPA and API feel like a single origin.',
        'The same OpenAPI schema can generate a typed HTTP client in the frontend with Orval, keeping the client and contract aligned as the API evolves.',
      ],
    },
    {
      heading: 'Observability',
      paragraphs: [
        'The backend exposes Spring Boot Actuator, and Micrometer with OpenTelemetry exports traces to Arize Phoenix over OTLP (gRPC). That gives one place to inspect call chains and latency without scattering tracing logic across the codebase.',
        'Phoenix runs as its own Docker Compose service (alongside the OTLP endpoint), so the same layout works locally as in a typical compose-based environment.',
      ],
    },
    {
      heading: 'Runtime and operations',
      paragraphs: [
        'The application is orchestrated with Docker Compose: MySQL, Chroma, Phoenix (trace UI and OTLP), the Spring backend, and an nginx-served frontend container. That gives a coherent picture of how services talk to each other without manual setup every time.',
      ],
    },
  ]
})
</script>

<template>
  <main class="min-h-screen bg-gray-50 pt-20 pb-16">
    <div class="mx-auto max-w-3xl px-4 sm:px-6">
      <h1 class="text-3xl font-bold tracking-tight text-gray-900 sm:text-4xl">
        {{ title }}
      </h1>
      <p class="mt-2 text-sm text-gray-500">
        {{ lastUpdated }}
      </p>

      <div class="mt-10 space-y-10">
        <section
          v-for="(section, idx) in sections"
          :key="idx"
          class="rounded-xl border border-gray-200 bg-white p-6 shadow-sm"
        >
          <h2 class="text-lg font-semibold text-gray-900">
            {{ section.heading }}
          </h2>
          <div class="mt-4 space-y-3 text-sm leading-relaxed text-gray-700">
            <p v-for="(p, pIdx) in section.paragraphs" :key="pIdx">
              {{ p }}
            </p>
          </div>
        </section>
      </div>
    </div>
  </main>
</template>
