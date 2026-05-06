import { defineStore } from 'pinia'
import {
  listChatModels,
  type ChatModelOption,
  ChatModelOptionProvider,
  ModelTag,
} from '@/api/generated/portfolio'
import { useAuthStore } from '@/stores/auth'

// Mirrors backend allow-list: only models returned by GET /chat/models are selectable in the UI.
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

    /** Switch UI to a provider: signed-in users prefer REASONING; anonymous prefer FAST (premium gate). */
    selectFirstForProvider(p: ChatProvider) {
      const list = this.modelsForProvider(p)
      useAuthStore().restore()
      const authed = Boolean(useAuthStore().basicToken)
      const pick = authed
        ? list.find((m) => m.tags?.includes(ModelTag.REASONING)) ?? list[0]
        : list.find((m) => m.tags?.includes(ModelTag.FAST)) ?? list[0]
      if (pick?.id) {
        this.setSelectedModelId(pick.id)
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
        useAuthStore().restore()
        const authed = Boolean(useAuthStore().basicToken)
        const first = authed
          ? this.models.find((m) => m.tags?.includes(ModelTag.REASONING)) ?? this.models[0]
          : this.models.find((m) => m.tags?.includes(ModelTag.FAST)) ?? this.models[0]
        if (first.id) {
          this.selectedModelId = first.id
          this.persistModelId()
        }
      }
    },

    /** Fetches catalog once; coalesces parallel callers via loadInFlight. Safe to call from ChatView on mount. */
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
