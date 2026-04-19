import { afterEach, describe, expect, it, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import TypewriterAnimation from '../TypewriterAnimation.vue'

describe('TypewriterAnimation', () => {
  afterEach(() => {
    vi.useRealTimers()
  })

  it('emits finished after typing full text with fake timers', async () => {
    vi.useFakeTimers()
    const wrapper = mount(TypewriterAnimation, {
      props: { text: 'ab', speed: 5, autoStart: true },
      global: {
        stubs: { SafeMarkdown: { props: ['source'], template: '<span>{{ source }}</span>' } },
      },
    })
    await flushPromises()
    await vi.runAllTimersAsync()
    expect(wrapper.emitted('finished')).toBeTruthy()
    expect(wrapper.emitted('finished')).toHaveLength(1)
  })

  it('exposed reset clears displayed text', async () => {
    vi.useFakeTimers()
    const wrapper = mount(TypewriterAnimation, {
      props: { text: 'x', speed: 1, autoStart: true },
      global: {
        stubs: { SafeMarkdown: { props: ['source'], template: '<span class="md">{{ source }}</span>' } },
      },
    })
    await flushPromises()
    await vi.runAllTimersAsync()
    ;(wrapper.vm as { reset: () => void }).reset()
    await flushPromises()
    expect(wrapper.find('.md').exists()).toBe(false)
  })
})
