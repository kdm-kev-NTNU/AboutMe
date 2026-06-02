import { beforeEach, describe, expect, it, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createMemoryHistory, createRouter } from 'vue-router'
import { createPinia, setActivePinia } from 'pinia'
import FloatingChatButton from '../FloatingChatButton.vue'
import { useLangStore } from '@/stores/lang'

describe('FloatingChatButton', () => {
	beforeEach(() => {
		sessionStorage.clear()
		vi.restoreAllMocks()
		setActivePinia(createPinia())
	})

	function makeRouter() {
		return createRouter({
			history: createMemoryHistory(),
			routes: [
				{ path: '/', name: 'home', component: { template: '<div />' } },
				{ path: '/chat', name: 'chat', component: { template: '<div />' } },
			],
		})
	}

	it('does not render when there is no transcript in sessionStorage', async () => {
		const router = makeRouter()
		await router.push('/')
		await router.isReady()
		const wrapper = mount(FloatingChatButton, {
			global: {
				plugins: [router],
				stubs: { MessageCircle: { template: '<span class="icon" />' } },
			},
		})
		await flushPromises()
		expect(wrapper.find('button').exists()).toBe(false)
	})

	it('renders when off chat page and transcript exists', async () => {
		sessionStorage.setItem('chatMessages', JSON.stringify([{ role: 'user', content: 'hi' }]))
		const router = makeRouter()
		await router.push('/')
		await router.isReady()
		const wrapper = mount(FloatingChatButton, {
			global: {
				plugins: [router],
				stubs: { MessageCircle: { template: '<span class="icon" />' } },
			},
		})
		await flushPromises()
		expect(wrapper.find('button').exists()).toBe(true)
	})

	it('stays hidden on chat route even with transcript', async () => {
		sessionStorage.setItem('chatMessages', JSON.stringify([{ role: 'user', content: 'hi' }]))
		const router = makeRouter()
		await router.push('/chat')
		await router.isReady()
		const wrapper = mount(FloatingChatButton, {
			global: {
				plugins: [router],
				stubs: { MessageCircle: { template: '<span class="icon" />' } },
			},
		})
		await flushPromises()
		expect(wrapper.find('button').exists()).toBe(false)
	})

	it('ignores invalid chatMessages JSON', async () => {
		sessionStorage.setItem('chatMessages', 'not-json')
		const router = makeRouter()
		await router.push('/')
		await router.isReady()
		const wrapper = mount(FloatingChatButton, {
			global: {
				plugins: [router],
				stubs: { MessageCircle: { template: '<span class="icon" />' } },
			},
		})
		await flushPromises()
		expect(wrapper.find('button').exists()).toBe(false)
	})

	it('uses accessible aria-label in English', async () => {
		sessionStorage.setItem('chatMessages', JSON.stringify([{ role: 'user', text: 'hi' }]))
		useLangStore().setLanguage('en')
		const router = makeRouter()
		await router.push('/')
		await router.isReady()
		const wrapper = mount(FloatingChatButton, {
			global: {
				plugins: [router],
				stubs: { MessageCircle: { template: '<span class="icon" />' } },
			},
		})
		await flushPromises()
		expect(wrapper.find('button').attributes('aria-label')).toContain("Kevin's AI")
	})

	it('navigates to chat when clicked', async () => {
		sessionStorage.setItem('chatMessages', JSON.stringify([{ role: 'user', content: 'hi' }]))
		const router = makeRouter()
		const pushSpy = vi.spyOn(router, 'push')
		await router.push('/')
		await router.isReady()
		const wrapper = mount(FloatingChatButton, {
			global: {
				plugins: [router],
				stubs: { MessageCircle: { template: '<span class="icon" />' } },
			},
		})
		await flushPromises()
		await wrapper.find('button').trigger('click')
		expect(pushSpy).toHaveBeenCalledWith({ name: 'chat' })
	})
})
