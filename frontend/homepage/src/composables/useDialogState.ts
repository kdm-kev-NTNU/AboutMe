import { ref, readonly } from 'vue'

// Global state for tracking if welcome dialog is open
const isWelcomeDialogOpen = ref(false)

export function useDialogState() {
  const setWelcomeDialogOpen = (open: boolean) => {
    isWelcomeDialogOpen.value = open
  }

  return {
    isWelcomeDialogOpen: readonly(isWelcomeDialogOpen),
    setWelcomeDialogOpen
  }
}