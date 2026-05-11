import { ref, onUnmounted, type Ref } from 'vue'
import type { DisconnectionDetails, MessagePayload } from '@elevenlabs/types'
import type {
  RealtimeLookupResponse,
  RealtimeSdpFailure,
  RealtimeTokenFailure,
  RealtimeVoiceAnalytics,
  RealtimeVoiceModelOption,
  RealtimeVoiceSessionOptions,
  SpeechUiLang,
} from '@/lib/realtime-voice'
import {
  completeVoiceTrace,
  createElevenLabsConversationToken,
  exchangeRealtimeSdp,
  lookupRealtimeInfo,
  REALTIME_SESSION_MAX_MS,
} from '@/lib/realtime-voice'
import {
  captureProductAnalyticsEvent,
  captureClientException,
  getPosthogSessionIdForVoiceAnalytics,
} from '@/lib/analytics'
import { POSTHOG_VOICE_EVENTS } from '@/lib/posthog-sdk'

export type RealtimeConnectionState = 'idle' | 'connecting' | 'connected' | 'error'

type ElevenLabsConversation = {
  endSession?: () => Promise<void>
  getId?: () => string
}

type VoiceErrorDiagnostics = Record<string, string | number | boolean>

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

function compactWhitespace(value: string): string {
  return value.replace(/\s+/g, ' ').trim()
}

function appendDiagnosticSuffix(
  message: string,
  diagnostics?: { closeCode?: number; closeReason?: string },
): string {
  const closeCode = diagnostics?.closeCode
  const closeReason = diagnostics?.closeReason ? compactWhitespace(diagnostics.closeReason) : ''
  if (closeCode == null && closeReason === '') return message

  const parts: string[] = []
  if (closeCode != null) parts.push(`close code ${closeCode}`)
  if (closeReason !== '') parts.push(closeReason)
  return `${message} (${parts.join(': ')})`
}

function extractElevenLabsDiagnostics(
  details:
    | {
      reason?: string
      closeCode?: number
      closeReason?: string
      message?: string
    }
    | null
    | undefined,
  stage: string,
): VoiceErrorDiagnostics {
  const diagnostics: VoiceErrorDiagnostics = { provider: 'ELEVENLABS', stage }
  if (details?.reason) diagnostics.disconnect_reason = details.reason
  if (typeof details?.closeCode === 'number') diagnostics.close_code = details.closeCode
  if (typeof details?.closeReason === 'string' && details.closeReason.trim() !== '') {
    diagnostics.close_reason = compactWhitespace(details.closeReason)
  }
  if (typeof details?.message === 'string' && details.message.trim() !== '') {
    diagnostics.message = compactWhitespace(details.message)
  }
  return diagnostics
}

