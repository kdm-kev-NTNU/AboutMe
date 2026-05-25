import { beforeEach, describe, expect, it } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import type { Router } from 'vue-router'
import App from '../App.vue'
import { createPortfolioRouter } from '../router/createPortfolioRouter'
import Navbar from '../components/Navbar.vue'
import { useLangStore } from '../stores/lang'

describe('App shell', () => {
	beforeEach(() => {
		sessionStorage.clear()
		localStorage.clear()
	})

	function mountAppShell(router: Router) {
		const pinia = createPinia()
		setActivePinia(pinia)
		return {
			pinia,
			wrapper: mount(App, {
				global: {
					plugins: [pinia, router],
					// Avoid mounting HomeView here: onMounted triggers chat model fetch without test mocks.
					stubs: { RouterView: { template: '<div class="router-view-stub" />' } },
				},
			}),
		}
	}

	it('shows Navbar on public home route', async () => {
		const router = createPortfolioRouter({ useMemoryHistory: true })
		await router.push('/')
		const { wrapper } = mountAppShell(router)
		await flushPromises()
		expect(wrapper.findComponent(Navbar).exists()).toBe(true)
		expect(wrapper.text()).toMatch(/Privacy Policy|Personvernerklæring/)
	})

	it('hides Navbar when visiting an admin hub route with admin session', async () => {
		sessionStorage.setItem(
			'auth',
			JSON.stringify({ username: 'admin', role: 'ADMIN' }),
		)
		const router = createPortfolioRouter({ useMemoryHistory: true })
		const { wrapper } = mountAppShell(router)
		await router.push('/admin/tools')
		await flushPromises()
		expect(wrapper.findComponent(Navbar).exists()).toBe(false)
	})

	it('uses Norwegian footer labels when language is Norwegian', async () => {
		const router = createPortfolioRouter({ useMemoryHistory: true })
		await router.push('/')
		const { wrapper } = mountAppShell(router)
		useLangStore().setLanguage('no')
		await flushPromises()
		expect(wrapper.text()).toContain('Personvernerklæring')
		expect(wrapper.text()).toContain('Informasjonskapsler')
	})
})
