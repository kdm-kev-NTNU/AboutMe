import { describe, expect, it } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import type { Component } from 'vue'
import AboutView from '../AboutView.vue'
import TechStackView from '../TechStackView.vue'
import PrivacyPolicyView from '../PrivacyPolicyView.vue'
import ProjectsView from '../ProjectsView.vue'

describe('portfolio views (smoke)', () => {
	function mountView(component: Component) {
		const pinia = createPinia()
		setActivePinia(pinia)
		return mount(component, {
			global: { plugins: [pinia] },
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

	it('renders PrivacyPolicyView in English by default', async () => {
		const wrapper = mountView(PrivacyPolicyView)
		await flushPromises()
		expect(wrapper.text()).toContain('Privacy Policy')
		expect(wrapper.text()).toMatch(/cookies|Chat/i)
	})

	it('renders ProjectsView with project grid', async () => {
		const wrapper = mountView(ProjectsView)
		await flushPromises()
		expect(wrapper.text()).toContain('Projects')
		expect(wrapper.find('h1').exists()).toBe(true)
	})
})
