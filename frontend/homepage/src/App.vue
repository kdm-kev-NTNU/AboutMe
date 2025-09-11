<script setup lang="ts">
import { RouterView } from 'vue-router'
import InfoSheet from './components/InfoSheet.vue'
import Navbar from './components/Navbar.vue'
import { useLangStore } from './stores/lang'

// Initialize the language store early so detection runs on app boot
useLangStore()
</script>

<template>
  <div class="min-h-screen bg-gray-50">
    <Navbar />
    <RouterView />
    <InfoSheet />
  </div>
</template>

<style scoped>
/* App shell is intentionally minimal */
</style>
