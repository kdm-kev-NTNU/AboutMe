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

  // --- Language detection edge cases for Norwegian dialects ---

  it('detects nb (bokmål) as Norwegian', () => {
    Object.defineProperty(window.navigator, 'language', {
      configurable: true,
      value: 'nb',
    })
    const store = useLangStore()
    expect(store.language).toBe('no')
  })

  it('detects nn (nynorsk) as Norwegian', () => {
    Object.defineProperty(window.navigator, 'language', {
      configurable: true,
      value: 'nn',
    })
    const store = useLangStore()
    expect(store.language).toBe('no')
  })

  it('detects no-NO as Norwegian', () => {
    Object.defineProperty(window.navigator, 'language', {
      configurable: true,
      value: 'no-NO',
    })
    const store = useLangStore()
    expect(store.language).toBe('no')
  })

  it('detects nn-NO (nynorsk with region) as Norwegian', () => {
    vi.stubGlobal('navigator', {
      ...navigator,
      language: 'nn-NO',
      languages: ['nn-NO'],
    })
    setActivePinia(createPinia())
    const store = useLangStore()
    expect(store.language).toBe('no')
  })

  // --- English variant detection ---

  it('detects en-US as English', () => {
    Object.defineProperty(window.navigator, 'language', {
      configurable: true,
      value: 'en-US',
    })
    const store = useLangStore()
    expect(store.language).toBe('en')
  })

  it('detects en-GB as English', () => {
    Object.defineProperty(window.navigator, 'language', {
      configurable: true,
      value: 'en-GB',
    })
    const store = useLangStore()
    expect(store.language).toBe('en')
  })

  // --- Other languages default to English ---

  it('detects fr-FR (French) as English fallback', () => {
    Object.defineProperty(window.navigator, 'language', {
      configurable: true,
      value: 'fr-FR',
    })
    const store = useLangStore()
    expect(store.language).toBe('en')
  })

  it('detects de-DE (German) as English fallback', () => {
    Object.defineProperty(window.navigator, 'language', {
      configurable: true,
      value: 'de-DE',
    })
    const store = useLangStore()
    expect(store.language).toBe('en')
  })

  it('detects sv-SE (Swedish) as English fallback', () => {
    Object.defineProperty(window.navigator, 'language', {
      configurable: true,
      value: 'sv-SE',
    })
    const store = useLangStore()
    expect(store.language).toBe('en')
  })

  it('detects da-DK (Danish) as English fallback', () => {
    Object.defineProperty(window.navigator, 'language', {
      configurable: true,
      value: 'da-DK',
    })
    const store = useLangStore()
    expect(store.language).toBe('en')
  })

  // --- Language store toggle ---

  it('can toggle between en and no', () => {
    const store = useLangStore()
    store.setLanguage('en')
    expect(store.language).toBe('en')
    store.setLanguage('no')
    expect(store.language).toBe('no')
    store.setLanguage('en')
    expect(store.language).toBe('en')
  })

  it('persists explicit no to localStorage', () => {
    const store = useLangStore()
    store.setLanguage('no')
    expect(localStorage.getItem('lang')).toBe('no')
    store.setLanguage('en')
    expect(localStorage.getItem('lang')).toBe('en')
  })

  // --- Priority: localStorage over navigator ---

  it('prefers localStorage en even when browser is Norwegian', () => {
    localStorage.setItem('lang', 'en')
    Object.defineProperty(window.navigator, 'language', {
      configurable: true,
      value: 'nb-NO',
    })
    const store = useLangStore()
    expect(store.language).toBe('en')
  })

  it('prefers localStorage no even when browser is English', () => {
    localStorage.setItem('lang', 'no')
    Object.defineProperty(window.navigator, 'language', {
      configurable: true,
      value: 'en-US',
    })
    const store = useLangStore()
    expect(store.language).toBe('no')
  })
})
