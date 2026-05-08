import { computed, onScopeDispose, ref, shallowRef, watch, type Ref } from 'vue'

/**
 * Connects a microphone MediaStream to an AnalyserNode and fills time-domain samples each animation frame.
 */
export function useAudioWaveform(streamRef: Ref<MediaStream | null>) {
  const audioContextRef = shallowRef<AudioContext | null>(null)
  const analyserRef = shallowRef<AnalyserNode | null>(null)
  const sourceRef = shallowRef<MediaStreamAudioSourceNode | null>(null)
  /** Mutable buffer updated in-place each frame; length === fftSize */
  const timeDomainData = shallowRef<Float32Array | null>(null)
  /** Increments every frame so consumers can trigger redraws */
  const tick = ref(0)

  let rafId = 0

  const isActive = computed(
    () => streamRef.value !== null && analyserRef.value !== null && timeDomainData.value !== null,
  )

  function stopLoop() {
    if (rafId !== 0) {
      cancelAnimationFrame(rafId)
      rafId = 0
    }
  }

  function teardown() {
    stopLoop()
    try {
      sourceRef.value?.disconnect()
    } catch {
      /* ignore */
    }
    sourceRef.value = null
    try {
      analyserRef.value?.disconnect()
    } catch {
      /* ignore */
    }
    analyserRef.value = null
    void audioContextRef.value?.close().catch(() => {})
    audioContextRef.value = null
    timeDomainData.value = null
    tick.value = 0
  }

  watch(
    streamRef,
    async (stream) => {
      teardown()
      if (!stream) return

      const ctx = new AudioContext()
      audioContextRef.value = ctx

      if (ctx.state === 'suspended') {
        await ctx.resume().catch(() => {})
      }

      const source = ctx.createMediaStreamSource(stream)
      sourceRef.value = source

      const analyser = ctx.createAnalyser()
      analyser.fftSize = 2048
      analyser.smoothingTimeConstant = 0.35
      source.connect(analyser)
      analyserRef.value = analyser

      const sampleBytes = analyser.fftSize * Float32Array.BYTES_PER_ELEMENT
      const loopBuf = new Float32Array(new ArrayBuffer(sampleBytes))
      timeDomainData.value = loopBuf

      const loop = () => {
        const a = analyserRef.value
        if (!a) return
        a.getFloatTimeDomainData(loopBuf)
        tick.value++
        rafId = requestAnimationFrame(loop)
      }
      rafId = requestAnimationFrame(loop)
    },
    { flush: 'sync', immediate: true },
  )

  onScopeDispose(() => {
    teardown()
  })

  return {
    timeDomainData,
    tick,
    isActive,
    analyserRef,
  }
}
