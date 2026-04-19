import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { authLogin } from '@/api/generated/portfolio'
import { useAuthStore } from '../auth'

vi.mock('@/api/generated/portfolio', async (importOriginal) => {
  const mod = await importOriginal<typeof import('@/api/generated/portfolio')>()
  return { ...mod, authLogin: vi.fn() }
})

describe('auth store', () => {
  beforeEach(() => {
    sessionStorage.clear()
    setActivePinia(createPinia())
    vi.clearAllMocks()
    vi.mocked(authLogin).mockReset()
  })

  it('stores username, role and basic token on successful login', async () => {
    vi.mocked(authLogin).mockResolvedValue({
      status: 200,
      data: { username: 'GOAT', role: 'ADMIN' },
      headers: new Headers(),
    })

    const store = useAuthStore()
    await store.login('GOAT', 'secret')

    expect(store.username).toBe('GOAT')
    expect(store.role).toBe('ADMIN')
    expect(store.basicToken).toBe(btoa('GOAT:secret'))
    expect(sessionStorage.getItem('auth')).toContain('"username":"GOAT"')
  })

  it('throws on login failure and keeps state empty', async () => {
    vi.mocked(authLogin).mockResolvedValue({
      status: 401,
      data: { error: 'Invalid credentials' },
      headers: new Headers(),
    })

    const store = useAuthStore()

    await expect(store.login('GOAT', 'wrong')).rejects.toThrow('Invalid credentials')
    expect(store.username).toBeNull()
    expect(store.role).toBeNull()
    expect(store.basicToken).toBeNull()
  })

  it('clears state and session storage on logout', () => {
    const store = useAuthStore()
    store.$patch({
      username: 'GOAT',
      role: 'ADMIN',
      basicToken: 'token',
    })
    sessionStorage.setItem('auth', '{"username":"GOAT","role":"ADMIN","basicToken":"token"}')

    store.logout()

    expect(store.username).toBeNull()
    expect(store.role).toBeNull()
    expect(store.basicToken).toBeNull()
    expect(sessionStorage.getItem('auth')).toBeNull()
  })

  it('restores valid auth state and ignores invalid JSON', () => {
    const store = useAuthStore()

    sessionStorage.setItem('auth', '{"username":"kevin","role":"USER","basicToken":"abc"}')
    store.restore()
    expect(store.username).toBe('kevin')
    expect(store.role).toBe('USER')
    expect(store.basicToken).toBe('abc')

    sessionStorage.setItem('auth', '{invalid-json')
    store.$reset()
    store.restore()
    expect(store.username).toBeNull()
    expect(store.role).toBeNull()
    expect(store.basicToken).toBeNull()
  })
})
