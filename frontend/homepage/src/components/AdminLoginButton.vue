<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { RouterLink } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

// Floating login + ADMIN tools link; session cookie authenticates /admin/** APIs.
const auth = useAuthStore()
const showForm = ref(false)
const username = ref('')
const password = ref('')
const isLoading = ref(false)
const error = ref('')

onMounted(() => auth.restore())

const isAdmin = computed(() => auth.role === 'ADMIN')

async function submit() {
  error.value = ''
  isLoading.value = true
  try {
    await auth.login(username.value, password.value)
    showForm.value = false
  } catch {
    error.value = 'Feil brukernavn eller passord'
  } finally {
    isLoading.value = false
  }
}

async function logout() {
  await auth.logout()
}
</script>

<template>
  <div class="fixed top-4 right-4 z-50">
    <div v-if="!isAdmin" class="space-x-2">
      <button class="px-3 py-2 rounded-md text-white bg-blue-600 hover:bg-blue-700 cursor-pointer" @click="showForm = !showForm">Admin</button>
      <div v-if="showForm" class="mt-2 p-3 bg-white rounded-md shadow-lg border border-gray-200 w-64">
        <div class="mb-2 text-sm font-semibold">Admin login</div>
        <input v-model="username" type="text" placeholder="Brukernavn" class="w-full mb-2 border rounded px-2 py-1" />
        <input v-model="password" type="password" placeholder="Passord" class="w-full mb-2 border rounded px-2 py-1" />
        <button :disabled="isLoading" class="w-full px-3 py-2 rounded-md text-white bg-blue-600 hover:bg-blue-700 disabled:opacity-50 cursor-pointer" @click="submit">Logg inn</button>
        <div v-if="error" class="text-red-600 text-xs mt-1">{{ error }}</div>
      </div>
    </div>
    <div v-else class="flex items-center gap-2 flex-wrap justify-end max-w-[min(100vw-2rem,24rem)]">
      <RouterLink
        to="/admin/tools"
        class="px-3 py-2 rounded-md text-sm text-white bg-emerald-600 hover:bg-emerald-700 cursor-pointer"
      >
        Internal tools
      </RouterLink>
      <span class="text-sm text-gray-700">Innlogget som {{ auth.username }}</span>
      <button class="px-3 py-2 rounded-md text-white bg-gray-600 hover:bg-gray-700 cursor-pointer" @click="logout">Logg ut</button>
    </div>
  </div>
  
</template>

<style scoped>
</style>


