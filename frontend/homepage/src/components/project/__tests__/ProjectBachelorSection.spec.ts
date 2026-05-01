import { describe, expect, it } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { MotionPlugin } from '@vueuse/motion'
import ProjectBachelorSection from '../ProjectBachelorSection.vue'
import { useLangStore } from '@/stores/lang'

describe('ProjectBachelorSection', () => {
  it('renders English bachelor hero', async () => {
    const pinia = createPinia()
    setActivePinia(pinia)
    useLangStore().setLanguage('en')
    const wrapper = mount(ProjectBachelorSection, {
      global: { plugins: [pinia, MotionPlugin] },
    })
    await flushPromises()
    expect(wrapper.text()).toContain("Bachelor's thesis")
    expect(wrapper.text()).toMatch(/Piscada|Trondheim/i)
  })

  it('renders Norwegian bachelor hero', async () => {
    const pinia = createPinia()
    setActivePinia(pinia)
    useLangStore().setLanguage('no')
    const wrapper = mount(ProjectBachelorSection, {
      global: { plugins: [pinia, MotionPlugin] },
    })
    await flushPromises()
    expect(wrapper.text()).toContain('Bacheloroppgaven')
  })
})
