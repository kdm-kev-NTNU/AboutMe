<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { MessageCircle } from 'lucide-vue-next'

const router = useRouter()
const route = useRoute()

const isVisible = ref(false)
const hasActiveChat = ref(false)

// Check if we're currently on the chat page
const isOnChatPage = computed(() => route.name === 'chat')

// Check if there are messages in session storage
const checkForActiveChat = () => {
  try {
    const messages = sessionStorage.getItem('chatMessages')
    hasActiveChat.value = messages && JSON.parse(messages).length > 0
  } catch (error) {
    hasActiveChat.value = false
  }
}

// Navigate to chat
const goToChat = () => {
  router.push({ name: 'chat' })
}

// Show/hide button based on conditions
const updateVisibility = () => {
  isVisible.value = !isOnChatPage.value && hasActiveChat.value
}

onMounted(() => {
  checkForActiveChat()
  updateVisibility()
  
  // Listen for route changes
  router.afterEach(() => {
    updateVisibility()
  })
  
  // Listen for storage changes (when chat messages are updated)
  window.addEventListener('storage', (e) => {
    if (e.key === 'chatMessages') {
      checkForActiveChat()
      updateVisibility()
    }
  })
  
  // Also listen for custom events when messages are updated in the same tab
  window.addEventListener('chatMessagesUpdated', () => {
    checkForActiveChat()
    updateVisibility()
  })
})
</script>

<template>
  <Transition
    enter-active-class="transition-all duration-300 ease-out"
    enter-from-class="opacity-0 scale-75 translate-y-4"
    enter-to-class="opacity-100 scale-100 translate-y-0"
    leave-active-class="transition-all duration-200 ease-in"
    leave-from-class="opacity-100 scale-100 translate-y-0"
    leave-to-class="opacity-0 scale-75 translate-y-4"
  >
    <button
      v-if="isVisible"
      @click="goToChat"
      class="fixed bottom-6 right-6 z-50 bg-black hover:bg-gray-800 text-white rounded-full p-4 shadow-lg hover:shadow-xl transition-all duration-300 ease-in-out transform hover:scale-105 focus:outline-none focus:ring-4 focus:ring-gray-300"
      :title="'Continue your chat with Kevin\'s AI'"
    >
      <MessageCircle class="w-6 h-6" />
      
      <!-- Notification dot -->
      <div class="absolute -top-1 -right-1 w-3 h-3 bg-red-500 rounded-full animate-pulse"></div>
    </button>
  </Transition>
</template>

<style scoped>
/* Additional styles if needed */
</style>