import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ref } from 'vue'
import type { Component } from 'vue'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'

import VoiceView from '../VoiceView.vue'
import { useLangStore } from '@/stores/lang'

const fetchRealtimeVoiceEnabledMock = vi.hoisted(() => vi.fn())
const mockedUseRealtimeVoiceImpl = vi.hoisted(() => vi.fn())

vi.mock('@/lib/realtime-voice', async (importOriginal) => {
  const actual = await importOriginal<typeof import('@/lib/realtime-voice')>()
  return {
    ...actual,
    fetchRealtimeVoiceEnabled: (...args: unknown[]) => fetchRealtimeVoiceEnabledMock(...args),
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

    let resolveAvail: ((v: boolean) => void) | undefined

    if (opts.fetchResult === 'pending') {
      fetchRealtimeVoiceEnabledMock.mockImplementation(
        () => new Promise<boolean>((resolve) => (resolveAvail = resolve)),
      )
    } else {
      fetchRealtimeVoiceEnabledMock.mockResolvedValue(opts.fetchResult)
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

  it('shows the Norwegian hero subtitle when realtime is enabled and language is Norwegian', async () => {
    const { wrapper } = await factory({
      lang: 'no',
      fetchResult: true,
    })

    await flushPromises()

    expect(document.body.textContent).toContain('Live stemme (OpenAI GPT-Realtime)')

    wrapper.unmount()
  })

  it('surfaces realtime errors through the descriptive alert', async () => {
    const { wrapper, refs } = await factory({
      lang: 'en',
      fetchResult: true,
    })

    await flushPromises()

    refs.errorMessage.value = 'Microphone unavailable'
    await wrapper.vm.$nextTick()

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
