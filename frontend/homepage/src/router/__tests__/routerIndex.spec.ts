import { beforeEach, describe, expect, it } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { createPortfolioRouter } from '../createPortfolioRouter'

describe('application router (index)', () => {
	beforeEach(() => {
		sessionStorage.clear()
		setActivePinia(createPinia())
	})

	it('registers portfolio and admin route names', () => {
		const router = createPortfolioRouter({ useMemoryHistory: true })
		const names = new Set(router.getRoutes().map((r) => r.name as string | undefined))
		expect(names.has('home')).toBe(true)
		expect(names.has('chat')).toBe(true)
		expect(names.has('privacy-policy')).toBe(true)
		expect(names.has('future-work')).toBe(true)
		expect(names.has('career')).toBe(true)
		expect(names.has('bachelor')).toBe(true)
		expect(names.has('admin-tools')).toBe(true)
		expect(names.has('admin-experiments')).toBe(true)
	})

	it('navigates to lazy-loaded privacy policy route', async () => {
		const router = createPortfolioRouter({ useMemoryHistory: true })
		await router.push('/privacy-policy')
		expect(router.currentRoute.value.name).toBe('privacy-policy')
	})

	it('navigates to lazy-loaded bachelor route', async () => {
		const router = createPortfolioRouter({ useMemoryHistory: true })
		await router.push('/bachelor')
		expect(router.currentRoute.value.name).toBe('bachelor')
	})

	it('navigates to lazy-loaded tech-stack route', async () => {
		const router = createPortfolioRouter({ useMemoryHistory: true })
		await router.push('/tech-stack')
		expect(router.currentRoute.value.name).toBe('tech-stack')
	})

	it('navigates to lazy-loaded future-work route', async () => {
		const router = createPortfolioRouter({ useMemoryHistory: true })
		await router.push('/future-work')
		expect(router.currentRoute.value.name).toBe('future-work')
	})

	it('redirects legacy work-experience and education paths to career', async () => {
		const router = createPortfolioRouter({ useMemoryHistory: true })
		await router.push('/work-experience')
		expect(router.currentRoute.value.path).toBe('/career')
		expect(router.currentRoute.value.name).toBe('career')

		await router.push('/education')
		expect(router.currentRoute.value.path).toBe('/career')
		expect(router.currentRoute.value.name).toBe('career')
	})

	it('navigates to additional lazy-loaded public routes', async () => {
		const router = createPortfolioRouter({ useMemoryHistory: true })
		await router.push('/projects')
		expect(router.currentRoute.value.name).toBe('projects')

		await router.push('/career')
		expect(router.currentRoute.value.name).toBe('career')

		await router.push('/feedback')
		expect(router.currentRoute.value.name).toBe('feedback')
	})

	it('allows admin routes when ADMIN session is restored', async () => {
		sessionStorage.setItem(
			'auth',
			JSON.stringify({ username: 'admin', role: 'ADMIN', basicToken: 'dGVzdDp0ZXN0' }),
		)
		const router = createPortfolioRouter({ useMemoryHistory: true })

		await router.push('/admin/tools')
		expect(router.currentRoute.value.name).toBe('admin-tools')

		await router.push('/admin/pipeline')
		expect(router.currentRoute.value.name).toBe('admin-pipeline')

		await router.push('/admin/chunks')
		expect(router.currentRoute.value.name).toBe('admin-chunks')

		await router.push('/admin/prompts')
		expect(router.currentRoute.value.name).toBe('admin-prompts')

		await router.push('/admin/experiments')
		expect(router.currentRoute.value.name).toBe('admin-experiments')
	})
})
