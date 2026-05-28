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
  async function factory(opts: { lang: 'en' | 'no'; liveEnabled: boolean }) {
    vi.clearAllMocks()
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
      voice: 'cedar',
      reasoningEffort: 'medium',
    })

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
      MessageSquare: true,
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
    vi.clearAllMocks()
  })

  it('renders the live voice panel', async () => {
    const { wrapper } = await factory({ lang: 'en', liveEnabled: true })

    await flushPromises()
    expect(fetchRealtimeVoiceStatusMock).toHaveBeenCalledTimes(1)
    expect(wrapper.find('[data-testid="live-panel"]').exists()).toBe(true)
    wrapper.unmount()
  })

  it('renders Norwegian copy', async () => {
    const { wrapper } = await factory({ lang: 'no', liveEnabled: false })

    await flushPromises()
    expect(wrapper.text()).toContain('Snakk med Kevin sin AI')
    expect(wrapper.text()).toContain('Bruk tekstchat')
    wrapper.unmount()
  })
})
