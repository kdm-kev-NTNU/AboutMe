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
    expect(wrapper.text()).toContain('Output consistency and reproducibility')
    expect(wrapper.text()).toContain('Query analysis and corpus maintenance')
    expect(wrapper.text()).not.toContain('Retrieval pipeline improvements')
    expect(wrapper.text()).not.toContain('Continuous model lifecycle')
    expect(wrapper.text()).not.toContain('Custom voice clone')
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
    expect(wrapper.text()).toContain('Konsistens og reproduserbarhet i svar')
    expect(wrapper.text()).toContain('Spørringsanalyse og korpusvedlikehold')
    expect(wrapper.text()).not.toContain('Forbedringer i retrieval-pipelinen')
    expect(wrapper.text()).not.toContain('Kontinuerlig modell-livssyklus')
    expect(wrapper.text()).not.toContain('Egen stemmeklone')
  })
})
