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
    localStorage.clear()
    sessionStorage.clear()
    setActivePinia(createPinia())
    vi.mocked(fetchRealtimeVoiceModels).mockReset()
  })

  it('exposes the selected model through getters', async () => {
    vi.mocked(fetchRealtimeVoiceModels).mockResolvedValue([
      { provider: 'OPENAI', id: 'gpt-realtime-2', label: 'OpenAI GPT-Realtime-2', defaultOption: true },
    ])
    const store = useVoiceModelStore()

    await store.ensureModelsLoaded()

    expect(store.selectedModel?.id).toBe('gpt-realtime-2')
    expect(store.selectedProvider).toBe('OPENAI')
    expect(store.hasModels).toBe(true)
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
    expect(localStorage.getItem('voiceSelectedModel')).toBe('OPENAI:gpt-realtime-2')
  })

  it('restores a stored configured model', async () => {
    localStorage.setItem('voiceSelectedModel', 'ELEVENLABS:agent_1')
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

  it('restores a legacy id-only stored model key', async () => {
    localStorage.setItem('voiceSelectedModel', 'gpt-realtime-2')
    vi.mocked(fetchRealtimeVoiceModels).mockResolvedValue([
      { provider: 'OPENAI', id: 'gpt-realtime-2', label: 'OpenAI GPT-Realtime-2', defaultOption: true },
    ])
    const store = useVoiceModelStore()

    await store.ensureModelsLoaded()

    expect(store.selectedModelId).toBe('OPENAI:gpt-realtime-2')
  })

  it('re-applies selection when models are cached but selection is empty', async () => {
    vi.mocked(fetchRealtimeVoiceModels).mockResolvedValue([
      { provider: 'OPENAI', id: 'gpt-realtime-2', label: 'OpenAI GPT-Realtime-2', defaultOption: true },
    ])
    const store = useVoiceModelStore()
    await store.ensureModelsLoaded()
    store.selectedModelId = ''

    await store.ensureModelsLoaded()

    expect(store.selectedModelId).toBe('OPENAI:gpt-realtime-2')
    expect(fetchRealtimeVoiceModels).toHaveBeenCalledTimes(1)
  })

  it('deduplicates concurrent model loads', async () => {
    let resolveModels: (value: Awaited<ReturnType<typeof fetchRealtimeVoiceModels>>) => void
    vi.mocked(fetchRealtimeVoiceModels).mockImplementation(
      () =>
        new Promise((resolve) => {
          resolveModels = resolve
        }),
    )
    const store = useVoiceModelStore()
    const first = store.ensureModelsLoaded()
    const second = store.ensureModelsLoaded()

    resolveModels!([
      { provider: 'OPENAI', id: 'gpt-realtime-2', label: 'OpenAI GPT-Realtime-2', defaultOption: true },
    ])
    await Promise.all([first, second])
    expect(fetchRealtimeVoiceModels).toHaveBeenCalledTimes(1)
    expect(store.models).toHaveLength(1)
  })

  it('falls back to the first model when no OpenAI default exists', async () => {
    vi.mocked(fetchRealtimeVoiceModels).mockResolvedValue([
      { provider: 'ELEVENLABS', id: 'agent_1', label: 'ElevenLabs Agent', defaultOption: false },
    ])
    const store = useVoiceModelStore()

    await store.ensureModelsLoaded()

    expect(store.selectedModelId).toBe('ELEVENLABS:agent_1')
  })

  it('skips persisting when no model is selected', () => {
    const store = useVoiceModelStore()
    store.selectedModelId = ''
    store.persistModelId()
    expect(localStorage.getItem('voiceSelectedModel')).toBeNull()
  })

  it('exposes null provider when nothing is selected', () => {
    const store = useVoiceModelStore()
    expect(store.selectedProvider).toBeNull()
  })

  it('migrates legacy sessionStorage model keys to localStorage', async () => {
    sessionStorage.setItem('voiceSelectedModel', 'OPENAI:gpt-realtime-2')
    vi.mocked(fetchRealtimeVoiceModels).mockResolvedValue([
      { provider: 'OPENAI', id: 'gpt-realtime-2', label: 'OpenAI GPT-Realtime-2', defaultOption: true },
    ])
    const store = useVoiceModelStore()

    await store.ensureModelsLoaded()

    expect(store.selectedModelId).toBe('OPENAI:gpt-realtime-2')
    expect(localStorage.getItem('voiceSelectedModel')).toBe('OPENAI:gpt-realtime-2')
    expect(sessionStorage.getItem('voiceSelectedModel')).toBeNull()
  })

  it('ignores malformed stored model keys', async () => {
    localStorage.setItem('voiceSelectedModel', ':')
    vi.mocked(fetchRealtimeVoiceModels).mockResolvedValue([
      { provider: 'OPENAI', id: 'gpt-realtime-2', label: 'OpenAI GPT-Realtime-2', defaultOption: true },
    ])
    const store = useVoiceModelStore()

    await store.ensureModelsLoaded()

    expect(store.selectedModelId).toBe('OPENAI:gpt-realtime-2')
  })

  it('ignores localStorage failures when persisting selection', async () => {
    vi.mocked(fetchRealtimeVoiceModels).mockResolvedValue([
      { provider: 'OPENAI', id: 'gpt-realtime-2', label: 'OpenAI GPT-Realtime-2', defaultOption: true },
    ])
    const setItem = vi.spyOn(Storage.prototype, 'setItem').mockImplementation((key) => {
      if (key === 'voiceSelectedModel') {
        throw new Error('blocked')
      }
    })
    const store = useVoiceModelStore()
    await store.ensureModelsLoaded()
    store.setSelectedModelId('OPENAI:gpt-realtime-2')
    expect(store.selectedModelId).toBe('OPENAI:gpt-realtime-2')
    setItem.mockRestore()
  })
})
