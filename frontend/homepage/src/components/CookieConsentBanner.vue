<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useLangStore } from '@/stores/lang'
import {
  grantAllCookies,
  grantNecessaryCookiesOnly,
  rejectOptionalCookies,
  isCookieBannerDismissed,
  isPosthogEnabled,
} from '../lib/posthog-consent'
import { openCookieSettings } from '../lib/cookie-settings-state'

const langStore = useLangStore()
const showBanner = ref(false)

const isNo = computed(() => langStore.language === 'no')

const bannerTitle = computed(() =>
  isNo.value ? 'Informasjonskapsler og analyse' : 'Cookies and analytics',
)
const bannerBody = computed(() =>
  isNo.value
    ? 'Jeg bruker PostHog for analyse (sidevisninger, valgfritt sesjonsopptak, feilsporing og eksperimenter). Du kan godta alle, bare nødvendige, avslå valgfritt — eller trykke Tilpass for å velge hver kategori.'
    : 'I use PostHog for analytics (page views, optional session replay, error tracking, and experiments). You can accept all, necessary only, reject optional tracking — or tap Customize to choose each category.',
)
const acceptAllLabel = computed(() => (isNo.value ? 'Godta alle' : 'Accept all'))
const necessaryOnlyLabel = computed(() =>
  isNo.value ? 'Kun nødvendige' : 'Necessary only',
)
const rejectLabel = computed(() => (isNo.value ? 'Avslå' : 'Reject'))
const customizeLabel = computed(() => (isNo.value ? 'Tilpass' : 'Customize'))

function refreshBanner() {
  showBanner.value = isPosthogEnabled() && !isCookieBannerDismissed()
}

function acceptAll() {
  grantAllCookies('banner_accept_all')
  showBanner.value = false
}

function necessaryOnly() {
  grantNecessaryCookiesOnly('banner_necessary_only')
  showBanner.value = false
}

function rejectOptional() {
  rejectOptionalCookies('banner_reject')
  showBanner.value = false
}

function openSettings() {
  openCookieSettings()
  showBanner.value = false
}

onMounted(refreshBanner)
</script>

<template>
  <div
    v-if="showBanner"
    class="fixed inset-x-4 bottom-4 z-50 mx-auto max-w-3xl rounded-2xl border border-gray-200 bg-white p-4 shadow-xl ring-1 ring-black/5 sm:inset-x-0"
    role="region"
    aria-label="Cookie consent"
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
        class="rounded-md bg-gray-900 px-3 py-2 text-xs font-semibold text-white transition-colors hover:bg-gray-800"
        @click="acceptAll"
      >
        {{ acceptAllLabel }}
      </button>
      <button
        type="button"
        class="rounded-md border border-gray-300 bg-white px-3 py-2 text-xs font-medium text-gray-800 transition-colors hover:bg-gray-100"
        @click="necessaryOnly"
      >
        {{ necessaryOnlyLabel }}
      </button>
      <button
        type="button"
        class="rounded-md border border-gray-300 bg-white px-3 py-2 text-xs font-medium text-gray-800 transition-colors hover:bg-gray-100"
        @click="rejectOptional"
      >
        {{ rejectLabel }}
      </button>
      <button
        type="button"
        class="rounded-md border border-gray-200 bg-gray-50 px-3 py-2 text-xs font-medium text-gray-700 transition-colors hover:bg-gray-100"
        @click="openSettings"
      >
        {{ customizeLabel }}
      </button>
    </div>
  </div>
</template>
