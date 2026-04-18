import { defineStore } from 'pinia'
import { authLogin } from '@/api/generated/portfolio'

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
      const r = await authLogin({ username, password })
      if (r.status !== 200) {
        throw new Error('Invalid credentials')
      }
      const data = r.data
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
