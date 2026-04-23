import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'
import HomeView from '../HomeView.vue'
import ChatHistory from '../ChatHistory.vue'
import AdminToolsView from '../AdminToolsView.vue'
import AdminChunksView from '../AdminChunksView.vue'
import AdminPipelineView from '../AdminPipelineView.vue'
import AdminPromptsView from '../AdminPromptsView.vue'
import AdminExperimentsView from '../AdminExperimentsView.vue'
import { useLangStore } from '@/stores/lang'
import {
  listChatModels,
  healthChroma,
  adminDocumentsList,
  adminDocumentsCollections,
  adminDocumentsFiles,
  adminDocumentsChunks,
  promptVersionsNames,
} from '@/api/generated/portfolio'

vi.mock('@/api/generated/portfolio', async (importOriginal) => {
  const mod = await importOriginal<typeof import('@/api/generated/portfolio')>()
  return {
    ...mod,
    listChatModels: vi.fn(),
    healthChroma: vi.fn(),
    adminDocumentsList: vi.fn(),
    adminDocumentsCollections: vi.fn(),
    adminDocumentsFiles: vi.fn(),
    adminDocumentsChunks: vi.fn(),
    promptVersionsNames: vi.fn(),
  }
})

const headersJson = new Headers({ 'Content-Type': 'application/json' })

function setupPortfolioMocks() {
  vi.mocked(listChatModels).mockResolvedValue({ status: 200, data: [], headers: new Headers() })
  vi.mocked(healthChroma).mockResolvedValue({
    status: 200,
    data: { healthy: true, collectionName: 'c', embeddingCount: 0 },
    headers: headersJson,
  })
  vi.mocked(adminDocumentsList).mockResolvedValue({ status: 200, data: [], headers: headersJson })
  vi.mocked(adminDocumentsCollections).mockResolvedValue({
    status: 200,
    data: { activeCollectionName: 'c', collections: [], activeCollectionEmbeddingCount: 0 },
    headers: headersJson,
  })
  vi.mocked(adminDocumentsFiles).mockResolvedValue({ status: 200, data: [], headers: headersJson })
  vi.mocked(adminDocumentsChunks).mockResolvedValue({
    status: 200,
    data: { chunks: [], total: 0, limit: 25, offset: 0 },
    headers: headersJson,
  })
  vi.mocked(promptVersionsNames).mockResolvedValue({ status: 200, data: [], headers: headersJson })
}

function setupFetchForExperimentsAndHistory() {
  vi.stubGlobal(
    'fetch',
    vi.fn(async (input: RequestInfo | URL) => {
      const url =
        typeof input === 'string' ? input : input instanceof Request ? input.url : String(input)
      if (url === '/api/conversations' || url.startsWith('/api/conversations')) {
        if (url === '/api/conversations') {
          return new Response(JSON.stringify([]), { status: 200, headers: headersJson })
        }
        return new Response(
          JSON.stringify({
            id: 1,
            startedAt: '',
            endedAt: '',
            messages: [],
          }),
          { status: 200, headers: headersJson },
        )
      }
      if (url.includes('/api/admin/tools/experiments/config')) {
        return new Response(JSON.stringify({ posthogConfigured: false, posthogHost: '' }), {
          status: 200,
          headers: headersJson,
        })
      }
      if (url.includes('/api/admin/tools/documents')) {
        return new Response(JSON.stringify([]), { status: 200, headers: headersJson })
      }
      if (url.includes('/api/admin/tools/experiments/datasets') && !url.includes('/generate')) {
        return new Response(JSON.stringify([]), { status: 200, headers: headersJson })
      }
      if (url.includes('/api/admin/tools/experiments/models')) {
        return new Response(
          JSON.stringify([
            { id: 'gpt-smoke', label: 'G', provider: 'OPENAI' },
            { id: 'claude-smoke', label: 'C', provider: 'ANTHROPIC' },
          ]),
          { status: 200, headers: headersJson },
        )
      }
      if (url.includes('/api/admin/tools/experiments/runs')) {
        return new Response(JSON.stringify([]), { status: 200, headers: headersJson })
      }
      return new Response('{}', { status: 404, headers: headersJson })
    }),
  )
}

describe('HomeView, ChatHistory, admin views (smoke)', () => {
  let pinia: ReturnType<typeof createPinia>

  beforeEach(() => {
    sessionStorage.clear()
    pinia = createPinia()
    setActivePinia(pinia)
    useLangStore().setLanguage('en')
    vi.clearAllMocks()
    setupPortfolioMocks()
    setupFetchForExperimentsAndHistory()
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  function mountView(component: Parameters<typeof mount>[0]) {
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [
        { path: '/', name: 'home', component: { template: '<div />' } },
        { path: '/chat', name: 'chat', component: { template: '<div />' } },
        { path: '/chat-history', name: 'chat-history', component: { template: '<div />' } },
      ],
    })
    return mount(component, {
      global: {
        plugins: [pinia, router],
        stubs: {
          RouterLink: { template: '<a><slot /></a>', props: ['to'] },
          Github: true,
          Linkedin: true,
          Info: true,
          MessageSquare: true,
          Calendar: true,
          Eye: true,
          MessagesArea: true,
        },
      },
    })
  }

  it('renders HomeView hero', async () => {
    const wrapper = mountView(HomeView)
    await flushPromises()
    expect(wrapper.text()).toMatch(/Kevin'?s?\s+AI/i)
  })

  it('renders ChatHistory empty state in English', async () => {
    const wrapper = mountView(ChatHistory)
    await flushPromises()
    await vi.waitFor(() => {
      expect(wrapper.text()).toContain('No chat history yet')
    })
  })

  it('renders AdminToolsView hub', async () => {
    const wrapper = mountView(AdminToolsView)
    await flushPromises()
    expect(wrapper.text()).toContain('Internal tools')
  })

  it('renders AdminChunksView', async () => {
    const wrapper = mountView(AdminChunksView)
    await flushPromises()
    expect(wrapper.text()).toContain('PostgreSQL / pgvector chunks')
  })

  it('renders AdminPipelineView', async () => {
    const wrapper = mountView(AdminPipelineView)
    await flushPromises()
    expect(wrapper.text()).toContain('Document pipeline')
  })

  it('renders AdminPromptsView', async () => {
    const wrapper = mountView(AdminPromptsView)
    await flushPromises()
    expect(wrapper.text()).toContain('Prompt versions')
  })

  it('renders AdminExperimentsView', async () => {
    const wrapper = mountView(AdminExperimentsView)
    await flushPromises()
    await vi.waitFor(() => {
      expect(wrapper.text()).toContain('RAG-experiments')
    })
  })
})
