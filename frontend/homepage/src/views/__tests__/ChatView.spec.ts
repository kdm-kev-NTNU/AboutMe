import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'
import ChatView from '../ChatView.vue'
import {
  askQuestion,
  listChatModels,
  ChatModelOptionProvider,
  ModelTag,
  type askQuestionResponse,
} from '@/api/generated/portfolio'
import { useLangStore } from '@/stores/lang'
import { useChatModelStore } from '@/stores/model'
import { transcribeSpeech } from '@/lib/transcribe-audio'

vi.mock('@/api/generated/portfolio', async (importOriginal) => {
  const mod = await importOriginal<typeof import('@/api/generated/portfolio')>()
  return {
    ...mod,
    askQuestion: vi.fn(),
    listChatModels: vi.fn(),
  }
})

vi.mock('@/lib/transcribe-audio', () => ({
  transcribeSpeech: vi.fn(),
}))

describe('ChatView', () => {
  const HomeStub = { template: '<div>home-stub</div>' }

  /** Matches VoiceView.spec AiStatusDialog stub; keeps ChatView tests stable alongside Dialog stubs. */
  const chatViewTestStubs = {
    AiStatusDialog: {
      props: [
        'open',
        'title',
        'description',
        'message',
        'retryLabel',
        'showRetry',
        'dismissLabel',
      ],
      template: `
        <div v-if="open" role="dialog" data-testid="ai-status-dialog">
          <h2>{{ title }}</h2>
          <p data-testid="ai-status-message">{{ message }}</p>
          <button type="button" data-testid="ai-dismiss" @click="$emit('update:open', false)">{{ dismissLabel || 'OK' }}</button>
          <button v-if="showRetry" type="button" data-testid="ai-retry" @click="$emit('retry')">{{ retryLabel }}</button>
        </div>
      `,
    },
    Dialog: {
      props: ['open'],
      template: '<div v-if="open"><slot /></div>',
    },
    DialogContent: { template: '<div><slot /></div>' },
    DialogHeader: { template: '<div><slot /></div>' },
    DialogTitle: { template: '<h2><slot /></h2>' },
    DialogDescription: { template: '<p><slot /></p>' },
    DialogFooter: { template: '<div><slot /></div>' },
    MessagesArea: {
      props: ['messages', 'isLoading', 'isReadOnly'],
      template: '<div class="stub-messages">{{ messages.map(m => m.text).join(",") }}</div>',
    },
  }

  function makeRouter() {
    return createRouter({
      history: createMemoryHistory(),
      routes: [
        { path: '/', name: 'home', component: HomeStub },
        { path: '/project', name: 'project', component: HomeStub },
        { path: '/chat', name: 'chat', component: ChatView },
      ],
    })
  }

  beforeEach(() => {
    sessionStorage.clear()
    localStorage.clear()
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
        stubs: chatViewTestStubs,
      },
    })
    await flushPromises()
    return { wrapper, router, pinia }
  }

  it('shows model FAST label and defaults to OpenAI FAST when anonymous', async () => {
    vi.mocked(listChatModels).mockResolvedValue({
      status: 200,
      data: [
        {
          id: 'gpt-5.4-mini',
          provider: ChatModelOptionProvider.OPENAI,
          label: 'GPT-5.4 mini',
          tags: [ModelTag.FAST],
        },
        {
          id: 'claude-haiku-4-5-20251001',
          provider: ChatModelOptionProvider.ANTHROPIC,
          label: 'Claude Haiku 4.5',
          tags: [ModelTag.FAST],
        },
      ],
      headers: new Headers(),
    })
    const { wrapper } = await mountChat({})
    await flushPromises()
    const sel = wrapper.find('#chat-model-select')
    expect(sel.exists()).toBe(true)
    const opts = sel.findAll('option')
    expect(opts.some((o) => o.text().includes('Fast'))).toBe(true)
    expect((sel.element as HTMLSelectElement).value).toBe('gpt-5.4-mini')
  })

  it('defaults to first catalog model when signed in and no REASONING options exist', async () => {
    sessionStorage.setItem(
      'auth',
      JSON.stringify({ username: 'u', role: 'USER', basicToken: 'dGVzdA==' }),
    )
    vi.mocked(listChatModels).mockResolvedValue({
      status: 200,
      data: [
        {
          id: 'gpt-5.4-mini',
          provider: ChatModelOptionProvider.OPENAI,
          label: 'GPT-5.4 mini',
          tags: [ModelTag.FAST],
        },
        {
          id: 'claude-haiku-4-5-20251001',
          provider: ChatModelOptionProvider.ANTHROPIC,
          label: 'Claude Haiku 4.5',
          tags: [ModelTag.FAST],
        },
      ],
      headers: new Headers(),
    })
    const { wrapper } = await mountChat({})
    await flushPromises()
    const sel = wrapper.find('#chat-model-select')
    expect((sel.element as HTMLSelectElement).value).toBe('gpt-5.4-mini')
  })

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

  it('maps 403 to server error field when present', async () => {
    vi.mocked(askQuestion).mockResolvedValueOnce({
      status: 403,
      data: { error: 'This model requires sign-in. Use a public model or authenticate.' },
      headers: new Headers(),
    } as unknown as askQuestionResponse)
    const { wrapper } = await mountChat({ q: 'hi' })
    await flushPromises()
    expect(wrapper.text()).toContain('This model requires sign-in')
  })

  it('maps network failure to Norwegian network message', async () => {
    vi.mocked(askQuestion).mockRejectedValueOnce(new Error('offline'))
    const { wrapper } = await mountChat({ q: 'hi' })
    await flushPromises()
    expect(wrapper.text()).toContain('Nettverksfeil')
  })

  it('AI status retry submits again when the composer still has text after a failed ask', async () => {
    vi.mocked(askQuestion)
      .mockRejectedValueOnce(new Error('offline'))
      .mockResolvedValueOnce({
        status: 200,
        data: { answer: 'Recovered' },
        headers: new Headers(),
      })

    const { wrapper } = await mountChat({})
    const input = wrapper.find('input[type="text"]')
    await input.setValue('retry-query')
    await wrapper.find('form').trigger('submit.prevent')
    await flushPromises()

    expect(wrapper.text()).toContain('Nettverksfeil')
    expect(vi.mocked(askQuestion)).toHaveBeenCalledTimes(1)

    await input.setValue('retry-query')
    await wrapper.vm.$nextTick()

    const retryBtn = wrapper.find('[data-testid="ai-retry"]')
    expect(retryBtn.exists()).toBe(true)
    await retryBtn.trigger('click')
    await flushPromises()

    expect(vi.mocked(askQuestion)).toHaveBeenCalledTimes(2)
    expect(vi.mocked(askQuestion).mock.calls[1][0]).toEqual({ question: 'retry-query' })
    expect(wrapper.text()).toContain('Recovered')
  })

  it('clearChat clears transcript and stays on chat', async () => {
    sessionStorage.setItem('chatMessages', JSON.stringify([{ role: 'user', text: 'x' }]))
    const { wrapper, router } = await mountChat({})
    const replaceSpy = vi.spyOn(router, 'replace')
    const pushSpy = vi.spyOn(router, 'push')

    const clearBtn = wrapper
      .findAll('button')
      .find((b) => /clear chat/i.test(b.text()))
    expect(clearBtn).toBeDefined()
    await clearBtn!.trigger('click')
    await flushPromises()

    expect(JSON.parse(sessionStorage.getItem('chatMessages') || '[]')).toEqual([])
    expect(wrapper.find('.stub-messages').text()).toBe('')
    expect(replaceSpy).toHaveBeenCalledWith(expect.objectContaining({ name: 'chat', query: {} }))
    expect(pushSpy).not.toHaveBeenCalledWith({ name: 'home' })
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

  it('shows updated first-time popup and saves versioned dismissal key', async () => {
    const { wrapper } = await mountChat({})
    expect(wrapper.text()).toContain('This portfolio keeps evolving')

    const dismissBtn = wrapper.findAll('button').find((b) => /got it/i.test(b.text()))
    expect(dismissBtn).toBeDefined()
    await dismissBtn!.trigger('click')

    expect(localStorage.getItem('chatInfoPopupDismissed.v2')).toBe('true')
  })

  it('shows updated popup even when legacy dismissal key exists', async () => {
    localStorage.setItem('chatInfoPopupDismissed', 'true')
    const { wrapper } = await mountChat({})
    expect(wrapper.text()).toContain('This portfolio keeps evolving')
  })

  // --- Language-dependent UI tests ---

  it('shows Norwegian placeholder when language is no', async () => {
    const pinia = createPinia()
    setActivePinia(pinia)
    useLangStore().setLanguage('no')
    useChatModelStore().$reset()

    const router = makeRouter()
    await router.push({ path: '/chat' })
    await router.isReady()

    const wrapper = mount(ChatView, {
      global: {
        plugins: [pinia, router],
        stubs: chatViewTestStubs,
      },
    })
    await flushPromises()

    const input = wrapper.find('input[type="text"]')
    expect(input.exists()).toBe(true)
    expect(input.attributes('placeholder')).toContain('Spør')
  })

  it('shows English placeholder when language is en', async () => {
    const { wrapper } = await mountChat({})
    const input = wrapper.find('input[type="text"]')
    expect(input.exists()).toBe(true)
    expect(input.attributes('placeholder')).toContain('Ask')
  })

  it('shows Norwegian popup text when language is no', async () => {
    const pinia = createPinia()
    setActivePinia(pinia)
    useLangStore().setLanguage('no')
    useChatModelStore().$reset()

    const router = makeRouter()
    await router.push({ path: '/chat' })
    await router.isReady()

    const wrapper = mount(ChatView, {
      global: {
        plugins: [pinia, router],
        stubs: chatViewTestStubs,
      },
    })
    await flushPromises()

    expect(wrapper.text()).toContain('Porteføljen oppdateres fortløpende')
  })

  it('shows English popup text when language is en', async () => {
    const { wrapper } = await mountChat({})
    expect(wrapper.text()).toContain('This portfolio keeps evolving')
  })

  it('shows Norwegian dismiss button when language is no', async () => {
    const pinia = createPinia()
    setActivePinia(pinia)
    useLangStore().setLanguage('no')
    useChatModelStore().$reset()

    const router = makeRouter()
    await router.push({ path: '/chat' })
    await router.isReady()

    const wrapper = mount(ChatView, {
      global: {
        plugins: [pinia, router],
        stubs: chatViewTestStubs,
      },
    })
    await flushPromises()

    const dismissBtn = wrapper.findAll('button').find((b) => /forstått/i.test(b.text()))
    expect(dismissBtn).toBeDefined()
  })

  it('shows Norwegian error when prompt is too long and language is no', async () => {
    const pinia = createPinia()
    setActivePinia(pinia)
    useLangStore().setLanguage('no')
    useChatModelStore().$reset()

    const router = makeRouter()
    await router.push({ path: '/chat' })
    await router.isReady()

    const wrapper = mount(ChatView, {
      global: {
        plugins: [pinia, router],
        stubs: chatViewTestStubs,
      },
    })
    await flushPromises()

    const input = wrapper.find('input[type="text"]')
    expect(input.exists()).toBe(true)
    await input.setValue('x'.repeat(3001))
    await wrapper.find('form').trigger('submit.prevent')
    await flushPromises()

    expect(wrapper.text()).toMatch(/for lang|3000/i)
  })

  it('shows Norwegian model label when language is no', async () => {
    vi.mocked(listChatModels).mockResolvedValue({
      status: 200,
      data: [
        {
          id: 'gpt-5.4-mini',
          provider: ChatModelOptionProvider.OPENAI,
          label: 'GPT-5.4 mini',
          tags: [ModelTag.FAST],
        },
      ],
      headers: new Headers(),
    })

    const pinia = createPinia()
    setActivePinia(pinia)
    useLangStore().setLanguage('no')
    useChatModelStore().$reset()

    const router = makeRouter()
    await router.push({ path: '/chat' })
    await router.isReady()

    const wrapper = mount(ChatView, {
      global: {
        plugins: [pinia, router],
        stubs: chatViewTestStubs,
      },
    })
    await flushPromises()

    expect(wrapper.text()).toContain('Modell')
  })

  it('shows English model label when language is en', async () => {
    vi.mocked(listChatModels).mockResolvedValue({
      status: 200,
      data: [
        {
          id: 'gpt-5.4-mini',
          provider: ChatModelOptionProvider.OPENAI,
          label: 'GPT-5.4 mini',
          tags: [ModelTag.FAST],
        },
      ],
      headers: new Headers(),
    })

    const { wrapper } = await mountChat({})
    await flushPromises()

    expect(wrapper.text()).toContain('Model')
  })

  it('shows Norwegian tag label (Rask) when language is no', async () => {
    vi.mocked(listChatModels).mockResolvedValue({
      status: 200,
      data: [
        {
          id: 'gpt-5.4-mini',
          provider: ChatModelOptionProvider.OPENAI,
          label: 'GPT-5.4 mini',
          tags: [ModelTag.FAST],
        },
      ],
      headers: new Headers(),
    })

    const pinia = createPinia()
    setActivePinia(pinia)
    useLangStore().setLanguage('no')
    useChatModelStore().$reset()

    const router = makeRouter()
    await router.push({ path: '/chat' })
    await router.isReady()

    const wrapper = mount(ChatView, {
      global: {
        plugins: [pinia, router],
        stubs: chatViewTestStubs,
      },
    })
    await flushPromises()

    const opts = wrapper.findAll('option')
    expect(opts.some((o) => o.text().includes('Rask'))).toBe(true)
  })

  describe('voice input control', () => {
    const origMediaDevices = globalThis.navigator.mediaDevices

    function stubMediaRecorder() {
      class StubRecorder {
        static isTypeSupported = () => true
        state = 'inactive'
        ondataavailable: ((ev: { data: Blob }) => void) | null = null
        private listeners: Record<string, Array<(ev?: Event) => void>> = {}
        start() {
          this.state = 'recording'
        }
        stop() {
          this.state = 'inactive'
          for (const cb of this.listeners['stop'] ?? []) cb()
        }
        addEventListener(type: string, cb: (ev?: Event) => void) {
          if (!this.listeners[type]) this.listeners[type] = []
          this.listeners[type].push(cb)
        }
        requestData = () => {
          this.ondataavailable?.({ data: new Blob([new Uint8Array([1])]) } as BlobEvent)
        }
      }
      vi.stubGlobal('MediaRecorder', StubRecorder as unknown as typeof MediaRecorder)
    }

    function stubWebAudioAndResize() {
      class FakeAnalyser {
        fftSize = 2048
        smoothingTimeConstant = 0
        connect() {
          return this
        }
        disconnect() {}
        getFloatTimeDomainData(arr: Float32Array) {
          for (let i = 0; i < arr.length; i++) arr[i] = 0
        }
      }
      class FakeMediaStreamAudioSourceNode {
        connect() {
          return new FakeAnalyser()
        }
        disconnect() {}
      }
      class FakeAudioContext {
        state: AudioContextState = 'running'
        resume = vi.fn().mockResolvedValue(undefined)
        close = vi.fn().mockResolvedValue(undefined)
        createMediaStreamSource = vi.fn(() => new FakeMediaStreamAudioSourceNode())
        createAnalyser = vi.fn(() => new FakeAnalyser())
      }
      vi.stubGlobal('AudioContext', FakeAudioContext as unknown as typeof AudioContext)
      vi.stubGlobal(
        'ResizeObserver',
        class {
          observe() {}
          unobserve() {}
          disconnect() {}
        },
      )
    }

    afterEach(() => {
      Object.defineProperty(globalThis.navigator, 'mediaDevices', {
        value: origMediaDevices,
        configurable: true,
      })
      vi.unstubAllGlobals()
    })

    it('hides the mic when mediaDevices is unavailable', async () => {
      Object.defineProperty(globalThis.navigator, 'mediaDevices', {
        value: undefined,
        configurable: true,
      })
      const { wrapper } = await mountChat({})
      expect(wrapper.find('[aria-label="Voice input"]').exists()).toBe(false)
    })

    it('shows the mic when MediaRecorder and getUserMedia exist', async () => {
      Object.defineProperty(globalThis.navigator, 'mediaDevices', {
        value: { getUserMedia: vi.fn().mockResolvedValue({ getTracks: () => [{ stop: vi.fn() }] }) },
        configurable: true,
      })
      stubMediaRecorder()

      const { wrapper } = await mountChat({})
      expect(wrapper.find('[aria-label="Voice input"]').exists()).toBe(true)
    })

    it('replaces the text field with a live waveform while recording, then restores after stop', async () => {
      Object.defineProperty(globalThis.navigator, 'mediaDevices', {
        value: { getUserMedia: vi.fn().mockResolvedValue({ getTracks: () => [{ stop: vi.fn() }] }) },
        configurable: true,
      })
      stubMediaRecorder()
      stubWebAudioAndResize()

      vi.mocked(transcribeSpeech).mockResolvedValue({
        status: 200,
        data: { text: 'hello' },
        headers: new Headers(),
      })

      const { wrapper } = await mountChat({})
      expect(wrapper.find('input[type="text"]').exists()).toBe(true)

      await wrapper.find('[aria-label="Voice input"]').trigger('click')
      await flushPromises()

      expect(wrapper.find('input[type="text"]').exists()).toBe(false)
      const wave = wrapper.find('[role="img"]')
      expect(wave.exists()).toBe(true)
      expect(wave.attributes('aria-label')).toBe('Audio level while recording')

      await wrapper.find('[aria-label="Voice input"]').trigger('click')
      await flushPromises()

      expect(wrapper.find('input[type="text"]').exists()).toBe(true)
      expect(wrapper.find('[role="img"]').exists()).toBe(false)
      expect(vi.mocked(transcribeSpeech)).toHaveBeenCalledWith(expect.any(Blob), 'en')
    })
  })
})
