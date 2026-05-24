import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { computed, defineComponent, effectScope, h, ref } from 'vue'
import { flushPromises, mount } from '@vue/test-utils'
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

  function createApi(languageConfirmed = true, language: 'en' | 'no' = 'en') {
    const scope = effectScope()
    let api!: ReturnType<typeof useStandardVoice>
    scope.run(() => {
      api = useStandardVoice({
        language: computed(() => language),
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

  it('requires language confirmation in Norwegian', async () => {
    const { api } = createApi(false, 'no')
    await api.toggleRecording()
    expect(api.stage.value).toBe('error')
    expect(api.errorMessage.value).toContain('Velg språk først')
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

  it('speaks low-confidence lookup answers with hedging', async () => {
    vi.mocked(lookupRealtimeInfo).mockResolvedValue({
      found: true,
      confidence: 'low',
      snippets: [{ sourceType: 'profile', title: 'A', text: 'Maybe Kevin studies IT.' }],
    })

    const { api } = createApi(true)
    await api.toggleRecording()
    await flushPromises()

    expect(synthesizeSpeech).toHaveBeenCalledWith(
      expect.stringContaining('not sure I understood'),
      'en',
      expect.any(AbortSignal),
    )
  })

  it('speaks a not-found lookup answer', async () => {
    vi.mocked(lookupRealtimeInfo).mockResolvedValue({
      found: false,
      confidence: 'low',
      snippets: [],
    })

    const { api } = createApi(true)
    await api.toggleRecording()
    await flushPromises()

    expect(synthesizeSpeech).toHaveBeenCalledWith(
      expect.stringContaining("I don't have information about that"),
      'en',
      expect.any(AbortSignal),
    )
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

  it('sets transcribing stage after recording stops', async () => {
    useSpeechTranscriptionMock.mockImplementation(() => ({
      isRecording,
      isTranscribing,
      voiceError,
      recordingMediaStream,
      supportsSpeechInput: ref(true),
      toggleVoiceInput: vi.fn(async () => {
        isTranscribing.value = true
      }),
      cancel: vi.fn(),
    }))
    const { api } = createApi(true)
    await api.toggleRecording()
    expect(api.stage.value).toBe('transcribing')
  })

  it('returns early from toggleRecording when turn was cancelled during mic toggle', async () => {
    const { api } = createApi(true)
    toggleVoiceInputMock.mockImplementation(async () => {
      api.cancel()
    })
    await api.toggleRecording()
    expect(api.stage.value).toBe('idle')
  })

  it('surfaces Norwegian playback errors', async () => {
    vi.spyOn(HTMLMediaElement.prototype, 'play').mockRejectedValue(new Error('blocked'))

    const { api } = createApi(true, 'no')
    await api.toggleRecording()
    await flushPromises()

    expect(api.stage.value).toBe('error')
    expect(api.errorMessage.value).toContain('Kunne ikke spille av syntetisert lyd')
  })

  it('ignores lookup abort errors', async () => {
    let onTranscript!: (text: string) => void
    useSpeechTranscriptionMock.mockImplementation(({ onTranscript: onText }: { onTranscript: (text: string) => void }) => {
      onTranscript = onText
      return {
        isRecording,
        isTranscribing,
        voiceError,
        recordingMediaStream,
        supportsSpeechInput: ref(true),
        toggleVoiceInput: vi.fn(),
        cancel: vi.fn(),
      }
    })
    vi.mocked(lookupRealtimeInfo).mockRejectedValue(new DOMException('aborted', 'AbortError'))

    const { api } = createApi(true)
    onTranscript('hello')
    await flushPromises()

    expect(api.stage.value).not.toBe('error')
  })

  it('skips playback when the turn is cancelled after synthesis', async () => {
    const { api } = createApi(true)
    vi.mocked(synthesizeSpeech).mockImplementation(async () => {
      api.cancel()
      return { ok: true, blob: new Blob(['audio'], { type: 'audio/mpeg' }) }
    })

    await api.toggleRecording()
    await flushPromises()

    expect(api.stage.value).toBe('idle')
    expect(HTMLMediaElement.prototype.play).not.toHaveBeenCalled()
  })

  it('returns to idle when synthesized playback ends', async () => {
    const playSpy = vi.spyOn(HTMLMediaElement.prototype, 'play').mockImplementation(function (this: HTMLAudioElement) {
      return Promise.resolve()
    })

    const { api } = createApi(true)
    await api.toggleRecording()
    await flushPromises()
    expect(api.stage.value).toBe('speaking')

    const audioRef = playSpy.mock.contexts[0] as HTMLAudioElement
    expect(typeof audioRef.onended).toBe('function')
    audioRef.onended!(new Event('ended'))
    expect(api.stage.value).toBe('idle')
  })

  it('cleans up on component unmount', async () => {
    let api!: ReturnType<typeof useStandardVoice>
    const Host = defineComponent({
      setup() {
        api = useStandardVoice({
          language: computed(() => 'en' as const),
          languageConfirmed: computed(() => true),
        })
        return () => h('div')
      },
    })
    const wrapper = mount(Host)
    api.stage.value = 'recording'
    wrapper.unmount()
    expect(api.stage.value).toBe('idle')
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
