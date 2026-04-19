import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useLangStore } from '../lang'

describe('lang store', () => {
  beforeEach(() => {
    localStorage.clear()
    setActivePinia(createPinia())
  })

  afterEach(() => {
    vi.unstubAllGlobals()
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

  it('falls back to English when saved language is not en/no', () => {
    localStorage.setItem('lang', 'de')
    Object.defineProperty(window.navigator, 'language', {
      configurable: true,
      value: 'en-GB',
    })
    const store = useLangStore()
    expect(store.language).toBe('en')
  })

  it('uses navigator.languages[0] when language is empty', () => {
    vi.stubGlobal('navigator', {
      ...navigator,
      language: '',
      languages: ['nn-NO'],
    })
    setActivePinia(createPinia())
    const store = useLangStore()
    expect(store.language).toBe('no')
  })

  it('ignores localStorage read errors and still initializes', () => {
    const getItem = vi.spyOn(Storage.prototype, 'getItem').mockImplementation(() => {
      throw new Error('blocked')
    })
    setActivePinia(createPinia())
    const store = useLangStore()
    expect(['en', 'no']).toContain(store.language)
    getItem.mockRestore()
  })

  it('ignores localStorage write errors in setLanguage', () => {
    const setItem = vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => {
      throw new Error('quota')
    })
    const store = useLangStore()
    expect(() => store.setLanguage('en')).not.toThrow()
    expect(store.language).toBe('en')
    setItem.mockRestore()
  })
})
