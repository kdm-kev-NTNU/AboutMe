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
})
