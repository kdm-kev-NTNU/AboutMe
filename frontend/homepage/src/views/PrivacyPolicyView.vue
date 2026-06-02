<script setup lang="ts">
import { computed } from 'vue'
import { useLangStore } from '../stores/lang'

const langStore = useLangStore()
const isNo = computed(() => langStore.language === 'no')

const title = computed(() => (isNo.value ? 'Personvernerklæring' : 'Privacy Policy'))
const lastUpdated = computed(() =>
  isNo.value ? 'Sist oppdatert: juni 2026' : 'Last updated: June 2026',
)

const sections = computed(() => {
  if (isNo.value) {
    return [
      {
        heading: '1. Innledning',
        paragraphs: [
          'Denne personvernerklæringen beskriver hvordan porteføljesiden «Kevin\'s AI» (heretter «nettstedet») behandler personopplysninger og bruker informasjonskapsler (cookies).',
          'Informasjonen her gjelder i tillegg til de valgene du gjør i samtykkeløsningen for informasjonskapsler. Analyse og valgfrie cookies aktiveres bare når du samtykker.',
        ],
      },
      {
        heading: '2. Behandlingsansvarlig',
        paragraphs: [
          'Nettstedet drives som en personlig portefølje. For henvendelser om personvern, kontakt eier via lenkene som er oppgitt på nettstedet (for eksempel GitHub eller LinkedIn).',
        ],
      },
      {
        heading: '3. Informasjonskapsler og samtykke',
        paragraphs: [
          'Nettstedet bruker PostHog for analyse når du samtykker. Du kan velge hver kategori for seg: sidevisninger (navigasjon), valgfritt sesjonsopptak (anonymiserte replays med maskerte skjemafelt), feilsporing i nettleseren, samt funksjonsflagg og eksperimenter (f.eks. A/B-testing).',
          'Teknisk lagring knyttet til disse valgene skjer i nettleseren (for eksempel lokalt lager) i tråd med innstillingene du lagrer.',
          'Du kan når som helst åpne «Informasjonskapsler» i bunnteksten for å endre eller trekke tilbake samtykke per kategori.',
        ],
      },
      {
        heading: '4. Chat-funksjonen, taleinndata og AI',
        paragraphs: [
          'Når du bruker chat-funksjonen, sendes meldingene dine til en tjeneste som bruker en språkmodell (for eksempel OpenAI) for å generere svar.',
          'Chat-siden og forsiden tilbyr også taleinndata. Når du aktiverer mikrofonen, tas lyden opp i nettleseren og sendes til vår server, som videresender den til en ekstern transkripsjonstjeneste (for tiden OpenAI Whisper). Lydfilen behandles kun for å produsere tekst og lagres ikke permanent verken hos oss eller hos leverandøren utover det som kreves for selve transkripsjonen. Den transkriberte teksten settes inn i chat-feltet på samme måte som om du hadde skrevet den selv.',
          'Valgfri live stemmechat (når den er aktivert) bruker WebRTC slik at lyd kan strømmes mellom nettleseren din og OpenAI Realtime for sanntidssamtale. Vår server brukes til å starte økten og til grensekontroll (f.eks. hastighetsbegrensning); selve samtalen behandles av OpenAI i tråd med deres vilkår og personvernerklæring.',
          'Taleinndata krever at du gir nettleseren tilgang til mikrofonen. Ingen lydopptak starter uten at du aktivt trykker på mikrofonknappen.',
          'Unngå å sende sensitive personopplysninger, helseopplysninger, passord eller annet du ikke ønsker å dele med en ekstern leverandør — dette gjelder både tekst og tale.',
          'Tekst og filer som inngår i kunnskapsbasen for AI-svar, lastes inn via eiers administrerte dokumentpipeline. Lydopptak og transkripsjoner fra taleinndata inngår ikke automatisk i kunnskapsbasen uten egen, kontrollert behandling og opplasting.',
        ],
      },
      {
        heading: '5. Behandlingsgrunnlag',
        paragraphs: [
          'Samtykke (GDPR art. 6 nr. 1 bokstav a) for informasjonskapsler og valgfri analyse via PostHog.',
          'Berettiget interesse eller avtale om levering av tjenesten (art. 6 nr. 1 bokstav b eller f) når du aktivt bruker chat, tale eller tilbakemeldingsskjema — for å svare på forespørselen du sender.',
          'Vi bruker ikke chat- eller taledata til automatiserte avgjørelser med rettsvirkning eller tilsvarende vesentlig virkning for deg.',
        ],
      },
      {
        heading: '6. Oppbevaring',
        paragraphs: [
          'Chat-transkript i nettleseren lagres i sessionStorage på din enhet og slettes når du lukker fanen eller tømmer chatten.',
          'Tilbakemeldinger du sender via skjemaet lagres på serveren for å forbedre nettstedet inntil de slettes manuelt av eier.',
          'Serverlogger (f.eks. IP-adresse, tidspunkt) kan oppbevares kortvarig for drift og sikkerhet; nøyaktig varighet avhenger av hosting-oppsett (Railway). Kontakt eier for detaljer.',
          'PostHog-data oppbevares hos PostHog i tråd med deres vilkår og dine samtykkevalg.',
        ],
      },
      {
        heading: '7. Tilbakemeldinger',
        paragraphs: [
          'Tilbakemeldingssiden lar deg sende fritekst. Innholdet brukes kun til å forbedre nettstedet og deles ikke med tredjeparter for markedsføring.',
          'Ikke send sensitive opplysninger i tilbakemeldinger.',
        ],
      },
      {
        heading: '8. Underleverandører (databehandlere)',
        paragraphs: [
          'OpenAI — språkmodell (chat), Whisper (tale-til-tekst) og Realtime (live stemme), behandling i USA/EU etter deres vilkår.',
          'PostHog — analyse, sesjonsopptak og feilsporing når du samtykker; standard vertsregion EU (eu.posthog.com).',
          'Railway — hosting av backend og tilhørende infrastruktur.',
          'Behandling skjer på databehandleravtale eller tilsvarende standardvilkår der det er påkrevd.',
        ],
      },
      {
        heading: '9. Teknisk drift og sikkerhet',
        paragraphs: [
          'Som på de fleste nettsteder kan servere og infrastruktur logge teknisk informasjon (f.eks. IP-adresse, tidspunkt og forespørsel) som er nødvendig for drift og sikkerhet.',
        ],
      },
      {
        heading: '10. Dine rettigheter',
        paragraphs: [
          'Avhengig av gjeldende lov (f.eks. GDPR) kan du ha rett til innsyn, retting, sletting, begrensning av behandling, dataportabilitet og å protestere mot visse typer behandling.',
          'Kontakt eier via kontaktlenkene på nettstedet for å utøve rettigheter eller stille spørsmål.',
        ],
      },
      {
        heading: '11. AI-system og transparens',
        paragraphs: [
          'Chat og stemme på nettstedet er AI-assistenter som svarer på bakgrunn av eiers dokumenter og offentlig porteføljeinnhold — ikke en live samtale med Kevin personlig.',
          'I tråd med transparenskrav for AI-systemer (bl.a. EU AI Act art. 50 for visse chatboter) informerer vi om at innhold er AI-generert og kan være unøyaktig.',
          'Systemet tar ikke automatiserte beslutninger med rettsvirkning eller tilsvarende vesentlig virkning for besøkende.',
        ],
      },
      {
        heading: '12. Endringer',
        paragraphs: [
          'Denne erklæringen kan oppdateres ved endringer på nettstedet eller i lovkrav. «Sist oppdatert»-datoen øverst vil bli justert ved vesentlige endringer. Vesentlige endringer kan kreve nytt samtykke for informasjonskapsler.',
        ],
      },
    ]
  }
  return [
    {
      heading: '1. Introduction',
      paragraphs: [
        'This privacy policy explains how the «Kevin\'s AI» portfolio site (the «Site») handles personal data and uses cookies.',
        'This information applies in addition to the choices you make in the cookie consent tool. Analytics and optional cookies are enabled only when you consent.',
      ],
    },
    {
      heading: '2. Controller',
      paragraphs: [
        'The Site is operated as a personal portfolio. For privacy-related requests, contact the owner via the links provided on the Site (for example GitHub or LinkedIn).',
      ],
    },
    {
      heading: '3. Cookies and consent',
      paragraphs: [
        'The Site uses PostHog for analytics when you consent. You can choose each category separately: pageview tracking (navigation), optional session replay (anonymized replays with masked form fields), in-browser error reporting, and feature flags / experiments (for example A/B tests).',
        'Technical storage related to these choices may be kept in your browser (for example local storage) according to the preferences you save.',
        'You can open «Cookie Settings» in the footer at any time to change or withdraw consent by category.',
      ],
    },
    {
      heading: '4. Chat, voice input, and AI',
      paragraphs: [
        'When you use the chat feature, your messages are sent to a service that uses a language model (for example OpenAI) to generate replies.',
        'The chat page and the home page also offer voice input. When you activate the microphone, audio is recorded in your browser and sent to our server, which forwards it to an external transcription service (currently OpenAI Whisper). The audio file is processed only to produce text and is not permanently stored by us or the provider beyond what is needed for the transcription itself. The transcribed text is inserted into the chat field just as if you had typed it yourself.',
        'Optional live voice chat (when enabled) uses WebRTC so audio can stream between your browser and OpenAI Realtime for a real-time conversation. Our server is used mainly to start the session and enforce limits (such as rate limits); the conversation is processed by OpenAI according to their terms and privacy policy.',
        'Voice input requires you to grant the browser access to your microphone. No audio recording starts without you actively pressing the microphone button.',
        'Do not send sensitive personal data, health information, passwords, or anything you do not want shared with an external provider — this applies to both text and voice.',
        'Text and files that power the AI knowledge base are ingested through the owner’s controlled document pipeline. Audio recordings and transcriptions from voice input are not automatically included in the knowledge base without separate, controlled processing and upload.',
      ],
    },
    {
      heading: '5. Legal basis',
      paragraphs: [
        'Consent (GDPR Art. 6(1)(a)) for cookies and optional analytics via PostHog.',
        'Contract or legitimate interest (Art. 6(1)(b) or (f)) when you actively use chat, voice, or the feedback form — to respond to the request you send.',
        'We do not use chat or voice data for automated decisions with legal or similarly significant effects on you.',
      ],
    },
    {
      heading: '6. Retention',
      paragraphs: [
        'Chat transcripts in the browser are stored in sessionStorage on your device and are cleared when you close the tab or clear the chat.',
        'Feedback you submit via the form is stored on the server to improve the Site until deleted by the owner.',
        'Server logs (for example IP address and timestamps) may be kept briefly for operations and security; exact duration depends on hosting (Railway). Contact the owner for details.',
        'PostHog data is retained by PostHog according to their terms and your consent choices.',
      ],
    },
    {
      heading: '7. Feedback',
      paragraphs: [
        'The feedback page lets you send free text. Content is used only to improve the Site and is not shared with third parties for marketing.',
        'Do not send sensitive information in feedback messages.',
      ],
    },
    {
      heading: '8. Subprocessors',
      paragraphs: [
        'OpenAI — language model (chat), Whisper (speech-to-text), and Realtime (live voice), processing under their terms.',
        'PostHog — analytics, session replay, and error reporting when you consent; default EU hosting (eu.posthog.com).',
        'Railway — hosting for the backend and related infrastructure.',
        'Processing relies on processor terms or equivalent agreements where required.',
      ],
    },
    {
      heading: '9. Technical operations and security',
      paragraphs: [
        'Like most websites, servers and infrastructure may log technical information (such as IP address, timestamps, and requests) as needed for operation and security.',
      ],
    },
    {
      heading: '10. Your rights',
      paragraphs: [
        'Depending on applicable law (for example the GDPR), you may have rights of access, rectification, erasure, restriction of processing, data portability, and objection to certain processing.',
        'Contact the owner via the Site\'s contact links to exercise your rights or ask questions.',
      ],
    },
    {
      heading: '11. AI system and transparency',
      paragraphs: [
        'Chat and voice on the Site are AI assistants that answer based on the owner’s documents and public portfolio content — not a live conversation with Kevin in person.',
        'In line with transparency requirements for AI systems (including EU AI Act Art. 50 for certain chatbots), we disclose that content is AI-generated and may be inaccurate.',
        'The system does not make automated decisions with legal or similarly significant effects on visitors.',
      ],
    },
    {
      heading: '12. Changes',
      paragraphs: [
        'This policy may be updated when the Site or legal requirements change. The «Last updated» date at the top will be revised for material changes. Material changes may require renewed cookie consent.',
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
