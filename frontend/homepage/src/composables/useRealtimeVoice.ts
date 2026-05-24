import { ref, onUnmounted, type Ref } from 'vue'
import type {
  RealtimeLookupResponse,
  RealtimeSdpFailure,
  RealtimeTokenFailure,
  RealtimeVoiceModelOption,
  RealtimeVoiceSessionOptions,
  SpeechUiLang,
} from '@/lib/realtime-voice'
import {
  createElevenLabsConversationToken,
  exchangeRealtimeSdp,
  lookupRealtimeInfo,
  REALTIME_SESSION_MAX_MS,
} from '@/lib/realtime-voice'
import { captureProductAnalyticsEvent, captureClientException } from '@/lib/analytics'
import { POSTHOG_VOICE_EVENTS } from '@/lib/posthog-sdk'

export type RealtimeConnectionState = 'idle' | 'connecting' | 'connected' | 'error'

type ElevenLabsConversation = {
  endSession?: () => Promise<void>
  getId?: () => string
}

function mapGetUserMediaError(e: unknown, lang: SpeechUiLang): string {
  const en = lang === 'en'
  if (e instanceof DOMException) {
    if (e.name === 'NotAllowedError' || e.name === 'PermissionDeniedError') {
      return en ? 'Microphone permission denied.' : 'Mikrofontilgang ble nektet.'
    }
    if (e.name === 'NotFoundError' || e.name === 'DevicesNotFoundError') {
      return en ? 'No microphone found.' : 'Ingen mikrofon funnet.'
    }
    if (e.name === 'NotReadableError' || e.name === 'TrackStartError') {
      return en ? 'Microphone is busy or unavailable.' : 'Mikrofonen er opptatt eller utilgjengelig.'
    }
  }
  if (e instanceof Error && e.message) return e.message
  return en ? 'Could not access the microphone.' : 'Kunne ikke bruke mikrofonen.'
}

function openAiRejectedDetail(msg: string): string {
  const p = 'OpenAI rejected the session: '
  return msg.startsWith(p) ? msg.slice(p.length).trim() : msg
}

function elevenLabsRejectedDetail(msg: string): string {
  const p = 'ElevenLabs rejected the session: '
  return msg.startsWith(p) ? msg.slice(p.length).trim() : msg
}

function isLikelyGetUserMediaError(e: unknown): boolean {
  if (!(e instanceof DOMException)) return false
  const n = e.name
  return (
    n === 'NotAllowedError' ||
    n === 'PermissionDeniedError' ||
    n === 'NotFoundError' ||
    n === 'NotReadableError' ||
    n === 'TrackStartError' ||
    n === 'DevicesNotFoundError'
  )
}

/** Wait until ICE candidates are gathered (or timeout) so the SDP posted to the server includes candidates. */
const ICE_GATHERING_TIMEOUT_MS = 8000

function waitForIceGatheringComplete(pc: RTCPeerConnection, timeoutMs: number): Promise<void> {
  if (pc.iceGatheringState === 'complete') {
    return Promise.resolve()
  }
  return new Promise((resolve) => {
    let settled = false
    const finish = () => {
      if (settled) return
      settled = true
      clearTimeout(timer)
      pc.removeEventListener('icegatheringstatechange', onGatheringStateChange)
      resolve()
    }
    const timer = setTimeout(finish, timeoutMs)
    const onGatheringStateChange = () => {
      if (pc.iceGatheringState === 'complete') {
        finish()
      }
    }
    pc.addEventListener('icegatheringstatechange', onGatheringStateChange)
    if (pc.iceGatheringState === 'complete') {
      finish()
    }
  })
}

