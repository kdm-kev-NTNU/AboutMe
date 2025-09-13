import { ref, readonly } from 'vue'

// Global state for tracking if welcome dialog is open
const isWelcomeDialogOpen = ref(false)
// Global state for tracking if education dialog is open
const isEducationDialogOpen = ref(false)

export function useDialogState() {
  const setWelcomeDialogOpen = (open: boolean) => {
    isWelcomeDialogOpen.value = open
  }

  const setEducationDialogOpen = (open: boolean) => {
    isEducationDialogOpen.value = open
  }

  return {
    isWelcomeDialogOpen: readonly(isWelcomeDialogOpen),
    setWelcomeDialogOpen,
    isEducationDialogOpen: readonly(isEducationDialogOpen),
    setEducationDialogOpen
  }
}