<script setup lang="ts">
import { computed } from 'vue'
import { useLangStore } from '@/stores/lang'

const langStore = useLangStore()
const isNo = computed(() => langStore.language === 'no')

const title = computed(() => (isNo.value ? 'Tilgjengelighet' : 'Accessibility'))
const lastUpdated = computed(() =>
  isNo.value ? 'Sist oppdatert: juni 2026' : 'Last updated: June 2026',
)

const sections = computed(() => {
  if (isNo.value) {
    return [
      {
        heading: 'Mål',
        paragraphs: [
          'Denne porteføljen sikter mot å følge WCAG 2.1 nivå AA der det er praktisk mulig for en privat nettside.',
          'Tilbakemeldinger om hindringer hjelper meg å forbedre opplevelsen.',
        ],
      },
      {
        heading: 'Hva som er på plass',
        paragraphs: [
          'Semantiske landemerker (for eksempel hovedinnhold og navigasjon), tastaturfokus på interaktive elementer, språkattributt på dokumentet, hopp-til-innhold-lenke, og ARIA der det trengs for dialoger, informasjonskapsler og chat.',
          'Informasjonskapsler og analyse styres av eksplisitt samtykke. Se personvernerklæringen for detaljer.',
        ],
      },
      {
        heading: 'Kjente begrensninger',
        paragraphs: [
          'Live stemmechat er eksperimentell og kan være vanskelig å bruke med enkelte hjelpemidler.',
          'Noen dekorative layout-elementer bruker div-er uten egen semantikk; hovedinnhold og overskrifter er likevel strukturert.',
          'Automatisert WCAG-revisjon i CI er ikke satt opp ennå.',
        ],
      },
      {
        heading: 'Tilbakemelding',
        paragraphs: [
          'Oppdaget du en barriere? Kontakt meg via lenkene på forsiden (GitHub eller LinkedIn) og beskriv siden og hva som ikke fungerte.',
        ],
      },
    ]
  }
  return [
    {
      heading: 'Goal',
      paragraphs: [
        'This portfolio aims to follow WCAG 2.1 Level AA where practical for a private site.',
        'Feedback about barriers helps me improve the experience.',
      ],
    },
    {
      heading: 'What is in place',
      paragraphs: [
        'Semantic landmarks (for example main content and navigation), keyboard focus on interactive controls, a document language attribute, skip-to-content link, and ARIA where needed for dialogs, cookies, and chat.',
        'Cookies and analytics are controlled by explicit consent. See the privacy policy for details.',
      ],
    },
    {
      heading: 'Known limitations',
      paragraphs: [
        'Live voice chat is experimental and may be difficult to use with some assistive technologies.',
        'Some decorative layout wrappers use divs without their own semantics; main content and headings are still structured.',
        'Automated WCAG checks in CI are not set up yet.',
      ],
    },
    {
      heading: 'Feedback',
      paragraphs: [
        'Found a barrier? Contact me via the links on the home page (GitHub or LinkedIn) and describe the page and what did not work.',
      ],
    },
  ]
})
</script>

<template>
  <main id="main-content" class="min-h-screen bg-gray-50 pt-20 pb-16">
    <div class="mx-auto max-w-3xl px-4 sm:px-6 lg:px-8">
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
