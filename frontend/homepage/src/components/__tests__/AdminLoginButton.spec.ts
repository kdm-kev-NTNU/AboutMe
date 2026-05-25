import { beforeEach, describe, expect, it, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { authLogin, authLogout, authMe } from '@/api/generated/portfolio'
import AdminLoginButton from '../AdminLoginButton.vue'

vi.mock('@/api/generated/portfolio', async (importOriginal) => {
	const mod = await importOriginal<typeof import('@/api/generated/portfolio')>()
	return { ...mod, authLogin: vi.fn(), authLogout: vi.fn(), authMe: vi.fn() }
})

describe('AdminLoginButton', () => {
	beforeEach(() => {
		sessionStorage.clear()
		setActivePinia(createPinia())
		vi.mocked(authLogin).mockReset()
		vi.mocked(authLogout).mockReset()
		vi.mocked(authMe).mockResolvedValue({
			status: 200,
			data: { username: 'x', role: 'ADMIN' },
			headers: new Headers(),
		} as never)
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
			JSON.stringify({ username: 'alice', role: 'ADMIN' }),
		)
		const wrapper = mount(AdminLoginButton, { global: globalStubs })
		await flushPromises()
		expect(wrapper.text()).toContain('Internal tools')
		expect(wrapper.text()).toContain('alice')
	})

	it('submits credentials, closes form, and clears error on success', async () => {
		vi.mocked(authLogin).mockResolvedValue({
			status: 200,
			data: { username: 'bob', role: 'ADMIN' },
			headers: new Headers(),
		})
		const wrapper = mount(AdminLoginButton, { global: globalStubs })
		await wrapper.find('button').trigger('click')
		await wrapper.get('input[type="text"]').setValue('bob')
		await wrapper.get('input[type="password"]').setValue('secret')
		await wrapper.findAll('button').find((b) => b.text().includes('Logg inn'))!.trigger('click')
		await flushPromises()
		expect(wrapper.text()).toContain('Internal tools')
		expect(wrapper.text()).toContain('bob')
	})

	it('shows error message when login fails', async () => {
		vi.mocked(authLogin).mockResolvedValue({
			status: 401,
			data: { error: 'nope' },
			headers: new Headers(),
		})
		const wrapper = mount(AdminLoginButton, { global: globalStubs })
		await wrapper.find('button').trigger('click')
		await wrapper.get('input[type="text"]').setValue('u')
		await wrapper.get('input[type="password"]').setValue('p')
		await wrapper.findAll('button').find((b) => b.text().includes('Logg inn'))!.trigger('click')
		await flushPromises()
		expect(wrapper.text()).toContain('Feil brukernavn eller passord')
	})

	it('logs out from admin toolbar', async () => {
		vi.mocked(authLogout).mockResolvedValue({
			status: 204,
			data: undefined,
			headers: new Headers(),
		} as never)
		sessionStorage.setItem(
			'auth',
			JSON.stringify({ username: 'carol', role: 'ADMIN' }),
		)
		const wrapper = mount(AdminLoginButton, { global: globalStubs })
		await flushPromises()
		await wrapper.findAll('button').find((b) => b.text().includes('Logg ut'))!.trigger('click')
		await flushPromises()
		expect(sessionStorage.getItem('auth')).toBeNull()
		expect(wrapper.text()).toContain('Admin')
	})
})
