import { defineStore } from 'pinia'
import { authLogin, authLogout, authMe } from '@/api/generated/portfolio'
import { revokeOwnerIdentity, setOwnerIdentity } from '@/lib/analytics-identity'

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
    clearLocal() {
      this.username = null
      this.role = null
      sessionStorage.removeItem('auth')
    },
    /**
     * Validates the httpOnly session via GET /auth/me (also primes XSRF-TOKEN for admin mutations)
     * and refreshes client role from the server. Returns true only for an ADMIN session.
     */
    async ensureAdminSession(): Promise<boolean> {
      try {
        const r = await authMe()
        if (r.status !== 200) {
          this.clearLocal()
          return false
        }
        this.username = r.data.username
        this.role = r.data.role as 'USER' | 'ADMIN'
        sessionStorage.setItem('auth', JSON.stringify({ username: this.username, role: this.role }))
        syncOwnerAnalyticsIdentity(this.role, r.data.analyticsId)
        return this.role === 'ADMIN'
      } catch {
        this.clearLocal()
        return false
      }
    },
    async login(username: string, password: string) {
      const r = await authLogin({ username, password })
      if (r.status !== 200) {
        throw new Error('Invalid credentials')
      }
      const data = r.data
      this.username = data.username
      this.role = data.role as 'USER' | 'ADMIN'
      sessionStorage.setItem('auth', JSON.stringify({ username: this.username, role: this.role }))
      syncOwnerAnalyticsIdentity(this.role, data.analyticsId)
      try {
        await authMe()
      } catch {
        // Session cookie is set; CSRF priming is best-effort until the admin route guard runs.
      }
    },
    async logout() {
      try {
        await authLogout()
      } catch {
        // Clear local state even if the network call fails
      }
      revokeOwnerIdentity()
      this.clearLocal()
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

function syncOwnerAnalyticsIdentity(
  role: 'USER' | 'ADMIN' | null,
  analyticsId: string | null | undefined,
): void {
  if (role !== 'ADMIN') return
  const id = analyticsId?.trim()
  if (!id) return
  setOwnerIdentity(id)
}
