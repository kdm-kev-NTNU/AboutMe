import { describe, expect, it, vi } from 'vitest'
import { ref } from 'vue'
import { mount } from '@vue/test-utils'
import StandardVoicePanel from '../StandardVoicePanel.vue'

const useStandardVoiceMock = vi.hoisted(() => vi.fn())

vi.mock('@/composables/useStandardVoice', () => ({
  useStandardVoice: (...args: unknown[]) => useStandardVoiceMock(...args),
}))

function mockStandardVoice(overrides: Record<string, unknown> = {}) {
  const defaults = {
    stage: ref('idle'),
    errorMessage: ref(''),
    transcriptText: ref(''),
    answerText: ref(''),
    isWorking: ref(false),
    canCancel: ref(false),
    isRecording: ref(false),
    isTranscribing: ref(false),
    recordingMediaStream: ref<MediaStream | null>(null),
    toggleRecording: vi.fn(),
    cancel: vi.fn(),
  }
  const api = { ...defaults, ...overrides }
  useStandardVoiceMock.mockReturnValue(api)
  return api
}

const globalStubs = {
  Button: { template: '<button><slot /></button>' },
  Alert: { template: '<div><slot /></div>' },
  AlertTitle: { template: '<div><slot /></div>' },
  AlertDescription: { template: '<div><slot /></div>' },
  AudioWaveform: true,
}

describe('StandardVoicePanel', () => {
  it('renders intro copy and language picker when available', () => {
    mockStandardVoice()
    const wrapper = mount(StandardVoicePanel, {
      props: { language: 'en', available: true },
      global: { stubs: globalStubs },
    })

    expect(wrapper.text()).toContain('Robust voice mode')
    expect(wrapper.text()).toContain('English')
    expect(wrapper.text()).toContain('Norsk')
  })

  it('shows unavailable message in Norwegian', () => {
    mockStandardVoice()
    const wrapper = mount(StandardVoicePanel, {
      props: { language: 'no', available: false },
      global: { stubs: globalStubs },
    })

    expect(wrapper.text()).toContain('Standard stemmemodus er ikke tilgjengelig')
  })

  it('highlights selected language after confirmLanguage', async () => {
    mockStandardVoice()
    const wrapper = mount(StandardVoicePanel, {
      props: { language: 'en', available: true },
      global: { stubs: globalStubs },
    })

    await wrapper.findAll('button').find((b) => b.text() === 'Norsk')!.trigger('click')
    const norskBtn = wrapper.findAll('button').find((b) => b.text() === 'Norsk')!
    expect(norskBtn.classes()).toContain('border-blue-300')
  })

  it('shows cancel control and stage status while working', () => {
    mockStandardVoice({
      stage: ref('looking_up'),
      canCancel: ref(true),
      isWorking: ref(true),
    })
    const wrapper = mount(StandardVoicePanel, {
      props: { language: 'en', available: true },
      global: { stubs: globalStubs },
    })

    expect(wrapper.find('[data-testid="standard-voice-cancel"]').exists()).toBe(true)
    expect(wrapper.text()).toContain('Looking up facts...')
  })

  it('shows speaking status and transcript or answer blocks', () => {
    mockStandardVoice({
      stage: ref('speaking'),
      transcriptText: ref('Hello Kevin'),
      answerText: ref('Kevin studies at NTNU.'),
    })
    const wrapper = mount(StandardVoicePanel, {
      props: { language: 'en', available: true },
      global: { stubs: globalStubs },
    })

    expect(wrapper.text()).toContain('Speaking...')
    expect(wrapper.text()).toContain('Hello Kevin')
    expect(wrapper.text()).toContain('Kevin studies at NTNU.')
  })

  it('shows stop recording label while recording', () => {
    mockStandardVoice({
      isRecording: ref(true),
      isWorking: ref(true),
    })
    const wrapper = mount(StandardVoicePanel, {
      props: { language: 'en', available: true },
      global: { stubs: globalStubs },
    })

    expect(wrapper.text()).toContain('Stop recording')
  })

  it('renders error alert when errorMessage is set', () => {
    mockStandardVoice({
      errorMessage: ref('Microphone unavailable'),
    })
    const wrapper = mount(StandardVoicePanel, {
      props: { language: 'en', available: true },
      global: { stubs: globalStubs },
    })

    expect(wrapper.text()).toContain('Microphone unavailable')
  })
})
