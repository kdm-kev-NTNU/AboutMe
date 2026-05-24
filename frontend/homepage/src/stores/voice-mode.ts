import { defineStore } from 'pinia'

export type VoiceMode = 'standard' | 'live'

const VOICE_MODE_STORAGE_KEY = 'voiceMode'

export const useVoiceModeStore = defineStore('voiceMode', {
  state: () => ({
    mode: 'standard' as VoiceMode,
  }),
  actions: {
    load() {
      try {
        const raw = sessionStorage.getItem(VOICE_MODE_STORAGE_KEY)
        if (raw === 'standard' || raw === 'live') {
          this.mode = raw
        }
      } catch {
        // ignore
      }
    },
    setMode(mode: VoiceMode) {
      this.mode = mode
      try {
        sessionStorage.setItem(VOICE_MODE_STORAGE_KEY, mode)
      } catch {
        // ignore
      }
    },
  },
})
