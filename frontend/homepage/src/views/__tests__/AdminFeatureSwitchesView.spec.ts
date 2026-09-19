import { beforeEach, describe, expect, it, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'
import AdminFeatureSwitchesView from '../AdminFeatureSwitchesView.vue'

const customFetchMock = vi.hoisted(() => vi.fn())
const refreshMock = vi.hoisted(() => vi.fn())

vi.mock('@/api/orval-mutator', () => ({
  customFetch: (...args: unknown[]) => customFetchMock(...args),
}))

vi.mock('@/stores/voice-availability', () => ({
  useVoiceAvailabilityStore: () => ({
    refresh: refreshMock,
  }),
}))

vi.mock('@/stores/auth', () => ({
  useAuthStore: () => ({
    restore: vi.fn(),
  }),
}))

const voiceStatus = {
  killSwitchEngaged: false,
  killSwitchState: 'NOT_ENGAGED',
  liveEnabled: true,
  capability: true,
  source: 'FLAGS_POLL',
  lastSyncedAt: '2026-09-19T04:00:00Z',
  posthogConfigured: true,
  posthogReadConfigured: true,
  flagKey: 'aboutme_voice_kill_switch',
  posthogUrl: 'https://eu.posthog.com/project/162788/feature_flags/281659',
}

describe('AdminFeatureSwitchesView', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    customFetchMock.mockReset()
    refreshMock.mockReset()
    customFetchMock.mockResolvedValue({ status: 200, data: voiceStatus })
  })

  function mountView() {
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [
        { path: '/', name: 'home', component: { template: '<div />' } },
        { path: '/admin/tools', name: 'admin-tools', component: { template: '<div />' } },
        {
          path: '/admin/features',
          name: 'admin-features',
          component: AdminFeatureSwitchesView,
        },
      ],
    })
    return mount(AdminFeatureSwitchesView, {
      global: {
        plugins: [router],
        stubs: {
          RouterLink: { template: '<a><slot /></a>', props: ['to'] },
        },
      },
    })
  }

  it('loads voice kill-switch status and shows engage control', async () => {
    const wrapper = mountView()
    await flushPromises()

    expect(customFetchMock).toHaveBeenCalledWith('/admin/tools/features/voice', { method: 'GET' })
    expect(wrapper.text()).toContain('Av (voice kan tilbys)')
    expect(wrapper.find('[data-testid="voice-kill-switch-engage"]').exists()).toBe(true)
    wrapper.unmount()
  })

  it('requires confirmation before engaging the kill switch', async () => {
    const wrapper = mountView()
    await flushPromises()

    await wrapper.find('[data-testid="voice-kill-switch-engage"]').trigger('click')
    expect(wrapper.find('[data-testid="voice-kill-switch-confirm"]').exists()).toBe(true)

    customFetchMock.mockResolvedValueOnce({
      status: 200,
      data: { ...voiceStatus, killSwitchEngaged: true, liveEnabled: false },
    })
    await wrapper.find('[data-testid="voice-kill-switch-confirm"]').trigger('click')
    await flushPromises()

    expect(customFetchMock).toHaveBeenCalledWith(
      '/admin/tools/features/voice/kill-switch',
      expect.objectContaining({
        method: 'POST',
        body: JSON.stringify({ engaged: true }),
      }),
    )
    expect(refreshMock).toHaveBeenCalled()
    expect(wrapper.text()).toContain('På (voice av for besøkende)')
    wrapper.unmount()
  })

  it('shows configuration warning when PostHog management is missing', async () => {
    customFetchMock.mockResolvedValue({
      status: 200,
      data: { ...voiceStatus, posthogConfigured: false },
    })
    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.text()).toContain('PostHog management er ikke konfigurert')
    expect(wrapper.find('[data-testid="voice-kill-switch-engage"]').attributes('disabled')).toBeDefined()
    wrapper.unmount()
  })
})
