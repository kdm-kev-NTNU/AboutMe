import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { createRouter, createMemoryHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { authMe } from '@/api/generated/portfolio'
import { registerAdminRouteGuard } from '../guards'

vi.mock('@/api/generated/portfolio', async (importOriginal) => {
	const mod = await importOriginal<typeof import('@/api/generated/portfolio')>()
	return { ...mod, authMe: vi.fn() }
})

describe('registerAdminRouteGuard', () => {
	beforeEach(() => {
		sessionStorage.clear()
		setActivePinia(createPinia())
		vi.mocked(authMe).mockReset()
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

	it('redirects when server session is not admin', async () => {
		sessionStorage.setItem(
			'auth',
			JSON.stringify({ username: 'a', role: 'ADMIN' }),
		)
		vi.mocked(authMe).mockResolvedValue({
			status: 401,
			data: { error: 'Not authenticated' },
			headers: new Headers(),
		} as never)

		const router = buildRouter()
		await router.push('/admin/tools')
		expect(router.currentRoute.value.path).toBe('/')
		expect(authMe).toHaveBeenCalled()
	})

	it('allows admin after server session validation and CSRF priming via authMe', async () => {
		sessionStorage.setItem(
			'auth',
			JSON.stringify({ username: 'stale', role: 'ADMIN' }),
		)
		vi.mocked(authMe).mockResolvedValue({
			status: 200,
			data: { username: 'admin', role: 'ADMIN' },
			headers: new Headers(),
		} as never)

		const router = buildRouter()
		await router.push('/admin/tools')
		expect(router.currentRoute.value.path).toBe('/admin/tools')
		expect(authMe).toHaveBeenCalled()
		const auth = useAuthStore()
		expect(auth.username).toBe('admin')
		expect(auth.role).toBe('ADMIN')
	})

	it('redirects non-admin role from server', async () => {
		sessionStorage.setItem(
			'auth',
			JSON.stringify({ username: 'u', role: 'ADMIN' }),
		)
		vi.mocked(authMe).mockResolvedValue({
			status: 200,
			data: { username: 'u', role: 'USER' },
			headers: new Headers(),
		} as never)

		const router = buildRouter()
		await router.push('/admin/tools')
		expect(router.currentRoute.value.path).toBe('/')
	})
})
