import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'
import ChatHistory from '../ChatHistory.vue'

const headersJson = new Headers({ 'Content-Type': 'application/json' })

describe('ChatHistory', () => {
	function makeRouter() {
		return createRouter({
			history: createMemoryHistory(),
			routes: [
				{ path: '/', name: 'home', component: { template: '<div />' } },
				{ path: '/chat-history', name: 'chat-history', component: ChatHistory },
				{ path: '/chat', name: 'chat', component: { template: '<div />' } },
			],
		})
	}

	beforeEach(() => {
		sessionStorage.clear()
	})

	afterEach(() => {
		vi.unstubAllGlobals()
	})

	it('loads sessions and continues an existing conversation from the grid', async () => {
		const list = [
			{
				id: 9,
				startedAt: '2026-01-01T00:00:00Z',
				endedAt: '2026-01-02T00:00:00Z',
				messageCount: 2,
				preview: 'Hello world',
			},
		]
		const detail = {
			id: 9,
			startedAt: '2026-01-01T00:00:00Z',
			endedAt: '2026-01-02T00:00:00Z',
			messages: [
				{ id: 1, role: 'user', text: 'Hi', createdAt: '2026-01-01T00:00:01Z' },
				{ id: 2, role: 'assistant', text: 'Hey', createdAt: '2026-01-01T00:00:02Z' },
			],
		}
		vi.stubGlobal(
			'fetch',
			vi.fn(async (input: RequestInfo | URL) => {
				const url = typeof input === 'string' ? input : input instanceof Request ? input.url : String(input)
				if (url === '/api/conversations') {
					return new Response(JSON.stringify(list), { status: 200, headers: headersJson })
				}
				if (url === '/api/conversations/9') {
					return new Response(JSON.stringify(detail), { status: 200, headers: headersJson })
				}
				return new Response('{}', { status: 404, headers: headersJson })
			}),
		)
		const pinia = createPinia()
		setActivePinia(pinia)
		const router = makeRouter()
		const pushSpy = vi.spyOn(router, 'push')
		await router.push('/chat-history')
		const wrapper = mount(ChatHistory, {
			global: {
				plugins: [pinia, router],
				stubs: {
					MessageSquare: true,
					Calendar: true,
					Eye: true,
				},
			},
		})
		await flushPromises()
		await vi.waitFor(() => {
			expect(wrapper.text()).toContain('Hello world')
		})
		const continueBtn = wrapper.findAll('button').find((b) => b.text().includes('Continue'))
		expect(continueBtn).toBeTruthy()
		await continueBtn!.trigger('click')
		expect(pushSpy).toHaveBeenCalledWith({ name: 'chat', query: { conversationId: '9' } })
	})

	it('shows API error message when list request fails', async () => {
		vi.stubGlobal(
			'fetch',
			vi.fn(async () =>
				new Response(JSON.stringify({ error: 'nope' }), { status: 500, headers: headersJson }),
			),
		)
		const pinia = createPinia()
		setActivePinia(pinia)
		const router = makeRouter()
		await router.push('/chat-history')
		const wrapper = mount(ChatHistory, {
			global: {
				plugins: [pinia, router],
				stubs: { MessageSquare: true },
			},
		})
		await flushPromises()
		await vi.waitFor(() => {
			expect(wrapper.text()).toContain('nope')
		})
	})

	it('shows network error when fetch throws', async () => {
		vi.stubGlobal('fetch', vi.fn(async () => Promise.reject(new Error('down'))))
		const pinia = createPinia()
		setActivePinia(pinia)
		const router = makeRouter()
		await router.push('/chat-history')
		const wrapper = mount(ChatHistory, {
			global: {
				plugins: [pinia, router],
				stubs: { MessageSquare: true },
			},
		})
		await flushPromises()
		await vi.waitFor(() => {
			expect(wrapper.text()).toMatch(/Network error|Nettverksfeil/)
		})
	})
})
