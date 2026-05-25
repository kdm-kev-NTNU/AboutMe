import { beforeEach, describe, expect, it } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { createRouter, createMemoryHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { registerAdminRouteGuard } from '../guards'

describe('registerAdminRouteGuard', () => {
	beforeEach(() => {
		sessionStorage.clear()
		setActivePinia(createPinia())
	})

	function buildRouter() {
		const router = createRouter({
			history: createMemoryHistory(),
			routes: [
				{ path: '/', name: 'home', component: { template: '<div>home</div>' } },
				{
					path: '/admin/tools',
					name: 'admin-tools',
					meta: { requiresAdmin: true },
					component: { template: '<div>admin</div>' },
				},
			],
		})
		registerAdminRouteGuard(router)
		return router
	}

	it('redirects non-admin away from admin route', async () => {
		const router = buildRouter()
		await router.push('/admin/tools')
		expect(router.currentRoute.value.path).toBe('/')
	})

	it('allows admin to open admin route', async () => {
		sessionStorage.setItem(
			'auth',
			JSON.stringify({ username: 'a', role: 'ADMIN' }),
		)
		const router = buildRouter()
		await router.push('/admin/tools')
		expect(router.currentRoute.value.path).toBe('/admin/tools')
		const auth = useAuthStore()
		expect(auth.role).toBe('ADMIN')
	})
})
