import { beforeEach, describe, expect, it } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'

import { mount } from '@vue/test-utils'
import HelloWorld from '../HelloWorld.vue'
import { useLangStore } from '@/stores/lang'

describe('HelloWorld', () => {
  beforeEach(() => {
    localStorage.clear()
    setActivePinia(createPinia())
  })

  it('renders english content by default', () => {
    const wrapper = mount(HelloWorld, {
      props: { msg: 'Hello Vitest' },
      global: { plugins: [createPinia()] },
    })

    expect(wrapper.text()).toContain('Hello Vitest')
    expect(wrapper.text()).toContain("What's next?")
  })

  it('renders norwegian copy when language is set to no', () => {
    const pinia = createPinia()
    setActivePinia(pinia)
    const langStore = useLangStore()
    langStore.setLanguage('no')

    const wrapper = mount(HelloWorld, {
      props: { msg: 'Hei Vitest' },
      global: { plugins: [pinia] },
    })

    expect(wrapper.text()).toContain('Hei Vitest')
    expect(wrapper.text()).toContain('Hva nå?')
  })
})
