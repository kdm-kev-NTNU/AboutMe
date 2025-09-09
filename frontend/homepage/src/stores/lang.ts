import { defineStore } from 'pinia'

export type Language = 'en' | 'no'

function detectInitialLanguage(): Language {
  try {
    const saved = localStorage.getItem('lang') as Language | null
    if (saved === 'en' || saved === 'no') return saved
  } catch {}
  const nav = (navigator.language || navigator.languages?.[0] || 'en').toLowerCase()
  if (nav.startsWith('no') || nav.startsWith('nb') || nav.startsWith('nn')) return 'no'
  return 'en'
}

export const useLangStore = defineStore('lang', {
  state: () => ({
    language: detectInitialLanguage() as Language,
  }),
  actions: {
    setLanguage(lang: Language) {
      this.language = lang
      try { localStorage.setItem('lang', lang) } catch {}
    },
  },
})


