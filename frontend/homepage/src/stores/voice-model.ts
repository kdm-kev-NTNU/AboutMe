import { defineStore } from 'pinia'
import {
  fetchRealtimeVoiceModels,
  type RealtimeVoiceModelOption,
  type RealtimeVoiceProvider,
} from '@/lib/realtime-voice'

const VOICE_MODEL_STORAGE_KEY = 'voiceSelectedModel'

function makeVoiceModelKey(provider: RealtimeVoiceProvider, id: string): string {
  return `${provider}:${id}`
}

function resolveStoredVoiceModel(models: RealtimeVoiceModelOption[], stored: string): RealtimeVoiceModelOption | undefined {
  const trimmed = stored.trim()
  if (!trimmed) return undefined

  const separatorIndex = trimmed.indexOf(':')
  if (separatorIndex > 0) {
    const provider = trimmed.slice(0, separatorIndex)
    const id = trimmed.slice(separatorIndex + 1)
    if (provider && id) {
      return models.find((m) => m.provider === provider && m.id === id)
    }
  }

  return models.find((m) => m.id === trimmed)
}

export const useVoiceModelStore = defineStore('voiceModel', {
  state: () => ({
    models: [] as RealtimeVoiceModelOption[],
    selectedModelId: '' as string,
    loadInFlight: null as Promise<void> | null,
  }),

  getters: {
    selectedModel(state): RealtimeVoiceModelOption | undefined {
      return resolveStoredVoiceModel(state.models, state.selectedModelId)
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
      const selected = resolveStoredVoiceModel(this.models, id)
      if (!selected) return
      this.selectedModelId = makeVoiceModelKey(selected.provider, selected.id)
      this.persistModelId()
    },

    applyInitialSelection() {
      try {
        const stored = sessionStorage.getItem(VOICE_MODEL_STORAGE_KEY)
        const selected = stored ? resolveStoredVoiceModel(this.models, stored) : undefined
        if (selected) {
          this.selectedModelId = makeVoiceModelKey(selected.provider, selected.id)
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
        this.selectedModelId = makeVoiceModelKey(pick.provider, pick.id)
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
