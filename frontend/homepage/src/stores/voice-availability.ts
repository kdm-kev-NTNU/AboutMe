import { defineStore } from 'pinia'
import {
  fetchRealtimeVoiceStatus,
  type RealtimeReasoningEffort,
  type RealtimeVadEagerness,
  type RealtimeVoiceChoice,
  type RealtimeVoiceStatus,
} from '@/lib/realtime-voice'

export type VoiceAvailabilityState = 'unknown' | 'available' | 'unavailable'

/**
 * Shared public voice availability (and admin capability) from GET /realtime/status.
 * Dedupes the three previous independent fetchRealtimeVoiceStatus call sites.
 */
export const useVoiceAvailabilityStore = defineStore('voiceAvailability', {
  state: () => ({
    /** Public SPA state derived from liveEnabled. */
    state: 'unknown' as VoiceAvailabilityState,
    /** Realtime capability for admin interview (from enabled). */
    capability: null as boolean | null,
    liveDisabledReason: null as 'KILL_SWITCH' | null,
    status: null as RealtimeVoiceStatus | null,
    loadInFlight: null as Promise<void> | null,
  }),

  getters: {
    isPubliclyAvailable(state): boolean {
      return state.state === 'available'
    },
    voices(state): RealtimeVoiceChoice[] {
      return state.status?.voices ?? ['marin', 'cedar']
    },
    reasoningEfforts(state): RealtimeReasoningEffort[] {
      return state.status?.reasoningEfforts ?? ['low', 'medium', 'high']
    },
    vadEagernessOptions(state): RealtimeVadEagerness[] {
      return state.status?.vadEagernessOptions ?? ['low', 'medium', 'high', 'auto']
    },
    defaultVoice(state): RealtimeVoiceChoice {
      return state.status?.voice ?? 'marin'
    },
    defaultReasoningEffort(state): RealtimeReasoningEffort {
      return state.status?.reasoningEffort ?? 'low'
    },
    defaultVadEagerness(state): RealtimeVadEagerness {
      return state.status?.vadEagerness ?? 'low'
    },
  },

  actions: {
    async ensureLoaded(): Promise<void> {
      if (this.state !== 'unknown' && this.status) {
        return
      }
      if (this.loadInFlight) {
        return this.loadInFlight
      }
      this.loadInFlight = (async () => {
        const status = await fetchRealtimeVoiceStatus()
        this.status = status
        this.capability = status.enabled
        this.liveDisabledReason = status.liveDisabledReason
        this.state = status.liveEnabled ? 'available' : 'unavailable'
      })().finally(() => {
        this.loadInFlight = null
      })
      return this.loadInFlight
    },

    /** Force a refresh (e.g. after admin toggles the kill switch). */
    async refresh(): Promise<void> {
      this.state = 'unknown'
      this.status = null
      this.capability = null
      this.loadInFlight = null
      await this.ensureLoaded()
    },
  },
})
