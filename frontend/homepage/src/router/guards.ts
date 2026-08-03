import type { Router } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

/**
 * Blocks navigation to routes with meta.requiresAdmin unless the server session is ADMIN.
 * Calls GET /auth/me to validate the httpOnly cookie and prime XSRF-TOKEN before mutations.
 */
export function registerAdminRouteGuard(router: Router) {
	router.beforeEach(async (to) => {
		if (!to.meta.requiresAdmin) {
			return true
		}
		const auth = useAuthStore()
		auth.restore()
		const isAdmin = await auth.ensureAdminSession()
		if (!isAdmin) {
			return { path: '/' }
		}
		return true
	})
}
