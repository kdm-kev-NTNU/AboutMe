import { mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { nextTick } from 'vue'
import { createPinia, setActivePinia } from 'pinia'
import { useLangStore } from '@/stores/lang'
import CookieConsentBanner from '../CookieConsentBanner.vue'
import {
  grantAllCookies,
  grantNecessaryCookiesOnly,
  rejectOptionalCookies,
  isCookieBannerDismissed,
  isPosthogEnabled,
  __setPosthogTestEnv,
} from '../../lib/posthog-consent'
import { openCookieSettings } from '../../lib/cookie-settings-state'

vi.mock('../../lib/posthog-consent', async () => {
  const actual = await vi.importActual<typeof import('../../lib/posthog-consent')>(
    '../../lib/posthog-consent',
  )
  return {
    ...actual,
    grantAllCookies: vi.fn(),
    grantNecessaryCookiesOnly: vi.fn(),
    rejectOptionalCookies: vi.fn(),
    isCookieBannerDismissed: vi.fn(() => false),
    isPosthogEnabled: vi.fn(() => true),
  }
})

vi.mock('../../lib/cookie-settings-state', () => ({
  openCookieSettings: vi.fn(),
}))

describe('CookieConsentBanner', () => {
  const mountBanner = () =>
    mount(CookieConsentBanner, {
      global: {
        stubs: { RouterLink: { template: '<a><slot /></a>' } },
      },
    })

  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(isCookieBannerDismissed).mockReturnValue(false)
    vi.mocked(isPosthogEnabled).mockReturnValue(true)
    setActivePinia(createPinia())
    __setPosthogTestEnv({ enabled: true, key: 'phc_test', host: 'https://eu.i.posthog.com' })
  })

  it('renders when banner is not dismissed', async () => {
    const wrapper = mountBanner()
    await nextTick()
    expect(wrapper.find('[role="region"]').exists()).toBe(true)
  })

  it('mentions customization and session replay in English banner copy', async () => {
    useLangStore().setLanguage('en')
    const wrapper = mountBanner()
    await nextTick()
    expect(wrapper.text()).toContain('session replay')
    expect(wrapper.text()).toContain('Customize')
  })

  it('mentions customization in Norwegian banner copy', async () => {
    useLangStore().setLanguage('no')
    const wrapper = mountBanner()
    await nextTick()
    expect(wrapper.text()).toContain('sesjonsopptak')
    expect(wrapper.text()).toContain('Tilpass')
  })

  it('does not render when consent was previously recorded', async () => {
    vi.mocked(isCookieBannerDismissed).mockReturnValue(true)
    const wrapper = mountBanner()
    await nextTick()
    expect(wrapper.find('[role="region"]').exists()).toBe(false)
  })

  it('accepts all cookies and hides banner', async () => {
    const wrapper = mountBanner()
    await nextTick()
    await wrapper.get('button').trigger('click')
    expect(grantAllCookies).toHaveBeenCalledWith('banner_accept_all')
    expect(wrapper.find('[role="region"]').exists()).toBe(false)
  })

  it('accepts necessary only and hides banner', async () => {
    const wrapper = mountBanner()
    await nextTick()
    const buttons = wrapper.findAll('button')
    await buttons[1].trigger('click')
    expect(grantNecessaryCookiesOnly).toHaveBeenCalledWith('banner_necessary_only')
  })

  it('rejects optional cookies and hides banner', async () => {
    const wrapper = mountBanner()
    await nextTick()
    const buttons = wrapper.findAll('button')
    await buttons[2].trigger('click')
    expect(rejectOptionalCookies).toHaveBeenCalledWith('banner_reject')
  })

  it('opens cookie settings when customizing', async () => {
    const wrapper = mountBanner()
    await nextTick()
    const buttons = wrapper.findAll('button')
    await buttons[3].trigger('click')
    expect(openCookieSettings).toHaveBeenCalled()
  })
})
