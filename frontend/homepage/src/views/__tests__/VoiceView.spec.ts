import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ref } from 'vue'
import type { Component } from 'vue'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'

import VoiceView from '../VoiceView.vue'
import { useLangStore } from '@/stores/lang'

const fetchRealtimeVoiceEnabledMock = vi.hoisted(() => vi.fn())
const fetchRealtimeVoiceModelsMock = vi.hoisted(() => vi.fn())
const mockedUseRealtimeVoiceImpl = vi.hoisted(() => vi.fn())

vi.mock('@/lib/realtime-voice', async (importOriginal) => {
  const actual = await importOriginal<typeof import('@/lib/realtime-voice')>()
  return {
    ...actual,
    fetchRealtimeVoiceStatus: (...args: unknown[]) => fetchRealtimeVoiceEnabledMock(...args),
    fetchRealtimeVoiceModels: (...args: unknown[]) => fetchRealtimeVoiceModelsMock(...args),
  }
})

vi.mock('@/composables/useRealtimeVoice', () => ({
  useRealtimeVoice: (...args: unknown[]) => mockedUseRealtimeVoiceImpl(...args),
}))

vi.mock('@/lib/analytics', () => ({
  captureProductAnalyticsEvent: vi.fn(),
  captureClientException: vi.fn(),
}))

