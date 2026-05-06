<script setup lang="ts">
import { computed, watch, ref } from 'vue'
import { cookieSettingsOpen, closeCookieSettings } from '../lib/cookie-settings-state'
import {
  PRIVACY_POLICY_VERSION,
  getConsentRecord,
  saveGranularConsent,
} from '../lib/posthog-consent'
import { useLangStore } from '../stores/lang'

const langStore = useLangStore()

const pageviewsEnabled = ref(false)
const sessionRecordingEnabled = ref(false)
const errorTrackingEnabled = ref(false)
const featureFlagsEnabled = ref(false)

const isOpen = computed({
  get: () => cookieSettingsOpen.value,
  set: (value: boolean) => {
    cookieSettingsOpen.value = value
    if (!value) closeCookieSettings()
  },
})

function loadChoicesFromRecord(): void {
  const r = getConsentRecord()
  if (r?.dismissed) {
    pageviewsEnabled.value = r.pageviews
    sessionRecordingEnabled.value = r.sessionRecording
    errorTrackingEnabled.value = r.errorTracking
    featureFlagsEnabled.value = r.featureFlags
  } else {
    pageviewsEnabled.value = false
    sessionRecordingEnabled.value = false
    errorTrackingEnabled.value = false
    featureFlagsEnabled.value = false
  }
}

watch(
  () => cookieSettingsOpen.value,
  (value) => {
    if (value) {
      loadChoicesFromRecord()
    }
  },
  { immediate: true },
)

const title = computed(() =>
  langStore.language === 'no' ? 'Informasjonskapsler og analyse' : 'Cookies and analytics',
)
const description = computed(() =>
  langStore.language === 'no'
    ? 'Velg hvilke typer analyse og lagring du tillater. Valgene lagres i nettleseren og kan endres når som helst.'
    : 'Choose which types of analytics and storage you allow. Choices are saved in your browser and can be changed anytime.',
)

const necessaryLabel = computed(() =>
  langStore.language === 'no' ? 'Nødvendige' : 'Necessary',
)
const necessaryHelp = computed(() =>
  langStore.language === 'no'
    ? 'Kreves for at nettstedet skal fungere (f.eks. innlogging). Kan ikke slås av.'
    : 'Required for the site to function (e.g. login). Cannot be turned off.',
)

const pageviewsLabel = computed(() =>
  langStore.language === 'no' ? 'Sidevisninger (navigasjon)' : 'Pageview tracking',
)
const pageviewsHelp = computed(() =>
  langStore.language === 'no'
    ? `Samler inn hvilke sider som besøkes (aggregert). Personvernerklæring versjon ${PRIVACY_POLICY_VERSION}.`
    : `Collects which pages are visited (aggregate). Privacy policy version ${PRIVACY_POLICY_VERSION}.`,
)

const sessionLabel = computed(() =>
  langStore.language === 'no' ? 'Sesjonsopptak (replay)' : 'Session recordings',
)
const sessionHelp = computed(() =>
  langStore.language === 'no'
    ? 'Anonymiserte gjennomspillinger av økten for å forbedre nettstedet. Alle skjemafelt maskeres automatisk.'
    : 'Anonymized session replays to improve the site. All form inputs are automatically masked.',
)

const errorsLabel = computed(() =>
  langStore.language === 'no' ? 'Feilsporing' : 'Error tracking',
)
const errorsHelp = computed(() =>
  langStore.language === 'no'
    ? 'Sender tekniske feilmeldinger fra nettleseren slik at stabilitet kan forbedres.'
    : 'Sends technical error reports from the browser to improve stability.',
)

const flagsLabel = computed(() =>
  langStore.language === 'no' ? 'Funksjonsflagg og eksperimenter' : 'Feature flags & experiments',
)
const flagsHelp = computed(() =>
  langStore.language === 'no'
    ? 'Gjør det mulig å teste ulike varianter av funksjoner (f.eks. A/B-testing).'
    : 'Allows testing different variants of features (e.g. A/B tests).',
)

const saveLabel = computed(() => (langStore.language === 'no' ? 'Lagre valg' : 'Save choices'))
const cancelLabel = computed(() => (langStore.language === 'no' ? 'Avbryt' : 'Cancel'))

function handleSave(): void {
  saveGranularConsent(
    {
      pageviews: pageviewsEnabled.value,
      sessionRecording: sessionRecordingEnabled.value,
      errorTracking: errorTrackingEnabled.value,
      featureFlags: featureFlagsEnabled.value,
    },
    'settings',
  )
  closeCookieSettings()
}
</script>

