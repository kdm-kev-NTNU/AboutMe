import { beforeEach, describe, expect, it } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useLangStore } from '../lang'

describe('lang store', () => {
  beforeEach(() => {
    localStorage.clear()
    setActivePinia(createPinia())
  })

  it('uses stored language from localStorage when present', () => {
    localStorage.setItem('lang', 'no')

    const store = useLangStore()

    expect(store.language).toBe('no')
  })

  it('defaults to norwegian when browser language is norwegian', () => {
    Object.defineProperty(window.navigator, 'language', {
      configurable: true,
      value: 'nb-NO',
    })

    const store = useLangStore()

    expect(store.language).toBe('no')
  })

  it('persists language changes to localStorage', () => {
    const store = useLangStore()

    store.setLanguage('no')

    expect(store.language).toBe('no')
    expect(localStorage.getItem('lang')).toBe('no')
  })
})
