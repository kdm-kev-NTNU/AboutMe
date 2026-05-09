import { ref, onUnmounted, type Ref } from 'vue'
import type { SpeechUiLang } from '@/lib/realtime-voice'
import { exchangeRealtimeSdp, REALTIME_SESSION_MAX_MS } from '@/lib/realtime-voice'
import { captureProductAnalyticsEvent, captureClientException } from '@/lib/analytics'
import { POSTHOG_VOICE_EVENTS } from '@/lib/posthog-sdk'

export type RealtimeConnectionState = 'idle' | 'connecting' | 'connected' | 'error'

/**
 * WebRTC + OpenAI Realtime (oai-events) for live speech with Kevin's AI.
 */
export function useRealtimeVoice(language: Ref<SpeechUiLang>) {
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
  let sessionTimer: ReturnType<typeof setTimeout> | null = null
  let sessionStartedAt = 0

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

  function teardownMedia() {
    stopSessionTimer()
    cleanupTracks()
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

  function handleDataMessage(raw: string) {
    try {
      const ev = JSON.parse(raw) as {
        type?: string
        delta?: string
        transcript?: string
      }
      const t = ev.type ?? ''
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

  async function connect() {
    if (connectionState.value === 'connecting' || connectionState.value === 'connected') return
    errorMessage.value = ''
    sessionNotice.value = ''
    assistantTranscript.value = ''
    userTranscript.value = ''
    connectionState.value = 'connecting'

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

      const offer = await pc.createOffer()
      await pc.setLocalDescription(offer)
      const offerSdp = pc.localDescription?.sdp
      if (!offerSdp) {
        throw new Error('Missing local SDP')
      }

      const result = await exchangeRealtimeSdp(offerSdp, language.value)
      if (!result.ok) {
        throw new Error(result.message || `Session failed (${result.status})`)
      }

      await pc.setRemoteDescription({ type: 'answer', sdp: result.answerSdp })

      connectionState.value = 'connected'
      sessionStartedAt = Date.now()
      captureProductAnalyticsEvent(POSTHOG_VOICE_EVENTS.SESSION_STARTED, {
        language: language.value,
      })

      sessionTimer = setTimeout(() => {
        disconnect('timeout')
      }, REALTIME_SESSION_MAX_MS)
    } catch (e) {
      captureClientException(e)
      teardownMedia()
      connectionState.value = 'error'
      errorMessage.value =
        e instanceof Error ? e.message : language.value === 'no' ? 'Kunne ikke starte samtalen.' : 'Could not start voice session.'
      captureProductAnalyticsEvent(POSTHOG_VOICE_EVENTS.SESSION_ERROR, {
        message: errorMessage.value,
        language: language.value,
      })
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
      })
    }
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
    connect,
    disconnect: () => disconnect('user'),
    maxSessionMs: REALTIME_SESSION_MAX_MS,
  }
}
