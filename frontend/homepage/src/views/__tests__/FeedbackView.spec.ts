import { beforeEach, describe, expect, it, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'
import FeedbackView from '../FeedbackView.vue'
import { submitFeedback } from '@/api/generated/portfolio'
import { useLangStore } from '@/stores/lang'

vi.mock('@/api/generated/portfolio', async (importOriginal) => {
  const mod = await importOriginal<typeof import('@/api/generated/portfolio')>()
  return {
    ...mod,
    submitFeedback: vi.fn(),
  }
})

describe('FeedbackView', () => {
  const HomeStub = { template: '<div>home-stub</div>' }

  function makeRouter() {
    return createRouter({
      history: createMemoryHistory(),
      routes: [
        { path: '/', name: 'home', component: HomeStub },
        { path: '/feedback', name: 'feedback', component: FeedbackView },
      ],
    })
  }

  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(submitFeedback).mockResolvedValue({
      status: 204,
      data: undefined as never,
      headers: new Headers(),
    })
  })

  async function mountFeedback() {
    const pinia = createPinia()
    setActivePinia(pinia)
    useLangStore().setLanguage('en')

    const router = makeRouter()
    await router.push('/feedback')
    await router.isReady()

    const wrapper = mount(FeedbackView, {
      global: { plugins: [pinia, router] },
    })
    await flushPromises()
    return { wrapper, router, pinia }
  }

  it('renders the feedback form in English', async () => {
    const { wrapper } = await mountFeedback()
    expect(wrapper.text()).toContain('Feedback')
    expect(wrapper.find('textarea').exists()).toBe(true)
    expect(wrapper.find('button[type="submit"]').exists()).toBe(true)
  })

  it('submits feedback and shows success message', async () => {
    const { wrapper } = await mountFeedback()
    await wrapper.find('textarea').setValue('Great site!')
    await wrapper.find('form').trigger('submit.prevent')
    await flushPromises()

    expect(submitFeedback).toHaveBeenCalledWith({
      message: 'Great site!',
    })
    expect(wrapper.text()).toContain('Thank you!')
  })

  it('shows rate limit error on 429', async () => {
    vi.mocked(submitFeedback).mockResolvedValue({
      status: 429,
      data: { error: 'Too Many Requests' },
      headers: new Headers(),
    })

    const { wrapper } = await mountFeedback()
    await wrapper.find('textarea').setValue('More feedback')
    await wrapper.find('form').trigger('submit.prevent')
    await flushPromises()

    expect(wrapper.text()).toContain('Too many submissions')
  })

  it('shows generic error on network failure', async () => {
    vi.mocked(submitFeedback).mockRejectedValue(new Error('Network error'))

    const { wrapper } = await mountFeedback()
    await wrapper.find('textarea').setValue('Some feedback')
    await wrapper.find('form').trigger('submit.prevent')
    await flushPromises()

    expect(wrapper.text()).toContain('Something went wrong')
  })

  it('disables submit button when message is empty', async () => {
    const { wrapper } = await mountFeedback()
    const submitBtn = wrapper.find('button[type="submit"]')
    expect(submitBtn.attributes('disabled')).toBeDefined()
  })
})
