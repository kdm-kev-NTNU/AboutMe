<script setup lang="ts">
import { computed } from 'vue'
import { useLangStore } from '../stores/lang'

const langStore = useLangStore()
const isNo = computed(() => langStore.language === 'no')

const title = computed(() => (isNo.value ? 'Personvernerklæring' : 'Privacy Policy'))
const lastUpdated = computed(() =>
  isNo.value ? 'Sist oppdatert: april 2026' : 'Last updated: April 2026',
)

const sections = computed(() => {
  if (isNo.value) {
    return [
      {
        heading: '1. Innledning',
        paragraphs: [
          'Denne personvernerklæringen beskriver hvordan porteføljesiden «Kevin\'s AI» (heretter «nettstedet») behandler personopplysninger og bruker informasjonskapsler (cookies).',
          'Ved å bruke nettstedet godtar du vilkårene som fremgår her, i tillegg til valgene du gjør i samtykkeløsningen for informasjonskapsler.',
        ],
      },
      {
        heading: '2. Behandlingsansvarlig',
        paragraphs: [
          'Nettstedet drives som en personlig portefølje. For henvendelser om personvern, kontakt eier via lenkene som er oppgitt på nettstedet (for eksempel GitHub eller LinkedIn).',
        ],
      },
      {
        heading: '3. Informasjonskapsler og samtykke (Consentify)',
        paragraphs: [
          'Nettstedet bruker Consentify (consentify.app) for å vise et samtykkebanner, dokumentere samtykke til informasjonskapsler og la deg endre innstillinger senere.',
          'Consentify kan sette egne informasjonskapsler eller lignende teknologi for å lagre preferanser. Les Consentifys personvernerklæring for detaljer om deres behandling.',
          'Du kan når som helst åpne «Informasjonskapsler» i bunnteksten for å endre eller trekke tilbake samtykke.',
        ],
      },
      {
        heading: '4. Chat-funksjonen og AI',
        paragraphs: [
          'Når du bruker chat-funksjonen, sendes meldingene dine til en tjeneste som bruker en språkmodell (for eksempel OpenAI) for å generere svar.',
          'Unngå å sende sensitive personopplysninger, helseopplysninger, passord eller annet du ikke ønsker å dele med en ekstern leverandør.',
          'Behandlingen skjer for å levere funksjonen du ber om. Oppbevaring og logger avhenger av hvordan backend er konfigurert; kontakt eier ved spørsmål.',
        ],
      },
      {
        heading: '5. Teknisk drift og sikkerhet',
        paragraphs: [
          'Som på de fleste nettsteder kan servere og infrastruktur logge teknisk informasjon (f.eks. IP-adresse, tidspunkt og forespørsel) som er nødvendig for drift og sikkerhet.',
        ],
      },
      {
        heading: '6. Dine rettigheter',
        paragraphs: [
          'Avhengig av gjeldende lov (f.eks. GDPR) kan du ha rett til innsyn, retting, sletting, begrensning av behandling, dataportabilitet og å protestere mot visse typer behandling.',
          'Kontakt eier via kontaktlenkene på nettstedet for å utøve rettigheter eller stille spørsmål.',
        ],
      },
      {
        heading: '7. Endringer',
        paragraphs: [
          'Denne erklæringen kan oppdateres ved endringer på nettstedet eller i lovkrav. «Sist oppdatert»-datoen øverst vil bli justert ved vesentlige endringer.',
        ],
      },
    ]
  }
  return [
    {
      heading: '1. Introduction',
      paragraphs: [
        'This privacy policy explains how the «Kevin\'s AI» portfolio site (the «Site») handles personal data and uses cookies.',
        'By using the Site you agree to the practices described here, in addition to the choices you make in the cookie consent tool.',
      ],
    },
    {
      heading: '2. Controller',
      paragraphs: [
        'The Site is operated as a personal portfolio. For privacy-related requests, contact the owner via the links provided on the Site (for example GitHub or LinkedIn).',
      ],
    },
    {
      heading: '3. Cookies and consent (Consentify)',
      paragraphs: [
        'The Site uses Consentify (consentify.app) to display a consent banner, record cookie consent, and let you change your preferences later.',
        'Consentify may set its own cookies or similar technologies to store preferences. See Consentify\'s privacy policy for details about their processing.',
        'You can open «Cookie Settings» in the footer at any time to change or withdraw consent.',
      ],
    },
    {
      heading: '4. Chat and AI',
      paragraphs: [
        'When you use the chat feature, your messages are sent to a service that uses a language model (for example OpenAI) to generate replies.',
        'Do not send sensitive personal data, health information, passwords, or anything you do not want shared with an external provider.',
        'Processing is performed to deliver the functionality you request. Retention and logging depend on how the backend is configured; contact the owner if you have questions.',
      ],
    },
    {
      heading: '5. Technical operations and security',
      paragraphs: [
        'Like most websites, servers and infrastructure may log technical information (such as IP address, timestamps, and requests) as needed for operation and security.',
      ],
    },
    {
      heading: '6. Your rights',
      paragraphs: [
        'Depending on applicable law (for example the GDPR), you may have rights of access, rectification, erasure, restriction of processing, data portability, and objection to certain processing.',
        'Contact the owner via the Site\'s contact links to exercise your rights or ask questions.',
      ],
    },
    {
      heading: '7. Changes',
      paragraphs: [
        'This policy may be updated when the Site or legal requirements change. The «Last updated» date at the top will be revised for material changes.',
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
