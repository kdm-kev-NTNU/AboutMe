import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { fetchRealtimeVoiceModels } from '@/lib/realtime-voice'
import { useVoiceModelStore } from '../voice-model'

vi.mock('@/lib/realtime-voice', async (importOriginal) => {
  const mod = await importOriginal<typeof import('@/lib/realtime-voice')>()
  return { ...mod, fetchRealtimeVoiceModels: vi.fn() }
})

describe('useVoiceModelStore', () => {
  beforeEach(() => {
    sessionStorage.clear()
    setActivePinia(createPinia())
    vi.mocked(fetchRealtimeVoiceModels).mockReset()
  })

  it('loads models and prefers the OpenAI default', async () => {
    vi.mocked(fetchRealtimeVoiceModels).mockResolvedValue([
      { provider: 'ELEVENLABS', id: 'agent_1', label: 'ElevenLabs Agent', defaultOption: true },
      { provider: 'OPENAI', id: 'gpt-realtime-2', label: 'OpenAI GPT-Realtime-2', defaultOption: true },
    ])
    const store = useVoiceModelStore()

    await store.ensureModelsLoaded()

    expect(store.models).toHaveLength(2)
    expect(store.selectedModelId).toBe('OPENAI:gpt-realtime-2')
    expect(store.selectedProvider).toBe('OPENAI')
    expect(sessionStorage.getItem('voiceSelectedModel')).toBe('OPENAI:gpt-realtime-2')
  })

  it('restores a stored configured model', async () => {
    sessionStorage.setItem('voiceSelectedModel', 'ELEVENLABS:agent_1')
    vi.mocked(fetchRealtimeVoiceModels).mockResolvedValue([
      { provider: 'OPENAI', id: 'gpt-realtime-2', label: 'OpenAI GPT-Realtime-2', defaultOption: true },
      { provider: 'ELEVENLABS', id: 'agent_1', label: 'ElevenLabs Agent', defaultOption: false },
    ])
    const store = useVoiceModelStore()

    await store.ensureModelsLoaded()

    expect(store.selectedModelId).toBe('ELEVENLABS:agent_1')
    expect(store.selectedProvider).toBe('ELEVENLABS')
  })

  it('ignores selection ids outside the loaded catalog', async () => {
    vi.mocked(fetchRealtimeVoiceModels).mockResolvedValue([
      { provider: 'OPENAI', id: 'gpt-realtime-2', label: 'OpenAI GPT-Realtime-2', defaultOption: true },
    ])
    const store = useVoiceModelStore()
    await store.ensureModelsLoaded()

    store.setSelectedModelId('missing')

    expect(store.selectedModelId).toBe('OPENAI:gpt-realtime-2')
  })
})