describe('VoiceView.vue', () => {
  async function factory(opts: {
    lang: 'en' | 'no'
    fetchResult: boolean | 'pending'
  }) {
    vi.clearAllMocks()

    fetchRealtimeVoiceEnabledMock.mockReset()
    fetchRealtimeVoiceModelsMock.mockReset()
    fetchRealtimeVoiceModelsMock.mockResolvedValue([
      { provider: 'OPENAI', id: 'gpt-realtime-2', label: 'OpenAI GPT-Realtime-2', defaultOption: true },
      { provider: 'ELEVENLABS', id: 'agent_1', label: 'ElevenLabs Agent', defaultOption: false },
    ])

    let resolveAvail: ((v: boolean) => void) | undefined

    if (opts.fetchResult === 'pending') {
      fetchRealtimeVoiceEnabledMock.mockImplementation(
        () =>
          new Promise<{
            enabled: boolean
            voices: ['marin', 'cedar']
            reasoningEfforts: ['low', 'medium', 'high']
            voice: 'cedar'
            reasoningEffort: 'medium'
          }>((resolve) => {
            resolveAvail = (v: boolean) =>
              resolve({
                enabled: v,
                voices: ['marin', 'cedar'],
                reasoningEfforts: ['low', 'medium', 'high'],
                voice: 'cedar',
                reasoningEffort: 'medium',
              })
          }),
      )
    } else {
      fetchRealtimeVoiceEnabledMock.mockResolvedValue({
        enabled: opts.fetchResult,
        voices: ['marin', 'cedar'],
        reasoningEfforts: ['low', 'medium', 'high'],
        voice: 'cedar',
        reasoningEffort: 'medium',
      })
    }

    const stubConnect = vi.fn()
    const stubDisconnect = vi.fn()

    const connectionState =
      opts.fetchResult === 'pending' ? ref<'idle' | 'connecting' | 'connected'>('idle') : ref<'idle'>('idle')
    const errorMessage = ref('')
    const sessionNotice = ref('')
    const assistantTranscript = ref('')
    const userTranscript = ref('')

    mockedUseRealtimeVoiceImpl.mockImplementation(() => ({
      connectionState,
      errorMessage,
      sessionNotice,
      assistantTranscript,
      userTranscript,
      connect: stubConnect,
      disconnect: stubDisconnect,
      maxSessionMs: ref(180_000),
    }))

    const HomeStub = { template: '<div />' }

    const pinia = createPinia()
    setActivePinia(pinia)

    useLangStore().setLanguage(opts.lang)

    const router = createRouter({
      history: createMemoryHistory(),
      routes: [
        { path: '/', component: HomeStub },
        {
          path: '/voice',
          component: VoiceView as unknown as Component,
        },
      ],
    })

    await router.push('/voice')
    await router.isReady()

    const stubs = {
      RouterLink: { props: ['to'], template: '<a :href="to"><slot /></a>' },
      Button: {
        template: `
          <button type="button" @click="$emit('click')" class="__btn-portfolio">
            <slot />
          </button>
        `,
      },
      Alert: { template: '<div class="__alert-portfolio"><slot /></div>' },
      AlertTitle: { template: '<div><slot /></div>' },
      AlertDescription: { template: '<div><slot /></div>' },
      Info: true,
      Mic: true,
      MicOff: true,
      Loader2: true,
      MessageSquare: true,
      AiStatusDialog: {
        props: ['open', 'title', 'message'],
        template: '<div v-if="open" role="dialog"><h2>{{ title }}</h2><p>{{ message }}</p><button @click="$emit(\'update:open\', false)">OK</button></div>',
      },
    }

    const wrapper = mount(VoiceView, {
      attachTo: document.body,
      global: {
        plugins: [pinia, router],
        stubs,
      },
    })

    return {
      wrapper,
      resolveAvail,
      stubConnect,
      stubDisconnect,
      refs: {
        connectionState,
        errorMessage,
        sessionNotice,
        assistantTranscript,
        userTranscript,
      },
    }
  }

  beforeEach(async () => {
    document.body.innerHTML = ''
    sessionStorage.clear()
    vi.clearAllMocks()
  })

  it('shows a spinner overlay while realtime availability resolves', async () => {
    const { wrapper } = await factory({
      lang: 'en',
      fetchResult: 'pending',
    })

    expect(fetchRealtimeVoiceEnabledMock).toHaveBeenCalledTimes(1)
    await flushPromises()
    await wrapper.vm.$nextTick()

    await flushPromises()
    wrapper.unmount()
  })

  it('shows the unavailable banner when realtime is disabled on the backend', async () => {
    const { wrapper } = await factory({
      lang: 'en',
      fetchResult: false,
    })

    await flushPromises()

    expect(document.body.textContent).toContain('Voice chat is not enabled on the server right now.')

    wrapper.unmount()
  })

  it('shows localized unavailable copy when the UI language is Norwegian', async () => {
    const { wrapper } = await factory({
      lang: 'no',
      fetchResult: false,
    })

    await flushPromises()

    expect(document.body.textContent).toContain('Stemmechat er ikke slått på hos serveren akkurat nå.')

    wrapper.unmount()
  })

  it('shows the Norwegian hero title when realtime is enabled and language is Norwegian', async () => {
    const { wrapper } = await factory({
      lang: 'no',
      fetchResult: true,
    })

    await flushPromises()

    expect(document.body.textContent).toContain('Snakk med Kevin sin AI')
    expect(document.body.textContent).toContain('Sanntidsstemme med Kevin sin AI')

    wrapper.unmount()
  })

  it('shows the English hero subtitle mentioning ElevenLabs and Kevin AI', async () => {
    const { wrapper } = await factory({
      lang: 'en',
      fetchResult: true,
    })

    await flushPromises()

    expect(document.body.textContent).toContain("Real-time voice with Kevin's AI")
    expect(document.body.textContent).toContain('ElevenLabs')

    wrapper.unmount()
  })

  it('renders voice controls with backend defaults and localized labels', async () => {
    const { wrapper } = await factory({
      lang: 'en',
      fetchResult: true,
    })

    await flushPromises()

    const voiceSelect = wrapper.get('[data-testid="voice-select"]').element as HTMLSelectElement
    const reasoningSelect = wrapper.get('[data-testid="reasoning-select"]').element as HTMLSelectElement

    expect(document.body.textContent).toContain('Voice')
    expect(document.body.textContent).toContain('Reasoning')
    expect(document.body.textContent).toContain('Marin')
    expect(document.body.textContent).toContain('Cedar')
    expect(document.body.textContent).toContain('Fast')
    expect(document.body.textContent).toContain('Balanced')
    expect(document.body.textContent).toContain('Thorough')
    expect(voiceSelect.value).toBe('cedar')
    expect(reasoningSelect.value).toBe('medium')

    wrapper.unmount()
  })

  it('renders and persists the visitor voice model selection', async () => {
    const { wrapper } = await factory({
      lang: 'en',
      fetchResult: true,
    })

    await flushPromises()

    const select = wrapper.get('[data-testid="voice-model-select"]').element as HTMLSelectElement
    expect(document.body.textContent).toContain('Provider/model')
    expect(document.body.textContent).toContain('OpenAI GPT-Realtime-2')
    expect(document.body.textContent).toContain('ElevenLabs Agent')
    expect(select.value).toBe('gpt-realtime-2')

    select.value = 'agent_1'
    await wrapper.get('[data-testid="voice-model-select"]').trigger('change')

    expect(sessionStorage.getItem('voiceSelectedModel')).toBe('agent_1')

    wrapper.unmount()
  })

  it('disables voice controls while realtime is connected', async () => {
    const { wrapper, refs } = await factory({
      lang: 'en',
      fetchResult: true,
    })

    await flushPromises()

    refs.connectionState.value = 'connected'
    await wrapper.vm.$nextTick()

    expect((wrapper.get('[data-testid="voice-select"]').element as HTMLSelectElement).disabled).toBe(true)
    expect((wrapper.get('[data-testid="reasoning-select"]').element as HTMLSelectElement).disabled).toBe(true)

    wrapper.unmount()
  })

  it('surfaces realtime errors through the AI status dialog', async () => {
    const { wrapper, refs } = await factory({
      lang: 'en',
      fetchResult: true,
    })

    await flushPromises()

    refs.errorMessage.value = 'Microphone unavailable'
    await wrapper.vm.$nextTick()

    expect(document.body.querySelector('[role="dialog"]')).toBeTruthy()
    expect(document.body.textContent).toContain('Voice could not start')
    expect(document.body.textContent).toContain('Microphone unavailable')

    wrapper.unmount()
  })

  it('shows a session notice banner when realtime composables publish one', async () => {
    const { wrapper, refs } = await factory({
      lang: 'en',
      fetchResult: true,
    })

    await flushPromises()

    refs.sessionNotice.value = 'Time limit placeholder'
    await wrapper.vm.$nextTick()

    expect(document.body.textContent).toContain('Time limit placeholder')

    wrapper.unmount()
  })

  it('renders bilingual transcript labels alongside stored transcript text', async () => {
    const { wrapper, refs } = await factory({
      lang: 'en',
      fetchResult: true,
    })

    await flushPromises()

    refs.connectionState.value = 'connected'
    refs.userTranscript.value = 'speaker text'
    refs.assistantTranscript.value = 'model text'
    await wrapper.vm.$nextTick()

    const bodyText = document.body.textContent ?? ''
    expect(bodyText).toContain('You (transcript)')
    expect(bodyText).toContain('speaker text')
    expect(bodyText).toContain('Assistant (transcript)')
    expect(bodyText).toContain('model text')

    wrapper.unmount()
  })

  it('uses Norwegian transcript labels while language is Norwegian', async () => {
    const { wrapper, refs } = await factory({
      lang: 'no',
      fetchResult: true,
    })

    await flushPromises()

    refs.connectionState.value = 'connected'
    refs.userTranscript.value = 'bruker'
    refs.assistantTranscript.value = 'assistent'

    await wrapper.vm.$nextTick()

    expect(document.body.textContent).toContain('Du (transkripsjon)')
    expect(document.body.textContent).toContain('Assistent (transkripsjon)')

    wrapper.unmount()
  })

  it('wires the connect control to mocked composables', async () => {
    const { wrapper, stubConnect } = await factory({
      lang: 'en',
      fetchResult: true,
    })

    await flushPromises()

    const idleConnect = [...document.body.querySelectorAll('button')]
      .map((btn) => btn.textContent?.trim())
      .find((txt) => txt?.includes('Start voice'))

    expect(idleConnect).toBeTruthy()

    const button = [...document.body.querySelectorAll('button')]
      .find((el) => el.textContent?.includes('Start voice'))
    ;(button as HTMLButtonElement).click()

    expect(stubConnect).toHaveBeenCalled()

    wrapper.unmount()
  })

  it('shows disconnected UI when realtime session is actively connected', async () => {
    const { wrapper, refs, stubDisconnect } = await factory({
      lang: 'en',
      fetchResult: true,
    })

    await flushPromises()

    refs.connectionState.value = 'connected'
    await wrapper.vm.$nextTick()

    const button = [...document.body.querySelectorAll('button')]
      .find((el) => el.textContent?.includes('End session'))

    expect(button).toBeTruthy()

    ;(button as HTMLButtonElement).click()
    expect(stubDisconnect).toHaveBeenCalled()

    wrapper.unmount()
  })
})
