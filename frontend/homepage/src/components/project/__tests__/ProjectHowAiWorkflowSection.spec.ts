import { describe, expect, it } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { MotionPlugin } from '@vueuse/motion'
import ProjectHowAiWorkflowSection from '../ProjectHowAiWorkflowSection.vue'
import { useLangStore } from '@/stores/lang'

describe('ProjectHowAiWorkflowSection', () => {
  it('renders English workflow hero and embed', async () => {
    const pinia = createPinia()
    setActivePinia(pinia)
    useLangStore().setLanguage('en')
    const wrapper = mount(ProjectHowAiWorkflowSection, {
      global: { plugins: [pinia, MotionPlugin] },
    })
    await flushPromises()
    expect(wrapper.text()).toContain('How I use AI without outsourcing the thinking')
    expect(wrapper.find('iframe').attributes('src')).toContain('C87ITeVS9hs')
  })

  it('renders Norwegian workflow hero', async () => {
    const pinia = createPinia()
    setActivePinia(pinia)
    useLangStore().setLanguage('no')
    const wrapper = mount(ProjectHowAiWorkflowSection, {
      global: { plugins: [pinia, MotionPlugin] },
    })
    await flushPromises()
    expect(wrapper.text()).toContain('Slik bruker jeg AI uten å slippe ansvaret')
  })
})
