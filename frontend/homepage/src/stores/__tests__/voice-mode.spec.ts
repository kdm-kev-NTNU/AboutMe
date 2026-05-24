import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useVoiceModeStore } from '../voice-mode'

describe('useVoiceModeStore', () => {
  beforeEach(() => {
    sessionStorage.clear()
    setActivePinia(createPinia())
  })

  it('loads a stored voice mode', () => {
    sessionStorage.setItem('voiceMode', 'live')
    const store = useVoiceModeStore()
    store.load()
    expect(store.mode).toBe('live')
  })

  it('ignores invalid stored values', () => {
    sessionStorage.setItem('voiceMode', 'invalid')
    const store = useVoiceModeStore()
    store.load()
    expect(store.mode).toBe('standard')
  })

  it('persists setMode to sessionStorage', () => {
    const store = useVoiceModeStore()
    store.setMode('live')
    expect(store.mode).toBe('live')
    expect(sessionStorage.getItem('voiceMode')).toBe('live')
  })

  it('ignores sessionStorage failures when loading', () => {
    const getItem = vi.spyOn(Storage.prototype, 'getItem').mockImplementation(() => {
      throw new Error('blocked')
    })
    const store = useVoiceModeStore()
    store.load()
    expect(store.mode).toBe('standard')
    getItem.mockRestore()
  })

  it('ignores sessionStorage failures when saving', () => {
    const setItem = vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => {
      throw new Error('blocked')
    })
    const store = useVoiceModeStore()
    store.setMode('live')
    expect(store.mode).toBe('live')
    setItem.mockRestore()
  })
})
