import type { Router } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

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
