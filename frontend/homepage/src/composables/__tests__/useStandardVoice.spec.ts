import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { computed, effectScope, ref } from 'vue'
import { flushPromises } from '@vue/test-utils'
import { useStandardVoice } from '../useStandardVoice'
import { lookupRealtimeInfo } from '@/lib/realtime-voice'
import { synthesizeSpeech } from '@/lib/synthesize-speech'
import { captureProductAnalyticsEvent } from '@/lib/analytics'

const toggleVoiceInputMock = vi.hoisted(() => vi.fn())
const useSpeechTranscriptionMock = vi.hoisted(() => vi.fn())

vi.mock('@/composables/useSpeechTranscription', () => ({
  useSpeechTranscription: (...args: unknown[]) => useSpeechTranscriptionMock(...args),
}))

vi.mock('@/lib/realtime-voice', async (importOriginal) => {
  const actual = await importOriginal<typeof import('@/lib/realtime-voice')>()
  return {
    ...actual,
    lookupRealtimeInfo: vi.fn(),
  }
})

vi.mock('@/lib/synthesize-speech', () => ({
  synthesizeSpeech: vi.fn(),
}))

vi.mock('@/lib/analytics', () => ({
  captureProductAnalyticsEvent: vi.fn(),
}))

describe('useStandardVoice', () => {
  const isRecording = ref(false)
  const isTranscribing = ref(false)
  const voiceError = ref('')
  const recordingMediaStream = ref<MediaStream | null>(null)

  beforeEach(() => {
    vi.clearAllMocks()
    isRecording.value = false
    isTranscribing.value = false
    voiceError.value = ''
    recordingMediaStream.value = null
    toggleVoiceInputMock.mockResolvedValue(undefined)

    useSpeechTranscriptionMock.mockImplementation(({ onTranscript }: { onTranscript: (text: string) => void }) => ({
      isRecording,
      isTranscribing,
      voiceError,
      recordingMediaStream,
      supportsSpeechInput: ref(true),
      toggleVoiceInput: toggleVoiceInputMock.mockImplementation(async () => {
        onTranscript('what does Kevin study')
      }),
      cancel: vi.fn(),
    }))

    vi.mocked(lookupRealtimeInfo).mockResolvedValue({
      found: true,
      confidence: 'high',
      snippets: [{ sourceType: 'profile', title: 'NTNU', text: 'Kevin studies at NTNU.' }],
    })
    vi.mocked(synthesizeSpeech).mockResolvedValue({
      ok: true,
      blob: new Blob(['audio'], { type: 'audio/mpeg' }),
    })

    vi.spyOn(HTMLMediaElement.prototype, 'play').mockResolvedValue(undefined)
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  function createApi(languageConfirmed = true) {
    const scope = effectScope()
    let api!: ReturnType<typeof useStandardVoice>
    scope.run(() => {
      api = useStandardVoice({
        language: computed(() => 'en' as const),
        languageConfirmed: computed(() => languageConfirmed),
      })
    })
    return { api: api!, scope }
  }

  it('requires language confirmation before recording', async () => {
    const { api } = createApi(false)
    await api.toggleRecording()
    expect(api.stage.value).toBe('error')
    expect(api.errorMessage.value).toContain('Choose language first')
  })

  it('does not mark transcribing before stop is requested', async () => {
    isRecording.value = true
    const stagesDuringToggle: string[] = []
    const { api } = createApi(true)
    toggleVoiceInputMock.mockImplementation(async () => {
      stagesDuringToggle.push(api.stage.value)
    })
    api.stage.value = 'recording'
    await api.toggleRecording()
    expect(stagesDuringToggle).toEqual(['recording'])
    expect(toggleVoiceInputMock).toHaveBeenCalledTimes(1)
  })

  it('runs lookup and synthesis after transcript arrives', async () => {
    const { api } = createApi(true)
    await api.toggleRecording()
    await flushPromises()

    expect(lookupRealtimeInfo).toHaveBeenCalledWith('what does Kevin study', 'en', expect.any(AbortSignal))
    expect(synthesizeSpeech).toHaveBeenCalledWith('Kevin studies at NTNU.', 'en', expect.any(AbortSignal))
    expect(api.stage.value).toBe('speaking')
    expect(captureProductAnalyticsEvent).toHaveBeenCalled()
  })

  it('surfaces synthesis errors', async () => {
    vi.mocked(synthesizeSpeech).mockResolvedValue({
      ok: false,
      status: 503,
      message: 'Speech synthesis is temporarily unavailable.',
    })

    const { api } = createApi(true)
    await api.toggleRecording()
    await flushPromises()

    expect(api.stage.value).toBe('error')
    expect(api.errorMessage.value).toContain('Speech synthesis')
  })

  it('surfaces playback errors after successful synthesis', async () => {
    vi.spyOn(HTMLMediaElement.prototype, 'play').mockRejectedValue(new Error('blocked'))

    const { api } = createApi(true)
    await api.toggleRecording()
    await flushPromises()

    expect(api.stage.value).toBe('error')
    expect(api.errorMessage.value).toContain('Could not play synthesized audio')
  })

  it('surfaces transcription errors from the mic pipeline', async () => {
    useSpeechTranscriptionMock.mockImplementation(() => ({
      isRecording,
      isTranscribing,
      voiceError: ref('Microphone unavailable'),
      recordingMediaStream,
      supportsSpeechInput: ref(true),
      toggleVoiceInput: toggleVoiceInputMock,
      cancel: vi.fn(),
    }))

    const { api } = createApi(true)
    await api.toggleRecording()
    await flushPromises()

    expect(api.stage.value).toBe('error')
    expect(api.errorMessage.value).toBe('Microphone unavailable')
  })

  it('asks the user to repeat when transcript quality is uncertain', async () => {
    toggleVoiceInputMock.mockImplementation(async ({ onTranscript }: { onTranscript: (text: string) => void }) => {
      onTranscript('??')
    })
    useSpeechTranscriptionMock.mockImplementation(({ onTranscript }: { onTranscript: (text: string) => void }) => ({
      isRecording,
      isTranscribing,
      voiceError,
      recordingMediaStream,
      supportsSpeechInput: ref(true),
      toggleVoiceInput: toggleVoiceInputMock.mockImplementation(async () => {
        onTranscript('??')
      }),
      cancel: vi.fn(),
    }))

    const { api } = createApi(true)
    await api.toggleRecording()
    await flushPromises()

    expect(lookupRealtimeInfo).not.toHaveBeenCalled()
    expect(synthesizeSpeech).toHaveBeenCalledWith(
      expect.stringContaining("didn't catch that clearly"),
      'en',
      expect.any(AbortSignal),
    )
  })

  it('cancels an in-flight turn and returns to idle', async () => {
    let resolveLookup: ((value: Awaited<ReturnType<typeof lookupRealtimeInfo>>) => void) | undefined
    vi.mocked(lookupRealtimeInfo).mockImplementation(
      () =>
        new Promise((resolve) => {
          resolveLookup = resolve
        }),
    )

    const { api } = createApi(true)
    await api.toggleRecording()
    await flushPromises()
    expect(api.stage.value).toBe('looking_up')

    api.cancel()
    expect(api.stage.value).toBe('idle')

    resolveLookup?.({
      found: true,
      confidence: 'high',
      snippets: [{ sourceType: 'profile', title: 'NTNU', text: 'Late answer.' }],
    })
    await flushPromises()
    expect(api.stage.value).toBe('idle')
  })
})
