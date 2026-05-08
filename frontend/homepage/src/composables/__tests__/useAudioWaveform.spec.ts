import { beforeEach, afterEach, describe, expect, it, vi } from 'vitest'
import { effectScope, ref } from 'vue'
import { flushPromises } from '@vue/test-utils'
import { useAudioWaveform } from '../useAudioWaveform'

/** Spies for each constructed fake AudioContext (for teardown assertions) */
let audioContextCloseSpies: ReturnType<typeof vi.fn>[] = []

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

/** Deterministic rAF so tests can hit the loop guard when analyserRef is cleared mid-queue. */
function installDeterministicRaf() {
  let nextId = 1
  const pending = new Map<number, FrameRequestCallback>()

  const requestAnimationFrame = vi.fn((cb: FrameRequestCallback): number => {
    const id = nextId++
    pending.set(id, cb)
    return id
  })

  const cancelAnimationFrame = vi.fn((id: number) => {
    pending.delete(id)
  })

  function flushOne(): void {
    const iter = pending.entries().next()
    if (iter.done) return
    const [id, cb] = iter.value
    pending.delete(id)
    cb(0)
  }

  vi.stubGlobal('requestAnimationFrame', requestAnimationFrame)
  vi.stubGlobal('cancelAnimationFrame', cancelAnimationFrame)

  return { flushOne, pending, cancelAnimationFrame }
}

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
    expect(composable.analyserRef.value).not.toBeNull()
    expect(composable.analyserRef.value?.fftSize).toBe(2048)

    scope.stop()
    expect(composable.timeDomainData.value).toBeNull()
    expect(composable.tick.value).toBe(0)
    expect(composable.analyserRef.value).toBeNull()
    expect(audioContextCloseSpies.at(-1)).toHaveBeenCalled()
  })

  /** Regression: AudioWaveform mounts after stream is already set (v-if + existing ref). Watch must run immediately. */
  it('starts analysis when composable attaches to a stream that is already set (immediate watch)', async () => {
    const streamRef = ref<MediaStream | null>(new MediaStream())
    const scope = effectScope()
    let composable!: ReturnType<typeof useAudioWaveform>
    scope.run(() => {
      composable = useAudioWaveform(streamRef)
    })
    await flushPromises()

    await vi.waitFor(() => expect(composable.tick.value).toBeGreaterThan(0))
    expect(composable.isActive.value).toBe(true)
    scope.stop()
  })

  it('closes the previous AudioContext when the stream reference changes', async () => {
    const streamRef = ref<MediaStream | null>(new MediaStream())
    const scope = effectScope()
    let composable!: ReturnType<typeof useAudioWaveform>
    scope.run(() => {
      composable = useAudioWaveform(streamRef)
    })
    await flushPromises()
    await vi.waitFor(() => expect(composable.tick.value).toBeGreaterThan(0))

    const firstClose = audioContextCloseSpies[0]
    expect(firstClose).toBeDefined()
    streamRef.value = new MediaStream()
    await flushPromises()
    await vi.waitFor(() => expect(firstClose).toHaveBeenCalled())
    /** New stream schedules a fresh rAF loop; tick was reset during teardown(). */
    await vi.waitFor(() => expect(composable.tick.value).toBeGreaterThan(0))

    scope.stop()
  })

  it('calls resume when AudioContext state is suspended', async () => {
    const instances: Array<{ resume: ReturnType<typeof vi.fn> }> = []
    class SuspendedAudioContext extends FakeAudioContext {
      override state: AudioContextState = 'suspended'
      constructor() {
        super()
        instances.push(this)
      }
    }
    vi.stubGlobal('AudioContext', SuspendedAudioContext as unknown as typeof AudioContext)

    const streamRef = ref<MediaStream | null>(new MediaStream())
    const scope = effectScope()
    scope.run(() => {
      useAudioWaveform(streamRef)
    })
    await flushPromises()

    await vi.waitFor(() => expect(instances.length).toBeGreaterThan(0))
    expect(instances[0]!.resume).toHaveBeenCalled()

    scope.stop()
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
    expect(composable.analyserRef.value).toBeNull()
    expect(audioContextCloseSpies.at(-1)).toHaveBeenCalled()

    scope.stop()
  })

  it('sets isActive false when starting with null stream until a stream arrives', async () => {
    const streamRef = ref<MediaStream | null>(null)
    const scope = effectScope()
    let composable!: ReturnType<typeof useAudioWaveform>
    scope.run(() => {
      composable = useAudioWaveform(streamRef)
    })
    await flushPromises()
    expect(composable.isActive.value).toBe(false)

    streamRef.value = new MediaStream()
    await flushPromises()
    await vi.waitFor(() => expect(composable.isActive.value).toBe(true))

    scope.stop()
  })

  it('stops sampling when analyserRef is cleared before the next rAF callback (loop guard)', async () => {
    const { flushOne, pending } = installDeterministicRaf()

    const streamRef = ref<MediaStream | null>(new MediaStream())
    const scope = effectScope()
    let composable!: ReturnType<typeof useAudioWaveform>
    scope.run(() => {
      composable = useAudioWaveform(streamRef)
    })
    await flushPromises()

    expect(pending.size).toBeGreaterThan(0)
    flushOne()
    expect(composable.tick.value).toBe(1)

    composable.analyserRef.value = null
    expect(pending.size).toBeGreaterThan(0)
    flushOne()
    expect(composable.tick.value).toBe(1)
    expect(pending.size).toBe(0)

    streamRef.value = null
    await flushPromises()
    scope.stop()
  })

  it('still tears down when source disconnect throws', async () => {
    class ThrowingSource extends FakeMediaStreamAudioSourceNode {
      override disconnect(): void {
        throw new Error('disconnect(source)')
      }
    }

    class Ctx extends FakeAudioContext {
      override createMediaStreamSource = vi.fn(() => new ThrowingSource())
    }
    vi.stubGlobal('AudioContext', Ctx as unknown as typeof AudioContext)

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

    expect(composable.analyserRef.value).toBeNull()
    expect(composable.timeDomainData.value).toBeNull()
    expect(audioContextCloseSpies.at(-1)).toHaveBeenCalled()

    scope.stop()
  })

  it('still tears down when analyser disconnect throws', async () => {
    class ThrowingAnalyser extends FakeAnalyser {
      override disconnect(): void {
        throw new Error('disconnect(analyser)')
      }
    }

    class Ctx extends FakeAudioContext {
      override createAnalyser = vi.fn(() => new ThrowingAnalyser())
    }
    vi.stubGlobal('AudioContext', Ctx as unknown as typeof AudioContext)

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

    expect(composable.sourceRef).not.toBeDefined()
    expect(composable.analyserRef.value).toBeNull()
    scope.stop()
  })
})
