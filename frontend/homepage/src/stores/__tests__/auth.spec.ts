import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { authLogin, authLogout } from '@/api/generated/portfolio'
import { useAuthStore } from '../auth'

vi.mock('@/api/generated/portfolio', async (importOriginal) => {
  const mod = await importOriginal<typeof import('@/api/generated/portfolio')>()
  return { ...mod, authLogin: vi.fn(), authLogout: vi.fn() }
})

describe('auth store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    sessionStorage.clear()
    vi.mocked(authLogin).mockReset()
    vi.mocked(authLogout).mockReset()
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
  })

  it('throws on failed login', async () => {
    vi.mocked(authLogin).mockResolvedValue({
      status: 401,
      data: { error: 'Invalid credentials' },
      headers: new Headers(),
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
})
