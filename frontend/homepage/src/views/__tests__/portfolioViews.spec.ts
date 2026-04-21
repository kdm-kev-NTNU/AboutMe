import { describe, expect, it } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { MotionPlugin } from '@vueuse/motion'
import type { Component } from 'vue'
import AboutView from '../AboutView.vue'
import TechStackView from '../TechStackView.vue'
import PrivacyPolicyView from '../PrivacyPolicyView.vue'
import { useLangStore } from '@/stores/lang'
import ProjectsView from '../ProjectsView.vue'
import CareerView from '../CareerView.vue'
import FutureWorkView from '../FutureWorkView.vue'

describe('portfolio views (smoke)', () => {
	function mountView(component: Component) {
		const pinia = createPinia()
		setActivePinia(pinia)
		// Reset persisted language so tests do not inherit 'no' from earlier cases in this file.
		useLangStore().setLanguage('en')
		return mount(component, {
			global: { plugins: [pinia, MotionPlugin] },
		})
	}

	it('renders AboutView', () => {
		const wrapper = mountView(AboutView)
		expect(wrapper.text()).toContain('about page')
	})

	it('renders TechStackView in English by default', async () => {
		const wrapper = mountView(TechStackView)
		await flushPromises()
		expect(wrapper.text()).toContain('Tech stack')
		expect(wrapper.text()).toMatch(/Spring AI|Backend/i)
	})

	it('renders TechStackView in Norwegian when language is no', async () => {
		const pinia = createPinia()
		setActivePinia(pinia)
		useLangStore().setLanguage('no')
		const wrapper = mount(TechStackView, { global: { plugins: [pinia, MotionPlugin] } })
		await flushPromises()
		expect(wrapper.text()).toContain('Teknologistakk')
		expect(wrapper.text()).toMatch(/Spring AI|Backend/i)
	})

	it('renders PrivacyPolicyView in English by default', async () => {
		const wrapper = mountView(PrivacyPolicyView)
		await flushPromises()
		expect(wrapper.text()).toContain('Privacy Policy')
		expect(wrapper.text()).toMatch(/cookies|Chat/i)
	})

	it('renders PrivacyPolicyView in Norwegian when language is no', async () => {
		const pinia = createPinia()
		setActivePinia(pinia)
		useLangStore().setLanguage('no')
		const wrapper = mount(PrivacyPolicyView, { global: { plugins: [pinia] } })
		await flushPromises()
		expect(wrapper.text()).toContain('Personvernerklæring')
		expect(wrapper.text()).toMatch(/informasjonskapsler|Chat/i)
	})

	it('renders ProjectsView with project grid', async () => {
		const wrapper = mountView(ProjectsView)
		await flushPromises()
		expect(wrapper.text()).toContain('Projects')
		expect(wrapper.find('h1').exists()).toBe(true)
	})

	it('renders CareerView with work, education, and courses', async () => {
		const wrapper = mountView(CareerView)
		await flushPromises()
		expect(wrapper.text()).toContain('Experience & education')
		expect(wrapper.text()).toContain('Work Experience')
		expect(wrapper.text()).toMatch(/NTNU|Oslo Municipality/i)
		expect(wrapper.text()).toContain('Education')
		expect(wrapper.text()).toMatch(/Courses|Emner/)
	})

  it('renders CareerView in Norwegian with translated section labels', async () => {
    const pinia = createPinia()
    setActivePinia(pinia)
    useLangStore().setLanguage('no')
    const wrapper = mount(CareerView, { global: { plugins: [pinia, MotionPlugin] } })
    await flushPromises()
    expect(wrapper.text()).toContain('Erfaring og utdanning')
    expect(wrapper.text()).toContain('Arbeidserfaring')
    expect(wrapper.text()).toContain('Utdanning')
    expect(wrapper.text()).toContain('Emner')
    expect(wrapper.text()).toMatch(/studiepoeng|Karakter/)
  })

  it('renders FutureWorkView in English with references', async () => {
    const wrapper = mountView(FutureWorkView)
    await flushPromises()
    expect(wrapper.text()).toContain('Future work and improvements')
    expect(wrapper.text()).toContain('References (arXiv)')
    const links = wrapper.findAll('a[href^="https://arxiv.org/abs/"]')
    expect(links.length).toBeGreaterThan(0)
  })

  it('renders FutureWorkView in Norwegian with translated copy', async () => {
    const pinia = createPinia()
    setActivePinia(pinia)
    useLangStore().setLanguage('no')
    const wrapper = mount(FutureWorkView, { global: { plugins: [pinia, MotionPlugin] } })
    await flushPromises()
    expect(wrapper.text()).toContain('Videre arbeid og forbedringer')
    expect(wrapper.text()).toContain('Referanser (arXiv)')
    expect(wrapper.text()).toContain('Lokal modell og Obsidian-journal')
  })
})
