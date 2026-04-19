import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import SafeMarkdown from '../SafeMarkdown.vue'

describe('SafeMarkdown', () => {
  it('renders markdown as sanitized HTML', () => {
    const wrapper = mount(SafeMarkdown, {
      props: { source: 'Hello **world**' },
    })
    const html = wrapper.find('.safe-markdown').element.innerHTML
    expect(html).toMatch(/strong|b/i)
    expect(html).toContain('world')
  })

  it('treats empty source as empty output', () => {
    const wrapper = mount(SafeMarkdown, {
      props: { source: '' },
    })
    expect(wrapper.find('.safe-markdown').element.innerHTML.trim()).toBe('')
  })

  it('applies contentClass to root', () => {
    const wrapper = mount(SafeMarkdown, {
      props: { source: 'x', contentClass: 'text-red-500' },
    })
    expect(wrapper.find('.safe-markdown').classes()).toContain('text-red-500')
  })
})
