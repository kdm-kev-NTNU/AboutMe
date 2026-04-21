import { describe, expect, it, beforeEach, vi } from 'vitest'
import { defineComponent, h, nextTick } from 'vue'
import { mount } from '@vue/test-utils'
import { useConsent } from '../useConsent'

describe('useConsent', () => {
  beforeEach(() => {
    localStorage.clear()
  })

  function mountHarness() {
    return mount(
      defineComponent({
        setup() {
          const state = useConsent()
          return () => h('div', JSON.stringify(state.consent.value))
        },
      }),
    )
  }

  it('uses safe default consent before any storage value exists', async () => {
    const wrapper = mountHarness()
    await nextTick()
    expect(wrapper.text()).toContain('"analytics":false')
    expect(wrapper.text()).toContain('"marketing":false')
    expect(wrapper.text()).toContain('"necessary":true')
  })

  it('reads consent from localStorage on mount', async () => {
    localStorage.setItem('csfy_consent', JSON.stringify({ analytics: true, marketing: true }))
    const wrapper = mountHarness()
    await nextTick()
    expect(wrapper.text()).toContain('"analytics":true')
    expect(wrapper.text()).toContain('"marketing":true')
    expect(wrapper.text()).toContain('"necessary":true')
  })

  it('coerces non-boolean values to booleans when reading storage', async () => {
    localStorage.setItem('csfy_consent', JSON.stringify({ analytics: 'yes', marketing: 0 }))
    const wrapper = mountHarness()
    await nextTick()
    expect(wrapper.text()).toContain('"analytics":true')
    expect(wrapper.text()).toContain('"marketing":false')
  })

  it('keeps defaults when storage payload is invalid JSON', async () => {
    localStorage.setItem('csfy_consent', '{bad json')
    const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => {})
    const wrapper = mountHarness()
    await nextTick()
    expect(wrapper.text()).toContain('"analytics":false')
    expect(wrapper.text()).toContain('"marketing":false')
    expect(wrapper.text()).toContain('"necessary":true')
    consoleSpy.mockRestore()
  })
})
