import { beforeEach, describe, expect, it } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import AdminLoginButton from '../AdminLoginButton.vue'

describe('AdminLoginButton', () => {
	beforeEach(() => {
		sessionStorage.clear()
		setActivePinia(createPinia())
	})

	const globalStubs = {
		stubs: {
			RouterLink: { template: '<a><slot /></a>', props: ['to'] },
		},
	}

	it('shows admin entry when not authenticated', () => {
		const wrapper = mount(AdminLoginButton, { global: globalStubs })
		expect(wrapper.text()).toContain('Admin')
	})

	it('shows internal tools when session has admin role', async () => {
		sessionStorage.setItem(
			'auth',
			JSON.stringify({ username: 'alice', role: 'ADMIN', basicToken: 'dGVzdA==' }),
		)
		const wrapper = mount(AdminLoginButton, { global: globalStubs })
		await flushPromises()
		expect(wrapper.text()).toContain('Internal tools')
		expect(wrapper.text()).toContain('alice')
	})
})
