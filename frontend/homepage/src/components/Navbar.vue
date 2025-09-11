<script setup lang="ts">
import { useRouter, useRoute } from 'vue-router'

const router = useRouter()
const route = useRoute()

const navigateTo = (routeName: string) => {
  router.push({ name: routeName })
}

const isActive = (routeName: string) => {
  return route.name === routeName
}
</script>

<template>
  <nav class="center-nav">
    <div class="nav-container">
      <div class="relative bg-gray-200 rounded-full p-1 flex">
        <div
          class="absolute top-1 bottom-1 w-16 bg-white rounded-full shadow-md transition-transform duration-300 ease-in-out"
          :class="isActive('home') ? 'translate-x-0' : (isActive('about') ? 'translate-x-16' : 'translate-x-0 opacity-0')"
        ></div>
        <button
          class="relative z-10 w-16 py-2 text-sm font-medium transition-colors duration-300 cursor-pointer"
          :class="isActive('home') ? 'text-gray-700' : 'text-gray-500'"
          @click="navigateTo('home')"
        >
          Home
        </button>
        <button
          class="relative z-10 w-16 py-2 text-sm font-medium transition-colors duration-300 cursor-pointer"
          :class="isActive('about') ? 'text-gray-700' : 'text-gray-500'"
          @click="navigateTo('about')"
        >
          About
        </button>
      </div>
    </div>
  </nav>
</template>

<style scoped>
.center-nav {
  position: fixed;
  top: 2rem;
  left: 50%;
  transform: translateX(-50%);
  z-index: 100;
}

.nav-container {
  display: flex;
  justify-content: center;
}

/* Mobile adjustments */
@media (max-width: 768px) {
  .center-nav {
    top: 1rem;
  }
}
</style>
