import type { Router } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

/**
 * Blocks navigation to routes with meta.requiresAdmin unless the SPA has a restored ADMIN session
 * (see auth store: JSON + Basic token in sessionStorage after POST /auth/login).
 */
export function registerAdminRouteGuard(router: Router) {
	router.beforeEach((to) => {
		if (to.meta.requiresAdmin) {
			const auth = useAuthStore()
			auth.restore()
			if (auth.role !== 'ADMIN') {
				return { path: '/' }
			}
		}
		return true
	})
}
