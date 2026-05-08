import { beforeEach, afterEach, describe, expect, it, vi } from 'vitest'
import { effectScope, ref } from 'vue'
import { flushPromises } from '@vue/test-utils'
import { useAudioWaveform } from '../useAudioWaveform'

/** Spies for each constructed fake AudioContext (for teardown assertions) */
let audioContextCloseSpies: ReturnType<typeof vi.fn>[] = []

describe('useAudioWaveform', () => {
  beforeEach(() => {
    audioContextCloseSpies = []

    vi.stubGlobal(
      'MediaStream',
      class {
        getTracks() {
          return []
        }
      } as unknown as typeof MediaStream,
    )

    class FakeAnalyser {
      fftSize = 2048
      smoothingTimeConstant = 0
      connect() {
        return this
      }
      disconnect() {}
      getFloatTimeDomainData(arr: Float32Array) {
        arr.fill(0.25)
      }
    }
    class FakeMediaStreamAudioSourceNode {
      connect() {
        return new FakeAnalyser()
      }
      disconnect() {}
    }
    class FakeAudioContext {
      state: AudioContextState = 'running'
      resume = vi.fn().mockResolvedValue(undefined)
      close = vi.fn().mockResolvedValue(undefined)
      constructor() {
        audioContextCloseSpies.push(this.close)
      }
      createMediaStreamSource = vi.fn(() => new FakeMediaStreamAudioSourceNode())
      createAnalyser = vi.fn(() => new FakeAnalyser())
    }
    vi.stubGlobal('AudioContext', FakeAudioContext as unknown as typeof AudioContext)
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('allocates a time-domain buffer and increments tick while the stream is active', async () => {
    const streamRef = ref<MediaStream | null>(null)
    const scope = effectScope()
    let composable!: ReturnType<typeof useAudioWaveform>
    scope.run(() => {
      composable = useAudioWaveform(streamRef)
    })

    streamRef.value = new MediaStream()
    await flushPromises()

    await vi.waitFor(() => expect(composable.tick.value).toBeGreaterThan(0))

    expect(composable.timeDomainData.value?.length).toBe(2048)
    expect(composable.timeDomainData.value?.every((v) => v === 0.25)).toBe(true)
    expect(composable.isActive.value).toBe(true)

    scope.stop()
    expect(composable.timeDomainData.value).toBeNull()
    expect(composable.tick.value).toBe(0)
    expect(audioContextCloseSpies.at(-1)).toHaveBeenCalled()
  })

  it('stops the analysis loop and clears data when the stream is removed', async () => {
    const streamRef = ref<MediaStream | null>(null)
    const scope = effectScope()
    let composable!: ReturnType<typeof useAudioWaveform>
    scope.run(() => {
      composable = useAudioWaveform(streamRef)
    })
    streamRef.value = new MediaStream()
    await flushPromises()
    await vi.waitFor(() => expect(composable.tick.value).toBeGreaterThan(0))

    streamRef.value = null
    await flushPromises()

    expect(composable.timeDomainData.value).toBeNull()
    expect(composable.isActive.value).toBe(false)
    expect(composable.tick.value).toBe(0)
    expect(audioContextCloseSpies.at(-1)).toHaveBeenCalled()

    scope.stop()
  })
})
