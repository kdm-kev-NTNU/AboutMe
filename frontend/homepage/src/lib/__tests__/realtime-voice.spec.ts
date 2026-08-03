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

  it('fetchRealtimeVoiceStatus parses curated choices and defaults', async () => {
    mockCustomFetch.mockResolvedValue({
      status: 200,
      data: {
        enabled: true,
        voices: ['marin', 'cedar', 'alloy'],
        reasoningEfforts: ['low', 'medium', 'high', 'xhigh'],
        defaultVoice: 'cedar',
        defaultReasoningEffort: 'medium',
      },
    })

    const { fetchRealtimeVoiceStatus } = await import('../realtime-voice')
    await expect(fetchRealtimeVoiceStatus()).resolves.toEqual({
      enabled: true,
      liveEnabled: false,
      voices: ['marin', 'cedar'],
      reasoningEfforts: ['low', 'medium', 'high'],
      vadEagernessOptions: ['low', 'medium', 'high', 'auto'],
      voice: 'cedar',
      reasoningEffort: 'medium',
      vadEagerness: 'low',
    })
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
    expect(init.headers['X-Realtime-Voice']).toBe('marin')
    expect(init.headers['X-Realtime-Reasoning-Effort']).toBe('low')
    expect(init.headers['X-Realtime-Vad-Eagerness']).toBe('low')
    expect(init.body).toBe('offer')
  })

  it('exchangeRealtimeSdp sends selected voice and reasoning headers', async () => {
    mockCustomFetch.mockResolvedValue({
      status: 201,
      data: 'v=0 SDP_ANSWER',
    })
    const { exchangeRealtimeSdp } = await import('../realtime-voice')

    await exchangeRealtimeSdp(
      'offer',
      'no',
      { voice: 'cedar', reasoningEffort: 'high', vadEagerness: 'high' },
      'gpt-realtime-2',
    )

    const init = mockCustomFetch.mock.calls[0][1] as RequestInit & { headers: Record<string, string> }
    expect(init.headers['X-Chat-Language']).toBe('no')
    expect(init.headers['X-Realtime-Voice']).toBe('cedar')
    expect(init.headers['X-Realtime-Reasoning-Effort']).toBe('high')
    expect(init.headers['X-Realtime-Vad-Eagerness']).toBe('high')
    expect(init.headers['X-Realtime-Model']).toBe('gpt-realtime-2')
  })

  it('fetchRealtimeVoiceModels validates provider catalog rows', async () => {
    mockCustomFetch.mockResolvedValue({
      status: 200,
      data: [
        { provider: 'OPENAI', id: 'gpt-realtime-2', label: 'OpenAI GPT-Realtime-2', defaultOption: true },
        { provider: 'BAD', id: 'ignored', label: 'Ignored', defaultOption: false },
      ],
    })
    const { fetchRealtimeVoiceModels } = await import('../realtime-voice')

    await expect(fetchRealtimeVoiceModels()).resolves.toEqual([
      { provider: 'OPENAI', id: 'gpt-realtime-2', label: 'OpenAI GPT-Realtime-2', defaultOption: true },
    ])
  })

  it('lookupRealtimeInfo returns validated snippets from backend', async () => {
    mockCustomFetch.mockResolvedValue({
      status: 200,
      data: {
        found: true,
        snippets: [
          { sourceType: 'profile', title: 'Data engineering', text: 'Kevin studies at NTNU.' },
          { sourceType: 'bad', title: 'Ignored', text: 'Ignored' },
        ],
      },
    })
    const { lookupRealtimeInfo } = await import('../realtime-voice')

    await expect(lookupRealtimeInfo('NTNU', 'en')).resolves.toEqual({
      found: true,
      confidence: 'high',
      snippets: [{ sourceType: 'profile', title: 'Data engineering', text: 'Kevin studies at NTNU.' }],
    })

    expect(mockCustomFetch).toHaveBeenCalledWith('/realtime/lookup', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ query: 'NTNU', language: 'en' }),
    })
  })

  it('lookupRealtimeInfo returns empty result on backend failure or invalid body', async () => {
    mockCustomFetch.mockResolvedValue({ status: 503, data: { error: 'offline' } })
    const { lookupRealtimeInfo } = await import('../realtime-voice')

    await expect(lookupRealtimeInfo('NTNU', 'no')).resolves.toEqual({
      found: false,
      snippets: [],
      confidence: 'none',
    })

    mockCustomFetch.mockResolvedValueOnce({ status: 200, data: { found: true, snippets: [] } })
    await expect(lookupRealtimeInfo('NTNU', 'no')).resolves.toEqual({
      found: false,
      snippets: [],
      confidence: 'none',
    })
  })

  it('exchangeRealtimeSdp returns failure with parsed error JSON when backend sends ApiError', async () => {
    mockCustomFetch.mockResolvedValue({
      status: 503,
      data: { error: 'offline', code: 'CIRCUIT_OPEN' },
    })
    const { exchangeRealtimeSdp } = await import('../realtime-voice')

    await expect(exchangeRealtimeSdp('offer', 'no')).resolves.toEqual({
      ok: false,
      status: 503,
      message: 'offline',
      code: 'CIRCUIT_OPEN',
    })
  })

  it('exchangeRealtimeSdp passes Retry-After as retryAfterSeconds', async () => {
    mockCustomFetch.mockResolvedValue({
      status: 429,
      data: { error: 'Too many', code: 'RATE_LIMITED' },
      headers: new Headers({ 'Retry-After': '120' }),
    })
    const { exchangeRealtimeSdp } = await import('../realtime-voice')

    await expect(exchangeRealtimeSdp('offer', 'en')).resolves.toEqual({
      ok: false,
      status: 429,
      message: 'Too many',
      code: 'RATE_LIMITED',
      retryAfterSeconds: 120,
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

  it('REALTIME_SESSION_MAX_MS is five minutes', async () => {
    const { REALTIME_SESSION_MAX_MS, INTERVIEW_REALTIME_SESSION_MAX_MS } = await import('../realtime-voice')
    expect(REALTIME_SESSION_MAX_MS).toBe(300000)
    expect(INTERVIEW_REALTIME_SESSION_MAX_MS).toBe(1_800_000)
  })
})
