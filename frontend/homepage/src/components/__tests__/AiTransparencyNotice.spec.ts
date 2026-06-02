import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'
import AiTransparencyNotice from '../AiTransparencyNotice.vue'
import { useLangStore } from '@/stores/lang'

describe('AiTransparencyNotice.vue', () => {
  function mountNotice() {
    const pinia = createPinia()
    setActivePinia(pinia)
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [{ path: '/privacy-policy', name: 'privacy-policy', component: { template: '<div />' } }],
    })
    return mount(AiTransparencyNotice, {
      global: {
        plugins: [pinia, router],
        stubs: {
          Bot: { template: '<span />' },
          Alert: { template: '<div role="note"><slot /></div>' },
          AlertTitle: { template: '<h3><slot /></h3>' },
          AlertDescription: { template: '<div><slot /></div>' },
        },
      },
    })
  }

  it('renders English disclosure by default', () => {
    const wrapper = mountNotice()
    expect(wrapper.text()).toContain('AI assistant')
    expect(wrapper.text()).toContain('not Kevin in person')
  })

  it('renders Norwegian disclosure when language is no', () => {
    const pinia = createPinia()
    setActivePinia(pinia)
    useLangStore().setLanguage('no')
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [{ path: '/privacy-policy', name: 'privacy-policy', component: { template: '<div />' } }],
    })
    const wrapper = mount(AiTransparencyNotice, {
      global: {
        plugins: [pinia, router],
        stubs: {
          Bot: { template: '<span />' },
          Alert: { template: '<div role="note"><slot /></div>' },
          AlertTitle: { template: '<h3><slot /></h3>' },
          AlertDescription: { template: '<div><slot /></div>' },
        },
      },
    })
    expect(wrapper.text()).toContain('AI-assistent')
    expect(wrapper.text()).toContain('ikke Kevin personlig')
  })
})
