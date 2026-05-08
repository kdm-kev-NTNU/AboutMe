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
})
