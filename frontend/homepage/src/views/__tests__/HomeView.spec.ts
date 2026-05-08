import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'
import HomeView from '../HomeView.vue'
import { listChatModels, ChatModelOptionProvider } from '@/api/generated/portfolio'
import { useChatModelStore } from '@/stores/model'
import { useLangStore } from '@/stores/lang'
import { transcribeSpeech } from '@/lib/transcribe-audio'

vi.mock('@/api/generated/portfolio', async (importOriginal) => {
	const mod = await importOriginal<typeof import('@/api/generated/portfolio')>()
	return { ...mod, listChatModels: vi.fn() }
})

vi.mock('@/lib/transcribe-audio', () => ({
	transcribeSpeech: vi.fn(),
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

	describe('voice input to chat', () => {
		const origMediaDevices = globalThis.navigator.mediaDevices

		function stubMediaRecorder() {
			class StubRecorder {
				static isTypeSupported = () => true
				state = 'inactive'
				ondataavailable: ((ev: { data: Blob }) => void) | null = null
				private listeners: Record<string, Array<(ev?: Event) => void>> = {}
				start() {
					this.state = 'recording'
				}
				stop() {
					this.state = 'inactive'
					for (const cb of this.listeners['stop'] ?? []) cb()
				}
				addEventListener(type: string, cb: (ev?: Event) => void) {
					if (!this.listeners[type]) this.listeners[type] = []
					this.listeners[type].push(cb)
				}
				requestData = () => {
					this.ondataavailable?.({ data: new Blob([new Uint8Array([1])]) } as BlobEvent)
				}
			}
			vi.stubGlobal('MediaRecorder', StubRecorder as unknown as typeof MediaRecorder)
		}

		function stubWebAudioAndResize() {
			class FakeAnalyser {
				fftSize = 2048
				smoothingTimeConstant = 0
				connect() {
					return this
				}
				disconnect() {}
				getFloatTimeDomainData(arr: Float32Array) {
					for (let i = 0; i < arr.length; i++) arr[i] = 0
				}
			}
			class FakeMediaStreamAudioSourceNode {
				connect() {
					return new FakeAnalyser()
				}
				disconnect() {}
			}
			class FakeAudioContext {
				state: AudioContextState = 'running'
				resume = vi.fn().mockResolvedValue(undefined)
				close = vi.fn().mockResolvedValue(undefined)
				createMediaStreamSource = vi.fn(() => new FakeMediaStreamAudioSourceNode())
				createAnalyser = vi.fn(() => new FakeAnalyser())
			}
			vi.stubGlobal('AudioContext', FakeAudioContext as unknown as typeof AudioContext)
			vi.stubGlobal(
				'ResizeObserver',
				class {
					observe() {}
					unobserve() {}
					disconnect() {}
				},
			)
		}

		afterEach(() => {
			Object.defineProperty(globalThis.navigator, 'mediaDevices', {
				value: origMediaDevices,
				configurable: true,
			})
			vi.unstubAllGlobals()
		})

		function mountHome() {
			const pinia = createPinia()
			setActivePinia(pinia)
			useLangStore().setLanguage('en')
			const router = makeRouter()
			return router.push('/').then(() => ({
				router,
				wrapper: mount(HomeView, {
					global: {
						plugins: [pinia, router],
						stubs: {
							Input: { props: ['modelValue'], template: '<input />' },
							Button: buttonStub,
							Alert: {
								props: ['variant'],
								template: '<div :data-variant="variant"><slot /></div>',
							},
							AlertTitle: { template: '<div><slot /></div>' },
							AlertDescription: { template: '<div><slot /></div>' },
							Info: true,
							Github: true,
							Linkedin: true,
							MessageSquare: true,
							ChevronRight: true,
							Mic: true,
							Square: true,
							Loader2: true,
						},
					},
				}),
			}))
		}

		it('opens chat with transcribed query after finishing voice input', async () => {
			Object.defineProperty(globalThis.navigator, 'mediaDevices', {
				value: { getUserMedia: vi.fn().mockResolvedValue({ getTracks: () => [{ stop: vi.fn() }] }) },
				configurable: true,
			})
			stubMediaRecorder()
			stubWebAudioAndResize()

			vi.mocked(transcribeSpeech).mockResolvedValue({
				status: 200,
				data: { text: 'hello voice' },
				headers: new Headers(),
			})

			const { router, wrapper } = await mountHome()
			const pushSpy = vi.spyOn(router, 'push')
			await flushPromises()

			await wrapper.find('[aria-label="Voice input"]').trigger('click')
			await flushPromises()

			await wrapper.find('[aria-label="Voice input"]').trigger('click')
			await flushPromises()

			expect(pushSpy).toHaveBeenCalledWith({ name: 'chat', query: { q: 'hello voice' } })
		})

		it('shows the destructive voice-error alert when transcription fails with 500', async () => {
			Object.defineProperty(globalThis.navigator, 'mediaDevices', {
				value: { getUserMedia: vi.fn().mockResolvedValue({ getTracks: () => [{ stop: vi.fn() }] }) },
				configurable: true,
			})
			stubMediaRecorder()
			stubWebAudioAndResize()

			vi.mocked(transcribeSpeech).mockResolvedValue({
				status: 500,
				data: { error: 'server msg ignored on purpose' },
				headers: new Headers(),
			})

			const { router, wrapper } = await mountHome()
			const pushSpy = vi.spyOn(router, 'push')
			await flushPromises()

			await wrapper.find('[aria-label="Voice input"]').trigger('click')
			await flushPromises()
			await wrapper.find('[aria-label="Voice input"]').trigger('click')
			await flushPromises()

			expect(pushSpy).not.toHaveBeenCalledWith(
				expect.objectContaining({ name: 'chat' }),
			)
			// The composable maps 500s to a canned i18n message; HomeView surfaces it in the
			// destructive alert at the bottom.
			expect(wrapper.text()).toContain(
				'Transcription failed on the server. Please try again.',
			)
			expect(wrapper.html()).toContain('data-variant="destructive"')
		})

		it('forwards the active UI language to transcribeSpeech', async () => {
			Object.defineProperty(globalThis.navigator, 'mediaDevices', {
				value: { getUserMedia: vi.fn().mockResolvedValue({ getTracks: () => [{ stop: vi.fn() }] }) },
				configurable: true,
			})
			stubMediaRecorder()
			stubWebAudioAndResize()

			vi.mocked(transcribeSpeech).mockResolvedValue({
				status: 200,
				data: { text: 'hei' },
				headers: new Headers(),
			})

			const pinia = createPinia()
			setActivePinia(pinia)
			useLangStore().setLanguage('no')
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
						Mic: true,
						Square: true,
						Loader2: true,
					},
				},
			})
			await flushPromises()

			await wrapper.find('[aria-label="Taleinndata"]').trigger('click')
			await flushPromises()
			await wrapper.find('[aria-label="Taleinndata"]').trigger('click')
			await flushPromises()

			expect(transcribeSpeech).toHaveBeenCalledWith(expect.any(Blob), 'no')
		})
	})
})
