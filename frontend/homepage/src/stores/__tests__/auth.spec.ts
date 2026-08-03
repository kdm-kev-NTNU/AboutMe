import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { authLogin, authLogout, authMe } from '@/api/generated/portfolio'
import { useAuthStore } from '../auth'

vi.mock('@/api/generated/portfolio', async (importOriginal) => {
  const mod = await importOriginal<typeof import('@/api/generated/portfolio')>()
  return { ...mod, authLogin: vi.fn(), authLogout: vi.fn(), authMe: vi.fn() }
})

describe('auth store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    sessionStorage.clear()
    vi.mocked(authLogin).mockReset()
    vi.mocked(authLogout).mockReset()
    vi.mocked(authMe).mockReset()
    vi.mocked(authMe).mockResolvedValue({
      status: 200,
      data: { username: 'GOAT', role: 'ADMIN' },
      headers: new Headers(),
    } as never)
  })

  it('stores username and role after successful login', async () => {
    vi.mocked(authLogin).mockResolvedValue({
      status: 200,
      data: { username: 'GOAT', role: 'ADMIN' },
      headers: new Headers(),
    } as never)

    const store = useAuthStore()
    await store.login('GOAT', 'secret')

    expect(store.username).toBe('GOAT')
    expect(store.role).toBe('ADMIN')
    expect(store.isAdmin).toBe(true)
    expect(sessionStorage.getItem('auth')).toContain('"username":"GOAT"')
    expect(sessionStorage.getItem('auth')).not.toContain('basicToken')
    expect(authMe).toHaveBeenCalled()
  })

  it('throws on failed login', async () => {
    vi.mocked(authLogin).mockResolvedValue({
      status: 401,
      data: { error: 'Invalid credentials' },
    } as never)

    const store = useAuthStore()
    await expect(store.login('x', 'y')).rejects.toThrow('Invalid credentials')
    expect(store.role).toBeNull()
  })

  it('clears state and session storage on logout', async () => {
    vi.mocked(authLogout).mockResolvedValue({
      status: 204,
      data: undefined,
      headers: new Headers(),
    } as never)

    const store = useAuthStore()
    store.$patch({
      username: 'GOAT',
      role: 'ADMIN',
    })
    sessionStorage.setItem('auth', '{"username":"GOAT","role":"ADMIN"}')

    await store.logout()

    expect(store.username).toBeNull()
    expect(store.role).toBeNull()
    expect(sessionStorage.getItem('auth')).toBeNull()
  })

  it('restores valid auth state and ignores invalid JSON', () => {
    const store = useAuthStore()

    sessionStorage.setItem('auth', '{"username":"kevin","role":"USER"}')
    store.restore()
    expect(store.username).toBe('kevin')
    expect(store.role).toBe('USER')

    sessionStorage.setItem('auth', '{invalid-json')
    store.restore()
    expect(store.username).toBe('kevin')
    expect(store.role).toBe('USER')
  })

  it('ensureAdminSession refreshes role from server and returns false on 401', async () => {
    const store = useAuthStore()
    store.$patch({ username: 'stale', role: 'ADMIN' })

    vi.mocked(authMe).mockResolvedValueOnce({
      status: 200,
      data: { username: 'admin', role: 'ADMIN' },
      headers: new Headers(),
    } as never)
    await expect(store.ensureAdminSession()).resolves.toBe(true)
    expect(store.username).toBe('admin')

    vi.mocked(authMe).mockResolvedValueOnce({
      status: 401,
      data: { error: 'Not authenticated' },
      headers: new Headers(),
    } as never)
    await expect(store.ensureAdminSession()).resolves.toBe(false)
    expect(store.role).toBeNull()
    expect(sessionStorage.getItem('auth')).toBeNull()
  })
})
