import { beforeEach, describe, expect, it, vi } from 'vitest'

const mockCustomFetch = vi.hoisted(() => vi.fn())

vi.mock('@/api/orval-mutator', () => ({
  customFetch: mockCustomFetch,
}))

describe('interview-voice', () => {
  beforeEach(() => {
    vi.resetModules()
    mockCustomFetch.mockReset()
  })

  it('uploadInterviewDocument posts FormData and returns document', async () => {
    const doc = {
      id: 'doc1',
      originalFilename: 'cv.pdf',
      mimeType: 'application/pdf',
      charCount: 42,
      createdAt: '2026-01-01T00:00:00Z',
    }
    mockCustomFetch.mockResolvedValue({ status: 200, data: doc })

    const { uploadInterviewDocument } = await import('../interview-voice')
    const file = new File(['hello'], 'cv.pdf', { type: 'application/pdf' })

    await expect(uploadInterviewDocument(file)).resolves.toEqual(doc)

    expect(mockCustomFetch).toHaveBeenCalledWith('/admin/tools/interview/documents', {
      method: 'POST',
      body: expect.any(FormData),
    })
  })

  it('uploadInterviewDocument throws on non-200', async () => {
    mockCustomFetch.mockResolvedValue({ status: 400, data: {} })
    const { uploadInterviewDocument } = await import('../interview-voice')
    const file = new File(['x'], 'cv.pdf')

    await expect(uploadInterviewDocument(file)).rejects.toThrow('Upload failed (400)')
  })

  it('createInterviewTextDocument posts JSON body', async () => {
    const doc = {
      id: 'doc2',
      originalFilename: 'notes.md',
      charCount: 10,
      createdAt: '2026-01-01T00:00:00Z',
    }
    mockCustomFetch.mockResolvedValue({ status: 200, data: doc })

    const { createInterviewTextDocument } = await import('../interview-voice')
    await expect(createInterviewTextDocument('hello world', 'notes.md')).resolves.toEqual(doc)

    expect(mockCustomFetch).toHaveBeenCalledWith('/admin/tools/interview/documents/text', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ text: 'hello world', filename: 'notes.md' }),
    })
  })

  it('createInterviewSession posts documentId language and voice', async () => {
    const session = {
      id: 'sess1',
      documentId: 'doc1',
      language: 'no',
      status: 'ACTIVE',
      startedAt: '2026-01-01T00:00:00Z',
    }
    mockCustomFetch.mockResolvedValue({ status: 200, data: session })

    const { createInterviewSession } = await import('../interview-voice')
    await expect(createInterviewSession('doc1', 'no', 'cedar')).resolves.toEqual(session)

    expect(mockCustomFetch).toHaveBeenCalledWith('/admin/tools/interview/sessions', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ documentId: 'doc1', language: 'no', voice: 'cedar' }),
    })
  })

  it('appendInterviewTurns posts turn batch', async () => {
    mockCustomFetch.mockResolvedValue({ status: 200 })
    const { appendInterviewTurns } = await import('../interview-voice')

    await appendInterviewTurns('sess1', [
      { role: 'interviewer', text: 'Hello?', sequenceNo: 0 },
      { role: 'user', text: 'Hi', sequenceNo: 1 },
    ])

    expect(mockCustomFetch).toHaveBeenCalledWith('/admin/tools/interview/sessions/sess1/turns', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        turns: [
          { role: 'interviewer', text: 'Hello?', sequenceNo: 0 },
          { role: 'user', text: 'Hi', sequenceNo: 1 },
        ],
      }),
    })
  })

  it('finalizeInterviewTranscript returns transcript', async () => {
    const transcript = {
      id: 'tr1',
      sessionId: 'sess1',
      cleanStatus: 'PENDING',
      createdAt: '2026-01-01T00:00:00Z',
    }
    mockCustomFetch.mockResolvedValue({ status: 200, data: transcript })

    const { finalizeInterviewSession } = await import('../interview-voice')
    await expect(finalizeInterviewSession('sess1')).resolves.toEqual(transcript)
  })

  it('cleanInterviewTranscript returns cleaned transcript', async () => {
    const transcript = {
      id: 'tr1',
      sessionId: 'sess1',
      cleanedText: 'clean',
      cleanStatus: 'CLEANED',
      createdAt: '2026-01-01T00:00:00Z',
    }
    mockCustomFetch.mockResolvedValue({ status: 200, data: transcript })

    const { cleanInterviewTranscript } = await import('../interview-voice')
    await expect(cleanInterviewTranscript('tr1')).resolves.toEqual(transcript)
  })

  it('ingestInterviewTranscript appends force query when requested', async () => {
    mockCustomFetch.mockResolvedValue({ status: 200, data: { documentId: 'ing1' } })

    const { ingestInterviewTranscript } = await import('../interview-voice')
    await expect(ingestInterviewTranscript('tr1', true)).resolves.toEqual({ documentId: 'ing1' })

    expect(mockCustomFetch).toHaveBeenCalledWith(
      '/admin/tools/interview/transcripts/tr1/ingest?force=true',
      { method: 'POST' },
    )
  })

  it('exchangeInterviewRealtimeSdp returns SDP answer on success', async () => {
    mockCustomFetch.mockResolvedValue({ status: 201, data: 'v=0 SDP_ANSWER' })
    const { exchangeInterviewRealtimeSdp } = await import('../interview-voice')

    await expect(exchangeInterviewRealtimeSdp('sess1', 'offer', 'en')).resolves.toEqual({
      ok: true,
      answerSdp: 'v=0 SDP_ANSWER',
    })

    const init = mockCustomFetch.mock.calls[0][1] as RequestInit & { headers: Record<string, string> }
    expect(init.method).toBe('POST')
    expect(init.headers['Content-Type']).toBe('application/sdp')
    expect(init.headers['X-Chat-Language']).toBe('en')
    expect(init.headers['X-Realtime-Voice']).toBe('marin')
    expect(init.headers['X-Realtime-Reasoning-Effort']).toBe('low')
    expect(init.body).toBe('offer')
  })

  it('exchangeInterviewRealtimeSdp sends selected voice reasoning and model', async () => {
    mockCustomFetch.mockResolvedValue({ status: 200, data: 'v=0' })
    const { exchangeInterviewRealtimeSdp } = await import('../interview-voice')

    await exchangeInterviewRealtimeSdp(
      'sess1',
      'offer',
      'no',
      { voice: 'cedar', reasoningEffort: 'high' },
      'gpt-realtime-2',
    )

    const init = mockCustomFetch.mock.calls[0][1] as RequestInit & { headers: Record<string, string> }
    expect(init.headers['X-Chat-Language']).toBe('no')
    expect(init.headers['X-Realtime-Voice']).toBe('cedar')
    expect(init.headers['X-Realtime-Reasoning-Effort']).toBe('high')
    expect(init.headers['X-Realtime-Model']).toBe('gpt-realtime-2')
  })

  it('exchangeInterviewRealtimeSdp returns failure with parsed error', async () => {
    mockCustomFetch.mockResolvedValue({
      status: 503,
      data: { error: 'offline', code: 'CIRCUIT_OPEN' },
    })
    const { exchangeInterviewRealtimeSdp } = await import('../interview-voice')

    await expect(exchangeInterviewRealtimeSdp('sess1', 'offer', 'no')).resolves.toEqual({
      ok: false,
      status: 503,
      message: 'offline',
      code: 'CIRCUIT_OPEN',
    })
  })

  it('exchangeInterviewRealtimeSdp surfaces HTTP code when error body lacks error field', async () => {
    mockCustomFetch.mockResolvedValue({ status: 400, data: {} })
    const { exchangeInterviewRealtimeSdp } = await import('../interview-voice')

    await expect(exchangeInterviewRealtimeSdp('sess1', 'offer', 'en')).resolves.toMatchObject({
      ok: false,
      status: 400,
      message: 'HTTP 400',
    })
  })
})
