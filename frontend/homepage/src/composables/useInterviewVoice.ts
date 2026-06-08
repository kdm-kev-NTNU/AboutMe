import { ref, type Ref } from 'vue'
import type { RealtimeVoiceModelOption, RealtimeVoiceSessionOptions, SpeechUiLang } from '@/lib/realtime-voice'
import { useRealtimeVoice } from '@/composables/useRealtimeVoice'
import { appendInterviewTurns, exchangeInterviewRealtimeSdp, type InterviewTurn } from '@/lib/interview-voice'

const SAVE_DEBOUNCE_MS = 8_000

export function useInterviewVoice(
  sessionId: Ref<string | null>,
  language: Ref<SpeechUiLang>,
  sessionOptions?: Readonly<Ref<RealtimeVoiceSessionOptions>>,
  selectedModel?: Readonly<Ref<RealtimeVoiceModelOption | undefined>>,
) {
  const committedTurns = ref<InterviewTurn[]>([])
  const pendingTurns = ref<InterviewTurn[]>([])
  let sequenceCounter = 0
  let saveTimer: ReturnType<typeof setTimeout> | null = null

  function scheduleSave() {
    if (saveTimer !== null) clearTimeout(saveTimer)
    saveTimer = setTimeout(() => {
      void flushTurns()
    }, SAVE_DEBOUNCE_MS)
  }

  async function flushTurns() {
    const sid = sessionId.value
    if (!sid || pendingTurns.value.length === 0) return
    const batch = [...pendingTurns.value]
    pendingTurns.value = []
    try {
      await appendInterviewTurns(sid, batch)
    } catch {
      pendingTurns.value = [...batch, ...pendingTurns.value]
    }
  }

  function onTurnCommitted(role: 'user' | 'interviewer', text: string) {
    const turn: InterviewTurn = {
      role,
      text,
      sequenceNo: sequenceCounter++,
    }
    committedTurns.value = [...committedTurns.value, turn]
    pendingTurns.value = [...pendingTurns.value, turn]
    scheduleSave()
  }

  const voice = useRealtimeVoice(language, sessionOptions, selectedModel, {
    onTurnCommitted,
    exchangeSdp: (offerSdp, lang, opts, modelId) => {
      const sid = sessionId.value
      if (!sid) {
        return Promise.resolve({
          ok: false as const,
          status: 400,
          message: 'Missing interview session',
        })
      }
      return exchangeInterviewRealtimeSdp(sid, offerSdp, lang, opts, modelId)
    },
  })

  async function disconnectAndFlush() {
    voice.disconnect()
    if (saveTimer !== null) {
      clearTimeout(saveTimer)
      saveTimer = null
    }
    await flushTurns()
  }

  function resetTurns() {
    committedTurns.value = []
    pendingTurns.value = []
    sequenceCounter = 0
    if (saveTimer !== null) {
      clearTimeout(saveTimer)
      saveTimer = null
    }
  }

  return {
    ...voice,
    committedTurns,
    disconnectAndFlush,
    flushTurns,
    resetTurns,
  }
}
