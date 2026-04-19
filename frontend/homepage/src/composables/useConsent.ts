import { ref, onMounted } from 'vue'

interface ConsentState {
  analytics: boolean
  marketing: boolean
  necessary: boolean
}

export function useConsent() {
  const consent = ref<ConsentState>({
    analytics: false,
    marketing: false,
    necessary: true,
  })

  function readConsent() {
    try {
      const raw = localStorage.getItem('csfy_consent')
      if (raw) {
        const parsed = JSON.parse(raw)
        consent.value = {
          analytics: !!parsed.analytics,
          marketing: !!parsed.marketing,
          necessary: true,
        }
      }
    } catch {
      // Consent not yet given or invalid
    }
  }

  onMounted(readConsent)

  return { consent, readConsent }
}
