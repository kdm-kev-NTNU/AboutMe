import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import VoiceModeSwitcher from '../VoiceModeSwitcher.vue'

describe('VoiceModeSwitcher', () => {
  it('emits selected mode', async () => {
    const wrapper = mount(VoiceModeSwitcher, {
      props: { modelValue: 'standard', language: 'en' },
    })
    await wrapper.get('button:nth-of-type(2)').trigger('click')
    expect(wrapper.emitted('update:modelValue')?.[0]).toEqual(['live'])
  })

  it('renders Norwegian labels and highlights the active mode', () => {
    const wrapper = mount(VoiceModeSwitcher, {
      props: { modelValue: 'live', language: 'no' },
    })
    expect(wrapper.text()).toContain('Anbefalt')
    expect(wrapper.text()).toContain('Mindre stabil')
    expect(wrapper.get('button:nth-of-type(2)').classes()).toContain('bg-amber-50')
  })
})
