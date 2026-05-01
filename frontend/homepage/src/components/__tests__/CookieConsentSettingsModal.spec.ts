import { mount, flushPromises } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { nextTick } from 'vue'
import { createPinia, setActivePinia } from 'pinia'
import CookieConsentSettingsModal from '../CookieConsentSettingsModal.vue'
import { cookieSettingsOpen } from '@/lib/cookie-settings-state'
import { useLangStore } from '@/stores/lang'
import * as posthogConsent from '@/lib/posthog-consent'

vi.mock('@/lib/posthog-consent', async () => {
  const actual = await vi.importActual<typeof import('@/lib/posthog-consent')>('@/lib/posthog-consent')
  return {
    ...actual,
    saveAnalyticsConsent: vi.fn(),
    hasAnalyticsConsent: vi.fn(() => false),
  }
})

describe('CookieConsentSettingsModal', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    useLangStore().setLanguage('en')
    cookieSettingsOpen.value = false
    vi.clearAllMocks()
    vi.mocked(posthogConsent.hasAnalyticsConsent).mockReturnValue(false)
  })

  function mountModal() {
    return mount(CookieConsentSettingsModal, {
      global: { stubs: { transition: false } },
    })
  }

  it('does not render when settings are closed', async () => {
    const wrapper = mountModal()
    await nextTick()
    expect(wrapper.find('[role="dialog"]').exists()).toBe(false)
  })

  it('renders dialog when opened and shows English copy', async () => {
    const wrapper = mountModal()
    cookieSettingsOpen.value = true
    await nextTick()
    await flushPromises()

    const dialog = wrapper.find('[role="dialog"]')
    expect(dialog.exists()).toBe(true)
    expect(dialog.text()).toContain('Cookies and analytics')
    expect(dialog.text()).toContain('Necessary')
  })

  it('save calls saveAnalyticsConsent and closes', async () => {
    const wrapper = mountModal()
    cookieSettingsOpen.value = true
    await nextTick()
    await flushPromises()

    const analytics = wrapper.find('input[type="checkbox"]:not([disabled])')
    await analytics.setValue(true)

    const saveBtn = wrapper.findAll('button').find((b) => b.text().includes('Save choices'))
    expect(saveBtn).toBeDefined()
    await saveBtn!.trigger('click')
    await nextTick()

    expect(posthogConsent.saveAnalyticsConsent).toHaveBeenCalledWith(true, 'settings')
    expect(cookieSettingsOpen.value).toBe(false)
  })

  it('cancel closes the modal', async () => {
    const wrapper = mountModal()
    cookieSettingsOpen.value = true
    await nextTick()
    await flushPromises()

    const cancelBtn = wrapper.findAll('button').find((b) => b.text().includes('Cancel'))
    expect(cancelBtn).toBeDefined()
    await cancelBtn!.trigger('click')
    await nextTick()
    expect(cookieSettingsOpen.value).toBe(false)
  })

  it('renders Norwegian copy when language is no', async () => {
    useLangStore().setLanguage('no')
    const wrapper = mountModal()
    cookieSettingsOpen.value = true
    await nextTick()
    await flushPromises()

    expect(wrapper.find('[role="dialog"]').text()).toContain('Informasjonskapsler')
    expect(wrapper.find('[role="dialog"]').text()).toContain('Lagre valg')
  })
})
