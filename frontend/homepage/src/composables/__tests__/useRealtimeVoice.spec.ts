import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { effectScope, ref } from 'vue'
import { flushPromises } from '@vue/test-utils'
import type { SpeechUiLang } from '@/lib/realtime-voice'

const SESSION_MAX_MS = 180_000

const exchangeRealtimeSdpMock = vi.hoisted(() => vi.fn())

vi.mock('@/lib/realtime-voice', async (importOriginal) => {
  const mod = await importOriginal<typeof import('@/lib/realtime-voice')>()
  return {
    ...mod,
    exchangeRealtimeSdp: exchangeRealtimeSdpMock,
    REALTIME_SESSION_MAX_MS: SESSION_MAX_MS,
  }
})

vi.mock('@/lib/analytics', () => ({
  captureProductAnalyticsEvent: vi.fn(),
  captureClientException: vi.fn(),
}))

type MessageListener = (ev: MessageEvent) => void

describe('useRealtimeVoice', () => {
  let latestRtc: { dispatchRemoteTrack(): void } | null = null
  let messageListeners: MessageListener[] = []

  beforeEach(async () => {
    vi.restoreAllMocks()
    vi.unstubAllGlobals()

    exchangeRealtimeSdpMock.mockReset()
    messageListeners = []
    latestRtc = null

    const { captureProductAnalyticsEvent, captureClientException } = await import('@/lib/analytics')
    vi.mocked(captureProductAnalyticsEvent).mockReset()
    vi.mocked(captureClientException).mockReset()

    exchangeRealtimeSdpMock.mockResolvedValue({
      ok: true,
      answerSdp: 'v=0 ANSWER',
    })

    class StubRtcDataChannel implements Partial<RTCDataChannel> {
      readonly label = ''
      readonly readyState: RTCDataChannelState = 'open'

      addEventListener(_type: string, listener: EventListenerOrEventListenerObject | null): void {
        if (listener && typeof listener === 'function') {
          messageListeners.push(listener as MessageListener)
        }
      }

      removeEventListener(): void {}

      close(): void {}

      send(): void {}
    }

    vi.stubGlobal(
      'MediaStream',
      class MediaStreamStub {
        getTracks() {
          return []
        }
      } as unknown as typeof MediaStream,
    )

    vi.stubGlobal(
      'RTCPeerConnection',
      class StubRtc implements Partial<RTCPeerConnection> {
        localDescription: RTCSessionDescriptionInit | undefined
        readonly iceGatheringState: RTCIceGatheringState = 'complete'
        ontrack:
          | ((this: RTCPeerConnection, ev: RTCTrackEvent) => void)
          | null = null

        constructor() {
          latestRtc = {
            dispatchRemoteTrack: () => {
              const fakeStream = new MediaStream([])
              const ev = new Event('track') as unknown as RTCTrackEvent
              Object.assign(ev, { streams: [fakeStream], track: {} })
              const handler = this.ontrack
              if (handler) {
                handler.call(this as RTCPeerConnection, ev as RTCTrackEvent)
              }
            },
          }
        }

        createDataChannel(): RTCDataChannel {
          return new StubRtcDataChannel() as unknown as RTCDataChannel
        }

        async createOffer() {
          return { type: 'offer', sdp: 'v=0 OFFER_STUB' }
        }

        async setLocalDescription(init: RTCLocalSessionDescriptionInit) {
          const sdpPart = typeof init.sdp === 'string' ? init.sdp : undefined
          this.localDescription =
            sdpPart !== undefined || init.type
              ? { type: init.type ?? 'offer', sdp: sdpPart ?? 'v=0 OFFER_STUB' }
              : this.localDescription
        }

        async setRemoteDescription(): Promise<void> {}

        addTrack(): void {}

        close(): void {}
      } as unknown as typeof RTCPeerConnection,
    )

    Object.defineProperty(globalThis.navigator, 'mediaDevices', {
      configurable: true,
      value: {
        getUserMedia: vi.fn().mockResolvedValue({
          getTracks: () => [{ stop: vi.fn() }],
        }),
      },
    })

    vi.spyOn(document, 'createElement').mockImplementation((tag) => {
      if (String(tag) !== 'audio') {
        throw new Error(`unexpected createElement(${String(tag)})`)
      }

      const el = {
        autoplay: false,
        pause: vi.fn(),
        play: vi.fn(),
        remove: vi.fn(),
        srcObject: null as MediaStream | MediaSource | Blob | File | null,
      }
      vi.mocked(el.play).mockResolvedValue(undefined)
      return el as unknown as HTMLAudioElement
    })
  })

  afterEach(() => {
    vi.unstubAllGlobals()
    vi.restoreAllMocks()
    Reflect.deleteProperty(globalThis.navigator, 'mediaDevices')
  })

  async function simulateChannelPayload(raw: string) {
    const ev = new MessageEvent('message', { data: raw })
    for (const listener of [...messageListeners]) {
      listener(ev)
    }
    await flushPromises()
  }

  it('starts idle with empty transcripts and exposes maxSessionMs', async () => {
    const { useRealtimeVoice } = await import('../useRealtimeVoice')
    const lang = ref<SpeechUiLang>('en')

    const scope = effectScope()
    let api!: ReturnType<typeof useRealtimeVoice>
    scope.run(() => {
      api = useRealtimeVoice(lang)
    })

    expect(api.connectionState.value).toBe('idle')
    expect(api.errorMessage.value).toBe('')
    expect(api.sessionNotice.value).toBe('')
    expect(api.assistantTranscript.value).toBe('')
    expect(api.userTranscript.value).toBe('')
    expect(api.maxSessionMs).toBe(SESSION_MAX_MS)

    scope.stop()
  })

  it('enters connected state and fires session_started analytics after SDP exchange succeeds', async () => {
    const { useRealtimeVoice } = await import('../useRealtimeVoice')
    const { captureProductAnalyticsEvent } = await import('@/lib/analytics')

    const lang = ref<SpeechUiLang>('en')
    const scope = effectScope()
    let api!: ReturnType<typeof useRealtimeVoice>
    scope.run(() => {
      api = useRealtimeVoice(lang)
    })

    const connectPromise = api.connect()
    await flushPromises()
    latestRtc?.dispatchRemoteTrack()
    await connectPromise

    expect(api.connectionState.value).toBe('connected')
    expect(captureProductAnalyticsEvent).toHaveBeenCalledWith('portfolio_voice_session_started', {
      language: 'en',
    })

    api.disconnect()

    scope.stop()
  })

  it('captures SDP exchange failures as errors without leaving the UI connected', async () => {
    exchangeRealtimeSdpMock.mockResolvedValue({
      ok: false,
      status: 503,
      message: '',
    })

    const { useRealtimeVoice } = await import('../useRealtimeVoice')
    const { captureProductAnalyticsEvent } = await import('@/lib/analytics')

    const lang = ref<SpeechUiLang>('en')
    const scope = effectScope()
    let api!: ReturnType<typeof useRealtimeVoice>
    scope.run(() => {
      api = useRealtimeVoice(lang)
    })

    await api.connect()
    await flushPromises()

    expect(api.connectionState.value).toBe('error')
    expect(captureProductAnalyticsEvent).toHaveBeenCalledWith('portfolio_voice_session_error', {
      message: expect.any(String),
      language: 'en',
    })

    scope.stop()
  })

  it('maps getUserMedia failures to localized errors', async () => {
    vi.mocked(navigator.mediaDevices!.getUserMedia).mockRejectedValue(new Error('denied'))

    const { useRealtimeVoice } = await import('../useRealtimeVoice')

    const no = ref<SpeechUiLang>('no')
    const scopeNo = effectScope()
    let noApi!: ReturnType<typeof useRealtimeVoice>
    scopeNo.run(() => {
      noApi = useRealtimeVoice(no)
    })
    await noApi.connect()

    expect(noApi.connectionState.value).toBe('error')
    expect(noApi.errorMessage.value).toBe('denied')

    scopeNo.stop()
  })

  it('shows Norwegian fallback when browsers lack WebRTC', async () => {
    vi.unstubAllGlobals()
    vi.stubGlobal('RTCPeerConnection', undefined)

    const imported = await import('../useRealtimeVoice')

    const no = ref<SpeechUiLang>('no')
    const scope = effectScope()
    let api!: ReturnType<typeof imported.useRealtimeVoice>
    scope.run(() => {
      api = imported.useRealtimeVoice(no)
    })

    await api.connect()

    expect(api.connectionState.value).toBe('error')
    expect(api.errorMessage.value).toBe('Nettleseren støtter ikke WebRTC.')

    scope.stop()
  })

  it('parses OAI event channel transcripts', async () => {
    const { useRealtimeVoice } = await import('../useRealtimeVoice')
    const lang = ref<SpeechUiLang>('en')
    const scope = effectScope()
    let api!: ReturnType<typeof useRealtimeVoice>
    scope.run(() => {
      api = useRealtimeVoice(lang)
    })

    const connectPromise = api.connect()
    await flushPromises()
    latestRtc?.dispatchRemoteTrack()
    await connectPromise

    await simulateChannelPayload(
      JSON.stringify({ type: 'response.output_audio_transcript.delta', delta: 'hi ' }),
    )
    await simulateChannelPayload(
      JSON.stringify({ type: 'conversation.item.input_audio_transcription.delta', delta: 'u' }),
    )

    expect(api.assistantTranscript.value).toContain('hi')
    expect(api.userTranscript.value).toContain('u')

    api.disconnect()

    scope.stop()
  })

  it('schedules a session timeout aligned with REALTIME_SESSION_MAX_MS', async () => {
    const originalSetTimeout = globalThis.setTimeout
    vi.spyOn(globalThis, 'setTimeout').mockImplementation(((handler: TimerHandler, delay?: number) => {
      if (delay === SESSION_MAX_MS) {
        queueMicrotask(() => (handler as (...args: unknown[]) => void)())
        return 0 as unknown as ReturnType<typeof setTimeout>
      }
      return originalSetTimeout(handler, delay ?? 0)
    }) as typeof setTimeout)

    const { useRealtimeVoice } = await import('../useRealtimeVoice')
    const en = ref<SpeechUiLang>('en')
    const scope = effectScope()
    let api!: ReturnType<typeof useRealtimeVoice>
    scope.run(() => {
      api = useRealtimeVoice(en)
    })

    const connectPromise = api.connect()
    await flushPromises()
    latestRtc?.dispatchRemoteTrack()
    await connectPromise

    expect(api.connectionState.value).toBe('idle')
    expect(api.sessionNotice.value.toLowerCase()).toContain('3')

    scope.stop()
  })

  it('disconnect clears notice on normal end', async () => {
    const { useRealtimeVoice } = await import('../useRealtimeVoice')
    const en = ref<SpeechUiLang>('en')
    const scope = effectScope()
    let api!: ReturnType<typeof useRealtimeVoice>
    scope.run(() => {
      api = useRealtimeVoice(en)
    })

    api.sessionNotice.value = 'should clear'

    const connectPromise = api.connect()
    await flushPromises()
    latestRtc?.dispatchRemoteTrack()
    await connectPromise

    api.disconnect()

    expect(api.connectionState.value).toBe('idle')
    expect(api.sessionNotice.value).toBe('')

    scope.stop()
  })

  it('skips reconnect when already connected', async () => {
    const { useRealtimeVoice } = await import('../useRealtimeVoice')
    const en = ref<SpeechUiLang>('en')
    const scope = effectScope()
    let api!: ReturnType<typeof useRealtimeVoice>
    scope.run(() => {
      api = useRealtimeVoice(en)
    })

    exchangeRealtimeSdpMock.mockClear()

    const first = api.connect()
    await flushPromises()
    latestRtc?.dispatchRemoteTrack()
    await first

    exchangeRealtimeSdpMock.mockClear()
    await api.connect()

    expect(exchangeRealtimeSdpMock).not.toHaveBeenCalled()

    scope.stop()
  })

  it('unmount invokes disconnect cleanup when connecting', async () => {
    const { useRealtimeVoice } = await import('../useRealtimeVoice')
    exchangeRealtimeSdpMock.mockImplementation(
      () => new Promise<{ ok: true; answerSdp: string }>(() => {}),
    )

    const en = ref<SpeechUiLang>('en')

    let api!: ReturnType<typeof useRealtimeVoice>
    const scope = effectScope()
    scope.run(() => {
      api = useRealtimeVoice(en)
    })

    void api.connect()
    await flushPromises()
    scope.stop()

    expect(api.connectionState.value).not.toBe('connected')
  })
})
