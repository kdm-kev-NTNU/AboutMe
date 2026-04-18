<script setup lang="ts">
import { computed } from 'vue'
import { RouterView, RouterLink } from 'vue-router'
import InfoSheet from './components/InfoSheet.vue'
import Navbar from './components/Navbar.vue'
import FloatingChatButton from './components/FloatingChatButton.vue'
import AdminLoginButton from './components/AdminLoginButton.vue'
import { useLangStore } from './stores/lang'

// Initialize the language store early so detection runs on app boot
const langStore = useLangStore()

const privacyLabel = computed(() => (langStore.language === 'no' ? 'Personvernerklæring' : 'Privacy Policy'))
const cookieSettingsLabel = computed(() => (langStore.language === 'no' ? 'Informasjonskapsler' : 'Cookie Settings'))
</script>

<template>
  <div class="min-h-screen bg-gray-50">
    <Navbar />
    <AdminLoginButton />
    <RouterView />
    <footer class="relative z-20 border-t border-gray-100 bg-gray-50/80 py-3 text-center text-xs text-gray-400 backdrop-blur-sm">
      <RouterLink
        to="/privacy-policy"
        class="inline-block px-2 hover:text-gray-600 transition-colors"
      >
        {{ privacyLabel }}
      </RouterLink>
      <span class="text-gray-300" aria-hidden="true">|</span>
      <button
        id="revoke-consent-btn"
        type="button"
        class="inline-block px-2 bg-transparent border-none hover:text-gray-600 transition-colors cursor-pointer text-xs text-gray-400"
      >
        {{ cookieSettingsLabel }}
      </button>
    </footer>
    <InfoSheet />
    <FloatingChatButton class="cursor-pointer" />
  </div>
</template>

<style scoped>
/* App shell is intentionally minimal */
</style>
