import { describe, expect, it, vi } from 'vitest'
import { ref } from 'vue'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import RealtimeVoicePanel from '../RealtimeVoicePanel.vue'

vi.mock('@/composables/useRealtimeVoice', () => ({
  useRealtimeVoice: () => ({
    connectionState: ref('idle'),
    errorMessage: ref(''),
    sessionNotice: ref(''),
    assistantTranscript: ref(''),
    userTranscript: ref(''),
    connect: vi.fn(),
    disconnect: vi.fn(),
    maxSessionMs: 900_000,
  }),
}))

describe('RealtimeVoicePanel', () => {
  it('renders live voice intro when available', () => {
    const pinia = createPinia()
    setActivePinia(pinia)

    const wrapper = mount(RealtimeVoicePanel, {
      props: {
        language: 'en',
        available: true,
        voiceOptions: ['marin', 'cedar'],
        reasoningOptions: ['low', 'medium', 'high'],
        defaultVoice: 'marin',
        defaultReasoningEffort: 'low',
      },
      global: {
        plugins: [pinia],
        stubs: {
          Button: { template: '<button><slot /></button>' },
          Alert: { template: '<div><slot /></div>' },
          AlertTitle: { template: '<div><slot /></div>' },
          AlertDescription: { template: '<div><slot /></div>' },
          AiStatusDialog: true,
          AiTransparencyNotice: { template: '<div data-testid="ai-transparency" />' },
          Mic: true,
          MicOff: true,
          Loader2: true,
          TriangleAlert: true,
        },
      },
    })

    expect(wrapper.text()).toContain('Start live voice')
    expect(wrapper.text()).toContain('Experimental mode')
  })
})
