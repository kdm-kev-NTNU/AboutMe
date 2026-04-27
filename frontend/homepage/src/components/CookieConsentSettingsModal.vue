<script setup lang="ts">
import { computed, watch, ref } from 'vue'
import { cookieSettingsOpen, closeCookieSettings } from '../lib/cookie-settings-state'
import {
  hasAnalyticsConsent,
  PRIVACY_POLICY_VERSION,
  saveAnalyticsConsent,
} from '../lib/posthog-consent'
import { useLangStore } from '../stores/lang'

const langStore = useLangStore()
const analyticsEnabled = ref(false)

const isOpen = computed({
  get: () => cookieSettingsOpen.value,
  set: (value: boolean) => {
    cookieSettingsOpen.value = value
    if (!value) closeCookieSettings()
  },
})

watch(
  () => cookieSettingsOpen.value,
  (value) => {
    if (value) {
      analyticsEnabled.value = hasAnalyticsConsent()
    }
  },
  { immediate: true },
)

const title = computed(() =>
  langStore.language === 'no' ? 'Informasjonskapsler og analyse' : 'Cookies and analytics',
)
const description = computed(() =>
  langStore.language === 'no'
    ? 'Velg hvilke informasjonskapsler du tillater. Analyse (PostHog) er valgfritt og starter først etter at du slår det på og lagrer.'
    : 'Choose which cookies you allow. Analytics (PostHog) is optional and only starts after you enable it and save.',
)
const analyticsLabel = computed(() =>
  langStore.language === 'no' ? 'Analyse (PostHog)' : 'Analytics (PostHog)',
)
const analyticsHelp = computed(() =>
  langStore.language === 'no'
    ? `Hjelper oss å forstå bruk av siden (aggregert), inkludert anonymiserte sesjonsopptak. Alle skjemafelt maskeres automatisk. Behandles etter personvernerklæringen (versjon ${PRIVACY_POLICY_VERSION}).`
    : `Helps us understand site usage in aggregate, including anonymized session recordings. All form inputs are automatically masked. Governed by the privacy policy (version ${PRIVACY_POLICY_VERSION}).`,
)
const saveLabel = computed(() => (langStore.language === 'no' ? 'Lagre valg' : 'Save choices'))
const cancelLabel = computed(() => (langStore.language === 'no' ? 'Avbryt' : 'Cancel'))
const necessaryLabel = computed(() =>
  langStore.language === 'no' ? 'Nødvendige' : 'Necessary',
)
const necessaryHelp = computed(() =>
  langStore.language === 'no'
    ? 'Kreves for at nettstedet skal fungere (f.eks. innlogging). Kan ikke slås av.'
    : 'Required for the site to function (e.g. login). Cannot be turned off.',
)

function handleSave(): void {
  saveAnalyticsConsent(analyticsEnabled.value, 'settings')
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
      <div class="w-full max-w-lg rounded-2xl bg-white shadow-2xl ring-1 ring-black/5">
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
                  {{ analyticsLabel }}
                </p>
                <p class="mt-1 text-sm text-gray-600">
                  {{ analyticsHelp }}
                </p>
              </div>
              <input
                v-model="analyticsEnabled"
                type="checkbox"
                class="mt-1 h-4 w-4 rounded border-gray-300 text-gray-900 focus:ring-gray-900"
                :aria-label="analyticsLabel"
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
