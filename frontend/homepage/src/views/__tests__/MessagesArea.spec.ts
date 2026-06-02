import { beforeEach, describe, expect, it } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { nextTick } from 'vue'
import { createPinia, setActivePinia } from 'pinia'
import MessagesArea from '../MessagesArea.vue'

describe('MessagesArea', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  const globalStubs = {
    TypewriterAnimation: {
      props: ['text'],
      template: '<span class="typewriter-stub">{{ text }}</span>',
    },
    SafeMarkdown: {
      props: ['source'],
      template: '<div class="safe-markdown-stub">{{ source }}</div>',
    },
    Brain: { template: '<span class="icon-brain" />' },
    UserRound: { template: '<span class="icon-user" />' },
    MessageSquare: { template: '<span class="icon-msq" />' },
  }

  it('renders user and assistant message text', () => {
    const wrapper = mount(MessagesArea, {
      props: {
        messages: [
          { role: 'user', text: 'Hello user' },
          { role: 'assistant', text: 'Hello bot', isNew: false },
        ],
      },
      global: { stubs: globalStubs },
    })
    expect(wrapper.text()).toContain('Hello user')
    expect(wrapper.text()).toContain("Kevin's AI (generated)")
    expect(wrapper.text()).toContain('You')
  })

  it('exposes live region attributes on message log', () => {
    const wrapper = mount(MessagesArea, {
      props: {
        messages: [{ role: 'user', text: 'Hi' }],
        isLoading: true,
      },
      global: { stubs: globalStubs },
    })
    const log = wrapper.find('[role="log"]')
    expect(log.exists()).toBe(true)
    expect(log.attributes('aria-live')).toBe('polite')
    expect(log.attributes('aria-busy')).toBe('true')
  })

  it('shows optional header when showHeader is true', () => {
    const wrapper = mount(MessagesArea, {
      props: {
        messages: [{ role: 'user', text: 'x' }],
        showHeader: true,
        headerText: 'My header',
      },
      global: { stubs: globalStubs },
    })
    expect(wrapper.text()).toContain('My header')
  })

  it('shows loading row when isLoading', () => {
    const wrapper = mount(MessagesArea, {
      props: {
        messages: [],
        isLoading: true,
      },
      global: { stubs: globalStubs },
    })
    expect(wrapper.find('.animate-bounce').exists()).toBe(true)
  })

  it('shows empty state when no messages and not loading', () => {
    const wrapper = mount(MessagesArea, {
      props: { messages: [], isLoading: false },
      global: { stubs: globalStubs },
    })
    expect(wrapper.text()).toContain('No messages yet')
  })

  it('uses typewriter stub for new assistant messages when not read-only', () => {
    const wrapper = mount(MessagesArea, {
      props: {
        messages: [{ role: 'assistant', text: 'Typed', isNew: true }],
        isReadOnly: false,
      },
      global: { stubs: globalStubs },
    })
    expect(wrapper.find('.typewriter-stub').text()).toBe('Typed')
  })

  it('falls back to markdown renderer for new assistant message in read-only mode', () => {
    const wrapper = mount(MessagesArea, {
      props: {
        messages: [{ role: 'assistant', text: 'Read only text', isNew: true }],
        isReadOnly: true,
      },
      global: { stubs: globalStubs },
    })
    expect(wrapper.find('.typewriter-stub').exists()).toBe(false)
    expect(wrapper.find('.safe-markdown-stub').text()).toBe('Read only text')
  })

  it('applies read-only border class on message container', () => {
    const wrapper = mount(MessagesArea, {
      props: {
        messages: [{ role: 'user', text: 'x' }],
        isReadOnly: true,
      },
      global: { stubs: globalStubs },
    })
    const box = wrapper.find('.border-gray-200\\/50')
    expect(box.exists()).toBe(true)
  })

  it('updates scroll position when messages change', async () => {
    const wrapper = mount(MessagesArea, {
      props: {
        messages: [{ role: 'user', text: 'first' }],
      },
      global: { stubs: globalStubs },
    })
    const scrollEl = wrapper.find('.flex-1.overflow-y-auto').element as HTMLElement
    Object.defineProperty(scrollEl, 'scrollHeight', { configurable: true, value: 400 })
    scrollEl.scrollTop = 0
    await wrapper.setProps({
      messages: [
        { role: 'user', text: 'first' },
        { role: 'assistant', text: 'reply', isNew: false },
      ],
    })
    await flushPromises()
    await nextTick()
    expect(scrollEl.scrollTop).toBe(400)
  })
})
