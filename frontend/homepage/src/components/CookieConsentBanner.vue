<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import posthog from 'posthog-js'
import { useLangStore } from '@/stores/lang'

const langStore = useLangStore()
const showBanner = ref(false)

const isNo = computed(() => langStore.language === 'no')

const bannerTitle = computed(() =>
  isNo.value ? 'Informasjonskapsler og analyse' : 'Cookies and analytics',
)
const bannerBody = computed(() =>
  isNo.value
    ? 'Vi bruker PostHog for å forstå hvordan nettsiden brukes. Du kan godta eller avslå analyse-cookies.'
    : 'We use PostHog to understand how the website is used. You can accept or decline analytics cookies.',
)
const acceptLabel = computed(() => (isNo.value ? 'Godta' : 'Accept'))
const declineLabel = computed(() => (isNo.value ? 'Avslå' : 'Decline'))

function refreshConsentStatus() {
  showBanner.value = posthog.get_explicit_consent_status() === 'pending'
}

function acceptConsent() {
  posthog.opt_in_capturing()
  showBanner.value = false
}

function declineConsent() {
  posthog.opt_out_capturing()
  showBanner.value = false
}

function openConsentSettings() {
  posthog.clear_opt_in_out_capturing()
  refreshConsentStatus()
}

onMounted(refreshConsentStatus)

defineExpose({
  openConsentSettings,
})
</script>

<template>
  <div
    v-if="showBanner"
    class="fixed inset-x-4 bottom-4 z-50 mx-auto max-w-2xl rounded-xl border border-gray-200 bg-white p-4 shadow-lg sm:inset-x-0"
  >
    <h2 class="text-sm font-semibold text-gray-900">
      {{ bannerTitle }}
    </h2>
    <p class="mt-2 text-sm leading-relaxed text-gray-700">
      {{ bannerBody }}
    </p>
    <div class="mt-4 flex flex-wrap gap-2">
      <button
        type="button"
        class="rounded-md bg-gray-900 px-3 py-2 text-xs font-medium text-white transition-colors hover:bg-gray-700"
        @click="acceptConsent"
      >
        {{ acceptLabel }}
      </button>
      <button
        type="button"
        class="rounded-md border border-gray-300 bg-white px-3 py-2 text-xs font-medium text-gray-700 transition-colors hover:bg-gray-100"
        @click="declineConsent"
      >
        {{ declineLabel }}
      </button>
    </div>
  </div>
</template>
