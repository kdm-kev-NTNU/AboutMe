import { beforeEach, describe, expect, it, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { nextTick } from 'vue'
import CookieConsentBanner from '../CookieConsentBanner.vue'
import { useLangStore } from '@/stores/lang'
import posthog from 'posthog-js'

vi.mock('posthog-js', () => ({
  default: {
    get_explicit_consent_status: vi.fn(),
    opt_in_capturing: vi.fn(),
    opt_out_capturing: vi.fn(),
    clear_opt_in_out_capturing: vi.fn(),
  },
}))

describe('CookieConsentBanner', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    setActivePinia(createPinia())
  })

  it('shows banner when explicit consent is pending', async () => {
    vi.mocked(posthog.get_explicit_consent_status).mockReturnValue('pending')

    const wrapper = mount(CookieConsentBanner, {
      global: { plugins: [createPinia()] },
    })
    await nextTick()

    expect(wrapper.text()).toContain('Cookies and analytics')
    expect(wrapper.text()).toContain('Accept')
    expect(wrapper.text()).toContain('Decline')
  })

  it('hides banner when consent is not pending', async () => {
    vi.mocked(posthog.get_explicit_consent_status).mockReturnValue('granted')

    const wrapper = mount(CookieConsentBanner, {
      global: { plugins: [createPinia()] },
    })
    await nextTick()

    expect(wrapper.text()).toBe('')
  })

  it('accept button opts in capturing and hides banner', async () => {
    vi.mocked(posthog.get_explicit_consent_status).mockReturnValue('pending')

    const wrapper = mount(CookieConsentBanner, {
      global: { plugins: [createPinia()] },
    })
    await nextTick()

    await wrapper.get('button').trigger('click')
    expect(posthog.opt_in_capturing).toHaveBeenCalledTimes(1)
    expect(wrapper.text()).toBe('')
  })

  it('decline button opts out capturing and hides banner', async () => {
    vi.mocked(posthog.get_explicit_consent_status).mockReturnValue('pending')

    const wrapper = mount(CookieConsentBanner, {
      global: { plugins: [createPinia()] },
    })
    await nextTick()

    await wrapper.findAll('button')[1].trigger('click')
    expect(posthog.opt_out_capturing).toHaveBeenCalledTimes(1)
    expect(wrapper.text()).toBe('')
  })

  it('openConsentSettings clears status and refreshes banner visibility', async () => {
    vi.mocked(posthog.get_explicit_consent_status)
      .mockReturnValueOnce('granted')
      .mockReturnValueOnce('pending')

    const wrapper = mount(CookieConsentBanner, {
      global: { plugins: [createPinia()] },
    })

    expect(wrapper.text()).toBe('')
    ;(wrapper.vm as { openConsentSettings: () => void }).openConsentSettings()
    await wrapper.vm.$nextTick()

    expect(posthog.clear_opt_in_out_capturing).toHaveBeenCalledTimes(1)
    expect(wrapper.text()).toContain('Cookies and analytics')
  })

  it('renders norwegian text when language is no', async () => {
    vi.mocked(posthog.get_explicit_consent_status).mockReturnValue('pending')
    const pinia = createPinia()
    setActivePinia(pinia)
    useLangStore().setLanguage('no')

    const wrapper = mount(CookieConsentBanner, {
      global: { plugins: [pinia] },
    })
    await nextTick()

    expect(wrapper.text()).toContain('Informasjonskapsler og analyse')
    expect(wrapper.text()).toContain('Godta')
    expect(wrapper.text()).toContain('Avslå')
  })
})
