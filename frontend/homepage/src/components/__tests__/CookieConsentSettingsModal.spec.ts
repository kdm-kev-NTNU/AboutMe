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
    saveGranularConsent: vi.fn(),
    getConsentRecord: vi.fn(() => null),
  }
})

describe('CookieConsentSettingsModal', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    useLangStore().setLanguage('en')
    cookieSettingsOpen.value = false
    vi.clearAllMocks()
    vi.mocked(posthogConsent.getConsentRecord).mockReturnValue(null)
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
    expect(dialog.text()).toContain('Pageview tracking')
    expect(dialog.text()).toContain('Session recordings')
  })

  it('save calls saveGranularConsent and closes', async () => {
    const wrapper = mountModal()
    cookieSettingsOpen.value = true
    await nextTick()
    await flushPromises()

    const optionalBoxes = wrapper.findAll('input[type="checkbox"]:not([disabled])')
    expect(optionalBoxes.length).toBe(4)
    await optionalBoxes[0].setValue(true)
    await optionalBoxes[1].setValue(true)

    const saveBtn = wrapper.findAll('button').find((b) => b.text().includes('Save choices'))
    expect(saveBtn).toBeDefined()
    await saveBtn!.trigger('click')
    await nextTick()

    expect(posthogConsent.saveGranularConsent).toHaveBeenCalledWith(
      {
        pageviews: true,
        sessionRecording: true,
        errorTracking: false,
        featureFlags: false,
      },
      'settings',
    )
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
