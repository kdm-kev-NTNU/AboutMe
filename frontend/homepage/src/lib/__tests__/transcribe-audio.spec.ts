import { afterEach, describe, expect, it, vi } from 'vitest'
import * as mutator from '@/api/orval-mutator'
import { transcribeSpeech } from '../transcribe-audio'

describe('transcribeSpeech', () => {
  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('posts FormData with recording.webm to /transcribe via customFetch', async () => {
    vi.spyOn(mutator, 'customFetch').mockResolvedValue({
      data: { text: 'ok' },
      status: 200,
      headers: new Headers(),
    })

    const blob = new Blob(['x'], { type: 'audio/webm' })
    const result = await transcribeSpeech(blob)

    expect(mutator.customFetch).toHaveBeenCalledWith(
      '/transcribe',
      expect.objectContaining({
        method: 'POST',
        body: expect.any(FormData),
      }),
    )
    const init = vi.mocked(mutator.customFetch).mock.calls[0]![1] as RequestInit
    const fd = init.body as FormData
    const file = fd.get('file')
    expect(file).toBeInstanceOf(File)
    expect((file as File).name).toBe('recording.webm')

    expect(result.status).toBe(200)
    expect(result.data).toEqual({ text: 'ok' })
  })
})
