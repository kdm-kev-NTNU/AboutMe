import { describe, it, expect, vi, beforeEach } from 'vitest'
import { ref, nextTick } from 'vue'
import { useInterviewVoice } from '../useInterviewVoice'

vi.mock('@/lib/interview-voice', () => ({
  appendInterviewTurns: vi.fn().mockResolvedValue(undefined),
  exchangeInterviewRealtimeSdp: vi.fn(),
}))

vi.mock('@/composables/useRealtimeVoice', () => ({
  useRealtimeVoice: vi.fn((_lang, _opts, _model, voiceOptions) => {
    voiceOptions?.onTurnCommitted?.('user', 'hello')
    return {
      connectionState: ref('idle'),
      errorMessage: ref(''),
      sessionNotice: ref(''),
      assistantTranscript: ref(''),
      userTranscript: ref(''),
      isModelSpeaking: ref(false),
      connect: vi.fn(),
      disconnect: vi.fn(),
      stopResponse: vi.fn(),
      maxSessionMs: voiceOptions?.maxSessionMs ?? 300_000,
    }
  }),
}))

describe('useInterviewVoice', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('accumulates committed turns from realtime callbacks', async () => {
    const sessionId = ref('sess-1')
    const lang = ref<'en' | 'no'>('no')
    const { committedTurns } = useInterviewVoice(sessionId, lang)
    await nextTick()
    expect(committedTurns.value).toHaveLength(1)
    expect(committedTurns.value[0]?.role).toBe('user')
    expect(committedTurns.value[0]?.text).toBe('hello')
  })

  it('passes 30-minute maxSessionMs to useRealtimeVoice', async () => {
    const { useRealtimeVoice } = await import('../useRealtimeVoice')
    const sessionId = ref('sess-1')
    const lang = ref<'en' | 'no'>('no')
    const api = useInterviewVoice(sessionId, lang)
    expect(useRealtimeVoice).toHaveBeenCalledWith(
      lang,
      undefined,
      undefined,
      expect.objectContaining({ maxSessionMs: 1_800_000 }),
    )
    expect(api.maxSessionMs).toBe(1_800_000)
  })

  it('hydrateTurns restores committed turns and sequence counter', async () => {
    const sessionId = ref('sess-1')
    const lang = ref<'en' | 'no'>('no')
    const { hydrateTurns, committedTurns } = useInterviewVoice(sessionId, lang)
    hydrateTurns([
      { role: 'interviewer', text: 'Q1', sequenceNo: 2 },
      { role: 'user', text: 'A1', sequenceNo: 5 },
    ])
    expect(committedTurns.value).toEqual([
      { role: 'interviewer', text: 'Q1', sequenceNo: 2 },
      { role: 'user', text: 'A1', sequenceNo: 5 },
    ])
  })
})
