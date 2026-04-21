import { beforeEach, describe, expect, it, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'
import ChatView from '../ChatView.vue'
import { askQuestion, listChatModels } from '@/api/generated/portfolio'
import { useLangStore } from '@/stores/lang'
import { useChatModelStore } from '@/stores/model'

vi.mock('@/api/generated/portfolio', async (importOriginal) => {
  const mod = await importOriginal<typeof import('@/api/generated/portfolio')>()
  return {
    ...mod,
    askQuestion: vi.fn(),
    listChatModels: vi.fn(),
  }
})

describe('ChatView', () => {
  const HomeStub = { template: '<div>home-stub</div>' }

  function makeRouter() {
    return createRouter({
      history: createMemoryHistory(),
      routes: [
        { path: '/', name: 'home', component: HomeStub },
        { path: '/bachelor', name: 'bachelor', component: HomeStub },
        { path: '/chat', name: 'chat', component: ChatView },
      ],
    })
  }

  beforeEach(() => {
    sessionStorage.clear()
    vi.clearAllMocks()
    vi.mocked(listChatModels).mockResolvedValue({
      status: 200,
      data: [],
      headers: new Headers(),
    })
    vi.mocked(askQuestion).mockResolvedValue({
      status: 200,
      data: { answer: 'Assistant reply' },
      headers: new Headers(),
    })
  })

  async function mountChat(query: Record<string, string | string[] | undefined> = {}) {
    const pinia = createPinia()
    setActivePinia(pinia)
    useLangStore().setLanguage('en')
    useChatModelStore().$reset()

    const router = makeRouter()
    await router.push({ path: '/chat', query })
    await router.isReady()

    const wrapper = mount(ChatView, {
      global: {
        plugins: [pinia, router],
        stubs: {
          MessagesArea: {
            props: ['messages', 'isLoading', 'isReadOnly'],
            template: '<div class="stub-messages">{{ messages.map(m => m.text).join(",") }}</div>',
          },
        },
      },
    })
    await flushPromises()
    return { wrapper, router, pinia }
  }

  it('shows English error when prompt exceeds max length', async () => {
    const { wrapper } = await mountChat({})
    const input = wrapper.find('input[type="text"]')
    expect(input.exists()).toBe(true)
    await input.setValue('x'.repeat(3001))
    await wrapper.find('form').trigger('submit.prevent')
    await flushPromises()

    expect(wrapper.text()).toMatch(/too long|3000/i)
    expect(vi.mocked(askQuestion)).not.toHaveBeenCalled()
  })

  it('submits ?q= on mount and calls askQuestion', async () => {
    await mountChat({ q: 'hello' })
    await vi.waitFor(() => {
      expect(vi.mocked(askQuestion)).toHaveBeenCalled()
    })
    expect(vi.mocked(askQuestion).mock.calls[0][0]).toEqual({ question: 'hello' })
  })

  it('maps 429 to rate limit message', async () => {
    vi.mocked(askQuestion).mockResolvedValueOnce({
      status: 429,
      data: {} as never,
      headers: new Headers(),
    })
    const { wrapper } = await mountChat({ q: 'hi' })
    await flushPromises()
    expect(wrapper.text()).toContain('For mange forespørsler')
  })

  it('maps 400 to server error field when present', async () => {
    vi.mocked(askQuestion).mockResolvedValueOnce({
      status: 400,
      data: { error: 'Bad input' } as never,
      headers: new Headers(),
    })
    const { wrapper } = await mountChat({ q: 'hi' })
    await flushPromises()
    expect(wrapper.text()).toContain('Bad input')
  })

  it('maps network failure to Norwegian network message', async () => {
    vi.mocked(askQuestion).mockRejectedValueOnce(new Error('offline'))
    const { wrapper } = await mountChat({ q: 'hi' })
    await flushPromises()
    expect(wrapper.text()).toContain('Nettverksfeil')
  })

  it('clearChat removes storage and navigates home', async () => {
    sessionStorage.setItem('chatMessages', JSON.stringify([{ role: 'user', text: 'x' }]))
    const { wrapper, router } = await mountChat({})
    const pushSpy = vi.spyOn(router, 'push')

    const clearBtn = wrapper
      .findAll('button')
      .find((b) => /clear chat/i.test(b.text()))
    expect(clearBtn).toBeDefined()
    await clearBtn!.trigger('click')

    expect(sessionStorage.getItem('chatMessages')).toBeNull()
    expect(pushSpy).toHaveBeenCalledWith({ name: 'home' })
  })

  it('hydrates messages from conversation API when conversationId is set', async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({
        id: 1,
        startedAt: '',
        endedAt: '',
        messages: [{ id: 1, role: 'user', text: 'From API', createdAt: '' }],
      }),
    })
    vi.stubGlobal('fetch', fetchMock)

    const { wrapper } = await mountChat({ conversationId: '42' })
    await flushPromises()
    await vi.waitFor(() => {
      expect(fetchMock).toHaveBeenCalledWith(
        '/api/conversations/42',
        expect.objectContaining({ method: 'GET' }),
      )
    })
    expect(wrapper.find('.stub-messages').text()).toContain('From API')

    vi.unstubAllGlobals()
  })
})
