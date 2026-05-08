import { beforeEach, afterEach, describe, expect, it, vi } from 'vitest'
import { computed, effectScope, ref } from 'vue'
import { flushPromises } from '@vue/test-utils'
import { useSpeechTranscription } from '../useSpeechTranscription'
import { transcribeSpeech } from '@/lib/transcribe-audio'

vi.mock('@/lib/transcribe-audio', () => ({
  transcribeSpeech: vi.fn(),
}))

vi.mock('@/stores/auth', () => ({
  useAuthStore: () => ({ restore: vi.fn() }),
}))

describe('useSpeechTranscription', () => {
  const origMediaDevices = globalThis.navigator.mediaDevices

  beforeEach(() => {
    vi.mocked(transcribeSpeech).mockResolvedValue({
      status: 200,
      data: { text: 'hello from mic' },
      headers: new Headers(),
    })

    vi.stubGlobal(
      'MediaStream',
      class {
        getTracks() {
          return []
        }
      } as unknown as typeof MediaStream,
    )

    class StubRecorder {
      static isTypeSupported = () => true
      state = 'inactive'
      ondataavailable: ((ev: { data: Blob }) => void) | null = null
      private listeners: Record<string, Array<(ev?: Event) => void>> = {}
      start() {
        this.state = 'recording'
      }
      stop() {
        this.state = 'inactive'
        for (const cb of this.listeners['stop'] ?? []) cb()
      }
      addEventListener(type: string, cb: (ev?: Event) => void) {
        if (!this.listeners[type]) this.listeners[type] = []
        this.listeners[type].push(cb)
      }
      requestData = () => {
        this.ondataavailable?.({ data: new Blob([new Uint8Array([1])]) } as BlobEvent)
      }
    }
    vi.stubGlobal('MediaRecorder', StubRecorder as unknown as typeof MediaRecorder)

    Object.defineProperty(globalThis.navigator, 'mediaDevices', {
      value: {
        getUserMedia: vi.fn().mockResolvedValue({ getTracks: () => [{ stop: vi.fn() }] }),
      },
      configurable: true,
    })
  })

  afterEach(() => {
    Object.defineProperty(globalThis.navigator, 'mediaDevices', {
      value: origMediaDevices,
      configurable: true,
    })
    vi.unstubAllGlobals()
  })

  it('calls onTranscript after a successful recording round-trip', async () => {
    const language = ref<'en' | 'no'>('en')
    const blocked = ref(false)
    const onTranscript = vi.fn()

    const scope = effectScope()
    let api!: ReturnType<typeof useSpeechTranscription>
    scope.run(() => {
      api = useSpeechTranscription({
        language: computed(() => language.value),
        maxChars: 3000,
        isBlocked: computed(() => blocked.value),
        onTranscript,
      })
    })

    await api.toggleVoiceInput()
    await flushPromises()
    expect(api.isRecording.value).toBe(true)

    await api.toggleVoiceInput()
    await flushPromises()

    expect(onTranscript).toHaveBeenCalledWith('hello from mic')
    expect(transcribeSpeech).toHaveBeenCalledWith(expect.any(Blob), 'en')
    expect(api.isRecording.value).toBe(false)
    expect(api.isTranscribing.value).toBe(false)

    scope.stop()
  })

  it('does not start recording when isBlocked is true', async () => {
    const language = ref<'en' | 'no'>('en')
    const blocked = ref(true)
    const onTranscript = vi.fn()

    const scope = effectScope()
    let api!: ReturnType<typeof useSpeechTranscription>
    scope.run(() => {
      api = useSpeechTranscription({
        language: computed(() => language.value),
        maxChars: 3000,
        isBlocked: computed(() => blocked.value),
        onTranscript,
      })
    })

    await api.toggleVoiceInput()
    await flushPromises()

    expect(api.isRecording.value).toBe(false)
    expect(onTranscript).not.toHaveBeenCalled()

    scope.stop()
  })

  function makeApi(language: 'en' | 'no' = 'en') {
    const langRef = ref<'en' | 'no'>(language)
    const blocked = ref(false)
    const onTranscript = vi.fn()
    const scope = effectScope()
    let api!: ReturnType<typeof useSpeechTranscription>
    scope.run(() => {
      api = useSpeechTranscription({
        language: computed(() => langRef.value),
        maxChars: 3000,
        isBlocked: computed(() => blocked.value),
        onTranscript,
      })
    })
    return { api, scope, onTranscript, langRef }
  }

  async function recordOnce(api: ReturnType<typeof useSpeechTranscription>) {
    await api.toggleVoiceInput()
    await flushPromises()
    await api.toggleVoiceInput()
    await flushPromises()
  }

  it('shows i18n English server-error message on 500 response', async () => {
    vi.mocked(transcribeSpeech).mockResolvedValue({
      status: 500,
      data: { error: 'whatever the server returned' },
      headers: new Headers(),
    })
    const { api, scope, onTranscript } = makeApi('en')

    await recordOnce(api)

    expect(onTranscript).not.toHaveBeenCalled()
    // The composable deliberately maps 500s to a canned i18n message instead of leaking the
    // raw server text — matches the same pattern used for 429 and 503.
    expect(api.voiceError.value).toBe(
      'Transcription failed on the server. Please try again.',
    )
    scope.stop()
  })

  it('shows generic 500 server message in Norwegian when no error body', async () => {
    vi.mocked(transcribeSpeech).mockResolvedValue({
      status: 500,
      data: undefined,
      headers: new Headers(),
    })
    const { api, scope } = makeApi('no')

    await recordOnce(api)

    expect(api.voiceError.value).toBe(
      'Transkribering feilet på serveren. Prøv igjen.',
    )
    scope.stop()
  })

  it('shows rate-limit message on 429 response', async () => {
    vi.mocked(transcribeSpeech).mockResolvedValue({
      status: 429,
      data: { error: 'rate limited' },
      headers: new Headers(),
    })
    const { api, scope } = makeApi('en')

    await recordOnce(api)

    expect(api.voiceError.value).toBe(
      'Too many requests or budget limit. Wait and try again.',
    )
    scope.stop()
  })

  it('shows service-unavailable message on 503 response', async () => {
    vi.mocked(transcribeSpeech).mockResolvedValue({
      status: 503,
      data: { error: 'Speech-to-text is temporarily unavailable.' },
      headers: new Headers(),
    })
    const { api, scope } = makeApi('en')

    await recordOnce(api)

    expect(api.voiceError.value).toBe(
      'Speech-to-text is temporarily unavailable. Please try again later.',
    )
    scope.stop()
  })

  it('does not call onTranscript and surfaces empty-speech message when text is empty', async () => {
    vi.mocked(transcribeSpeech).mockResolvedValue({
      status: 200,
      data: { text: '   ' },
      headers: new Headers(),
    })
    const { api, scope, onTranscript } = makeApi('no')

    await recordOnce(api)

    expect(onTranscript).not.toHaveBeenCalled()
    expect(api.voiceError.value).toBe('Ingen tale oppdaget. Prøv igjen.')
    scope.stop()
  })

  it('shows network error when transcribeSpeech throws', async () => {
    vi.mocked(transcribeSpeech).mockRejectedValue(new TypeError('Failed to fetch'))
    const { api, scope } = makeApi('en')

    await recordOnce(api)

    expect(api.voiceError.value).toBe('Network error during transcription.')
    scope.stop()
  })

  it('forwards the current UI language to transcribeSpeech', async () => {
    vi.mocked(transcribeSpeech).mockResolvedValue({
      status: 200,
      data: { text: 'hei' },
      headers: new Headers(),
    })
    const { api, scope } = makeApi('no')

    await recordOnce(api)

    expect(transcribeSpeech).toHaveBeenCalledWith(expect.any(Blob), 'no')
    scope.stop()
  })

  it('flips isTranscribing on then off across the API call', async () => {
    let resolveCall: (v: {
      data: unknown
      status: number
      headers: Headers
    }) => void = () => {}
    vi.mocked(transcribeSpeech).mockReturnValue(
      new Promise((res) => {
        resolveCall = res
      }),
    )
    const { api, scope } = makeApi('en')

    await api.toggleVoiceInput()
    await flushPromises()
    // Start the stop-and-transcribe flow but DO NOT await yet — we need to inspect mid-flight state.
    const stopPromise = api.toggleVoiceInput()
    await flushPromises()
    expect(api.isTranscribing.value).toBe(true)

    resolveCall({
      status: 200,
      data: { text: 'mid-flight' },
      headers: new Headers(),
    })
    await stopPromise
    await flushPromises()
    expect(api.isTranscribing.value).toBe(false)
    scope.stop()
  })

  it('surfaces server-provided error string via readApiError when status is 4xx other than 429', async () => {
    vi.mocked(transcribeSpeech).mockResolvedValue({
      status: 400,
      data: { error: 'Audio file is empty.' },
      headers: new Headers(),
    })
    const { api, scope } = makeApi('en')

    await recordOnce(api)

    expect(api.voiceError.value).toBe('Audio file is empty.')
    scope.stop()
  })
})
