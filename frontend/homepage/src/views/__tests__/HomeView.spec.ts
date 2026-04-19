import { beforeEach, describe, expect, it, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'
import HomeView from '../HomeView.vue'
import { listChatModels, ChatModelOptionProvider } from '@/api/generated/portfolio'
import { useChatModelStore } from '@/stores/model'

vi.mock('@/api/generated/portfolio', async (importOriginal) => {
	const mod = await importOriginal<typeof import('@/api/generated/portfolio')>()
	return { ...mod, listChatModels: vi.fn() }
})

describe('HomeView', () => {
	function makeRouter() {
		return createRouter({
			history: createMemoryHistory(),
			routes: [
				{ path: '/', name: 'home', component: HomeView },
				{ path: '/chat', name: 'chat', component: { template: '<div>chat</div>' } },
			],
		})
	}

	beforeEach(() => {
		sessionStorage.clear()
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
					Button: { template: '<button type="submit"><slot /></button>' },
					Alert: { template: '<div><slot /></div>' },
					AlertTitle: { template: '<div><slot /></div>' },
					AlertDescription: { template: '<div><slot /></div>' },
					Info: true,
					Github: true,
					Linkedin: true,
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
					Button: { template: '<button type="submit"><slot /></button>' },
					Alert: { template: '<div><slot /></div>' },
					AlertTitle: { template: '<div><slot /></div>' },
					AlertDescription: { template: '<div><slot /></div>' },
					Info: true,
					Github: true,
					Linkedin: true,
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
					Button: { template: '<button type="submit"><slot /></button>' },
					Alert: { template: '<div><slot /></div>' },
					AlertTitle: { template: '<div><slot /></div>' },
					AlertDescription: { template: '<div><slot /></div>' },
					Info: true,
					Github: true,
					Linkedin: true,
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
})
