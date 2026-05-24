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

vi.mock('@/lib/analytics', () => ({
  captureProductAnalyticsEvent: vi.fn(),
}))

describe('VoiceView.vue', () => {
  async function factory(opts: {
    lang: 'en' | 'no'
    route?: string
    standardEnabled: boolean
    liveEnabled: boolean
  }) {
    vi.clearAllMocks()
    fetchRealtimeVoiceStatusMock.mockReset()
    fetchRealtimeVoiceModelsMock.mockReset()
    fetchRealtimeVoiceModelsMock.mockResolvedValue([
      { provider: 'OPENAI', id: 'gpt-realtime-2', label: 'OpenAI GPT-Realtime-2', defaultOption: true },
      { provider: 'ELEVENLABS', id: 'agent_1', label: 'ElevenLabs Agent', defaultOption: false },
    ])
    fetchRealtimeVoiceStatusMock.mockResolvedValue({
      enabled: opts.standardEnabled || opts.liveEnabled,
      standardEnabled: opts.standardEnabled,
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

    await router.push(opts.route ?? '/voice')
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
      VoiceModeSwitcher: {
        props: ['modelValue'],
        emits: ['update:modelValue'],
        template: '<div><button data-testid="switch-standard" @click="$emit(\'update:modelValue\', \'standard\')">standard</button><button data-testid="switch-live" @click="$emit(\'update:modelValue\', \'live\')">live</button><span data-testid="mode">{{ modelValue }}</span></div>',
      },
      StandardVoicePanel: { template: '<div data-testid="standard-panel">standard panel</div>' },
      RealtimeVoicePanel: { template: '<div data-testid="live-panel">live panel</div>' },
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
      router,
    }
  }

  beforeEach(async () => {
    document.body.innerHTML = ''
    sessionStorage.clear()
    vi.clearAllMocks()
  })

  it('defaults to standard mode and renders standard panel', async () => {
    const { wrapper } = await factory({
      lang: 'en',
      standardEnabled: true,
      liveEnabled: true,
    })

    await flushPromises()
    expect(fetchRealtimeVoiceStatusMock).toHaveBeenCalledTimes(1)
    expect(wrapper.find('[data-testid="standard-panel"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="live-panel"]').exists()).toBe(false)
    wrapper.unmount()
  })

  it('uses live mode when route query asks for it', async () => {
    const { wrapper } = await factory({
      lang: 'en',
      route: '/voice?mode=live',
      standardEnabled: true,
      liveEnabled: true,
    })
    await flushPromises()
    expect(wrapper.find('[data-testid="live-panel"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="standard-panel"]').exists()).toBe(false)
    wrapper.unmount()
  })

  it('renders Norwegian copy', async () => {
    const { wrapper } = await factory({
      lang: 'no',
      standardEnabled: true,
      liveEnabled: false,
    })
    await flushPromises()
    expect(wrapper.text()).toContain('Snakk med Kevin sin AI')
    expect(wrapper.text()).toContain('Bruk tekstchat')
    wrapper.unmount()
  })

  it('updates route query when switching to live mode', async () => {
    const { wrapper, router } = await factory({
      lang: 'en',
      standardEnabled: true,
      liveEnabled: true,
    })
    await flushPromises()
    await wrapper.find('[data-testid="switch-live"]').trigger('click')
    await flushPromises()
    expect(router.currentRoute.value.query.mode).toBe('live')
    expect(wrapper.find('[data-testid="live-panel"]').exists()).toBe(true)
    wrapper.unmount()
  })

  it('clears route query when switching back to standard mode', async () => {
    const { wrapper, router } = await factory({
      lang: 'en',
      route: '/voice?mode=live',
      standardEnabled: true,
      liveEnabled: true,
    })
    await flushPromises()
    await wrapper.find('[data-testid="switch-standard"]').trigger('click')
    await flushPromises()
    expect(router.currentRoute.value.query.mode).toBeUndefined()
    expect(wrapper.find('[data-testid="standard-panel"]').exists()).toBe(true)
    wrapper.unmount()
  })
})