function mapSdpFailureToUserMessage(lang: SpeechUiLang, f: RealtimeSdpFailure): string {
  const en = lang === 'en'
  const code = f.code ?? (f.status === 429 ? 'RATE_LIMITED' : undefined)

  switch (code) {
    case 'RATE_LIMITED': {
      const sec = f.retryAfterSeconds
      if (sec != null && sec > 0) {
        return en
          ? `Too many voice requests. Try again in about ${sec} seconds.`
          : `For mange stemme-forespørsler. Prøv igjen om ca. ${sec} sekunder.`
      }
      return en
        ? 'Too many voice sessions. Please wait and try again.'
        : 'Du har startet stemme for mange ganger. Vent litt og prøv igjen.'
    }
    case 'BUDGET_EXCEEDED':
      return en ? 'AI budget limit reached for now.' : 'AI-budsjettet er brukt opp for nå.'
    case 'CIRCUIT_OPEN':
      return en ? 'AI is temporarily unavailable.' : 'AI er midlertidig utilgjengelig.'
    case 'OPENAI_REJECTED': {
      const d = openAiRejectedDetail(f.message)
      return en
        ? `The service could not start the session${d ? `: ${d}` : '.'}`
        : `Tjenesten kunne ikke starte samtalen${d ? `: ${d}` : '.'}`
    }
    case 'OPENAI_SERVER_ERROR':
      return en
        ? 'Voice service returned an error. Please try again in a moment.'
        : 'Talesvaret fra tjenesten feilet. Prøv igjen om litt.'
    case 'OPENAI_UNREACHABLE':
      return en ? 'Could not reach the voice server. Try again.' : 'Kunne ikke nå taleserveren. Prøv igjen.'
    case 'ELEVENLABS_REJECTED': {
      const d = elevenLabsRejectedDetail(f.message)
      return en
        ? `ElevenLabs could not start the session${d ? `: ${d}` : '.'}`
        : `ElevenLabs kunne ikke starte samtalen${d ? `: ${d}` : '.'}`
    }
    case 'ELEVENLABS_SERVER_ERROR':
      return en
        ? 'ElevenLabs returned an error. Please try again in a moment.'
        : 'ElevenLabs feilet. Prøv igjen om litt.'
    case 'ELEVENLABS_UNREACHABLE':
      return en ? 'Could not reach ElevenLabs. Try again.' : 'Kunne ikke nå ElevenLabs. Prøv igjen.'
    case 'VOICE_MODEL_NOT_CONFIGURED':
      return en
        ? 'The selected voice model is not available.'
        : 'Den valgte stemmemodellen er ikke tilgjengelig.'
    case 'SESSION_CONFIG_FAILED':
      return en
        ? 'Voice session could not be configured. Please try again later.'
        : 'Stemmeøkta kunne ikke settes opp. Prøv igjen senere.'
    case 'API_KEY_MISSING':
    case 'REALTIME_DISABLED':
      return en ? 'Voice chat is not available on the server right now.' : 'Stemmechat er ikke tilgjengelig på serveren nå.'
    default:
      return f.message || (en ? 'Could not start voice session.' : 'Kunne ikke starte stemmeøkt.')
  }
}

function mapTokenFailureToUserMessage(lang: SpeechUiLang, f: RealtimeTokenFailure): string {
  return mapSdpFailureToUserMessage(lang, f)
}

/**
 * WebRTC + OpenAI Realtime (oai-events) for live speech with Kevin's AI.
 */
