import { beforeEach, describe, expect, it, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'
import HomeView from '../HomeView.vue'
import { useLangStore } from '@/stores/lang'

vi.mock('@/lib/realtime-voice', () => ({
	fetchRealtimeVoiceStatus: vi.fn().mockResolvedValue({
		enabled: false,
		standardEnabled: false,
		liveEnabled: false,
		voices: ['marin', 'cedar'],
		reasoningEfforts: ['low', 'medium', 'high'],
		voice: 'marin',
		reasoningEffort: 'low',
	}),
}))

vi.mock('@/stores/auth', () => ({
	useAuthStore: () => ({ restore: vi.fn() }),
}))

const buttonStub = {
	props: ['type'],
	template: '<button :type="type === \'submit\' ? \'submit\' : \'button\'"><slot /></button>',
}

const commonStubs = {
	Button: buttonStub,
	Info: true,
	Github: true,
	Linkedin: true,
	MessageSquare: true,
	ChevronRight: true,
	Mic: true,
	Headphones: true,
}

describe('HomeView', () => {
	function makeRouter() {
		return createRouter({
			history: createMemoryHistory(),
			routes: [
				{ path: '/', name: 'home', component: HomeView },
				{ path: '/chat', name: 'chat', component: { template: '<div>chat</div>' } },
				{ path: '/voice', name: 'voice', component: { template: '<div>voice</div>' } },
				{ path: '/feedback', name: 'feedback', component: { template: '<div>feedback</div>' } },
				{ path: '/project', name: 'project', component: { template: '<div>project</div>' } },
			],
		})
	}

	beforeEach(() => {
		sessionStorage.clear()
		localStorage.clear()
		vi.clearAllMocks()
	})

	it('switches language with EN/NO toggle and renders Norwegian voice status', async () => {
		const pinia = createPinia()
		setActivePinia(pinia)
		const router = makeRouter()
		await router.push('/')
		const wrapper = mount(HomeView, {
			global: {
				plugins: [pinia, router],
				stubs: commonStubs,
			},
		})
		await flushPromises()
		const noBtn = wrapper.findAll('button').find((b) => b.text().trim() === 'NO')
		expect(noBtn).toBeTruthy()
		await noBtn!.trigger('click')
		expect(wrapper.text()).toContain('Snakk med Kevin sin AI først.')
	})

	it('renders GitHub and LinkedIn social links', async () => {
		const pinia = createPinia()
		setActivePinia(pinia)
		const router = makeRouter()
		await router.push('/')
		const wrapper = mount(HomeView, {
			global: {
				plugins: [pinia, router],
				stubs: commonStubs,
			},
		})
		await flushPromises()
		expect(wrapper.find('a[href*="github.com"]').exists()).toBe(true)
		expect(wrapper.find('a[href*="linkedin.com"]').exists()).toBe(true)
	})

	it('links to the future work roadmap from the home hero', async () => {
		const pinia = createPinia()
		setActivePinia(pinia)
		const router = makeRouter()
		await router.push('/')
		await router.isReady()
		const wrapper = mount(HomeView, {
			global: {
				plugins: [pinia, router],
				stubs: commonStubs,
			},
		})
		await flushPromises()
		const futureLink = wrapper.get('a[href="/project#future-work"]')
		expect(futureLink.text()).toContain('Future work and improvements')
	})

	it('navigates to voice chat when voice CTA is clicked', async () => {
		const pinia = createPinia()
		setActivePinia(pinia)
		useLangStore().setLanguage('en')
		const router = makeRouter()
		await router.push('/')
		const pushSpy = vi.spyOn(router, 'push')
		const wrapper = mount(HomeView, {
			global: {
				plugins: [pinia, router],
				stubs: commonStubs,
			},
		})
		await flushPromises()

		await wrapper.find('[aria-label="Go to robust voice mode"]').trigger('click')
		expect(pushSpy).toHaveBeenCalledWith({ name: 'voice' })
	})

	it('navigates to voice chat with Norwegian aria-label when UI is NO', async () => {
		const pinia = createPinia()
		setActivePinia(pinia)
		useLangStore().setLanguage('no')
		const router = makeRouter()
		await router.push('/')
		const pushSpy = vi.spyOn(router, 'push')
		const wrapper = mount(HomeView, {
			global: {
				plugins: [pinia, router],
				stubs: commonStubs,
			},
		})
		await flushPromises()

		await wrapper.find('[aria-label="Gå til robust stemmemodus"]').trigger('click')
		expect(pushSpy).toHaveBeenCalledWith({ name: 'voice' })
	})
})
