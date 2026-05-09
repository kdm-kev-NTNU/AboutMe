import { computed, onUnmounted, ref, type Ref } from 'vue'

export type RealtimeVoicePhase = 'idle' | 'connecting' | 'connected' | 'error'

export type RealtimeStatus = {
  enabled: boolean
  sessionMaxMinutes: number
  model: string
}

function waitIceGatheringComplete(pc: RTCPeerConnection): Promise<void> {
  if (pc.iceGatheringState === 'complete') {
    return Promise.resolve()
  }
  return new Promise((resolve) => {
    const done = () => {
      pc.removeEventListener('icegatheringstatechange', onState)
      resolve()
    }
    const onState = () => {
      if (pc.iceGatheringState === 'complete') done()
    }
    pc.addEventListener('icegatheringstatechange', onState)
    setTimeout(done, 2500)
  })
}

function flushAssistantLine(
  buffer: Ref<string>,
  lines: Ref<string>,
  maxChars: number,
) {
  const t = buffer.value.trim()
  buffer.value = ''
  if (!t) return
  const block = (lines.value ? `${lines.value}\n\n` : '') + t
  lines.value = block.slice(-maxChars)
}

export function useOpenAiRealtimeVoice(language: Ref<'en' | 'no'>) {
  const phase = ref<RealtimeVoicePhase>('idle')
  const errorMessage = ref('')
  const status = ref<RealtimeStatus | null>(null)
  const userTranscript = ref('')
  const assistantTranscript = ref('')
  const assistantBuffer = ref('')

  const pcRef = ref<RTCPeerConnection | null>(null)
  const localStreamRef = ref<MediaStream | null>(null)
  const remoteAudioRef = ref<HTMLAudioElement | null>(null)
  let sessionTimer: ReturnType<typeof setTimeout> | undefined

  const sessionMs = computed(() => Math.max(1, status.value?.sessionMaxMinutes ?? 3) * 60_000)

  async function loadStatus(): Promise<void> {
    try {
      const res = await fetch('/api/realtime/status', { credentials: 'include' })
      if (!res.ok) {
        status.value = { enabled: false, sessionMaxMinutes: 3, model: '' }
        return
      }
      status.value = (await res.json()) as RealtimeStatus
    } catch {
      status.value = { enabled: false, sessionMaxMinutes: 3, model: '' }
    }
  }

  function teardownPeers() {
    if (sessionTimer !== undefined) {
      clearTimeout(sessionTimer)
      sessionTimer = undefined
    }
    localStreamRef.value?.getTracks().forEach((t) => t.stop())
    localStreamRef.value = null
    pcRef.value?.close()
    pcRef.value = null
    if (remoteAudioRef.value) {
      remoteAudioRef.value.srcObject = null
    }
  }

  function handleDataMessage(raw: string) {
    let msg: Record<string, unknown>
    try {
      msg = JSON.parse(raw) as Record<string, unknown>
    } catch {
      return
    }
    const type = typeof msg.type === 'string' ? msg.type : ''

    if (
      type === 'conversation.item.input_audio_transcription.completed' &&
      typeof msg.transcript === 'string'
    ) {
      const line = msg.transcript as string
      const next = (userTranscript.value ? `${userTranscript.value}\n` : '') + line
      userTranscript.value = next.slice(-12_000)
      return
    }

    if (type === 'response.audio_transcript.delta' && typeof msg.delta === 'string') {
      assistantBuffer.value += msg.delta
      return
    }

    if (type === 'response.audio_transcript.done') {
      flushAssistantLine(assistantBuffer, assistantTranscript, 24_000)
      return
    }

    // Alternate event shapes (API revisions)
    if (type.endsWith('output_audio_transcript.delta') && typeof msg.delta === 'string') {
      assistantBuffer.value += msg.delta as string
    }
    if (type.endsWith('output_audio_transcript.done')) {
      flushAssistantLine(assistantBuffer, assistantTranscript, 24_000)
    }
  }

  async function start(): Promise<void> {
    errorMessage.value = ''
    userTranscript.value = ''
    assistantTranscript.value = ''
    assistantBuffer.value = ''

    await loadStatus()
    if (!status.value?.enabled) {
      phase.value = 'error'
      errorMessage.value =
        language.value === 'no'
          ? 'Live stemme er ikke tilgjengelig (api eller funksjon av).'
          : 'Live voice is not available (API or feature disabled).'
      return
    }

    phase.value = 'connecting'
    teardownPeers()

    try {
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true })
      localStreamRef.value = stream

      const pc = new RTCPeerConnection({
        iceServers: [{ urls: 'stun:stun.l.google.com:19302' }],
      })
      pcRef.value = pc

      pc.ontrack = (ev) => {
        const [remoteStream] = ev.streams
        if (remoteAudioRef.value && remoteStream) {
          remoteAudioRef.value.srcObject = remoteStream
          void remoteAudioRef.value.play().catch(() => {})
        }
      }

      const wireDc = (ch: RTCDataChannel) => {
        ch.onmessage = (m) => {
          if (typeof m.data === 'string') handleDataMessage(m.data)
        }
      }
      wireDc(pc.createDataChannel('oai-events'))
      pc.ondatachannel = (ev) => wireDc(ev.channel)

      for (const track of stream.getAudioTracks()) {
        pc.addTrack(track, stream)
      }

      const offer = await pc.createOffer()
      await pc.setLocalDescription(offer)
      await waitIceGatheringComplete(pc)

      const sdp = pc.localDescription?.sdp
      if (!sdp) {
        throw new Error('Missing local SDP')
      }

      const res = await fetch('/api/realtime/webrtc', {
        method: 'POST',
        credentials: 'include',
        headers: {
          'Content-Type': 'application/json',
          'X-Chat-Language': language.value,
        },
        body: JSON.stringify({ sdp }),
      })

      const raw = await res.text()
      let data: { sdp?: string; error?: string } = {}
      if (raw) {
        try {
          data = JSON.parse(raw) as { sdp?: string; error?: string }
        } catch {
          /* non-JSON error body */
        }
      }
      if (!res.ok || !data.sdp) {
        throw new Error(data.error ?? `Realtime handshake failed (${res.status})`)
      }

      await pc.setRemoteDescription({ type: 'answer', sdp: data.sdp })
      phase.value = 'connected'

      sessionTimer = setTimeout(() => {
        stop()
      }, sessionMs.value)
    } catch (e) {
      teardownPeers()
      phase.value = 'error'
      errorMessage.value =
        e instanceof Error
          ? e.message
          : language.value === 'no'
            ? 'Kunne ikke starte stemme.'
            : 'Could not start voice.'
    }
  }

  function stop(): void {
    teardownPeers()
    phase.value = 'idle'
  }

  onUnmounted(() => {
    teardownPeers()
  })

  return {
    phase,
    errorMessage,
    status,
    userTranscript,
    assistantTranscript,
    assistantBuffer,
    remoteAudioRef,
    sessionMs,
    loadStatus,
    start,
    stop,
  }
}
