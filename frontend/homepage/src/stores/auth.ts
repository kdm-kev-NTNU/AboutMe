import { defineStore } from 'pinia'
import { authLogin, authLogout } from '@/api/generated/portfolio'

// Session: POST /auth/login sets an httpOnly cookie; we keep username/role in sessionStorage for UI only.
interface AuthState {
  username: string | null
  role: 'USER' | 'ADMIN' | null
}

export const useAuthStore = defineStore('auth', {
  state: (): AuthState => ({
    username: null,
    role: null,
  }),
  getters: {
    isAuthenticated: (state) => state.role != null,
    isAdmin: (state) => state.role === 'ADMIN',
  },
  actions: {
    async login(username: string, password: string) {
      const r = await authLogin({ username, password })
      if (r.status !== 200) {
        throw new Error('Invalid credentials')
      }
      const data = r.data
      this.username = data.username
      this.role = data.role as 'USER' | 'ADMIN'
      sessionStorage.setItem('auth', JSON.stringify({ username: this.username, role: this.role }))
    },
    async logout() {
      try {
        await authLogout()
      } catch {
        // Clear local state even if the network call fails
      }
      this.username = null
      this.role = null
      sessionStorage.removeItem('auth')
    },
    /** Hydrates Pinia from sessionStorage on hard refresh (router guard calls this before admin routes). */
    restore() {
      const raw = sessionStorage.getItem('auth')
      if (raw) {
        try {
          const parsed = JSON.parse(raw) as AuthState
          this.username = parsed.username
          this.role = parsed.role
        } catch {
          sessionStorage.removeItem('auth')
        }
      }
    },
  },
})