export function useRealtimeVoice(
  language: Ref<SpeechUiLang>,
  sessionOptions?: Readonly<Ref<RealtimeVoiceSessionOptions>>,
  selectedModel?: Readonly<Ref<RealtimeVoiceModelOption | undefined>>,
) {
  const connectionState = ref<RealtimeConnectionState>('idle')
  const errorMessage = ref('')
  /** Non-error notice (e.g. session time limit). */
  const sessionNotice = ref('')
  /** Latest assistant transcript (streaming) */
  const assistantTranscript = ref('')
  /** Latest user transcript from input audio transcription */
  const userTranscript = ref('')
  /** True while the model is generating a spoken response */
  const isModelSpeaking = ref(false)

  let pc: RTCPeerConnection | null = null
  let localStream: MediaStream | null = null
  let dc: RTCDataChannel | null = null
  let remoteAudio: HTMLAudioElement | null = null
  let elevenLabsConversation: ElevenLabsConversation | null = null
  let sessionTimer: ReturnType<typeof setTimeout> | null = null
  let sessionStartedAt = 0
  /** True while user clicked disconnect / timeout / we are tearing down on purpose. */
  let userEndedSession = false
  let midSessionFailureHandled = false

  function stopSessionTimer() {
    if (sessionTimer !== null) {
      clearTimeout(sessionTimer)
      sessionTimer = null
    }
  }

  function cleanupTracks() {
    localStream?.getTracks().forEach((t) => t.stop())
    localStream = null
  }

  async function teardownElevenLabs() {
    const conversation = elevenLabsConversation
    elevenLabsConversation = null
    if (conversation?.endSession) {
      try {
        await conversation.endSession()
      } catch {
        /* ignore */
      }
    }
  }

  function teardownMedia() {
    stopSessionTimer()
    cleanupTracks()
    void teardownElevenLabs()
    if (dc) {
      try {
        dc.close()
      } catch {
        /* ignore */
      }
      dc = null
    }
    if (pc) {
      try {
        pc.close()
      } catch {
        /* ignore */
      }
      pc = null
    }
    if (remoteAudio) {
      remoteAudio.srcObject = null
      remoteAudio.remove()
      remoteAudio = null
    }
  }

  function handleMidSessionFailure(msg: string) {
    if (midSessionFailureHandled) return
    midSessionFailureHandled = true
    userEndedSession = true
    captureClientException(new Error(`voice_mid_session: ${msg}`))
    sessionStartedAt = 0
    teardownMedia()
    connectionState.value = 'error'
    errorMessage.value = msg
    captureProductAnalyticsEvent(POSTHOG_VOICE_EVENTS.SESSION_ERROR, {
      message: msg,
      language: language.value,
      reason: 'mid_session',
    })
  }

  function sendRealtimeClientEvent(event: unknown) {
    if (!dc || dc.readyState !== 'open') return
    dc.send(JSON.stringify(event))
  }

  async function handleLookupFunctionCall(item: {
    type?: unknown
    name?: unknown
    call_id?: unknown
    arguments?: unknown
  }) {
    if (item.type !== 'function_call' || item.name !== 'lookup_kevin_info' || typeof item.call_id !== 'string') return

    let query = ''
    if (typeof item.arguments === 'string' && item.arguments.trim() !== '') {
      try {
        const parsed = JSON.parse(item.arguments) as { query?: unknown }
        if (typeof parsed.query === 'string') {
          query = parsed.query.trim()
        }
      } catch {
        query = ''
      }
    }

    let output: RealtimeLookupResponse = { found: false, snippets: [], confidence: 'none' }
    if (query !== '') {
      try {
        output = await lookupRealtimeInfo(query, language.value)
      } catch {
        output = { found: false, snippets: [], confidence: 'none' }
      }
    }

    if (userEndedSession || midSessionFailureHandled) return
    sendRealtimeClientEvent({
      type: 'conversation.item.create',
      item: {
        type: 'function_call_output',
        call_id: item.call_id,
        output: JSON.stringify(output),
      },
    })
    sendRealtimeClientEvent({ type: 'response.create' })
  }

  function handleResponseDone(ev: { response?: { output?: unknown } }) {
    const output = ev.response?.output
    if (!Array.isArray(output)) return
    for (const item of output) {
      if (item && typeof item === 'object') {
        void handleLookupFunctionCall(
          item as { type?: unknown; name?: unknown; call_id?: unknown; arguments?: unknown },
        )
      }
    }
  }

  function handleDataMessage(raw: string) {
    try {
      const ev = JSON.parse(raw) as {
        type?: string
        delta?: string
        transcript?: string
        response?: {
          output?: unknown
        }
        error?: {
          message?: unknown
          code?: unknown
        }
      }
      const t = ev.type ?? ''
      if (t === 'error') {
        const message =
          typeof ev.error?.message === 'string' && ev.error.message.trim() !== ''
            ? ev.error.message.trim()
            : language.value === 'no'
              ? 'Stemmesesjonen returnerte en feil.'
              : 'Voice session returned an error.'
        handleMidSessionFailure(message)
        return
      }
      if (t === 'response.created') {
        isModelSpeaking.value = true
        return
      }
      if (t === 'response.done' || t === 'response.cancelled') {
        isModelSpeaking.value = false
        handleResponseDone(ev)
        return
      }
      if (t === 'response.output_audio_transcript.delta' && typeof ev.delta === 'string') {
        assistantTranscript.value += ev.delta
        return
      }
      if (t === 'response.output_audio_transcript.done' && typeof ev.transcript === 'string') {
        assistantTranscript.value = ev.transcript
        return
      }
      if (t === 'conversation.item.input_audio_transcription.delta' && typeof ev.delta === 'string') {
        userTranscript.value += ev.delta
        return
      }
      if (
        t === 'conversation.item.input_audio_transcription.completed' &&
        typeof ev.transcript === 'string'
      ) {
        userTranscript.value = ev.transcript
      }
    } catch {
      /* ignore malformed */
    }
  }

  function handleElevenLabsMessage(message: unknown) {
    if (message === null || typeof message !== 'object') return
    const m = message as Record<string, unknown>
    const text =
      typeof m.message === 'string'
        ? m.message
        : typeof m.text === 'string'
          ? m.text
          : typeof m.transcript === 'string'
            ? m.transcript
            : ''
    if (text.trim() === '') return
    const source = typeof m.source === 'string' ? m.source.toLowerCase() : ''
    const role = typeof m.role === 'string' ? m.role.toLowerCase() : ''
    if (source.includes('user') || role.includes('user')) {
      userTranscript.value = text
    } else {
      assistantTranscript.value = text
    }
  }

  function stopResponse() {
    if (connectionState.value !== 'connected' || userEndedSession || midSessionFailureHandled) {
      return
    }
    if (selectedProvider() === 'OPENAI') {
      sendRealtimeClientEvent({ type: 'response.cancel' })
      isModelSpeaking.value = false
    }
  }

  function selectedProvider(): 'OPENAI' | 'ELEVENLABS' {
    return selectedModel?.value?.provider ?? 'OPENAI'
  }

  function selectedModelId(): string | undefined {
    const id = selectedModel?.value?.id
    return id && id.trim() !== '' ? id.trim() : undefined
  }

  async function connectOpenAi() {
    if (typeof RTCPeerConnection === 'undefined') {
      errorMessage.value =
        language.value === 'no'
          ? 'Nettleseren støtter ikke WebRTC.'
          : 'This browser does not support WebRTC.'
      connectionState.value = 'error'
      return
    }

    try {
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true })
      localStream = stream

      pc = new RTCPeerConnection({
        iceServers: [{ urls: 'stun:stun.l.google.com:19302' }],
      })

      pc.onconnectionstatechange = () => {
        if (!pc || userEndedSession || midSessionFailureHandled) return
        if (pc.connectionState === 'failed' && connectionState.value === 'connected') {
          const msg =
            language.value === 'no' ? 'Stemmekoblingen falt ut.' : 'Voice connection failed.'
          handleMidSessionFailure(msg)
        }
      }

      remoteAudio = document.createElement('audio')
      remoteAudio.autoplay = true
      pc.ontrack = (e) => {
        if (remoteAudio && e.streams[0]) {
          remoteAudio.srcObject = e.streams[0]
        }
      }

      stream.getTracks().forEach((track) => pc!.addTrack(track, stream))

      dc = pc.createDataChannel('oai-events')
      dc.addEventListener('message', (e) => {
        if (typeof e.data === 'string') handleDataMessage(e.data)
      })
      dc.addEventListener('close', () => {
        if (connectionState.value === 'connected' && !userEndedSession && !midSessionFailureHandled) {
          const msg =
            language.value === 'no' ? 'Stemmesesjonen ble avbrutt.' : 'Voice session was interrupted.'
          handleMidSessionFailure(msg)
        }
      })

      const offer = await pc.createOffer()
      await pc.setLocalDescription(offer)
      await waitForIceGatheringComplete(pc, ICE_GATHERING_TIMEOUT_MS)
      const offerSdp = pc.localDescription?.sdp
      if (!offerSdp) {
        throw new Error('Missing local SDP')
      }

      const result = await exchangeRealtimeSdp(offerSdp, language.value, sessionOptions?.value, selectedModelId())
      if (!result.ok) {
        throw new Error(mapSdpFailureToUserMessage(language.value, result))
      }

      await pc.setRemoteDescription({ type: 'answer', sdp: result.answerSdp })

      connectionState.value = 'connected'
      sessionStartedAt = Date.now()
      captureProductAnalyticsEvent(POSTHOG_VOICE_EVENTS.SESSION_STARTED, {
        language: language.value,
        provider: 'OPENAI',
        model_id: selectedModelId(),
      })

      sessionTimer = setTimeout(() => {
        disconnect('timeout')
      }, REALTIME_SESSION_MAX_MS)
    } catch (e) {
      captureClientException(e)
      teardownMedia()
      connectionState.value = 'error'
      let msg: string
      if (isLikelyGetUserMediaError(e)) {
        msg = mapGetUserMediaError(e, language.value)
      } else if (e instanceof Error && e.message === 'Missing local SDP') {
        msg =
          language.value === 'no'
            ? 'Kunne ikke forberede lydtilkoblingen.'
            : 'Could not prepare the audio connection.'
      } else if (e instanceof Error && e.message) {
        msg = e.message
      } else {
        msg = language.value === 'no' ? 'Kunne ikke starte samtalen.' : 'Could not start voice session.'
      }
      errorMessage.value = msg
      captureProductAnalyticsEvent(POSTHOG_VOICE_EVENTS.SESSION_ERROR, {
        message: errorMessage.value,
        language: language.value,
      })
    }
  }

  async function connectElevenLabs() {
    if (!selectedModelId()) {
      throw new Error(
        language.value === 'no'
          ? 'Ingen ElevenLabs-agent er konfigurert.'
          : 'No ElevenLabs agent is configured.',
      )
    }
    try {
      const permissionProbe = await navigator.mediaDevices.getUserMedia({ audio: true })
      permissionProbe.getTracks().forEach((track) => track.stop())

      const tokenResult = await createElevenLabsConversationToken(selectedModelId()!)
      if (!tokenResult.ok) {
        throw new Error(mapTokenFailureToUserMessage(language.value, tokenResult))
      }

      const { Conversation } = await import('@elevenlabs/client')
      elevenLabsConversation = (await Conversation.startSession({
        conversationToken: tokenResult.token,
        connectionType: 'webrtc',
        onConnect: () => {
          if (userEndedSession || midSessionFailureHandled) return
          connectionState.value = 'connected'
        },
        onDisconnect: () => {
          if (userEndedSession || midSessionFailureHandled) return
          const msg =
            language.value === 'no' ? 'Stemmesesjonen ble avbrutt.' : 'Voice session was interrupted.'
          handleMidSessionFailure(msg)
        },
        onMessage: (message: unknown) => {
          handleElevenLabsMessage(message)
        },
        onError: (error: unknown) => {
          const msg = error instanceof Error && error.message
            ? error.message
            : language.value === 'no'
              ? 'ElevenLabs-sesjonen returnerte en feil.'
              : 'ElevenLabs session returned an error.'
          handleMidSessionFailure(msg)
        },
      })) as ElevenLabsConversation

      connectionState.value = 'connected'
      sessionStartedAt = Date.now()
      captureProductAnalyticsEvent(POSTHOG_VOICE_EVENTS.SESSION_STARTED, {
        language: language.value,
        provider: 'ELEVENLABS',
        model_id: selectedModelId(),
        conversation_id: elevenLabsConversation?.getId?.(),
      })

      sessionTimer = setTimeout(() => {
        disconnect('timeout')
      }, REALTIME_SESSION_MAX_MS)
    } catch (e) {
      captureClientException(e)
      teardownMedia()
      connectionState.value = 'error'
      let msg: string
      if (isLikelyGetUserMediaError(e)) {
        msg = mapGetUserMediaError(e, language.value)
      } else if (e instanceof Error && e.message) {
        msg = e.message
      } else {
        msg = language.value === 'no' ? 'Kunne ikke starte samtalen.' : 'Could not start voice session.'
      }
      errorMessage.value = msg
      captureProductAnalyticsEvent(POSTHOG_VOICE_EVENTS.SESSION_ERROR, {
        message: errorMessage.value,
        language: language.value,
        provider: 'ELEVENLABS',
      })
    }
  }

  async function connect() {
    if (connectionState.value === 'connecting' || connectionState.value === 'connected') return
    errorMessage.value = ''
    sessionNotice.value = ''
    assistantTranscript.value = ''
    userTranscript.value = ''
    isModelSpeaking.value = false
    userEndedSession = false
    midSessionFailureHandled = false
    connectionState.value = 'connecting'

    if (selectedProvider() === 'ELEVENLABS') {
      await connectElevenLabs()
    } else {
      await connectOpenAi()
    }
  }

  function disconnect(reason?: 'user' | 'timeout') {
    const durationSec =
      sessionStartedAt > 0 ? Math.round((Date.now() - sessionStartedAt) / 1000) : undefined
    if (connectionState.value === 'connected' && durationSec !== undefined) {
      const provider = selectedProvider()
      const modelId = selectedModelId()
      const conversationId = elevenLabsConversation?.getId?.()
      captureProductAnalyticsEvent(POSTHOG_VOICE_EVENTS.SESSION_ENDED, {
        duration_seconds: durationSec,
        reason: reason ?? 'user',
        language: language.value,
        provider,
        ...(modelId ? { model_id: modelId } : {}),
        ...(conversationId ? { conversation_id: conversationId } : {}),
      })
    }
    userEndedSession = true
    sessionStartedAt = 0
    teardownMedia()
    connectionState.value = 'idle'
    if (reason === 'timeout') {
      sessionNotice.value =
        language.value === 'no'
          ? 'Tidsbegrensning nådd (3 min). Start på nytt om du vil fortsette.'
          : 'Time limit reached (3 minutes). Connect again to continue.'
    } else {
      sessionNotice.value = ''
    }
  }

  onUnmounted(() => {
    if (connectionState.value === 'connected' || connectionState.value === 'connecting') {
      disconnect('user')
    } else {
      teardownMedia()
    }
  })

  return {
    connectionState,
    errorMessage,
    sessionNotice,
    assistantTranscript,
    userTranscript,
    isModelSpeaking,
    connect,
    disconnect: () => disconnect('user'),
    stopResponse,
    maxSessionMs: REALTIME_SESSION_MAX_MS,
  }
}
