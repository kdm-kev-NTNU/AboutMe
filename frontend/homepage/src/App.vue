<script setup lang="ts">
import { computed, ref } from 'vue'
import { RouterView, RouterLink, useRoute } from 'vue-router'
import Navbar from './components/Navbar.vue'
import FloatingChatButton from './components/FloatingChatButton.vue'
import AdminLoginButton from './components/AdminLoginButton.vue'
import CookieConsentBanner from './components/CookieConsentBanner.vue'
import { useLangStore } from './stores/lang'

// Initialize the language store early so detection runs on app boot
const langStore = useLangStore()
const route = useRoute()

const adminRouteNames = new Set(['admin-tools', 'admin-pipeline', 'admin-chunks', 'admin-prompts'])
const showPublicPageHeader = computed(() => !adminRouteNames.has(String(route.name ?? '')))

const privacyLabel = computed(() => (langStore.language === 'no' ? 'Personvernerklæring' : 'Privacy Policy'))
const cookieSettingsLabel = computed(() => (langStore.language === 'no' ? 'Informasjonskapsler' : 'Cookie Settings'))
const cookieConsentBannerRef = ref<InstanceType<typeof CookieConsentBanner> | null>(null)

function openCookieSettings() {
  cookieConsentBannerRef.value?.openConsentSettings()
}
</script>

<template>
  <div class="flex min-h-dvh flex-col bg-gray-50">
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
    </footer>
    <CookieConsentBanner ref="cookieConsentBannerRef" />
    <FloatingChatButton class="cursor-pointer" />
  </div>
</template>

<style scoped>
/* App shell is intentionally minimal */
</style>
