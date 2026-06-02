<script setup lang="ts">
import { computed } from 'vue'
import { RouterView, RouterLink, useRoute } from 'vue-router'
import Navbar from './components/Navbar.vue'
import FloatingChatButton from './components/FloatingChatButton.vue'
import AdminLoginButton from './components/AdminLoginButton.vue'
import CookieConsentBanner from './components/CookieConsentBanner.vue'
import CookieConsentSettingsModal from './components/CookieConsentSettingsModal.vue'
import { useLangStore } from './stores/lang'
import { openCookieSettings } from './lib/cookie-settings-state'

// Initialize the language store early so detection runs on app boot
const langStore = useLangStore()
const route = useRoute()

const adminRouteNames = new Set([
  'admin-tools',
  'admin-pipeline',
  'admin-chunks',
  'admin-question-suggestions',
  'admin-prompts',
])
const showPublicPageHeader = computed(() => !adminRouteNames.has(String(route.name ?? '')))

const privacyLabel = computed(() => (langStore.language === 'no' ? 'Personvernerklæring' : 'Privacy Policy'))
const cookieSettingsLabel = computed(() => (langStore.language === 'no' ? 'Informasjonskapsler' : 'Cookie Settings'))
const accessibilityLabel = computed(() =>
  langStore.language === 'no' ? 'Tilgjengelighet' : 'Accessibility',
)
const skipLinkLabel = computed(() =>
  langStore.language === 'no' ? 'Hopp til innhold' : 'Skip to content',
)
</script>

<template>
  <div class="flex min-h-dvh flex-col bg-gray-50">
    <a
      href="#main-content"
      class="sr-only focus:not-sr-only focus:absolute focus:left-4 focus:top-4 focus:z-[100] focus:rounded-lg focus:bg-white focus:px-4 focus:py-2 focus:text-sm focus:font-semibold focus:text-blue-700 focus:shadow-lg focus:ring-2 focus:ring-blue-600"
    >
      {{ skipLinkLabel }}
    </a>
    <Navbar v-if="showPublicPageHeader" />
    <AdminLoginButton />
    <div class="flex min-h-0 w-full flex-1 flex-col">
      <RouterView />
    </div>
    <footer class="relative z-20 shrink-0 border-t border-gray-100 bg-gray-50/80 py-3 text-center text-xs text-gray-400 backdrop-blur-sm">
      <RouterLink
        to="/privacy-policy"
        class="inline-block px-2 hover:text-gray-600 transition-colors"
      >
        {{ privacyLabel }}
      </RouterLink>
      <span class="text-gray-300" aria-hidden="true">|</span>
      <button
        type="button"
        class="inline-block px-2 bg-transparent border-none hover:text-gray-600 transition-colors cursor-pointer text-xs text-gray-400"
        @click="openCookieSettings"
      >
        {{ cookieSettingsLabel }}
      </button>
      <span class="text-gray-300" aria-hidden="true">|</span>
      <RouterLink
        to="/accessibility"
        class="inline-block px-2 hover:text-gray-600 transition-colors"
      >
        {{ accessibilityLabel }}
      </RouterLink>
    </footer>
    <CookieConsentSettingsModal />
    <CookieConsentBanner />
    <FloatingChatButton class="cursor-pointer" />
  </div>
</template>

<style scoped>
/* App shell is intentionally minimal */
</style>
