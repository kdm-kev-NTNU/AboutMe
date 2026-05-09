import { beforeEach, describe, expect, it, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'
import HomeView from '../HomeView.vue'
import { useLangStore } from '@/stores/lang'

vi.mock('@/composables/useOpenAiRealtimeVoice', () => {
	const ref = (v: unknown) => ({ value: v })
	return {
		useOpenAiRealtimeVoice: vi.fn(() => ({
			status: ref({ enabled: true, sessionMaxMinutes: 3, model: 'gpt-4o-realtime' }),
			loadStatus: vi.fn().mockResolvedValue(undefined),
		})),
	}
})

vi.mock('@/stores/auth', () => ({
	useAuthStore: () => ({ restore: vi.fn() }),
}))

const buttonStub = {
	props: ['type', 'variant', 'disabled'],
	template: '<button :type="type === \'submit\' ? \'submit\' : \'button\'" :disabled="disabled"><slot /></button>',
}

describe('HomeView - Dual Mode Gateway', () => {
	function makeRouter() {
		return createRouter({
			history: createMemoryHistory(),
			routes: [
				{ path: '/', name: 'home', component: HomeView },
				{ path: '/chat', name: 'chat', component: { template: '<div>chat</div>' } },
				{ path: '/voice', name: 'voice', component: { template: '<div>voice</div>' } },
			],
		})
	}

	beforeEach(() => {
		sessionStorage.clear()
		localStorage.clear()
		vi.clearAllMocks()
	})

	it('renders the dual mode gateway with Talk and Ask cards', async () => {
		const pinia = createPinia()
		setActivePinia(pinia)
		const router = makeRouter()
		await router.push('/')
		const wrapper = mount(HomeView, {
			global: {
				plugins: [pinia, router],
				stubs: {
					Button: buttonStub,
					VoiceOrb: { template: '<div class="voice-orb-stub" />' },
					BudgetDialog: { props: ['open'], template: '<div />' },
				},
			},
		})
		await flushPromises()

		expect(wrapper.text()).toContain('Choose how you want to meet the AI')
		expect(wrapper.text()).toContain('Talk')
		expect(wrapper.text()).toContain('Ask')
		expect(wrapper.text()).toContain('Start voice')
		expect(wrapper.text()).toContain('Open chat')
	})

	it('switches language between EN and NO', async () => {
		const pinia = createPinia()
		setActivePinia(pinia)
		const router = makeRouter()
		await router.push('/')
		const wrapper = mount(HomeView, {
			global: {
				plugins: [pinia, router],
				stubs: {
					Button: buttonStub,
					VoiceOrb: { template: '<div />' },
					BudgetDialog: { props: ['open'], template: '<div />' },
				},
			},
		})
		await flushPromises()

		const noBtn = wrapper.findAll('button').find((b) => b.text().trim() === 'NO')
		expect(noBtn).toBeTruthy()
		await noBtn!.trigger('click')
		expect(wrapper.text()).toContain('Velg hvordan du vil møte AI-en')
		expect(wrapper.text()).toContain('Snakk')
	})

	it('navigates to /voice when Start voice is clicked and voice is enabled', async () => {
		const pinia = createPinia()
		setActivePinia(pinia)
		const router = makeRouter()
		await router.push('/')
		const pushSpy = vi.spyOn(router, 'push')
		const wrapper = mount(HomeView, {
			global: {
				plugins: [pinia, router],
				stubs: {
					Button: buttonStub,
					VoiceOrb: { template: '<div />' },
					BudgetDialog: { props: ['open'], template: '<div />' },
				},
			},
		})
		await flushPromises()

		const startBtn = wrapper.findAll('button').find((b) => b.text().includes('Start voice'))
		expect(startBtn).toBeTruthy()
		await startBtn!.trigger('click')
		await flushPromises()

		expect(pushSpy).toHaveBeenCalledWith({ name: 'voice' })
	})

	it('navigates to /chat when Open chat is clicked', async () => {
		const pinia = createPinia()
		setActivePinia(pinia)
		const router = makeRouter()
		await router.push('/')
		const pushSpy = vi.spyOn(router, 'push')
		const wrapper = mount(HomeView, {
			global: {
				plugins: [pinia, router],
				stubs: {
					Button: buttonStub,
					VoiceOrb: { template: '<div />' },
					BudgetDialog: { props: ['open'], template: '<div />' },
				},
			},
		})
		await flushPromises()

		const chatBtn = wrapper.findAll('button').find((b) => b.text().includes('Open chat'))
		expect(chatBtn).toBeTruthy()
		await chatBtn!.trigger('click')
		await flushPromises()

		expect(pushSpy).toHaveBeenCalledWith({ name: 'chat' })
	})

	it('shows budget dialog when voice is disabled', async () => {
		const { useOpenAiRealtimeVoice } = await import('@/composables/useOpenAiRealtimeVoice')
		vi.mocked(useOpenAiRealtimeVoice).mockReturnValue({
			status: { value: { enabled: false, sessionMaxMinutes: 3, model: '' } } as any,
			loadStatus: vi.fn().mockResolvedValue(undefined),
		} as any)

		const pinia = createPinia()
		setActivePinia(pinia)
		const router = makeRouter()
		await router.push('/')
		const wrapper = mount(HomeView, {
			global: {
				plugins: [pinia, router],
				stubs: {
					Button: buttonStub,
					VoiceOrb: { template: '<div />' },
					BudgetDialog: { props: ['open'], template: '<div v-if="open" class="budget-dialog">budget popup</div>' },
				},
			},
		})
		await flushPromises()

		const startBtn = wrapper.findAll('button').find((b) => b.text().includes('Start voice'))
		await startBtn!.trigger('click')
		await flushPromises()

		expect(wrapper.find('.budget-dialog').exists()).toBe(true)
	})
})