function formatElevenLabsStartupError(error: unknown, lang: SpeechUiLang): {
  message: string
  diagnostics: VoiceErrorDiagnostics
} {
  const en = lang === 'en'
  if (!(error instanceof Error)) {
    return {
      message: en ? 'Could not start voice session.' : 'Kunne ikke starte samtalen.',
      diagnostics: { provider: 'ELEVENLABS', stage: 'start_exception', message: String(error) },
    }
  }

  const closeCode = 'closeCode' in error && typeof error.closeCode === 'number' ? error.closeCode : undefined
  const closeReason =
    'closeReason' in error && typeof error.closeReason === 'string' && error.closeReason.trim() !== ''
      ? compactWhitespace(error.closeReason)
      : ''
  const rawMessage = compactWhitespace(error.message || '')
  const diagnostics: VoiceErrorDiagnostics = {
    provider: 'ELEVENLABS',
    stage: 'start_exception',
    error_name: error.name || 'Error',
    message: rawMessage || (en ? 'Unknown startup error' : 'Ukjent oppstartsfeil'),
  }
  if (closeCode != null) diagnostics.close_code = closeCode
  if (closeReason !== '') diagnostics.close_reason = closeReason

  return {
    message: appendDiagnosticSuffix(
      rawMessage || (en ? 'Could not start voice session.' : 'Kunne ikke starte samtalen.'),
      { closeCode, closeReason },
    ),
    diagnostics,
  }
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

function formatElevenLabsDisconnectMessage(details: DisconnectionDetails, lang: SpeechUiLang): string {
  const en = lang === 'en'
  if (details.reason === 'error') {
    const m = details.message.trim()
    if (m !== '') {
      return appendDiagnosticSuffix(m, {
        closeCode: details.closeCode,
        closeReason: details.closeReason,
      })
    }
    return appendDiagnosticSuffix(en ? 'Voice connection error.' : 'Feil i stemmekoblingen.', {
      closeCode: details.closeCode,
      closeReason: details.closeReason,
    })
  }
  if (details.reason === 'agent') {
    const rawReason = details.closeReason?.trim()
      || (details.context instanceof CloseEvent ? details.context.reason?.trim() : '')
      || ''
    if (rawReason === 'agent disconnected' || rawReason === '') {
      return en
        ? 'The voice agent could not start. Please try again later.'
        : 'Stemmeagenten kunne ikke starte. Prøv igjen senere.'
    }
    return appendDiagnosticSuffix(rawReason, {
      closeCode: details.closeCode,
      closeReason: details.closeReason,
    })
  }
  return en ? 'Voice session was interrupted.' : 'Stemmesesjonen ble avbrutt.'
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

  /** PostHog voice trace: set after mic OK, marked when first realtime API call runs. */
  let voiceTraceSession: {
    analytics: RealtimeVoiceAnalytics
    backendContacted: boolean
    finalized: boolean
  } | null = null

  function finalizeVoiceTraceIfNeeded(opts: {
    error: boolean
    errorMessage?: string
    durationSeconds?: number
  }) {
    const s = voiceTraceSession
    if (!s?.backendContacted || s.finalized) return
    s.finalized = true
    void completeVoiceTrace({
      traceId: s.analytics.traceId,
      ...(s.analytics.sessionId !== undefined ? { sessionId: s.analytics.sessionId } : {}),
      durationSeconds: Math.max(0, opts.durationSeconds ?? 0),
      error: opts.error,
      ...(opts.errorMessage !== undefined ? { errorMessage: opts.errorMessage } : {}),
    })
  }

  function voiceTraceProps(): Record<string, string> {
    const a = voiceTraceSession?.analytics
    if (!a) return {}
    const o: Record<string, string> = { ai_trace_id: a.traceId }
    if (a.sessionId !== undefined && a.sessionId.trim() !== '') {
      o.posthog_session_id = a.sessionId.trim()
    }
    return o
  }

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

  function handleMidSessionFailure(msg: string, diagnostics?: VoiceErrorDiagnostics) {
    if (midSessionFailureHandled) return
    midSessionFailureHandled = true
    userEndedSession = true
    captureClientException(new Error(`voice_mid_session: ${msg}`))
    if (diagnostics) {
      console.warn('[voice] mid-session failure', diagnostics)
    }
    const sessionStartForTrace = sessionStartedAt
    sessionStartedAt = 0
    teardownMedia()
    connectionState.value = 'error'
    errorMessage.value = msg
    captureProductAnalyticsEvent(POSTHOG_VOICE_EVENTS.SESSION_ERROR, {
      message: msg,
      language: language.value,
      reason: 'mid_session',
      ...(diagnostics ?? {}),
      ...voiceTraceProps(),
    })
    finalizeVoiceTraceIfNeeded({
      error: true,
      errorMessage: msg,
      durationSeconds:
        sessionStartForTrace > 0
          ? Math.max(0, Math.round((Date.now() - sessionStartForTrace) / 1000))
          : 0,
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

    let output: RealtimeLookupResponse = { found: false, snippets: [] }
    if (query !== '') {
      try {
        output = await lookupRealtimeInfo(query, language.value, voiceTraceSession?.analytics)
      } catch {
        output = { found: false, snippets: [] }
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
      if (t === 'response.output_audio_transcript.delta' && typeof ev.delta === 'string') {
        assistantTranscript.value += ev.delta
        return
      }
      if (t === 'response.output_audio_transcript.done' && typeof ev.transcript === 'string') {
        assistantTranscript.value = ev.transcript
        return
      }
      if (t === 'response.done') {
        handleResponseDone(ev)
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

  function handleElevenLabsMessage(payload: MessagePayload) {
    const text = payload.message ?? ''
    if (text.trim() === '') return
    if (payload.role === 'user') {
      userTranscript.value = text
    } else {
      assistantTranscript.value = text
    }
  }

  function selectedProvider(): 'OPENAI' | 'ELEVENLABS' {
    return selectedModel?.value?.provider ?? 'OPENAI'
  }

  function selectedModelId(): string | undefined {
    const id = selectedModel?.value?.id
    return id && id.trim() !== '' ? id.trim() : undefined
  }

  async function connectOpenAi(analytics: RealtimeVoiceAnalytics) {
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
      voiceTraceSession = { analytics, backendContacted: false, finalized: false }

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

      if (voiceTraceSession) {
        voiceTraceSession.backendContacted = true
      }
      const result = await exchangeRealtimeSdp(
        offerSdp,
        language.value,
        sessionOptions?.value,
        selectedModelId(),
        analytics,
      )
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
        ...voiceTraceProps(),
      })

      sessionTimer = setTimeout(() => {
        disconnect('timeout')
      }, REALTIME_SESSION_MAX_MS)
    } catch (e) {
      captureClientException(e)
      const contacted = voiceTraceSession?.backendContacted === true
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
        ...voiceTraceProps(),
      })
      if (contacted) {
        finalizeVoiceTraceIfNeeded({ error: true, errorMessage: msg, durationSeconds: 0 })
      }
      voiceTraceSession = null
    }
  }

  async function connectElevenLabs(analytics: RealtimeVoiceAnalytics) {
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

      voiceTraceSession = { analytics, backendContacted: true, finalized: false }
      const tokenResult = await createElevenLabsConversationToken(selectedModelId()!, analytics)
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
        onDisconnect: (details: DisconnectionDetails) => {
          if (userEndedSession || midSessionFailureHandled) return
          const diagnostics = extractElevenLabsDiagnostics(details, 'disconnect')
          console.warn('[voice] ElevenLabs disconnect', diagnostics)
          handleMidSessionFailure(formatElevenLabsDisconnectMessage(details, language.value), diagnostics)
        },
        onMessage: (payload) => {
          handleElevenLabsMessage(payload)
        },
        onError: (message: string) => {
          const msg = typeof message === 'string' && message.trim() !== ''
            ? message.trim()
            : language.value === 'no'
              ? 'ElevenLabs-sesjonen returnerte en feil.'
              : 'ElevenLabs session returned an error.'
          captureClientException(new Error(`elevenlabs_onError: ${msg}`))
          const diagnostics: VoiceErrorDiagnostics = {
            provider: 'ELEVENLABS',
            stage: 'onError',
            message: compactWhitespace(msg),
          }
          console.warn('[voice] ElevenLabs onError', diagnostics)
          handleMidSessionFailure(msg, diagnostics)
        },
      })) as ElevenLabsConversation

      if (userEndedSession || midSessionFailureHandled) {
        return
      }

      if (connectionState.value !== 'connected') {
        connectionState.value = 'connected'
      }
      sessionStartedAt = Date.now()
      captureProductAnalyticsEvent(POSTHOG_VOICE_EVENTS.SESSION_STARTED, {
        language: language.value,
        provider: 'ELEVENLABS',
        model_id: selectedModelId(),
        conversation_id: elevenLabsConversation?.getId?.(),
        ...voiceTraceProps(),
      })

      sessionTimer = setTimeout(() => {
        disconnect('timeout')
      }, REALTIME_SESSION_MAX_MS)
    } catch (e) {
      captureClientException(e)
      const contacted = voiceTraceSession?.backendContacted === true
      teardownMedia()
      connectionState.value = 'error'
      let msg: string
      let diagnostics: VoiceErrorDiagnostics | undefined
      if (isLikelyGetUserMediaError(e)) {
        msg = mapGetUserMediaError(e, language.value)
      } else if (e instanceof Error && e.message) {
        const formatted = formatElevenLabsStartupError(e, language.value)
        msg = formatted.message
        diagnostics = formatted.diagnostics
      } else {
        msg = language.value === 'no' ? 'Kunne ikke starte samtalen.' : 'Could not start voice session.'
      }
      errorMessage.value = msg
      if (diagnostics) {
        console.warn('[voice] ElevenLabs start failed', diagnostics)
      }
      captureProductAnalyticsEvent(POSTHOG_VOICE_EVENTS.SESSION_ERROR, {
        message: errorMessage.value,
        language: language.value,
        provider: 'ELEVENLABS',
        ...(diagnostics ?? {}),
        ...voiceTraceProps(),
      })
      if (contacted) {
        finalizeVoiceTraceIfNeeded({ error: true, errorMessage: msg, durationSeconds: 0 })
      }
      voiceTraceSession = null
    }
  }

  async function connect() {
    if (connectionState.value === 'connecting' || connectionState.value === 'connected') return
    errorMessage.value = ''
    sessionNotice.value = ''
    assistantTranscript.value = ''
    userTranscript.value = ''
    userEndedSession = false
    midSessionFailureHandled = false
    voiceTraceSession = null
    connectionState.value = 'connecting'

    const traceId = crypto.randomUUID()
    const sessionIdPh = getPosthogSessionIdForVoiceAnalytics()
    const analytics: RealtimeVoiceAnalytics =
      sessionIdPh !== undefined ? { traceId, sessionId: sessionIdPh } : { traceId }

    if (selectedProvider() === 'ELEVENLABS') {
      await connectElevenLabs(analytics)
    } else {
      await connectOpenAi(analytics)
    }
  }

  function disconnect(reason?: 'user' | 'timeout') {
    const durationSec =
      sessionStartedAt > 0 ? Math.round((Date.now() - sessionStartedAt) / 1000) : undefined
    if (connectionState.value === 'connected' && durationSec !== undefined) {
      captureProductAnalyticsEvent(POSTHOG_VOICE_EVENTS.SESSION_ENDED, {
        duration_seconds: durationSec,
        reason: reason ?? 'user',
        language: language.value,
        ...voiceTraceProps(),
      })
      finalizeVoiceTraceIfNeeded({ error: false, durationSeconds: durationSec })
    }
    userEndedSession = true
    sessionStartedAt = 0
    voiceTraceSession = null
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
    connect,
    disconnect: () => disconnect('user'),
    maxSessionMs: REALTIME_SESSION_MAX_MS,
  }
}
