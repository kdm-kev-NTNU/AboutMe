import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { listChatModels, ChatModelOptionProvider } from '@/api/generated/portfolio'
import { isChatProvider, useChatModelStore } from '../model'

vi.mock('@/api/generated/portfolio', async (importOriginal) => {
  const mod = await importOriginal<typeof import('@/api/generated/portfolio')>()
  return { ...mod, listChatModels: vi.fn() }
})

describe('isChatProvider', () => {
  it('narrows known providers', () => {
    expect(isChatProvider(ChatModelOptionProvider.OPENAI)).toBe(true)
    expect(isChatProvider(ChatModelOptionProvider.ANTHROPIC)).toBe(true)
    expect(isChatProvider('OTHER')).toBe(false)
    expect(isChatProvider(undefined)).toBe(false)
  })
})

describe('useChatModelStore', () => {
  beforeEach(() => {
    sessionStorage.clear()
    setActivePinia(createPinia())
    vi.clearAllMocks()
    vi.mocked(listChatModels).mockReset()
  })

  it('modelsForProvider returns only models with id for that provider', () => {
    const store = useChatModelStore()
    store.$patch({
      models: [
        { id: 'a', provider: ChatModelOptionProvider.OPENAI, label: 'A' },
        { id: '', provider: ChatModelOptionProvider.OPENAI, label: 'Skip' },
        { id: 'b', provider: ChatModelOptionProvider.ANTHROPIC, label: 'B' },
      ],
    })
    expect(store.modelsForProvider(ChatModelOptionProvider.OPENAI)).toHaveLength(1)
    expect(store.modelsForProvider(ChatModelOptionProvider.OPENAI)[0].id).toBe('a')
  })

  it('selectedModel and activeProvider reflect selection', () => {
    const store = useChatModelStore()
    store.$patch({
      models: [
        { id: 'x', provider: ChatModelOptionProvider.ANTHROPIC, label: 'X' },
      ],
      selectedModelId: 'x',
    })
    expect(store.selectedModel?.id).toBe('x')
    expect(store.activeProvider).toBe(ChatModelOptionProvider.ANTHROPIC)
  })

  it('activeProvider is null when selected model has no provider', () => {
    const store = useChatModelStore()
    store.$patch({
      models: [{ id: 'z', label: 'Z' }],
      selectedModelId: 'z',
    })
    expect(store.activeProvider).toBeNull()
  })

  it('hasOpenAI and hasAnthropic', () => {
    const store = useChatModelStore()
    expect(store.hasOpenAI).toBe(false)
    expect(store.hasAnthropic).toBe(false)
    store.$patch({
      models: [
        { id: '1', provider: ChatModelOptionProvider.OPENAI, label: '1' },
        { id: '2', provider: ChatModelOptionProvider.ANTHROPIC, label: '2' },
      ],
    })
    expect(store.hasOpenAI).toBe(true)
    expect(store.hasAnthropic).toBe(true)
  })

  it('setSelectedModelId persists to sessionStorage', () => {
    const store = useChatModelStore()
    store.$patch({
      models: [{ id: 'm1', provider: ChatModelOptionProvider.OPENAI, label: 'M' }],
    })
    store.setSelectedModelId('m1')
    expect(sessionStorage.getItem('chatSelectedModel')).toBe('m1')
  })

  it('persistModelId does nothing when id empty', () => {
    const store = useChatModelStore()
    store.persistModelId()
    expect(sessionStorage.getItem('chatSelectedModel')).toBeNull()
  })

  it('applyInitialSelection uses stored id when valid', () => {
    sessionStorage.setItem('chatSelectedModel', 'stored')
    const store = useChatModelStore()
    store.$patch({
      models: [
        { id: 'other', provider: ChatModelOptionProvider.OPENAI, label: 'O' },
        { id: 'stored', provider: ChatModelOptionProvider.OPENAI, label: 'S' },
      ],
    })
    store.applyInitialSelection()
    expect(store.selectedModelId).toBe('stored')
  })

  it('applyInitialSelection ignores invalid stored id', () => {
    sessionStorage.setItem('chatSelectedModel', 'missing')
    const store = useChatModelStore()
    store.$patch({
      models: [{ id: 'first', provider: ChatModelOptionProvider.OPENAI, label: 'F' }],
    })
    store.applyInitialSelection()
    expect(store.selectedModelId).toBe('first')
    expect(sessionStorage.getItem('chatSelectedModel')).toBe('first')
  })

  it('applyInitialSelection picks first when no storage', () => {
    const store = useChatModelStore()
    store.$patch({
      models: [{ id: 'only', provider: ChatModelOptionProvider.OPENAI, label: 'O' }],
    })
    store.applyInitialSelection()
    expect(store.selectedModelId).toBe('only')
  })

  it('selectFirstForProvider sets first model id for provider', () => {
    const store = useChatModelStore()
    store.$patch({
      models: [
        { id: 'o2', provider: ChatModelOptionProvider.OPENAI, label: '2' },
        { id: 'o1', provider: ChatModelOptionProvider.OPENAI, label: '1' },
      ],
    })
    store.selectFirstForProvider(ChatModelOptionProvider.OPENAI)
    expect(store.selectedModelId).toBe('o2')
  })

  it('selectFirstForProvider does nothing when list empty', () => {
    const store = useChatModelStore()
    store.$patch({ selectedModelId: 'keep', models: [] })
    store.selectFirstForProvider(ChatModelOptionProvider.OPENAI)
    expect(store.selectedModelId).toBe('keep')
  })

  it('ensureModelsLoaded applies selection when models already present', async () => {
    const store = useChatModelStore()
    store.$patch({
      models: [{ id: 'a', provider: ChatModelOptionProvider.OPENAI, label: 'A' }],
      selectedModelId: '',
    })
    await store.ensureModelsLoaded()
    expect(store.selectedModelId).toBe('a')
    expect(listChatModels).not.toHaveBeenCalled()
  })

  it('ensureModelsLoaded coalesces parallel in-flight fetches', async () => {
    let resolveFn!: (v: Awaited<ReturnType<typeof listChatModels>>) => void
    const p = new Promise<Awaited<ReturnType<typeof listChatModels>>>((resolve) => {
      resolveFn = resolve
    })
    vi.mocked(listChatModels).mockReturnValue(p)
    const store = useChatModelStore()
    const a = store.ensureModelsLoaded()
    const b = store.ensureModelsLoaded()
    expect(listChatModels).toHaveBeenCalledTimes(1)
    resolveFn!({
      status: 200,
      data: [{ id: 'x', provider: ChatModelOptionProvider.OPENAI, label: 'X' }],
      headers: new Headers(),
    })
    await Promise.all([a, b])
    expect(store.models).toHaveLength(1)
    expect(store.selectedModelId).toBe('x')
  })

  it('ensureModelsLoaded skips body when status not 200', async () => {
    vi.mocked(listChatModels).mockResolvedValue({
      status: 500,
      data: [{ id: 'n', provider: ChatModelOptionProvider.OPENAI, label: 'N' }],
      headers: new Headers(),
    } as unknown as Awaited<ReturnType<typeof listChatModels>>)
    const store = useChatModelStore()
    await store.ensureModelsLoaded()
    expect(store.models).toHaveLength(0)
  })

  it('ensureModelsLoaded filters models without id', async () => {
    vi.mocked(listChatModels).mockResolvedValue({
      status: 200,
      data: [
        { id: 'ok', provider: ChatModelOptionProvider.OPENAI, label: 'OK' },
        { id: '', provider: ChatModelOptionProvider.OPENAI, label: 'Bad' },
      ],
      headers: new Headers(),
    })
    const store = useChatModelStore()
    await store.ensureModelsLoaded()
    expect(store.models).toHaveLength(1)
    expect(store.models[0].id).toBe('ok')
  })

  it('ensureModelsLoaded swallows listChatModels rejection', async () => {
    vi.mocked(listChatModels).mockRejectedValue(new Error('network'))
    const store = useChatModelStore()
    await expect(store.ensureModelsLoaded()).resolves.toBeUndefined()
    expect(store.models).toHaveLength(0)
  })
})
