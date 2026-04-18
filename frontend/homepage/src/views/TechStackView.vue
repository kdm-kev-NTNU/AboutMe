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
          'Chroma kjører i Docker sammen med resten av stakken, som gjør det enkelt å få opp et konsistent miljø lokalt og i deploy.',
        ],
      },
      {
        heading: 'Frontend',
        paragraphs: [
          'Frontend er bygget med Vue 3, Vite og TypeScript. Tilstand og ruter håndteres med Pinia og Vue Router, og UI er satt sammen med Tailwind CSS og komponenter i samme ånd som shadcn (Reka UI).',
          'Målet er god utvikleropplevelse og raske iterasjoner, samtidig som resultatet holder seg ryddig og tilgjengelig.',
        ],
      },
      {
        heading: 'Kjøring og drift',
        paragraphs: [
          'Applikasjonen orkestreres med Docker Compose: MySQL, Chroma, Spring-backend og en nginx-basert frontend-container. Det gir et helhetlig bilde av hvordan tjenestene snakker sammen uten å måtte sette opp alt manuelt hver gang.',
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
        'Chroma runs in Docker alongside the rest of the stack, which makes it easy to spin up a consistent environment locally and in deployment.',
      ],
    },
    {
      heading: 'Frontend',
      paragraphs: [
        'The frontend is built with Vue 3, Vite, and TypeScript. State and routing are handled with Pinia and Vue Router, and the UI is composed with Tailwind CSS and components in the same spirit as shadcn (Reka UI).',
        'The goal is a strong developer experience and fast iteration, while keeping the end result clean and accessible.',
      ],
    },
    {
      heading: 'Runtime and operations',
      paragraphs: [
        'The application is orchestrated with Docker Compose: MySQL, Chroma, the Spring backend, and an nginx-served frontend container. That gives a coherent picture of how services talk to each other without manual setup every time.',
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
