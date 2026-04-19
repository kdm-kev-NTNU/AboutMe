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
import WorkExperienceView from '../WorkExperienceView.vue'
import EducationView from '../EducationView.vue'

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

	it('renders WorkExperienceView with timeline content', async () => {
		const wrapper = mountView(WorkExperienceView)
		await flushPromises()
		expect(wrapper.text()).toContain('Work Experience')
		expect(wrapper.text()).toMatch(/NTNU|Oslo Municipality/i)
	})

	it('renders EducationView with courses section', async () => {
		const wrapper = mountView(EducationView)
		await flushPromises()
		expect(wrapper.text()).toContain('Education')
		expect(wrapper.text()).toMatch(/Courses|Emner/)
	})
})
