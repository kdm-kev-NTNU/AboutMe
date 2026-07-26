import { beforeEach, describe, expect, it, vi } from 'vitest'
import type { Component } from 'vue'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'

import VoiceView from '../VoiceView.vue'
import { useLangStore } from '@/stores/lang'

const fetchRealtimeVoiceStatusMock = vi.hoisted(() => vi.fn())
const fetchRealtimeVoiceModelsMock = vi.hoisted(() => vi.fn())

vi.mock('@/lib/realtime-voice', async (importOriginal) => {
  const actual = await importOriginal<typeof import('@/lib/realtime-voice')>()
  return {
    ...actual,
    fetchRealtimeVoiceStatus: (...args: unknown[]) => fetchRealtimeVoiceStatusMock(...args),
    fetchRealtimeVoiceModels: (...args: unknown[]) => fetchRealtimeVoiceModelsMock(...args),
  }
})

describe('VoiceView.vue', () => {
  async function factory(opts: { lang: 'en' | 'no'; liveEnabled: boolean; prepDismissed?: boolean }) {
    vi.clearAllMocks()
    localStorage.clear()
    if (opts.prepDismissed) {
      localStorage.setItem('voicePrepDismissed.v1', 'true')
    }
    fetchRealtimeVoiceStatusMock.mockReset()
    fetchRealtimeVoiceModelsMock.mockReset()
    fetchRealtimeVoiceModelsMock.mockResolvedValue([
      { provider: 'OPENAI', id: 'gpt-realtime-2', label: 'OpenAI GPT-Realtime-2', defaultOption: true },
    ])
    fetchRealtimeVoiceStatusMock.mockResolvedValue({
      enabled: opts.liveEnabled,
      liveEnabled: opts.liveEnabled,
      voices: ['marin', 'cedar'],
      reasoningEfforts: ['low', 'medium', 'high'],
      vadEagernessOptions: ['low', 'medium', 'high', 'auto'],
      voice: 'cedar',
      reasoningEffort: 'medium',
      vadEagerness: 'low',
    })

    const HomeStub = { template: '<div />' }
    const ChatStub = { template: '<div />' }

    const pinia = createPinia()
    setActivePinia(pinia)

    useLangStore().setLanguage(opts.lang)

    const router = createRouter({
      history: createMemoryHistory(),
      routes: [
        { path: '/', component: HomeStub },
        { path: '/chat', name: 'chat', component: ChatStub },
        {
          path: '/voice',
          component: VoiceView as unknown as Component,
        },
      ],
    })

    await router.push('/voice')
    await router.isReady()

    const stubs = {
      RouterLink: { props: ['to'], template: '<a :href="typeof to === \'string\' ? to : to"><slot /></a>' },
      Button: {
        props: ['asChild'],
        template: `
          <button type="button" @click="$emit('click')" class="__btn-portfolio">
            <slot />
          </button>
        `,
      },
      Dialog: {
        props: ['open'],
        emits: ['update:open'],
        template: '<div v-if="open" data-testid="voice-prep-dialog"><slot /></div>',
      },
      DialogContent: { template: '<div><slot /></div>' },
      DialogHeader: { template: '<div><slot /></div>' },
      DialogTitle: { template: '<div><slot /></div>' },
      DialogDescription: { template: '<div><slot /></div>' },
      DialogFooter: { template: '<div><slot /></div>' },
      Alert: { template: '<div class="__alert-portfolio"><slot /></div>' },
      AlertTitle: { template: '<div><slot /></div>' },
      AlertDescription: { template: '<div><slot /></div>' },
      MessageSquare: true,
      Headphones: true,
      RealtimeVoicePanel: { template: '<div data-testid="live-panel">live panel</div>' },
    }

    const wrapper = mount(VoiceView, {
      attachTo: document.body,
      global: {
        plugins: [pinia, router],
        stubs,
      },
    })

    return { wrapper }
  }

  beforeEach(async () => {
    document.body.innerHTML = ''
    localStorage.clear()
    vi.clearAllMocks()
  })

  it('renders the live voice panel', async () => {
    const { wrapper } = await factory({ lang: 'en', liveEnabled: true, prepDismissed: true })

    await flushPromises()
    expect(fetchRealtimeVoiceStatusMock).toHaveBeenCalledTimes(1)
    expect(wrapper.find('[data-testid="live-panel"]').exists()).toBe(true)
    wrapper.unmount()
  })

  it('renders Norwegian copy and strengthened chat alternative', async () => {
    const { wrapper } = await factory({ lang: 'no', liveEnabled: false, prepDismissed: true })

    await flushPromises()
    expect(wrapper.text()).toContain('Snakk med Kevin sin AI')
    expect(wrapper.text()).toContain('Foretrekker du å skrive? Bruk tekstchat')
    expect(wrapper.find('[data-testid="voice-chat-alternative"]').exists()).toBe(true)
    wrapper.unmount()
  })

  it('shows prep dialog on first visit and persists dismiss', async () => {
    const { wrapper } = await factory({ lang: 'en', liveEnabled: true })

    await flushPromises()
    expect(wrapper.find('[data-testid="voice-prep-dialog"]').exists()).toBe(true)
    expect(wrapper.text()).toContain('Before you start')
    expect(wrapper.text()).toContain('headset')

    await wrapper.find('[data-testid="voice-prep-dismiss"]').trigger('click')
    await flushPromises()

    expect(localStorage.getItem('voicePrepDismissed.v1')).toBe('true')
    expect(wrapper.find('[data-testid="voice-prep-dialog"]').exists()).toBe(false)
    wrapper.unmount()
  })

  it('does not show prep dialog when already dismissed', async () => {
    const { wrapper } = await factory({ lang: 'en', liveEnabled: true, prepDismissed: true })

    await flushPromises()
    expect(wrapper.find('[data-testid="voice-prep-dialog"]').exists()).toBe(false)
    wrapper.unmount()
  })
})
