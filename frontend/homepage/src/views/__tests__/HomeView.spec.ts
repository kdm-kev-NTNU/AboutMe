import { beforeEach, describe, expect, it, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'
import HomeView from '../HomeView.vue'
import { useLangStore } from '@/stores/lang'

const fetchRealtimeVoiceStatus = vi.fn()

vi.mock('@/lib/realtime-voice', () => ({
	fetchRealtimeVoiceStatus: (...args: unknown[]) => fetchRealtimeVoiceStatus(...args),
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

const voiceOnStatus = {
	enabled: true,
	liveEnabled: true,
	liveDisabledReason: null,
	voices: ['marin', 'cedar'],
	reasoningEfforts: ['low', 'medium', 'high'],
	vadEagernessOptions: ['low', 'medium', 'high', 'auto'],
	voice: 'marin',
	reasoningEffort: 'low',
	vadEagerness: 'medium',
}

const voiceOffStatus = {
	...voiceOnStatus,
	liveEnabled: false,
	liveDisabledReason: 'KILL_SWITCH' as const,
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
				{ path: '/how', name: 'how', component: { template: '<div>how</div>' } },
			],
		})
	}

	beforeEach(() => {
		sessionStorage.clear()
		localStorage.clear()
		vi.clearAllMocks()
		fetchRealtimeVoiceStatus.mockResolvedValue(voiceOnStatus)
	})

	it('switches language with EN/NO toggle and renders Norwegian voice-first hero when voice is on', async () => {
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
		expect(wrapper.find('[data-testid="home-voice-cta"]').exists()).toBe(true)
	})

	it('renders text-first hero when voice is unavailable', async () => {
		fetchRealtimeVoiceStatus.mockResolvedValue(voiceOffStatus)
		const pinia = createPinia()
		setActivePinia(pinia)
		useLangStore().setLanguage('en')
		const router = makeRouter()
		await router.push('/')
		const wrapper = mount(HomeView, {
			global: {
				plugins: [pinia, router],
				stubs: commonStubs,
			},
		})
		await flushPromises()
		expect(wrapper.text()).toContain("Ask Kevin's AI in text.")
		expect(wrapper.find('[data-testid="home-voice-cta"]').exists()).toBe(false)
		expect(wrapper.find('[data-testid="home-chat-primary"]').exists()).toBe(true)
		expect(wrapper.find('[data-testid="home-chat-illustration"]').exists()).toBe(true)
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

		await wrapper.find('[data-testid="home-voice-cta"]').trigger('click')
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

		await wrapper.find('[aria-label="Gå til stemmemodus"]').trigger('click')
		expect(pushSpy).toHaveBeenCalledWith({ name: 'voice' })
	})
})
