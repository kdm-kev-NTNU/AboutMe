import { describe, expect, it } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import ProjectFutureWorkSection from '../ProjectFutureWorkSection.vue'
import { useLangStore } from '@/stores/lang'

describe('ProjectFutureWorkSection', () => {
  it('renders English roadmap title and first section', async () => {
    const pinia = createPinia()
    setActivePinia(pinia)
    useLangStore().setLanguage('en')
    const wrapper = mount(ProjectFutureWorkSection, {
      global: { plugins: [pinia] },
    })
    await flushPromises()
    expect(wrapper.text()).toContain('Future work and improvements')
    expect(wrapper.text()).toMatch(/ElevenLabs|retrieval/i)
  })

  it('renders Norwegian roadmap title', async () => {
    const pinia = createPinia()
    setActivePinia(pinia)
    useLangStore().setLanguage('no')
    const wrapper = mount(ProjectFutureWorkSection, {
      global: { plugins: [pinia] },
    })
    await flushPromises()
    expect(wrapper.text()).toContain('Videre arbeid og forbedringer')
  })
})
