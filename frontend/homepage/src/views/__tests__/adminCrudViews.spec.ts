import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'
import AdminChunksView from '../AdminChunksView.vue'
import AdminPipelineView from '../AdminPipelineView.vue'
import AdminPromptsView from '../AdminPromptsView.vue'
import AdminExperimentsView from '../AdminExperimentsView.vue'
import {
  adminDocumentsChunks,
  adminDocumentsCollections,
  adminDocumentsDelete,
  adminDocumentsFiles,
  adminDocumentsIngestByPath,
  adminDocumentsList,
  adminDocumentsReseed,
  healthChroma,
  listChatModels,
  promptVersionsActivate,
  promptVersionsDeleteVariant,
  promptVersionsDiff,
  promptVersionsHistory,
  promptVersionsNames,
  promptVersionsSeed,
} from '@/api/generated/portfolio'

const headersJson = new Headers({ 'Content-Type': 'application/json' })

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
    adminDocumentsReseed: vi.fn(),
    adminDocumentsUpload: vi.fn(),
    adminDocumentsIngestByPath: vi.fn(),
    adminDocumentsDelete: vi.fn(),
    promptVersionsNames: vi.fn(),
    promptVersionsHistory: vi.fn(),
    promptVersionsDiff: vi.fn(),
    promptVersionsActivate: vi.fn(),
    promptVersionsCreate: vi.fn(),
    promptVersionsSeed: vi.fn(),
    promptVersionsDeleteVariant: vi.fn(),
  }
})

function setupListAndCollections() {
  vi.mocked(listChatModels).mockResolvedValue({ status: 200, data: [], headers: new Headers() })
  vi.mocked(healthChroma).mockResolvedValue({
    status: 200,
    data: { healthy: true, collectionName: 'c', embeddingCount: 0 },
    headers: headersJson,
  })
  vi.mocked(adminDocumentsList).mockResolvedValue({
    status: 200,
    data: [
      {
        documentId: 'hash-abc',
        filename: 'readme.pdf',
        chunkCount: 3,
      },
    ],
    headers: headersJson,
  })
  vi.mocked(adminDocumentsCollections).mockResolvedValue({
    status: 200,
    data: {
      activeCollectionName: 'portfolio-documents',
      activeCollectionEmbeddingCount: 12,
      collections: [{ id: 'col-1', name: 'portfolio-documents' }],
    },
    headers: headersJson,
  })
  vi.mocked(adminDocumentsFiles).mockResolvedValue({
    status: 200,
    data: ['/data/docs/a.md'],
    headers: headersJson,
  })
}

function mountAdmin(component: Parameters<typeof mount>[0]) {
  const pinia = createPinia()
  setActivePinia(pinia)
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [{ path: '/', name: 'home', component: { template: '<div />' } }],
  })
  return mount(component, {
    global: {
      plugins: [pinia, router],
      stubs: {
        RouterLink: { template: '<a><slot /></a>', props: ['to'] },
      },
    },
  })
}

