import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as portfolioApi from '@/api/generated/portfolio'
import type { synthesizeSpeechResponse } from '@/api/generated/portfolio'
import { synthesizeSpeech } from '../synthesize-speech'

vi.mock('@/api/generated/portfolio', async (importOriginal) => {
  const actual = await importOriginal<typeof portfolioApi>()
  return { ...actual, synthesizeSpeech: vi.fn() }
})

describe('synthesizeSpeech', () => {
  beforeEach(() => {
    vi.mocked(portfolioApi.synthesizeSpeech).mockReset()
  })

  it('returns audio blob on success', async () => {
    const blob = new Blob(['audio'], { type: 'audio/mpeg' })
    vi.mocked(portfolioApi.synthesizeSpeech).mockResolvedValue({
      status: 200,
      data: blob,
      headers: new Headers(),
    })

    const result = await synthesizeSpeech('Hello', 'en')
    expect(result).toEqual({ ok: true, blob })
    expect(portfolioApi.synthesizeSpeech).toHaveBeenCalledWith(
      { text: 'Hello' },
      expect.objectContaining({
        headers: { 'X-Chat-Language': 'en' },
      }),
    )
  })

  it('returns API error message when synthesis fails', async () => {
    vi.mocked(portfolioApi.synthesizeSpeech).mockResolvedValue({
      status: 503,
      data: { error: 'Speech synthesis is temporarily unavailable.' },
      headers: new Headers(),
    } as unknown as synthesizeSpeechResponse)

    const result = await synthesizeSpeech('Hello', 'no')
    expect(result).toEqual({
      ok: false,
      status: 503,
      message: 'Speech synthesis is temporarily unavailable.',
    })
  })

  it('returns network error when API throws', async () => {
    vi.mocked(portfolioApi.synthesizeSpeech).mockRejectedValue(new Error('offline'))

    const result = await synthesizeSpeech('Hello', 'en')
    expect(result).toEqual({ ok: false, status: 0, message: 'Network error' })
  })

  it('falls back to HTTP status when error body is not JSON', async () => {
    vi.mocked(portfolioApi.synthesizeSpeech).mockResolvedValue({
      status: 500,
      data: 'not json',
      headers: new Headers(),
    } as unknown as synthesizeSpeechResponse)

    const result = await synthesizeSpeech('Hello', 'en')
    expect(result).toEqual({ ok: false, status: 500, message: 'HTTP 500' })
  })
})
