import { beforeEach, describe, expect, it } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'
import Navbar from '../Navbar.vue'

describe('Navbar', () => {
	beforeEach(() => {
		localStorage.clear()
		setActivePinia(createPinia())
	})

	function makeRouter() {
		return createRouter({
			history: createMemoryHistory(),
			routes: [
				{ path: '/', name: 'home', component: { template: '<div />' } },
				{ path: '/reason', name: 'reason', component: { template: '<div />' } },
				{ path: '/how', name: 'how', component: { template: '<div />' } },
			],
		})
	}

	it('shows English nav labels by default', async () => {
		const router = makeRouter()
		await router.push('/')
		await router.isReady()
		const wrapper = mount(Navbar, {
			global: {
				plugins: [createPinia(), router],
				stubs: { RouterLink: { template: '<a><slot /></a>', props: ['to'] } },
			},
		})
		await flushPromises()
		expect(wrapper.text()).toContain('Home')
		expect(wrapper.text()).toContain('Experience and education')
		expect(wrapper.text()).toContain('How')
	})

	it('shows Norwegian labels when language is no', async () => {
		const pinia = createPinia()
		setActivePinia(pinia)
		const { useLangStore } = await import('../../stores/lang')
		useLangStore().setLanguage('no')

		const router = makeRouter()
		await router.push('/')
		await router.isReady()
		const wrapper = mount(Navbar, {
			global: {
				plugins: [pinia, router],
				stubs: { RouterLink: { template: '<a><slot /></a>', props: ['to'] } },
			},
		})
		await flushPromises()
		expect(wrapper.text()).toContain('Hjem')
		expect(wrapper.text()).toContain('Erfaring og utdanning')
		expect(wrapper.text()).toContain('Hvordan')
	})

	it('marks active route with stronger button styling', async () => {
		const router = makeRouter()
		await router.push('/reason')
		await router.isReady()
		const wrapper = mount(Navbar, {
			global: {
				plugins: [createPinia(), router],
				stubs: { RouterLink: { template: '<a><slot /></a>', props: ['to'] } },
			},
		})
		await flushPromises()
		const links = wrapper.findAll('a')
		const experienceLink = links.find((a) => a.text().includes('Experience and education'))
		expect(experienceLink?.classes().join(' ')).toContain('font-semibold')
	})

	it('opens mobile drawer from hamburger and lists all nav links', async () => {
		const router = makeRouter()
		await router.push('/')
		await router.isReady()
		const wrapper = mount(Navbar, {
			global: {
				plugins: [createPinia(), router],
				stubs: { RouterLink: { template: '<a><slot /></a>', props: ['to'] } },
			},
		})
		await flushPromises()
		expect(wrapper.find('#mobile-nav-drawer').exists()).toBe(false)

		const menuBtn = wrapper.find('button[aria-controls="mobile-nav-drawer"]')
		expect(menuBtn.exists()).toBe(true)
		await menuBtn.trigger('click')
		await flushPromises()

		const drawer = wrapper.find('#mobile-nav-drawer')
		expect(drawer.exists()).toBe(true)
		expect(drawer.findAll('a').length).toBe(3)
		expect(drawer.text()).toContain('Home')
		expect(drawer.text()).toContain('Experience and education')
		expect(drawer.text()).toContain('How')
	})
})