describe('Admin CRUD views (integration-style)', () => {
  beforeEach(() => {
    sessionStorage.clear()
    vi.clearAllMocks()
    setupListAndCollections()
  })

  afterEach(() => {
    vi.restoreAllMocks()
    vi.unstubAllGlobals()
  })

  it('AdminChunksView loads chunk rows, expands text, and paginates', async () => {
    const longText = 'L'.repeat(220)
    vi.mocked(adminDocumentsChunks)
      .mockResolvedValueOnce({
        status: 200,
        data: {
          collectionName: 'portfolio-documents',
          total: 60,
          totalMatching: 40,
          limit: 25,
          offset: 0,
          chunks: [
            {
              id: 'chunk-1',
              documentTitle: 'readme.pdf',
              chunkIndex: 0,
              text: longText,
              metadata: { content_hash: 'abcdef0123456789' },
            },
          ],
        },
        headers: headersJson,
      })
      .mockResolvedValueOnce({
        status: 200,
        data: {
          collectionName: 'portfolio-documents',
          total: 60,
          totalMatching: 40,
          limit: 25,
          offset: 25,
          chunks: [
            {
              id: 'chunk-2',
              documentTitle: 'readme.pdf',
              chunkIndex: 1,
              text: 'short',
              metadata: {},
            },
          ],
        },
        headers: headersJson,
      })

    const wrapper = mountAdmin(AdminChunksView)
    await flushPromises()

    const fetchChunksBtn = wrapper.findAll('button').find((b) => b.text().includes('Hent chunks'))
    expect(fetchChunksBtn).toBeTruthy()
    await fetchChunksBtn!.trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('Vis mer')
    const visMer = wrapper.findAll('button').find((b) => b.text().includes('Vis mer'))
    await visMer!.trigger('click')
    await flushPromises()
    expect(wrapper.text()).toContain('Vis mindre')

    const nextBtn = wrapper.findAll('button').find((b) => b.text().includes('Neste'))
    expect(nextBtn?.attributes('disabled')).toBeUndefined()
    await nextBtn!.trigger('click')
    await flushPromises()
    const lastChunksArg = vi.mocked(adminDocumentsChunks).mock.calls.at(-1)?.[0] as {
      offset?: number
    }
    expect(lastChunksArg).toMatchObject({ offset: 25 })
    expect(wrapper.text()).toContain('short')

    const shortRow = wrapper.findAll('tbody tr').find((r) => r.text().includes('short'))
    expect(shortRow).toBeTruthy()
    await shortRow!.trigger('click')
    await flushPromises()
    expect(wrapper.text()).toContain('Chunk-ID')

    const prevBtn = wrapper.findAll('button').find((b) => b.text().includes('Forrige'))
    await prevBtn!.trigger('click')
    await flushPromises()
    const backArg = vi.mocked(adminDocumentsChunks).mock.calls.at(-1)?.[0] as { offset?: number }
    expect(backArg).toMatchObject({ offset: 0 })
  })

  it('AdminPipelineView completes reseed when confirmed', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true)
    vi.mocked(adminDocumentsReseed).mockResolvedValue({
      status: 200,
      data: [
        {
          filename: 'seed.md',
          chunksIngested: 2,
          skipped: false,
          message: 'OK',
        },
      ],
      headers: headersJson,
    })

    const wrapper = mountAdmin(AdminPipelineView)
    await flushPromises()

    const reseedBtn = wrapper.findAll('button').find((b) => b.text().includes('Re-seed seed-dokumenter'))
    expect(reseedBtn).toBeTruthy()
    await reseedBtn!.trigger('click')
    await flushPromises()

    expect(wrapper.text()).toMatch(/Reseed ferdig/)
  })

  it('AdminPipelineView shows message when upload has no files', async () => {
    const wrapper = mountAdmin(AdminPipelineView)
    await flushPromises()
    const uploadSummary = wrapper.findAll('summary').find((s) => s.text().includes('Last opp'))
    expect(uploadSummary).toBeTruthy()
    await uploadSummary!.trigger('click')
    await flushPromises()
    const ingestBtn = wrapper.findAll('button').find((b) => b.text().includes('Kjør ingest'))
    expect(ingestBtn).toBeTruthy()
    await ingestBtn!.trigger('click')
    await flushPromises()
    expect(wrapper.text()).toContain('Velg minst én fil')
  })

  it('AdminPipelineView runs path ingest for selected server files', async () => {
    vi.mocked(adminDocumentsIngestByPath).mockResolvedValue({
      status: 200,
      data: [
        {
          filename: '/data/docs/a.md',
          chunksIngested: 4,
          skipped: false,
          message: 'OK',
        },
      ],
      headers: headersJson,
    })
    const wrapper = mountAdmin(AdminPipelineView)
    await flushPromises()
    const batchSummary = wrapper.findAll('summary').find((s) => s.text().includes('Batch-ingest'))
    expect(batchSummary).toBeTruthy()
    await batchSummary!.trigger('click')
    await flushPromises()
    const cb = wrapper.find('#srv-file-0')
    expect(cb.exists()).toBe(true)
    await cb.setValue(true)
    await flushPromises()
    const pathBtn = wrapper
      .findAll('button')
      .find((b) => b.text().includes('Kjør batch-ingest (valgte filer)'))
    expect(pathBtn).toBeTruthy()
    await pathBtn!.trigger('click')
    await flushPromises()
    expect(adminDocumentsIngestByPath).toHaveBeenCalled()
    expect(wrapper.text()).toContain('/data/docs/a.md')
  })

  it('AdminPipelineView deletes a document when confirmed', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true)
    vi.mocked(adminDocumentsDelete).mockResolvedValue({ status: 204, data: undefined, headers: headersJson })
    const wrapper = mountAdmin(AdminPipelineView)
    await flushPromises()
    const delBtn = wrapper.findAll('button').find((b) => b.text() === 'Slett')
    expect(delBtn).toBeTruthy()
    await delBtn!.trigger('click')
    await flushPromises()
    expect(adminDocumentsDelete).toHaveBeenCalledWith('hash-abc')
  })

  it('AdminPipelineView surfaces 401 from document list', async () => {
    vi.mocked(adminDocumentsList).mockResolvedValue({
      status: 401,
      data: { error: 'Unauthorized' },
      headers: headersJson,
    } as unknown as Awaited<ReturnType<typeof adminDocumentsList>>)
    vi.mocked(adminDocumentsCollections).mockResolvedValue({
      status: 200,
      data: { activeCollectionName: 'x', collections: [], activeCollectionEmbeddingCount: 0 },
      headers: headersJson,
    })
    const wrapper = mountAdmin(AdminPipelineView)
    await flushPromises()
    expect(wrapper.text()).toContain('Ikke autorisert')
  })

  it('AdminPromptsView loads history and diff for a variant', async () => {
    vi.mocked(promptVersionsNames).mockResolvedValue({
      status: 200,
      data: [
        {
          name: 'rag_portfolio',
          language: 'en',
          provider: 'openai',
          activeVersion: 2,
          activeId: 10,
          createdAt: '2026-01-01T12:00:00Z',
        },
      ],
      headers: headersJson,
    })
    vi.mocked(promptVersionsHistory).mockResolvedValue({
      status: 200,
      data: [
        {
          id: 10,
          name: 'rag_portfolio',
          version: 2,
          language: 'en',
          provider: 'openai',
          content: 'You are helpful.',
          contentHash: 'hashhashhashhash',
          isActive: true,
          description: 'baseline',
          createdAt: '2026-01-01T12:00:00Z',
        },
        {
          id: 9,
          name: 'rag_portfolio',
          version: 1,
          language: 'en',
          provider: 'openai',
          content: 'Old.',
          contentHash: 'oldoldoldoldold',
          isActive: false,
          createdAt: '2025-12-01T12:00:00Z',
        },
      ],
      headers: headersJson,
    })
    vi.mocked(promptVersionsDiff).mockResolvedValue({
      status: 200,
      data: {
        name: 'rag_portfolio',
        language: 'en',
        provider: 'openai',
        hasDbActive: true,
        hasCodeFallback: true,
        isEqual: false,
        dbContent: 'db',
        fallbackContent: 'cp',
      },
      headers: headersJson,
    })

    const wrapper = mountAdmin(AdminPromptsView)
    await flushPromises()

    const row = wrapper.findAll('tbody tr').find((r) => r.text().includes('rag_portfolio'))
    expect(row).toBeTruthy()
    await row!.trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('Historikk:')
    expect(wrapper.text()).toContain('v2')

    const diffBtn = wrapper.findAll('button').find((b) => b.text() === 'Diff')
    expect(diffBtn).toBeTruthy()
    await diffBtn!.trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('Classpath fallback')
  })

  it('AdminPromptsView validates create form and can activate an inactive version', async () => {
    vi.mocked(promptVersionsNames).mockResolvedValue({
      status: 200,
      data: [
        {
          name: 'rag_portfolio',
          language: 'en',
          provider: 'openai',
          activeVersion: 2,
          activeId: 10,
          createdAt: '2026-01-01T12:00:00Z',
        },
      ],
      headers: headersJson,
    })
    vi.mocked(promptVersionsHistory).mockResolvedValue({
      status: 200,
      data: [
        {
          id: 10,
          version: 2,
          isActive: true,
          content: 'A',
          contentHash: 'h2',
          createdAt: '2026-01-01T12:00:00Z',
        },
        {
          id: 9,
          version: 1,
          isActive: false,
          content: 'B',
          contentHash: 'h1',
          createdAt: '2025-12-01T12:00:00Z',
        },
      ],
      headers: headersJson,
    })
    vi.mocked(promptVersionsActivate).mockResolvedValue({
      status: 200,
      data: { id: 9, version: 1, isActive: true },
      headers: headersJson,
    })

    const wrapper = mountAdmin(AdminPromptsView)
    await flushPromises()

    const nyBtn = wrapper.findAll('button').find((b) => b.text().includes('Ny versjon'))
    await nyBtn!.trigger('click')
    await flushPromises()
    const createBtn = wrapper.findAll('button').find((b) => b.text().includes('Opprett versjon'))
    await createBtn!.trigger('click')
    await flushPromises()
    expect(wrapper.text()).toContain('Navn og innhold er påkrevd')

    const row = wrapper.findAll('tbody tr').find((r) => r.text().includes('rag_portfolio'))
    await row!.trigger('click')
    await flushPromises()
    const aktiver = wrapper.findAll('button').find((b) => b.text() === 'Aktiver')
    expect(aktiver).toBeTruthy()
    await aktiver!.trigger('click')
    await flushPromises()
    expect(promptVersionsActivate).toHaveBeenCalled()
  })

  it('AdminPromptsView expands a history row to show full content', async () => {
    vi.mocked(promptVersionsNames).mockResolvedValue({
      status: 200,
      data: [{ name: 'p', language: null, provider: null, activeVersion: 1, activeId: 1 }],
      headers: headersJson,
    })
    vi.mocked(promptVersionsHistory).mockResolvedValue({
      status: 200,
      data: [
        {
          id: 1,
          version: 1,
          isActive: true,
          content: 'full prompt text',
          contentHash: 'cccccccccccc',
          createdAt: '2026-01-02T00:00:00Z',
        },
      ],
      headers: headersJson,
    })
    const wrapper = mountAdmin(AdminPromptsView)
    await flushPromises()
    await wrapper.find('tbody tr').trigger('click')
    await flushPromises()
    const histRow = wrapper.findAll('tbody tr').find((r) => r.text().includes('cccccccccccc'))
    expect(histRow).toBeTruthy()
    await histRow!.trigger('click')
    await flushPromises()
    expect(wrapper.text()).toContain('full prompt text')
  })

  it('AdminExperimentsView shows validation when starting run without dataset', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn(async (input: RequestInfo | URL) => {
        const url =
          typeof input === 'string' ? input : input instanceof Request ? input.url : String(input)
        if (url.includes('/api/admin/tools/experiments/config')) {
          return new Response(JSON.stringify({ posthogConfigured: true, posthogHost: 'https://eu.i.posthog.com' }), {
            status: 200,
            headers: headersJson,
          })
        }
        if (url.includes('/api/admin/tools/documents')) {
          return new Response(JSON.stringify([]), { status: 200, headers: headersJson })
        }
        if (url.includes('/api/admin/tools/experiments/datasets') && !url.includes('/generate')) {
          return new Response(JSON.stringify([{ id: 'ds1', name: 'Demo', exampleCount: 3 }]), {
            status: 200,
            headers: headersJson,
          })
        }
        if (url.includes('/api/admin/tools/experiments/models')) {
          return new Response(
            JSON.stringify([
              { id: 'gpt-test', label: 'Test', provider: 'OPENAI' },
              { id: 'claude-judge', label: 'Judge', provider: 'ANTHROPIC' },
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

    const wrapper = mountAdmin(AdminExperimentsView)
    await flushPromises()
    await vi.waitFor(() => {
      expect(wrapper.text()).toContain('RAG-experiments')
    })

    const startBtn = wrapper.findAll('button').find((b) => b.text().includes('Start experiment'))
    expect(startBtn?.attributes('disabled')).toBeUndefined()
    await startBtn!.trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('Velg et datasett.')
  })

  it('AdminExperimentsView evaluator dropdown only lists models from the other provider', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn(async (input: RequestInfo | URL) => {
        const url =
          typeof input === 'string' ? input : input instanceof Request ? input.url : String(input)
        if (url.includes('/api/admin/tools/experiments/config')) {
          return new Response(JSON.stringify({ posthogConfigured: true, posthogHost: 'https://eu.i.posthog.com' }), {
            status: 200,
            headers: headersJson,
          })
        }
        if (url.includes('/api/admin/tools/documents')) {
          return new Response(JSON.stringify([]), { status: 200, headers: headersJson })
        }
        if (url.includes('/api/admin/tools/experiments/datasets') && !url.includes('/generate')) {
          return new Response(JSON.stringify([{ id: 'ds1', name: 'Demo', exampleCount: 1 }]), {
            status: 200,
            headers: headersJson,
          })
        }
        if (url.includes('/api/admin/tools/experiments/models')) {
          return new Response(
            JSON.stringify([
              { id: 'o1', label: 'O1', provider: 'OPENAI' },
              { id: 'o2', label: 'O2', provider: 'OPENAI' },
              { id: 'a1', label: 'A1', provider: 'ANTHROPIC' },
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

    const wrapper = mountAdmin(AdminExperimentsView)
    await flushPromises()
    await vi.waitFor(() => {
      expect(wrapper.text()).toContain('RAG-experiments')
    })

    const genSelect = wrapper.find('[data-testid="exp-generator-select"]')
    const evSelect = wrapper.find('[data-testid="exp-evaluator-select"]')
    expect(genSelect.exists()).toBe(true)
    expect(evSelect.exists()).toBe(true)

    expect(genSelect.element.value).toBe('o1')
    expect(evSelect.findAll('option')).toHaveLength(1)
    expect(evSelect.findAll('option')[0]!.text()).toContain('A1')

    await genSelect.setValue('a1')
    await flushPromises()
    expect(evSelect.findAll('option').length).toBe(2)
    const labels = evSelect.findAll('option').map((o) => o.text())
    expect(labels.some((t) => t.includes('O1'))).toBe(true)
    expect(labels.some((t) => t.includes('O2'))).toBe(true)
  })

  it('AdminExperimentsView shows dataset error when list fails', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn(async (input: RequestInfo | URL) => {
        const url =
          typeof input === 'string' ? input : input instanceof Request ? input.url : String(input)
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
          return new Response(JSON.stringify({ error: 'Database utilgjengelig' }), { status: 500, headers: headersJson })
        }
        if (url.includes('/api/admin/tools/experiments/models')) {
          return new Response(
            JSON.stringify([
              { id: 'm1', label: 'M', provider: 'OPENAI' },
              { id: 'c1', label: 'C', provider: 'ANTHROPIC' },
            ]),
            {
              status: 200,
              headers: headersJson,
            },
          )
        }
        if (url.includes('/api/admin/tools/experiments/runs')) {
          return new Response(JSON.stringify([]), { status: 200, headers: headersJson })
        }
        return new Response('{}', { status: 404, headers: headersJson })
      }),
    )
    const wrapper = mountAdmin(AdminExperimentsView)
    await flushPromises()
    await vi.waitFor(() => {
      expect(wrapper.text()).toContain('Database utilgjengelig')
    })
  })

  it('AdminPromptsView runs classpath seed when confirmed', async () => {
    vi.stubGlobal('confirm', vi.fn(() => true))
    vi.mocked(promptVersionsNames).mockResolvedValue({ status: 200, data: [], headers: headersJson })
    vi.mocked(promptVersionsSeed).mockResolvedValue({
      status: 200,
      data: { created: 2, skipped: 1 },
      headers: headersJson,
    })
    const wrapper = mountAdmin(AdminPromptsView)
    await flushPromises()
    const seedBtn = wrapper.findAll('button').find((b) => b.text().includes('Seed fra classpath'))
    expect(seedBtn).toBeTruthy()
    await seedBtn!.trigger('click')
    await flushPromises()
    await vi.waitFor(() => {
      expect(promptVersionsSeed).toHaveBeenCalled()
    })
    // successMsg is cleared again when loadNames() runs (clearMessages); API outcome is what we verify.
    expect(vi.mocked(promptVersionsNames).mock.calls.length).toBeGreaterThanOrEqual(2)
  })

  it('AdminPromptsView deletes a variant when confirmed', async () => {
    vi.stubGlobal('confirm', vi.fn(() => true))
    vi.mocked(promptVersionsNames).mockResolvedValue({
      status: 200,
      data: [{ name: 'x', language: 'en', provider: 'openai', activeVersion: 1, activeId: 1 }],
      headers: headersJson,
    })
    vi.mocked(promptVersionsDeleteVariant).mockResolvedValue({
      status: 200,
      data: { deleted: 3 },
      headers: headersJson,
    })
    const wrapper = mountAdmin(AdminPromptsView)
    await flushPromises()
    const slett = wrapper.findAll('button').find((b) => b.text() === 'Slett')
    expect(slett).toBeTruthy()
    await slett!.trigger('click')
    await flushPromises()
    expect(promptVersionsDeleteVariant).toHaveBeenCalled()
    expect(vi.mocked(promptVersionsNames).mock.calls.length).toBeGreaterThanOrEqual(2)
  })

  it('AdminExperimentsView lists runs and opens detail with formatted scores', async () => {
    const runSummary = {
      id: 7,
      name: 'Eval run',
      datasetName: 'Demo',
      generatorModel: 'g1',
      evaluatorModel: 'e1',
      status: 'COMPLETED',
      totalExamples: 1,
      meanFaithfulness: 0.81234,
      meanRelevance: null,
      meanCorrectness: 0.5,
      meanConciseness: Number.NaN,
      errorMessage: null,
      createdAt: '2026-01-01T00:00:00Z',
      completedAt: '2026-01-01T00:01:00Z',
    }
    const runDetail = {
      ...runSummary,
      evalDatasetId: 1,
      posthogHost: 'https://eu.i.posthog.com',
      results: [
        {
          id: 1,
          question: 'Q?',
          referenceAnswer: 'A',
          ragResponse: 'R',
          documentsPreview: null,
          faithfulness: 0.9,
          relevance: 0.8,
          correctness: null,
          conciseness: 0.7,
          faithfulnessExplanation: null,
          relevanceExplanation: null,
          correctnessExplanation: null,
          concisenessExplanation: null,
        },
      ],
    }
    vi.stubGlobal(
      'fetch',
      vi.fn(async (input: RequestInfo | URL) => {
        const url =
          typeof input === 'string' ? input : input instanceof Request ? input.url : String(input)
        if (url.includes('/api/admin/tools/experiments/config')) {
          return new Response(JSON.stringify({ posthogConfigured: true, posthogHost: 'https://eu.i.posthog.com' }), {
            status: 200,
            headers: headersJson,
          })
        }
        if (url.includes('/api/admin/tools/documents')) {
          return new Response(JSON.stringify([]), { status: 200, headers: headersJson })
        }
        if (url.includes('/api/admin/tools/experiments/datasets') && !url.includes('/generate')) {
          return new Response(JSON.stringify([{ id: 'ds1', name: 'Demo', exampleCount: 1 }]), {
            status: 200,
            headers: headersJson,
          })
        }
        if (url.includes('/api/admin/tools/experiments/models')) {
          return new Response(
            JSON.stringify([
              { id: 'g1', label: 'G', provider: 'OPENAI' },
              { id: 'e1', label: 'E', provider: 'ANTHROPIC' },
            ]),
            { status: 200, headers: headersJson },
          )
        }
        if (url.endsWith('/api/admin/tools/experiments/runs')) {
          return new Response(JSON.stringify([runSummary]), { status: 200, headers: headersJson })
        }
        if (url.includes('/api/admin/tools/experiments/runs/7') && !url.includes('status')) {
          return new Response(JSON.stringify(runDetail), { status: 200, headers: headersJson })
        }
        return new Response('{}', { status: 404, headers: headersJson })
      }),
    )
    const wrapper = mountAdmin(AdminExperimentsView)
    await flushPromises()
    await vi.waitFor(() => {
      expect(wrapper.text()).toContain('#7')
    })
    const runBtn = wrapper.findAll('button').find((b) => b.text().includes('#7'))
    expect(runBtn).toBeTruthy()
    await runBtn!.trigger('click')
    await flushPromises()
    expect(wrapper.text()).toContain('0.812')
    expect(wrapper.text()).toContain('PostHog (LLM-observabilitet)')
    expect(wrapper.text()).toContain('Eval-datasett-ID:')
    expect(wrapper.text()).toContain('Q?')
  })
})
