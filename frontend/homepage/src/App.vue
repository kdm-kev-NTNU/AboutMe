<script setup lang="ts">
import { RouterView } from 'vue-router'
import InfoSheet from './components/InfoSheet.vue'
import { useLangStore } from './stores/lang'

// Initialize the language store early so detection runs on app boot
useLangStore()
</script>

<template>
  <RouterView />
  <InfoSheet />
  
</template>

<style scoped>
/* App shell is intentionally minimal */
</style>
