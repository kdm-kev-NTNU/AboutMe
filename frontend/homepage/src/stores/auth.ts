import { defineStore } from 'pinia'

interface AuthState {
  username: string | null
  role: 'USER' | 'ADMIN' | null
  basicToken: string | null
}

export const useAuthStore = defineStore('auth', {
  state: (): AuthState => ({
    username: null,
    role: null,
    basicToken: null,
  }),
  actions: {
    async login(username: string, password: string) {
      const res = await fetch('/api/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username, password })
      })
      if (!res.ok) {
        throw new Error('Invalid credentials')
      }
      const data = await res.json() as { username: string; role: 'USER' | 'ADMIN' }
      this.username = data.username
      this.role = data.role
      this.basicToken = btoa(`${username}:${password}`)
      sessionStorage.setItem('auth', JSON.stringify({ username: this.username, role: this.role, basicToken: this.basicToken }))
    },
    logout() {
      this.username = null
      this.role = null
      this.basicToken = null
      sessionStorage.removeItem('auth')
    },
    restore() {
      const raw = sessionStorage.getItem('auth')
      if (raw) {
        try {
          const parsed = JSON.parse(raw) as AuthState
          this.username = parsed.username
          this.role = parsed.role
          this.basicToken = parsed.basicToken
        } catch {}
      }
    }
  }
})


