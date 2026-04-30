import { describe, expect, it } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { MotionPlugin } from '@vueuse/motion'
import type { Component } from 'vue'
import { createMemoryHistory, createRouter } from 'vue-router'
import AboutView from '../AboutView.vue'
import PrivacyPolicyView from '../PrivacyPolicyView.vue'
import { useLangStore } from '@/stores/lang'
import ProjectsView from '../ProjectsView.vue'
import CareerView from '../CareerView.vue'
import ProjectPageView from '../ProjectPageView.vue'

describe('portfolio views (smoke)', () => {
	function mountView(component: Component) {
		const pinia = createPinia()
		setActivePinia(pinia)
		// Reset persisted language so tests do not inherit 'no' from earlier cases in this file.
		useLangStore().setLanguage('en')
		return mount(component, {
			global: { plugins: [pinia, MotionPlugin] },
		})
	}

	const routerLinkStub = { template: '<a><slot /></a>' }

	function makeProjectRouter() {
		return createRouter({
			history: createMemoryHistory(),
			routes: [
				{ path: '/', name: 'home', component: { template: '<div />' } },
				{ path: '/chat', name: 'chat', component: { template: '<div />' } },
				{ path: '/projects', name: 'projects', component: { template: '<div />' } },
				{ path: '/project', name: 'project', component: ProjectPageView },
			],
		})
	}

	function mountProjectPage() {
		const pinia = createPinia()
		setActivePinia(pinia)
		useLangStore().setLanguage('en')
		const router = makeProjectRouter()
		return mount(ProjectPageView, {
			global: {
				plugins: [pinia, router, MotionPlugin],
				stubs: { RouterLink: routerLinkStub },
			},
		})
	}

	it('renders AboutView', () => {
		const wrapper = mountView(AboutView)
		expect(wrapper.text()).toContain('about page')
	})

	it(
		'renders ProjectPageView in English with section headers',
		async () => {
			const wrapper = mountProjectPage()
			await flushPromises()
			expect(wrapper.text()).toContain('The project')
			expect(wrapper.text()).toContain('Tech stack')
			expect(wrapper.text()).toContain("Bachelor's thesis")
			expect(wrapper.text()).toContain('Future work')
		},
		20_000,
	)

	it(
		'renders ProjectPageView in Norwegian when language is no',
		async () => {
			const pinia = createPinia()
			setActivePinia(pinia)
			useLangStore().setLanguage('no')
			const router = makeProjectRouter()
			const wrapper = mount(ProjectPageView, {
				global: {
					plugins: [pinia, router, MotionPlugin],
					stubs: { RouterLink: routerLinkStub },
				},
			})
			await flushPromises()
			expect(wrapper.text()).toContain('Prosjektet')
			expect(wrapper.text()).toContain('Teknologistakk')
			expect(wrapper.text()).toContain('Bacheloroppgaven')
			expect(wrapper.text()).toContain('Videre arbeid')
		},
		20_000,
	)

	it(
		'expands tech stack accordion and shows stack content',
		async () => {
			const wrapper = mountProjectPage()
			await flushPromises()
			const techBtn = wrapper.find('#section-tech-stack')
			expect(techBtn.exists()).toBe(true)
			await techBtn.trigger('click')
			await flushPromises()
			expect(wrapper.text()).toMatch(/Spring AI|Backend/i)
		},
		25_000,
	)

	it('renders PrivacyPolicyView in English by default', async () => {
		const wrapper = mountView(PrivacyPolicyView)
		await flushPromises()
		expect(wrapper.text()).toContain('Privacy Policy')
		expect(wrapper.text()).toMatch(/cookies|Chat/i)
	})

	it('renders PrivacyPolicyView in Norwegian when language is no', async () => {
		const pinia = createPinia()
		setActivePinia(pinia)
		useLangStore().setLanguage('no')
		const wrapper = mount(PrivacyPolicyView, { global: { plugins: [pinia] } })
		await flushPromises()
		expect(wrapper.text()).toContain('Personvernerklæring')
		expect(wrapper.text()).toMatch(/informasjonskapsler|Chat/i)
	})

	it('renders ProjectsView with project grid', async () => {
		const wrapper = mountView(ProjectsView)
		await flushPromises()
		expect(wrapper.text()).toContain('Projects')
		expect(wrapper.find('h1').exists()).toBe(true)
	})

	it('renders CareerView with work, education, and courses', async () => {
		const wrapper = mountView(CareerView)
		await flushPromises()
		expect(wrapper.text()).toContain('Experience & education')
		expect(wrapper.text()).toContain('Work Experience')
		expect(wrapper.text()).toMatch(/NTNU|Oslo Municipality/i)
		expect(wrapper.text()).toContain('Education')
		expect(wrapper.text()).toMatch(/Courses|Emner/)
	})

	it('renders CareerView in Norwegian with translated section labels', async () => {
		const pinia = createPinia()
		setActivePinia(pinia)
		useLangStore().setLanguage('no')
		const wrapper = mount(CareerView, { global: { plugins: [pinia, MotionPlugin] } })
		await flushPromises()
		expect(wrapper.text()).toContain('Erfaring og utdanning')
		expect(wrapper.text()).toContain('Arbeidserfaring')
		expect(wrapper.text()).toContain('Utdanning')
		expect(wrapper.text()).toContain('Emner')
		expect(wrapper.text()).toMatch(/studiepoeng|Karakter/)
	})

	it(
		'renders future work copy when future accordion is expanded',
		async () => {
			const wrapper = mountProjectPage()
			await flushPromises()
			await wrapper.find('#section-future-work').trigger('click')
			await flushPromises()
			expect(wrapper.text()).toContain('Future work and improvements')
			expect(wrapper.text()).toContain('References (arXiv)')
			const links = wrapper.findAll('a[href^="https://arxiv.org/abs/"]')
			expect(links.length).toBeGreaterThan(0)
		},
		25_000,
	)

	it(
		'renders bachelor copy when bachelor accordion is expanded',
		async () => {
			const wrapper = mountProjectPage()
			await flushPromises()
			await wrapper.find('#section-bachelor').trigger('click')
			await flushPromises()
			expect(wrapper.text()).toContain("Bachelor's thesis")
			expect(wrapper.text()).toContain('active work in progress')
			expect(wrapper.text()).toContain('shaping the portfolio now')
		},
		25_000,
	)
})
