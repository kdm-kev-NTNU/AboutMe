import { beforeEach, describe, expect, it, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'
import HomeView from '../HomeView.vue'
import { listChatModels, ChatModelOptionProvider } from '@/api/generated/portfolio'
import { useChatModelStore } from '@/stores/model'
import { useLangStore } from '@/stores/lang'

vi.mock('@/api/generated/portfolio', async (importOriginal) => {
	const mod = await importOriginal<typeof import('@/api/generated/portfolio')>()
	return { ...mod, listChatModels: vi.fn() }
})

vi.mock('@/lib/realtime-voice', () => ({
	fetchRealtimeVoiceEnabled: vi.fn().mockResolvedValue(false),
}))

vi.mock('@/stores/auth', () => ({
	useAuthStore: () => ({ restore: vi.fn() }),
}))

const buttonStub = {
	props: ['type'],
	template: '<button :type="type === \'submit\' ? \'submit\' : \'button\'"><slot /></button>',
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
		vi.mocked(listChatModels).mockResolvedValue({
			status: 200,
			data: [
				{ id: 'o1', label: 'GPT', provider: ChatModelOptionProvider.OPENAI },
				{ id: 'a1', label: 'Claude', provider: ChatModelOptionProvider.ANTHROPIC },
			],
			headers: new Headers(),
		})
	})

	it('switches language with EN/NO toggles and shows Norwegian disclaimer', async () => {
		const pinia = createPinia()
		setActivePinia(pinia)
		const router = makeRouter()
		await router.push('/')
		const pushSpy = vi.spyOn(router, 'push')
		const wrapper = mount(HomeView, {
			global: {
				plugins: [pinia, router],
				stubs: {
					Input: { props: ['modelValue'], template: '<input />' },
					Button: buttonStub,
					Alert: { template: '<div><slot /></div>' },
					AlertTitle: { template: '<div><slot /></div>' },
					AlertDescription: { template: '<div><slot /></div>' },
					Info: true,
					Github: true,
					Linkedin: true,
					MessageSquare: true,
					ChevronRight: true,
				},
			},
		})
		await flushPromises()
		const noBtn = wrapper.findAll('button').find((b) => b.text().trim() === 'NO')
		expect(noBtn).toBeTruthy()
		await noBtn!.trigger('click')
		expect(wrapper.text()).toContain('Før du chatter')

		const firstQuick = wrapper.findAll('section.quick button').at(0)
		expect(firstQuick).toBeTruthy()
		await firstQuick!.trigger('click')
		expect(pushSpy).toHaveBeenCalled()
	})

	it('submits quick question via form and navigates to chat', async () => {
		const pinia = createPinia()
		setActivePinia(pinia)
		const router = makeRouter()
		await router.push('/')
		const pushSpy = vi.spyOn(router, 'push')
		const wrapper = mount(HomeView, {
			global: {
				plugins: [pinia, router],
				stubs: {
					Input: {
						props: ['modelValue'],
						emits: ['update:modelValue'],
						template: '<input @input="$emit(\'update:modelValue\', $event.target.value)" />',
					},
					Button: buttonStub,
					Alert: { template: '<div><slot /></div>' },
					AlertTitle: { template: '<div><slot /></div>' },
					AlertDescription: { template: '<div><slot /></div>' },
					Info: true,
					Github: true,
					Linkedin: true,
					MessageSquare: true,
					ChevronRight: true,
				},
			},
		})
		await flushPromises()
		const input = wrapper.get('form input')
		await input.setValue('  custom question  ')
		await wrapper.get('form').trigger('submit.prevent')
		expect(pushSpy).toHaveBeenCalledWith({ name: 'chat', query: { q: 'custom question' } })
	})

	it('toggles AI provider when both are available', async () => {
		const pinia = createPinia()
		setActivePinia(pinia)
		const router = makeRouter()
		await router.push('/')
		const wrapper = mount(HomeView, {
			global: {
				plugins: [pinia, router],
				stubs: {
					Input: { props: ['modelValue'], template: '<input />' },
					Button: buttonStub,
					Alert: { template: '<div><slot /></div>' },
					AlertTitle: { template: '<div><slot /></div>' },
					AlertDescription: { template: '<div><slot /></div>' },
					Info: true,
					Github: true,
					Linkedin: true,
					MessageSquare: true,
					ChevronRight: true,
				},
			},
		})
		await flushPromises()
		const anthropicBtn = wrapper.findAll('button[type="button"]').find((b) => b.text().includes('Anthropic'))
		expect(anthropicBtn).toBeTruthy()
		await anthropicBtn!.trigger('click')
		await flushPromises()
		expect(useChatModelStore().selectedModelId).toBe('a1')
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
				stubs: {
					Input: { props: ['modelValue'], template: '<input />' },
					Button: buttonStub,
					Alert: { template: '<div><slot /></div>' },
					AlertTitle: { template: '<div><slot /></div>' },
					AlertDescription: { template: '<div><slot /></div>' },
					Info: true,
					Github: true,
					Linkedin: true,
					MessageSquare: true,
					ChevronRight: true,
				},
			},
		})
		await flushPromises()
		const futureLink = wrapper.get('a[href="/project#future-work"]')
		expect(futureLink.text()).toContain('Future work and improvements')
	})

	it('navigates to voice chat when Speak button is clicked', async () => {
		const pinia = createPinia()
		setActivePinia(pinia)
		useLangStore().setLanguage('en')
		const router = makeRouter()
		await router.push('/')
		const pushSpy = vi.spyOn(router, 'push')
		const wrapper = mount(HomeView, {
			global: {
				plugins: [pinia, router],
				stubs: {
					Input: { props: ['modelValue'], template: '<input />' },
					Button: buttonStub,
					Alert: { template: '<div><slot /></div>' },
					AlertTitle: { template: '<div><slot /></div>' },
					AlertDescription: { template: '<div><slot /></div>' },
					Info: true,
					Github: true,
					Linkedin: true,
					MessageSquare: true,
					ChevronRight: true,
					Mic: true,
					Headphones: true,
				},
			},
		})
		await flushPromises()

		await wrapper.find('[aria-label="Go to live voice chat"]').trigger('click')
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
				stubs: {
					Input: { props: ['modelValue'], template: '<input />' },
					Button: buttonStub,
					Alert: { template: '<div><slot /></div>' },
					AlertTitle: { template: '<div><slot /></div>' },
					AlertDescription: { template: '<div><slot /></div>' },
					Info: true,
					Github: true,
					Linkedin: true,
					MessageSquare: true,
					ChevronRight: true,
					Mic: true,
					Headphones: true,
				},
			},
		})
		await flushPromises()

		await wrapper.find('[aria-label="Gå til live stemmechat"]').trigger('click')
		expect(pushSpy).toHaveBeenCalledWith({ name: 'voice' })
	})
})
