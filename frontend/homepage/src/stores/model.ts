import { defineStore } from 'pinia'
import {
  listChatModels,
  type ChatModelOption,
  ChatModelOptionProvider,
} from '@/api/generated/portfolio'

const MODEL_STORAGE_KEY = 'chatSelectedModel'

export type ChatProvider = (typeof ChatModelOptionProvider)[keyof typeof ChatModelOptionProvider]

export function isChatProvider(p: string | undefined): p is ChatProvider {
  return p === ChatModelOptionProvider.OPENAI || p === ChatModelOptionProvider.ANTHROPIC
}

export const useChatModelStore = defineStore('chatModel', {
  state: () => ({
    models: [] as ChatModelOption[],
    selectedModelId: '' as string,
    loadInFlight: null as Promise<void> | null,
  }),

  getters: {
    modelsForProvider:
      (state) =>
      (p: ChatProvider): ChatModelOption[] =>
        state.models.filter((m) => m.provider === p && m.id),

    selectedModel(state): ChatModelOption | undefined {
      return state.models.find((m) => m.id === state.selectedModelId)
    },

    activeProvider(): ChatProvider | null {
      const p = this.selectedModel?.provider
      return isChatProvider(p) ? p : null
    },

    hasOpenAI(): boolean {
      return this.models.some((m) => m.provider === ChatModelOptionProvider.OPENAI)
    },

    hasAnthropic(): boolean {
      return this.models.some((m) => m.provider === ChatModelOptionProvider.ANTHROPIC)
    },
  },

  actions: {
    persistModelId() {
      if (!this.selectedModelId) return
      try {
        sessionStorage.setItem(MODEL_STORAGE_KEY, this.selectedModelId)
      } catch {
        // ignore
      }
    },

    setSelectedModelId(id: string) {
      this.selectedModelId = id
      this.persistModelId()
    },

    /** Switch UI to a provider and pick its first available model. */
    selectFirstForProvider(p: ChatProvider) {
      const list = this.modelsForProvider(p)
      if (list.length > 0 && list[0].id) {
        this.setSelectedModelId(list[0].id)
      }
    },

    applyInitialSelection() {
      try {
        const stored = sessionStorage.getItem(MODEL_STORAGE_KEY)
        if (stored && this.models.some((m) => m.id === stored)) {
          this.selectedModelId = stored
          return
        }
      } catch {
        // ignore
      }
      if (this.models.length > 0) {
        const first = this.models[0]
        if (first.id) {
          this.selectedModelId = first.id
          this.persistModelId()
        }
      }
    },

    async ensureModelsLoaded(): Promise<void> {
      if (this.models.length > 0) {
        if (!this.selectedModelId) {
          this.applyInitialSelection()
        }
        return
      }
      if (this.loadInFlight) {
        return this.loadInFlight
      }
      this.loadInFlight = (async () => {
        try {
          const r = await listChatModels()
          if (r.status !== 200) {
            return
          }
          this.models = (r.data ?? []).filter((m) => m.id)
          this.applyInitialSelection()
        } catch {
          // non-fatal: backend applies default model
        }
      })().finally(() => {
        this.loadInFlight = null
      })
      return this.loadInFlight
    },
  },
})
