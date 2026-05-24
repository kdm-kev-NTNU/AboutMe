import { describe, expect, it, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import StandardVoicePanel from '../StandardVoicePanel.vue'

vi.mock('@/composables/useStandardVoice', () => ({
  useStandardVoice: () => ({
    stage: { value: 'idle' },
    errorMessage: { value: '' },
    transcriptText: { value: '' },
    answerText: { value: '' },
    isWorking: { value: false },
    isRecording: { value: false },
    isTranscribing: { value: false },
    recordingMediaStream: { value: null },
    toggleRecording: vi.fn(),
  }),
}))

describe('StandardVoicePanel', () => {
  it('renders intro copy and language picker when available', () => {
    const wrapper = mount(StandardVoicePanel, {
      props: { language: 'en', available: true },
      global: {
        stubs: {
          Button: { template: '<button><slot /></button>' },
          Alert: { template: '<div><slot /></div>' },
          AlertTitle: { template: '<div><slot /></div>' },
          AlertDescription: { template: '<div><slot /></div>' },
          AudioWaveform: true,
        },
      },
    })

    expect(wrapper.text()).toContain('Robust voice mode')
    expect(wrapper.text()).toContain('English')
    expect(wrapper.text()).toContain('Norsk')
  })

  it('shows unavailable message in Norwegian', () => {
    const wrapper = mount(StandardVoicePanel, {
      props: { language: 'no', available: false },
      global: {
        stubs: {
          Button: { template: '<button><slot /></button>' },
          Alert: { template: '<div><slot /></div>' },
          AlertTitle: { template: '<div><slot /></div>' },
          AlertDescription: { template: '<div><slot /></div>' },
          AudioWaveform: true,
        },
      },
    })

    expect(wrapper.text()).toContain('Standard stemmemodus er ikke tilgjengelig')
  })
})
