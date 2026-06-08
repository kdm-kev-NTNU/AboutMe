import { describe, expect, it } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'
import { MotionPlugin } from '@vueuse/motion'
import ProjectCardsSection from '../ProjectCardsSection.vue'
import { useLangStore } from '@/stores/lang'

describe('ProjectCardsSection', () => {
  function mountSection(lang: 'en' | 'no' = 'en') {
    const pinia = createPinia()
    setActivePinia(pinia)
    useLangStore().setLanguage(lang)
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [
        { path: '/', component: { template: '<div />' } },
        { path: '/projects/heathen-army', component: { template: '<div />' } },
      ],
    })
    return mount(ProjectCardsSection, {
      global: {
        plugins: [pinia, router, MotionPlugin],
        stubs: {
          FilmDemoCard: { template: '<div data-test="film-demo-card" />' },
        },
      },
    })
  }

  it('renders English project cards with status and detail link', async () => {
    const wrapper = mountSection('en')
    await flushPromises()
    expect(wrapper.text()).toContain('Heathen Army')
    expect(wrapper.text()).toContain('AboutMe')
    expect(wrapper.text()).toContain('Ongoing')
    expect(wrapper.text()).toContain('Completed')
    expect(wrapper.find('a[href="/projects/heathen-army"]').exists()).toBe(true)
  })

  it('renders Norwegian project cards', async () => {
    const wrapper = mountSection('no')
    await flushPromises()
    expect(wrapper.text()).toContain('Detaljer')
    expect(wrapper.text()).toContain('Paagaaende')
  })
})
