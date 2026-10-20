import { describe, expect, it } from 'vitest'
import type { VueWrapper } from '@vue/test-utils'
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

  function cardText(wrapper: VueWrapper, title: string) {
    const card = wrapper.findAll('.shadow-sm').find((node) => node.text().includes(title))
    expect(card).toBeTruthy()
    return card!.text()
  }

  it('renders English project cards with status and detail link', async () => {
    const wrapper = mountSection('en')
    await flushPromises()
    expect(wrapper.text()).toContain('Heathen Army')
    expect(wrapper.text()).toContain('Sep 2026')
    expect(cardText(wrapper, 'Explaining Markets')).toContain('Ongoing')
    expect(cardText(wrapper, 'AboutMe')).toContain('Completed')
    expect(cardText(wrapper, 'AboutMe')).not.toContain('Ongoing')
    expect(wrapper.find('a[href="/projects/heathen-army"]').exists()).toBe(true)
    expect(wrapper.find('a[href="https://kevindmazali.me"]').exists()).toBe(false)
    expect(wrapper.find('a[href="https://krisefikser.app"]').exists()).toBe(true)
    expect(wrapper.find('a[href="https://github.com/kdm-kev-NTNU/explaining-markets-agent"]').exists()).toBe(true)
    expect(
      wrapper.find(
        'a[href="https://explaining-markets-agent-fwbn8nvafzed3fchxufia8.streamlit.app/"]',
      ).exists(),
    ).toBe(true)
    const marketsCard = wrapper
      .findAll('.shadow-sm')
      .find((node) => node.text().includes('Explaining Markets'))
    expect(marketsCard?.find('[data-test="film-demo-card"]').exists()).toBe(true)
  })

  it('renders Norwegian project cards', async () => {
    const wrapper = mountSection('no')
    await flushPromises()
    expect(wrapper.text()).toContain('Detaljer')
    expect(wrapper.text()).toContain('sep. 2026')
    expect(cardText(wrapper, 'Explaining Markets')).toContain('Paagaaende')
    expect(cardText(wrapper, 'AboutMe')).toContain('Fullfort')
    expect(cardText(wrapper, 'AboutMe')).not.toContain('Paagaaende')
  })
})
