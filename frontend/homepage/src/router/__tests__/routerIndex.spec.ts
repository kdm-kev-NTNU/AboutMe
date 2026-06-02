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
		expect(names.has('accessibility')).toBe(true)
		expect(names.has('how')).toBe(true)
		expect(names.has('project-heathen-army')).toBe(true)
		expect(names.has('reason')).toBe(true)
		expect(names.has('admin-tools')).toBe(true)
		expect(names.has('admin-experiments')).toBe(true)
	})

	it('navigates to lazy-loaded privacy policy route', async () => {
		const router = createPortfolioRouter({ useMemoryHistory: true })
		await router.push('/privacy-policy')
		expect(router.currentRoute.value.name).toBe('privacy-policy')
	})

	it('navigates to accessibility route', async () => {
		const router = createPortfolioRouter({ useMemoryHistory: true })
		await router.push('/accessibility')
		expect(router.currentRoute.value.name).toBe('accessibility')
	})

	it('redirects legacy bachelor path to how with bachelor hash', async () => {
		const router = createPortfolioRouter({ useMemoryHistory: true })
		await router.push('/bachelor')
		expect(router.currentRoute.value.path).toBe('/how')
		expect(router.currentRoute.value.hash).toBe('#bachelor')
		expect(router.currentRoute.value.name).toBe('how')
	})

	it('redirects legacy tech-stack path to how', async () => {
		const router = createPortfolioRouter({ useMemoryHistory: true })
		await router.push('/tech-stack')
		expect(router.currentRoute.value.path).toBe('/how')
	})

	it('redirects legacy future-work path to how with future-work hash', async () => {
		const router = createPortfolioRouter({ useMemoryHistory: true })
		await router.push('/future-work')
		expect(router.currentRoute.value.path).toBe('/how')
		expect(router.currentRoute.value.hash).toBe('#future-work')
	})

	it('navigates to lazy-loaded how route', async () => {
		const router = createPortfolioRouter({ useMemoryHistory: true })
		await router.push('/how')
		expect(router.currentRoute.value.name).toBe('how')
	})

	it('redirects legacy work-experience and education paths to reason', async () => {
		const router = createPortfolioRouter({ useMemoryHistory: true })
		await router.push('/work-experience')
		expect(router.currentRoute.value.path).toBe('/reason')
		expect(router.currentRoute.value.name).toBe('reason')

		await router.push('/education')
		expect(router.currentRoute.value.path).toBe('/reason')
		expect(router.currentRoute.value.name).toBe('reason')
	})

	it('redirects legacy projects paths to reason with projects hash', async () => {
		const router = createPortfolioRouter({ useMemoryHistory: true })
		await router.push('/projects')
		expect(router.currentRoute.value.path).toBe('/reason')
		expect(router.currentRoute.value.hash).toBe('#projects')
		expect(router.currentRoute.value.name).toBe('reason')

		await router.push('/project')
		expect(router.currentRoute.value.path).toBe('/reason')
		expect(router.currentRoute.value.hash).toBe('#projects')
		expect(router.currentRoute.value.name).toBe('reason')
	})

	it('navigates to additional lazy-loaded public routes', async () => {
		const router = createPortfolioRouter({ useMemoryHistory: true })
		await router.push('/projects/heathen-army')
		expect(router.currentRoute.value.name).toBe('project-heathen-army')

		await router.push('/career')
		expect(router.currentRoute.value.name).toBe('reason')

		await router.push('/feedback')
		expect(router.currentRoute.value.name).toBe('feedback')
	})

	it('allows admin routes when ADMIN session is restored', async () => {
		sessionStorage.setItem(
			'auth',
			JSON.stringify({ username: 'admin', role: 'ADMIN' }),
		)
		const router = createPortfolioRouter({ useMemoryHistory: true })

		await router.push('/admin/tools')
		expect(router.currentRoute.value.name).toBe('admin-tools')

		await router.push('/admin/pipeline')
		expect(router.currentRoute.value.name).toBe('admin-pipeline')

		await router.push('/admin/chunks')
		expect(router.currentRoute.value.name).toBe('admin-chunks')

		await router.push('/admin/question-suggestions')
		expect(router.currentRoute.value.name).toBe('admin-question-suggestions')

		await router.push('/admin/prompts')
		expect(router.currentRoute.value.name).toBe('admin-prompts')

		await router.push('/admin/experiments')
		expect(router.currentRoute.value.name).toBe('admin-experiments')
	})

	it('creates a router with default web-history option path', () => {
		const router = createPortfolioRouter()
		expect(router).toBeTruthy()
		expect(router.currentRoute.value.path).toBe('/')
	})
})
