import { ref } from 'vue'

export const cookieSettingsOpen = ref(false)

export function openCookieSettings(): void {
  cookieSettingsOpen.value = true
}

export function closeCookieSettings(): void {
  cookieSettingsOpen.value = false
}
