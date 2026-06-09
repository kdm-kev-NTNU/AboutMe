import { describe, expect, it } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { MotionPlugin } from '@vueuse/motion'
import type { Component } from 'vue'
import { createMemoryHistory, createRouter } from 'vue-router'
import PrivacyPolicyView from '../PrivacyPolicyView.vue'
import { useLangStore } from '@/stores/lang'
import ProjectsView from '../ProjectsView.vue'
import CareerView from '../CareerView.vue'
import ProjectPageView from '../ProjectPageView.vue'
import HowView from '../HowView.vue'
import ReasonView from '../ReasonView.vue'
import HeathenArmyView from '../HeathenArmyView.vue'

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

	function mountProjectsView() {
		const pinia = createPinia()
		setActivePinia(pinia)
		useLangStore().setLanguage('en')
		const router = createRouter({
			history: createMemoryHistory(),
			routes: [
				{ path: '/', name: 'home', component: { template: '<div />' } },
				{ path: '/projects', name: 'projects', component: { template: '<div />' } },
				{
					path: '/projects/heathen-army',
					name: 'project-heathen-army',
					component: { template: '<div />' },
				},
			],
		})
		return mount(ProjectsView, {
			global: { plugins: [pinia, router, MotionPlugin] },
		})
	}

	async function mountHowViewWithRoute(hash = '') {
		const pinia = createPinia()
		setActivePinia(pinia)
		useLangStore().setLanguage('en')
		const router = createRouter({
			history: createMemoryHistory(),
			routes: [{ path: '/how', name: 'how', component: HowView }],
		})
		await router.push(`/how${hash}`)
		await router.isReady()
		return mount(HowView, {
			global: {
				plugins: [pinia, router, MotionPlugin],
				stubs: {
					RouterLink: routerLinkStub,
					ProjectCardsSection: { template: '<div>AboutMe</div>' },
				},
			},
		})
	}

	function mountReasonView(lang: 'en' | 'no' = 'en') {
		const pinia = createPinia()
		setActivePinia(pinia)
		useLangStore().setLanguage(lang)
		const router = createRouter({
			history: createMemoryHistory(),
			routes: [
				{ path: '/reason', name: 'reason', component: ReasonView },
				{ path: '/projects/heathen-army', component: { template: '<div />' } },
			],
		})
		return mount(ReasonView, {
			global: {
				plugins: [pinia, router, MotionPlugin],
				stubs: { RouterLink: routerLinkStub },
			},
		})
	}

	function mountHeathenArmyView() {
		const pinia = createPinia()
		setActivePinia(pinia)
		useLangStore().setLanguage('en')
		return mount(HeathenArmyView, {
			global: {
				plugins: [pinia, MotionPlugin],
				stubs: {
					RouterLink: routerLinkStub,
					Dialog: { template: '<div><slot /></div>' },
					DialogContent: { template: '<div><slot /></div>' },
					DialogHeader: { template: '<div><slot /></div>' },
					DialogTitle: { template: '<h3><slot /></h3>' },
				},
			},
		})
	}

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
		const wrapper = mountProjectsView()
		await flushPromises()
		expect(wrapper.text()).toContain('Projects')
		expect(wrapper.find('h1').exists()).toBe(true)
	})

	it('renders CareerView with work and education', async () => {
		const wrapper = mountView(CareerView)
		await flushPromises()
		expect(wrapper.text()).toContain('Experience & education')
		expect(wrapper.text()).toContain('Work Experience')
		expect(wrapper.text()).toMatch(/NTNU|Oslo Municipality/i)
		expect(wrapper.text()).toContain('Education')
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
			expect(wrapper.text()).toContain('What the evaluation showed')
		},
		25_000,
	)

	it('renders ReasonView in English and Norwegian', async () => {
		const wrapperEn = mountReasonView('en')
		await flushPromises()
		expect(wrapperEn.text()).toContain('Experience and education')
		expect(wrapperEn.text()).toContain('Work Experience')
		expect(wrapperEn.text()).toContain('Education')
		expect(wrapperEn.text()).toContain('Projects')
		expect(wrapperEn.text()).toContain('AboutMe')
		expect(wrapperEn.text()).toContain('IMAT3011')
		expect(wrapperEn.text()).toContain('TDT4172')
		expect(wrapperEn.text()).toContain('Will assist with teaching and exercises')

		const wrapperNo = mountReasonView('no')
		await flushPromises()
		expect(wrapperNo.text()).toContain('Erfaring og utdanning')
		expect(wrapperNo.text()).toContain('Arbeidserfaring')
		expect(wrapperNo.text()).toContain('Utdanning')
		expect(wrapperNo.text()).toContain('Prosjekter')
		expect(wrapperNo.text()).toContain('AboutMe')
		expect(wrapperNo.text()).toContain('IMAT3011')
		expect(wrapperNo.text()).toContain('TDT4172')
		expect(wrapperNo.text()).toContain('Kommer til å bistå med undervisning og øvinger')
	})

	it('opens HowView accordions from route hash and toggles sections', async () => {
		const wrapper = await mountHowViewWithRoute('#future-work')
		await flushPromises()
		expect(wrapper.text()).toContain('How')
		expect(wrapper.text()).toContain('AI in the development workflow')
		expect(wrapper.text()).toContain('Future work')
		expect(wrapper.text()).not.toContain('Heathen Army (Vikings blog)')
		expect(wrapper.text()).toContain('References (arXiv)')

		const toggles = wrapper.findAll('button')
		await toggles[0].trigger('click')
		await flushPromises()
		expect(wrapper.text()).toContain("Bachelor's thesis")
	})

	it('opens HowView AI workflow accordion from route hash', async () => {
		const wrapper = await mountHowViewWithRoute('#ai-workflow')
		await flushPromises()
		expect(wrapper.text()).toContain('How I use AI without outsourcing the thinking')
		const iframe = wrapper.find('#panel-ai-workflow iframe')
		expect(iframe.exists()).toBe(true)
		expect(iframe.attributes('src')).toContain('C87ITeVS9hs')
	})

	it('renders HeathenArmyView and opens gallery details', async () => {
		const wrapper = mountHeathenArmyView()
		await flushPromises()
		expect(wrapper.text()).toContain('Heathen Army')
		expect(wrapper.text()).toContain('Signals from the era')
		expect(wrapper.text()).toContain('Screenshots')

		const galleryButtons = wrapper.findAll('figure button')
		expect(galleryButtons.length).toBeGreaterThan(0)
		await galleryButtons[0].trigger('click')
		await flushPromises()
		expect(wrapper.text()).toContain('Open image')
	})
})