<template>
  <transition name="fade">
    <div
      v-if="isOpen"
      class="fixed inset-0 z-50 flex items-center justify-center bg-black/40 px-4 py-8 backdrop-blur-sm"
      role="dialog"
      aria-modal="true"
    >
      <div class="max-h-[90vh] w-full max-w-lg overflow-y-auto rounded-2xl bg-white shadow-2xl ring-1 ring-black/5">
        <div class="border-b border-gray-100 px-6 py-4">
          <h2 class="text-lg font-semibold text-gray-900">
            {{ title }}
          </h2>
          <p class="mt-1 text-sm text-gray-600">
            {{ description }}
          </p>
        </div>

        <div class="space-y-4 px-6 py-5">
          <div class="rounded-xl border border-gray-200 bg-gray-50/60 p-4">
            <div class="flex items-start justify-between gap-3">
              <div>
                <p class="font-medium text-gray-900">
                  {{ necessaryLabel }}
                </p>
                <p class="mt-1 text-sm text-gray-600">
                  {{ necessaryHelp }}
                </p>
              </div>
              <input
                type="checkbox"
                checked
                disabled
                class="mt-1 h-4 w-4 rounded border-gray-300"
                aria-label="Necessary cookies (always on)"
              />
            </div>
          </div>

          <div class="rounded-xl border border-gray-200 p-4">
            <div class="flex items-start justify-between gap-3">
              <div>
                <p class="font-medium text-gray-900">
                  {{ pageviewsLabel }}
                </p>
                <p class="mt-1 text-sm text-gray-600">
                  {{ pageviewsHelp }}
                </p>
              </div>
              <input
                v-model="pageviewsEnabled"
                type="checkbox"
                class="mt-1 h-4 w-4 rounded border-gray-300 text-gray-900 focus:ring-gray-900"
                :aria-label="pageviewsLabel"
              />
            </div>
          </div>

          <div class="rounded-xl border border-gray-200 p-4">
            <div class="flex items-start justify-between gap-3">
              <div>
                <p class="font-medium text-gray-900">
                  {{ sessionLabel }}
                </p>
                <p class="mt-1 text-sm text-gray-600">
                  {{ sessionHelp }}
                </p>
              </div>
              <input
                v-model="sessionRecordingEnabled"
                type="checkbox"
                class="mt-1 h-4 w-4 rounded border-gray-300 text-gray-900 focus:ring-gray-900"
                :aria-label="sessionLabel"
              />
            </div>
          </div>

          <div class="rounded-xl border border-gray-200 p-4">
            <div class="flex items-start justify-between gap-3">
              <div>
                <p class="font-medium text-gray-900">
                  {{ errorsLabel }}
                </p>
                <p class="mt-1 text-sm text-gray-600">
                  {{ errorsHelp }}
                </p>
              </div>
              <input
                v-model="errorTrackingEnabled"
                type="checkbox"
                class="mt-1 h-4 w-4 rounded border-gray-300 text-gray-900 focus:ring-gray-900"
                :aria-label="errorsLabel"
              />
            </div>
          </div>

          <div class="rounded-xl border border-gray-200 p-4">
            <div class="flex items-start justify-between gap-3">
              <div>
                <p class="font-medium text-gray-900">
                  {{ flagsLabel }}
                </p>
                <p class="mt-1 text-sm text-gray-600">
                  {{ flagsHelp }}
                </p>
              </div>
              <input
                v-model="featureFlagsEnabled"
                type="checkbox"
                class="mt-1 h-4 w-4 rounded border-gray-300 text-gray-900 focus:ring-gray-900"
                :aria-label="flagsLabel"
              />
            </div>
          </div>
        </div>

        <div class="flex items-center justify-end gap-3 border-t border-gray-100 px-6 py-4">
          <button
            type="button"
            class="rounded-lg border border-gray-300 bg-white px-4 py-2 text-sm font-medium text-gray-700 transition-colors hover:bg-gray-50"
            @click="closeCookieSettings"
          >
            {{ cancelLabel }}
          </button>
          <button
            type="button"
            class="rounded-lg bg-gray-900 px-4 py-2 text-sm font-semibold text-white transition-colors hover:bg-gray-800"
            @click="handleSave"
          >
            {{ saveLabel }}
          </button>
        </div>
      </div>
    </div>
  </transition>
</template>

<style scoped>
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.15s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
