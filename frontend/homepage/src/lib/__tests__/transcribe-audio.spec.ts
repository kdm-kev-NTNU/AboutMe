import { beforeEach, describe, expect, it, vi } from 'vitest'

const { mockCustomFetch } = vi.hoisted(() => ({
  mockCustomFetch: vi.fn(),
}))

vi.mock('@/api/orval-mutator', () => ({
  customFetch: mockCustomFetch,
}))

describe('transcribeSpeech', () => {
  beforeEach(() => {
    vi.resetModules()
    mockCustomFetch.mockReset()
  })

  it('posts FormData audio blob to /transcribe via customFetch', async () => {
    mockCustomFetch.mockResolvedValue({
      data: { text: 'hello' },
      status: 200,
      headers: new Headers({ 'Content-Type': 'application/json' }),
    })
    const { transcribeSpeech } = await import('../transcribe-audio')
    const blob = new Blob(['bogus'], { type: 'audio/webm' })

    await expect(transcribeSpeech(blob)).resolves.toEqual({
      data: { text: 'hello' },
      status: 200,
      headers: expect.any(Headers),
    })

    expect(mockCustomFetch).toHaveBeenCalledTimes(1)
    expect(mockCustomFetch).toHaveBeenCalledWith(
      '/transcribe',
      expect.objectContaining({
        method: 'POST',
        body: expect.any(FormData),
      }),
    )

    const body = mockCustomFetch.mock.calls[0][1] as RequestInit & { body: FormData }
    expect(body.body.get('file')).toBeTruthy()
  })

  it('uses "recording.webm" as the multipart filename', async () => {
    mockCustomFetch.mockResolvedValue({
      data: { text: '' },
      status: 200,
      headers: new Headers(),
    })
    const { transcribeSpeech } = await import('../transcribe-audio')
    const blob = new Blob(['x'], { type: 'audio/webm' })

    await transcribeSpeech(blob)

    const body = mockCustomFetch.mock.calls[0][1] as RequestInit & { body: FormData }
    const file = body.body.get('file')
    // The browser exposes the filename as a property on the FormData entry; in tests, casting to File works.
    expect((file as File).name).toBe('recording.webm')
  })

  it('does NOT manually set Content-Type so the browser supplies the multipart boundary', async () => {
    mockCustomFetch.mockResolvedValue({
      data: { text: '' },
      status: 200,
      headers: new Headers(),
    })
    const { transcribeSpeech } = await import('../transcribe-audio')
    const blob = new Blob(['x'], { type: 'audio/webm' })

    await transcribeSpeech(blob)

    const init = mockCustomFetch.mock.calls[0][1] as RequestInit & {
      headers: Record<string, string>
    }
    // Headers must be a plain object (per impl) and must NOT contain Content-Type — browsers set
    // it automatically with the random boundary token for FormData bodies.
    expect(init.headers).toBeDefined()
    const keys = Object.keys(init.headers).map((k) => k.toLowerCase())
    expect(keys).not.toContain('content-type')
  })

  it('passes language as X-Chat-Language header when provided', async () => {
    mockCustomFetch.mockResolvedValue({
      data: { text: 'hi' },
      status: 200,
      headers: new Headers(),
    })
    const { transcribeSpeech } = await import('../transcribe-audio')
    const blob = new Blob(['x'], { type: 'audio/webm' })

    await transcribeSpeech(blob, 'no')

    const init = mockCustomFetch.mock.calls[0][1] as RequestInit & {
      headers: Record<string, string>
    }
    expect(init.headers['X-Chat-Language']).toBe('no')
  })

  it('omits X-Chat-Language when language is not supplied', async () => {
    mockCustomFetch.mockResolvedValue({
      data: { text: 'hi' },
      status: 200,
      headers: new Headers(),
    })
    const { transcribeSpeech } = await import('../transcribe-audio')
    const blob = new Blob(['x'], { type: 'audio/webm' })

    await transcribeSpeech(blob)

    const init = mockCustomFetch.mock.calls[0][1] as RequestInit & {
      headers: Record<string, string>
    }
    expect(init.headers['X-Chat-Language']).toBeUndefined()
  })

  it('returns error responses unchanged from customFetch (no try/catch swallowing)', async () => {
    mockCustomFetch.mockResolvedValue({
      data: { error: 'boom' },
      status: 500,
      headers: new Headers(),
    })
    const { transcribeSpeech } = await import('../transcribe-audio')
    const blob = new Blob(['x'], { type: 'audio/webm' })

    const r = await transcribeSpeech(blob, 'en')
    expect(r.status).toBe(500)
    expect(r.data).toEqual({ error: 'boom' })
  })
})
