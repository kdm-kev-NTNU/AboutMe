import { beforeEach, describe, expect, it, vi } from 'vitest'

const mockCustomFetch = vi.hoisted(() => vi.fn())

vi.mock('@/api/orval-mutator', () => ({
  customFetch: mockCustomFetch,
}))

describe('realtime-voice', () => {
  beforeEach(() => {
    vi.resetModules()
    mockCustomFetch.mockReset()
  })

  it('fetchRealtimeVoiceEnabled is true only when backend returns enabled true JSON', async () => {
    mockCustomFetch.mockResolvedValue({
      status: 200,
      data: { enabled: true },
    })

    const { fetchRealtimeVoiceEnabled } = await import('../realtime-voice')
    await expect(fetchRealtimeVoiceEnabled()).resolves.toBe(true)

    mockCustomFetch.mockResolvedValueOnce({
      status: 200,
      data: { enabled: false },
    })
    await expect(fetchRealtimeVoiceEnabled()).resolves.toBe(false)
  })

  it('fetchRealtimeVoiceEnabled is false when status is non-200', async () => {
    mockCustomFetch.mockResolvedValue({ status: 503, data: { enabled: true } })
    const { fetchRealtimeVoiceEnabled } = await import('../realtime-voice')
    await expect(fetchRealtimeVoiceEnabled()).resolves.toBe(false)
  })

  it('fetchRealtimeVoiceEnabled is false when body is invalid', async () => {
    mockCustomFetch.mockResolvedValue({ status: 200, data: null })
    const { fetchRealtimeVoiceEnabled } = await import('../realtime-voice')
    await expect(fetchRealtimeVoiceEnabled()).resolves.toBe(false)

    mockCustomFetch.mockResolvedValueOnce({ status: 200, data: 'oops' })
    await expect(fetchRealtimeVoiceEnabled()).resolves.toBe(false)
  })

  it('exchangeRealtimeSdp returns SDP answer when customFetch resolves 2xx with string body', async () => {
    mockCustomFetch.mockResolvedValue({
      status: 201,
      data: 'v=0 SDP_ANSWER',
    })
    const { exchangeRealtimeSdp } = await import('../realtime-voice')

    await expect(exchangeRealtimeSdp('offer', 'en')).resolves.toEqual({
      ok: true,
      answerSdp: 'v=0 SDP_ANSWER',
    })

    expect(mockCustomFetch).toHaveBeenCalledWith('/realtime/session', expect.any(Object))

    const init = mockCustomFetch.mock.calls[0][1] as RequestInit & { headers: Record<string, string> }
    expect(init.method).toBe('POST')
    expect(init.headers['Content-Type']).toBe('application/sdp')
    expect(init.headers['X-Chat-Language']).toBe('en')
    expect(init.body).toBe('offer')
  })

  it('exchangeRealtimeSdp returns failure with parsed error JSON when backend sends ApiError', async () => {
    mockCustomFetch.mockResolvedValue({
      status: 503,
      data: { error: 'offline' },
    })
    const { exchangeRealtimeSdp } = await import('../realtime-voice')

    await expect(exchangeRealtimeSdp('offer', 'no')).resolves.toEqual({
      ok: false,
      status: 503,
      message: 'offline',
    })
  })

  it('exchangeRealtimeSdp surfaces HTTP code when backend error body lacks error field', async () => {
    mockCustomFetch.mockResolvedValue({
      status: 400,
      data: {},
    })
    const { exchangeRealtimeSdp } = await import('../realtime-voice')

    await expect(exchangeRealtimeSdp('offer', 'en')).resolves.toMatchObject({
      ok: false,
      status: 400,
      message: 'HTTP 400',
    })
  })

  it('REALTIME_SESSION_MAX_MS is three minutes', async () => {
    const { REALTIME_SESSION_MAX_MS } = await import('../realtime-voice')
    expect(REALTIME_SESSION_MAX_MS).toBe(180000)
  })
})
