import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'

const fetchRealtimeVoiceStatus = vi.fn()

vi.mock('@/lib/realtime-voice', async () => {
  const actual = await vi.importActual<typeof import('@/lib/realtime-voice')>('@/lib/realtime-voice')
  return {
    ...actual,
    fetchRealtimeVoiceStatus: (...args: unknown[]) => fetchRealtimeVoiceStatus(...args),
  }
})

describe('useVoiceAvailabilityStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    fetchRealtimeVoiceStatus.mockReset()
  })

  it('loads once and maps liveEnabled to available', async () => {
    fetchRealtimeVoiceStatus.mockResolvedValue({
      enabled: true,
      liveEnabled: true,
      liveDisabledReason: null,
      voices: ['marin', 'cedar'],
      reasoningEfforts: ['low'],
      vadEagernessOptions: ['low'],
      voice: 'marin',
      reasoningEffort: 'low',
      vadEagerness: 'low',
    })
    const { useVoiceAvailabilityStore } = await import('../voice-availability')
    const store = useVoiceAvailabilityStore()
    await store.ensureLoaded()
    await store.ensureLoaded()
    expect(fetchRealtimeVoiceStatus).toHaveBeenCalledTimes(1)
    expect(store.state).toBe('available')
    expect(store.capability).toBe(true)
  })

  it('maps kill switch off to unavailable while keeping capability', async () => {
    fetchRealtimeVoiceStatus.mockResolvedValue({
      enabled: true,
      liveEnabled: false,
      liveDisabledReason: 'KILL_SWITCH',
      voices: ['marin'],
      reasoningEfforts: ['low'],
      vadEagernessOptions: ['low'],
      voice: 'marin',
      reasoningEffort: 'low',
      vadEagerness: 'low',
    })
    const { useVoiceAvailabilityStore } = await import('../voice-availability')
    const store = useVoiceAvailabilityStore()
    await store.ensureLoaded()
    expect(store.state).toBe('unavailable')
    expect(store.capability).toBe(true)
    expect(store.liveDisabledReason).toBe('KILL_SWITCH')
  })
})
