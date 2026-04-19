import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { customFetch } from '../orval-mutator'

describe('customFetch', () => {
  const originalFetch = globalThis.fetch

  beforeEach(() => {
    sessionStorage.clear()
    vi.restoreAllMocks()
  })

  afterEach(() => {
    globalThis.fetch = originalFetch
  })

  it('prefixes relative paths with /api', async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(JSON.stringify({ ok: true }), {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      }),
    )
    globalThis.fetch = fetchMock as typeof fetch

    await customFetch<{ data: { ok: boolean }; status: number }>('/foo')

    expect(fetchMock).toHaveBeenCalledWith(
      '/api/foo',
      expect.objectContaining({ credentials: 'include' }),
    )
  })

  it('leaves absolute http URLs unchanged', async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      new Response('{}', {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      }),
    )
    globalThis.fetch = fetchMock as typeof fetch

    await customFetch<{ data: unknown; status: number }>('https://example.com/x')

    expect(fetchMock).toHaveBeenCalledWith(
      'https://example.com/x',
      expect.anything(),
    )
  })

  it('sets Authorization from session auth basicToken', async () => {
    sessionStorage.setItem(
      'auth',
      JSON.stringify({ username: 'u', role: 'ADMIN', basicToken: 'dGVzdA==' }),
    )
    const fetchMock = vi.fn().mockResolvedValue(
      new Response('{}', {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      }),
    )
    globalThis.fetch = fetchMock as typeof fetch

    await customFetch<{ data: unknown; status: number }>('/x')

    const init = fetchMock.mock.calls[0][1] as RequestInit
    const headers = new Headers(init.headers)
    expect(headers.get('Authorization')).toBe('Basic dGVzdA==')
  })

  it('returns undefined data for 204', async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response(null, { status: 204 }))
    globalThis.fetch = fetchMock as typeof fetch

    const r = await customFetch<{ data: unknown; status: number }>('/del')
    expect(r.status).toBe(204)
    expect(r.data).toBeUndefined()
  })

  it('returns undefined data for 205', async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response(null, { status: 205 }))
    globalThis.fetch = fetchMock as typeof fetch

    const r = await customFetch<{ data: unknown; status: number }>('/x')
    expect(r.status).toBe(205)
    expect(r.data).toBeUndefined()
  })

  it('parses JSON when content-type is application/json', async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(JSON.stringify({ a: 1 }), {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      }),
    )
    globalThis.fetch = fetchMock as typeof fetch

    const r = await customFetch<{ data: { a: number }; status: number }>('/j')
    expect(r.data).toEqual({ a: 1 })
  })

  it('uses text body when not JSON', async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      new Response('plain', {
        status: 200,
        headers: { 'Content-Type': 'text/plain' },
      }),
    )
    globalThis.fetch = fetchMock as typeof fetch

    const r = await customFetch<{ data: string; status: number }>('/t')
    expect(r.data).toBe('plain')
  })

  it('sets data undefined when json parse fails', async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      new Response('{bad', {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      }),
    )
    globalThis.fetch = fetchMock as typeof fetch

    const r = await customFetch<{ data: unknown; status: number }>('/badjson')
    expect(r.data).toBeUndefined()
    expect(r.status).toBe(200)
  })

  it('ignores malformed auth JSON in sessionStorage', async () => {
    sessionStorage.setItem('auth', 'not-json')
    const fetchMock = vi.fn().mockResolvedValue(
      new Response('{}', {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      }),
    )
    globalThis.fetch = fetchMock as typeof fetch

    await customFetch<{ data: unknown; status: number }>('/x')
    const init = fetchMock.mock.calls[0][1] as RequestInit
    const headers = new Headers(init.headers)
    expect(headers.get('Authorization')).toBeNull()
  })
})
