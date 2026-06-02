import { beforeEach, describe, expect, it } from 'vitest'
import { nextTick } from 'vue'
import { createPinia, setActivePinia } from 'pinia'
import { setupDocumentLang } from '../useDocumentLang'
import { useLangStore } from '@/stores/lang'

describe('setupDocumentLang', () => {
  beforeEach(() => {
    document.documentElement.lang = 'en'
    setActivePinia(createPinia())
  })

  it('sets document lang from store on setup', () => {
    useLangStore().setLanguage('no')
    setupDocumentLang()
    expect(document.documentElement.lang).toBe('no')
  })

  it('updates document lang when store language changes', async () => {
    setupDocumentLang()
    useLangStore().setLanguage('no')
    await nextTick()
    expect(document.documentElement.lang).toBe('no')
    useLangStore().setLanguage('en')
    await nextTick()
    expect(document.documentElement.lang).toBe('en')
  })
})
