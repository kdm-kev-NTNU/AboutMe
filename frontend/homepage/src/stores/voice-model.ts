import { defineStore } from 'pinia'
import {
  fetchRealtimeVoiceModels,
  type RealtimeVoiceModelOption,
  type RealtimeVoiceProvider,
} from '@/lib/realtime-voice'

const VOICE_MODEL_STORAGE_KEY = 'voiceSelectedModel'

export const useVoiceModelStore = defineStore('voiceModel', {
  state: () => ({
    models: [] as RealtimeVoiceModelOption[],
    selectedModelId: '' as string,
    loadInFlight: null as Promise<void> | null,
  }),

  getters: {
    selectedModel(state): RealtimeVoiceModelOption | undefined {
      return state.models.find((m) => m.id === state.selectedModelId)
    },

    selectedProvider(): RealtimeVoiceProvider | null {
      return this.selectedModel?.provider ?? null
    },

    hasModels(state): boolean {
      return state.models.length > 0
    },
  },

  actions: {
    persistModelId() {
      if (!this.selectedModelId) return
      try {
        sessionStorage.setItem(VOICE_MODEL_STORAGE_KEY, this.selectedModelId)
      } catch {
        // ignore
      }
    },

    setSelectedModelId(id: string) {
      if (!this.models.some((m) => m.id === id)) return
      this.selectedModelId = id
      this.persistModelId()
    },

    applyInitialSelection() {
      try {
        const stored = sessionStorage.getItem(VOICE_MODEL_STORAGE_KEY)
        if (stored && this.models.some((m) => m.id === stored)) {
          this.selectedModelId = stored
          return
        }
      } catch {
        // ignore
      }
      const pick =
        this.models.find((m) => m.provider === 'OPENAI' && m.defaultOption) ??
        this.models.find((m) => m.provider === 'OPENAI') ??
        this.models.find((m) => m.defaultOption) ??
        this.models[0]
      if (pick?.id) {
        this.selectedModelId = pick.id
        this.persistModelId()
      }
    },

    async ensureModelsLoaded(): Promise<void> {
      if (this.models.length > 0) {
        if (!this.selectedModelId) this.applyInitialSelection()
        return
      }
      if (this.loadInFlight) {
        return this.loadInFlight
      }
      this.loadInFlight = (async () => {
        this.models = await fetchRealtimeVoiceModels()
        this.applyInitialSelection()
      })().finally(() => {
        this.loadInFlight = null
      })
      return this.loadInFlight
    },
  },
})
