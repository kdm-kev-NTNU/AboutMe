import { watch } from 'vue'
import { useLangStore } from '@/stores/lang'

/** Keeps document.documentElement.lang in sync with the active UI language (WCAG 3.1.1). */
export function setupDocumentLang(): void {
  const langStore = useLangStore()

  const apply = () => {
    document.documentElement.lang = langStore.language === 'no' ? 'no' : 'en'
  }

  apply()
  watch(() => langStore.language, apply)
}
