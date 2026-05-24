import { beforeEach, describe, expect, it, vi } from 'vitest'
import { synthesizeSpeech } from '../synthesize-speech'

describe('synthesizeSpeech', () => {
  beforeEach(() => {
    vi.restoreAllMocks()
  })

  it('returns audio blob on success', async () => {
    const blob = new Blob(['audio'], { type: 'audio/mpeg' })
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue({
        ok: true,
        blob: () => Promise.resolve(blob),
      }),
    )

    const result = await synthesizeSpeech('Hello', 'en')
    expect(result).toEqual({ ok: true, blob })
    expect(fetch).toHaveBeenCalledWith('/api/synthesize', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'X-Chat-Language': 'en',
      },
      credentials: 'include',
      body: JSON.stringify({ text: 'Hello' }),
    })
  })

  it('returns API error message when synthesis fails', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue({
        ok: false,
        status: 503,
        json: () => Promise.resolve({ error: 'Speech synthesis is temporarily unavailable.' }),
      }),
    )

    const result = await synthesizeSpeech('Hello', 'no')
    expect(result).toEqual({
      ok: false,
      status: 503,
      message: 'Speech synthesis is temporarily unavailable.',
    })
  })

  it('returns network error when fetch throws', async () => {
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new Error('offline')))

    const result = await synthesizeSpeech('Hello', 'en')
    expect(result).toEqual({ ok: false, status: 0, message: 'Network error' })
  })

  it('falls back to HTTP status when error body is not JSON', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue({
        ok: false,
        status: 500,
        json: () => Promise.reject(new Error('not json')),
      }),
    )

    const result = await synthesizeSpeech('Hello', 'en')
    expect(result).toEqual({ ok: false, status: 500, message: 'HTTP 500' })
  })
})
