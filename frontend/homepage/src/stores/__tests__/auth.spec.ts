import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useAuthStore } from '../auth'

describe('auth store', () => {
  beforeEach(() => {
    sessionStorage.clear()
    setActivePinia(createPinia())
    vi.restoreAllMocks()
  })

  it('stores username, role and basic token on successful login', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue({
        ok: true,
        json: async () => ({ username: 'GOAT', role: 'ADMIN' }),
      }),
    )

    const store = useAuthStore()
    await store.login('GOAT', 'secret')

    expect(store.username).toBe('GOAT')
    expect(store.role).toBe('ADMIN')
    expect(store.basicToken).toBe(btoa('GOAT:secret'))
    expect(sessionStorage.getItem('auth')).toContain('"username":"GOAT"')
  })

  it('throws on login failure and keeps state empty', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue({
        ok: false,
      }),
    )

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
