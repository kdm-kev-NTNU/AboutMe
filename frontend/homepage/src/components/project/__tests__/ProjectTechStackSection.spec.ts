import { describe, expect, it } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { MotionPlugin } from '@vueuse/motion'
import { createMemoryHistory, createRouter } from 'vue-router'
import ProjectTechStackSection from '../ProjectTechStackSection.vue'
import { useLangStore } from '@/stores/lang'

const routerLinkStub = { template: '<a><slot /></a>' }

function makeRouter() {
  return createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/', component: { template: '<div />' } },
      { path: '/chat', component: { template: '<div />' } },
      { path: '/projects', component: { template: '<div />' } },
      { path: '/project', component: { template: '<div />' } },
    ],
  })
}

describe('ProjectTechStackSection', () => {
  it('renders tech stack title and pillar links in English', async () => {
    const pinia = createPinia()
    setActivePinia(pinia)
    useLangStore().setLanguage('en')
    const router = makeRouter()
    const wrapper = mount(ProjectTechStackSection, {
      global: {
        plugins: [pinia, router, MotionPlugin],
        stubs: { RouterLink: routerLinkStub },
      },
    })
    await flushPromises()
    expect(wrapper.text()).toContain('Tech stack')
    expect(wrapper.text()).toMatch(/AI & RAG|Open chat/i)
  })

  it('renders Norwegian title', async () => {
    const pinia = createPinia()
    setActivePinia(pinia)
    useLangStore().setLanguage('no')
    const router = makeRouter()
    const wrapper = mount(ProjectTechStackSection, {
      global: {
        plugins: [pinia, router, MotionPlugin],
        stubs: { RouterLink: routerLinkStub },
      },
    })
    await flushPromises()
    expect(wrapper.text()).toContain('Teknologistakk')
  })
})
