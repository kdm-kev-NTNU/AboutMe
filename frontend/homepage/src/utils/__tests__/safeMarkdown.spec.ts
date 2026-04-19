import { describe, expect, it } from 'vitest'
import { renderSafeMarkdown } from '../safeMarkdown'

describe('renderSafeMarkdown', () => {
  it('renders markdown and strips unsafe html', () => {
    const html = renderSafeMarkdown('Hello **world**')
    expect(html).toContain('Hello')
    expect(html).toContain('world')
    expect(html).not.toMatch(/<script/i)
  })

  it('treats empty input as empty string', () => {
    expect(renderSafeMarkdown('')).toMatch(/^\s*$/)
  })
})
