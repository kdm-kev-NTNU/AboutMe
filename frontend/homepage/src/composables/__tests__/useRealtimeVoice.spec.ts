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
type CloseListener = (ev: Event) => void

describe('useRealtimeVoice', () => {
  let latestRtc: { dispatchRemoteTrack(): void; failConnection(): void; fireDataChannelClose(): void } | null =
    null
  let messageListeners: MessageListener[] = []
  let closeListeners: CloseListener[] = []

  beforeEach(async () => {
    vi.restoreAllMocks()
    vi.unstubAllGlobals()

    exchangeRealtimeSdpMock.mockReset()
    messageListeners = []
    closeListeners = []
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

      addEventListener(type: string, listener: EventListenerOrEventListenerObject | null): void {
        if (!listener || typeof listener !== 'function') return
        if (type === 'message') {
          messageListeners.push(listener as MessageListener)
        }
        if (type === 'close') {
          closeListeners.push(listener as CloseListener)
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
      class StubRtc {
        localDescription: any = undefined
        readonly iceGatheringState: RTCIceGatheringState = 'complete'
        ontrack:
          | ((this: RTCPeerConnection, ev: RTCTrackEvent) => void)
          | null = null
        onconnectionstatechange: ((this: RTCPeerConnection, ev: Event) => void) | null = null
        private _connectionState: RTCPeerConnectionState = 'new'

        get connectionState(): RTCPeerConnectionState {
          return this._connectionState
        }

        constructor() {
          const self = this
          latestRtc = {
            dispatchRemoteTrack: () => {
              const fakeStream = new MediaStream([])
              const ev = new Event('track') as unknown as RTCTrackEvent
              Object.assign(ev, { streams: [fakeStream], track: {} })
              const handler = self.ontrack
              if (handler) {
                handler.call(self as any, ev as RTCTrackEvent)
              }
            },
            failConnection: () => {
              self._connectionState = 'failed'
              const h = self.onconnectionstatechange
              if (h) {
                h.call(self as any, new Event('connectionstatechange'))
              }
            },
            fireDataChannelClose: () => {
              const ev = new Event('close')
              for (const l of [...closeListeners]) {
                l(ev)
              }
            },
          }
        }

        createDataChannel(): RTCDataChannel {
          return new StubRtcDataChannel() as unknown as RTCDataChannel
        }

        async createOffer() {
          return { type: 'offer' as const, sdp: 'v=0 OFFER_STUB' }
        }

        async setLocalDescription(init: RTCLocalSessionDescriptionInit) {
          const sdpPart = typeof init.sdp === 'string' ? init.sdp : undefined
          this.localDescription =
            sdpPart !== undefined || init.type
              ? { type: init.type ?? 'offer', sdp: sdpPart ?? 'v=0 OFFER_STUB' }
              : this.localDescription
        }

        async setRemoteDescription(): Promise<void> {
          this._connectionState = 'connected'
        }

        addTrack(): any {}

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
    expect(api.errorMessage.value).toBe('Could not start voice session.')
    expect(captureProductAnalyticsEvent).toHaveBeenCalledWith('portfolio_voice_session_error', {
      message: expect.any(String),
      language: 'en',
    })

    scope.stop()
  })

  it('maps RATE_LIMITED with retry-after to localized copy', async () => {
    exchangeRealtimeSdpMock.mockResolvedValue({
      ok: false,
      status: 429,
      message: '',
      code: 'RATE_LIMITED',
      retryAfterSeconds: 33,
    })

    const { useRealtimeVoice } = await import('../useRealtimeVoice')
    const en = ref<SpeechUiLang>('en')
    const scope = effectScope()
    let api!: ReturnType<typeof useRealtimeVoice>
    scope.run(() => {
      api = useRealtimeVoice(en)
    })

    await api.connect()
    await flushPromises()

    expect(api.errorMessage.value).toContain('33')
    expect(api.errorMessage.value.toLowerCase()).toContain('seconds')

    scope.stop()
  })

  it('maps RATE_LIMITED without retry-after to generic rate limit copy', async () => {
    exchangeRealtimeSdpMock.mockResolvedValue({
      ok: false,
      status: 429,
      message: '',
      code: 'RATE_LIMITED',
    })

    const { useRealtimeVoice } = await import('../useRealtimeVoice')
    const no = ref<SpeechUiLang>('no')
    const scope = effectScope()
    let api!: ReturnType<typeof useRealtimeVoice>
    scope.run(() => {
      api = useRealtimeVoice(no)
    })

    await api.connect()
    await flushPromises()

    expect(api.errorMessage.value).toContain('Vent')

    scope.stop()
  })

  it('maps CIRCUIT_OPEN and OPENAI_SERVER_ERROR to localized copy', async () => {
    exchangeRealtimeSdpMock.mockResolvedValueOnce({
      ok: false,
      status: 503,
      message: '',
      code: 'CIRCUIT_OPEN',
    })

    const first = await import('../useRealtimeVoice')
    const lang = ref<SpeechUiLang>('no')
    const s1 = effectScope()
    let api1!: ReturnType<typeof first.useRealtimeVoice>
    s1.run(() => {
      api1 = first.useRealtimeVoice(lang)
    })
    await api1.connect()
    await flushPromises()
    expect(api1.errorMessage.value).toContain('utilgjengelig')
    s1.stop()

    exchangeRealtimeSdpMock.mockResolvedValueOnce({
      ok: false,
      status: 502,
      message: '',
      code: 'OPENAI_SERVER_ERROR',
    })
    const second = await import('../useRealtimeVoice')
    const s2 = effectScope()
    let api2!: ReturnType<typeof second.useRealtimeVoice>
    s2.run(() => {
      api2 = second.useRealtimeVoice(ref<SpeechUiLang>('en'))
    })
    await api2.connect()
    await flushPromises()
    expect(api2.errorMessage.value.toLowerCase()).toContain('please try again')
    s2.stop()
  })

  it('maps NotFoundError from getUserMedia to English copy', async () => {
    vi.mocked(navigator.mediaDevices!.getUserMedia).mockRejectedValue(
      new DOMException('none', 'NotFoundError'),
    )

    const { useRealtimeVoice } = await import('../useRealtimeVoice')
    const en = ref<SpeechUiLang>('en')
    const scope = effectScope()
    let api!: ReturnType<typeof useRealtimeVoice>
    scope.run(() => {
      api = useRealtimeVoice(en)
    })
    await api.connect()
    await flushPromises()

    expect(api.errorMessage.value).toContain('No microphone found')

    scope.stop()
  })

  it('maps BUDGET_EXCEEDED to localized copy', async () => {
    exchangeRealtimeSdpMock.mockResolvedValue({
      ok: false,
      status: 429,
      message: 'Daily cap',
      code: 'BUDGET_EXCEEDED',
    })

    const { useRealtimeVoice } = await import('../useRealtimeVoice')
    const no = ref<SpeechUiLang>('no')
    const scope = effectScope()
    let api!: ReturnType<typeof useRealtimeVoice>
    scope.run(() => {
      api = useRealtimeVoice(no)
    })

    await api.connect()
    await flushPromises()

    expect(api.connectionState.value).toBe('error')
    expect(api.errorMessage.value).toContain('budsjett')

    scope.stop()
  })

  it('maps OPENAI_REJECTED to English copy with detail', async () => {
    exchangeRealtimeSdpMock.mockResolvedValue({
      ok: false,
      status: 502,
      message: 'OpenAI rejected the session: invalid model xyz',
      code: 'OPENAI_REJECTED',
    })

    const { useRealtimeVoice } = await import('../useRealtimeVoice')
    const en = ref<SpeechUiLang>('en')
    const scope = effectScope()
    let api!: ReturnType<typeof useRealtimeVoice>
    scope.run(() => {
      api = useRealtimeVoice(en)
    })

    await api.connect()
    await flushPromises()

    expect(api.errorMessage.value).toContain('invalid model xyz')

    scope.stop()
  })

  it('maps getUserMedia failures to localized errors', async () => {
    vi.mocked(navigator.mediaDevices!.getUserMedia).mockRejectedValue(
      new DOMException('blocked', 'NotAllowedError'),
    )

    const { useRealtimeVoice } = await import('../useRealtimeVoice')

    const no = ref<SpeechUiLang>('no')
    const scopeNo = effectScope()
    let noApi!: ReturnType<typeof useRealtimeVoice>
    scopeNo.run(() => {
      noApi = useRealtimeVoice(no)
    })
    await noApi.connect()

    expect(noApi.connectionState.value).toBe('error')
    expect(noApi.errorMessage.value).toContain('Mikrofontilgang')

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

  it('surfaces WebRTC connection failure after connect', async () => {
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

    latestRtc?.failConnection()
    await flushPromises()

    expect(api.connectionState.value).toBe('error')
    expect(api.errorMessage.value).toContain('Voice connection failed')
    expect(captureProductAnalyticsEvent).toHaveBeenCalledWith(
      'portfolio_voice_session_error',
      expect.objectContaining({ reason: 'mid_session' }),
    )

    scope.stop()
  })

  it('surfaces data channel close as interrupted session', async () => {
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

    latestRtc?.fireDataChannelClose()
    await flushPromises()

    expect(api.connectionState.value).toBe('error')
    expect(api.errorMessage.value).toContain('interrupted')

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
