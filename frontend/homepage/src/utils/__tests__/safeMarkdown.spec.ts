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

  it('strips javascript: links', () => {
    const html = renderSafeMarkdown('[click](javascript:alert(1))')
    expect(html.toLowerCase()).not.toContain('javascript:')
  })

  it('strips inline event handlers', () => {
    const html = renderSafeMarkdown('<img src=x onerror=alert(1)>')
    expect(html.toLowerCase()).not.toContain('onerror')
  })

  it('strips nested script payloads', () => {
    const html = renderSafeMarkdown('<<script>alert(1)<</script>>')
    expect(html).not.toMatch(/<script/i)
  })
})
