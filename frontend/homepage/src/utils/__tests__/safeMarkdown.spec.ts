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

  it('does not emit javascript: anchor hrefs', () => {
    const html = renderSafeMarkdown('[click](javascript:alert(1))')
    expect(html).not.toMatch(/<a[^>]+href=["']?javascript:/i)
  })

  it('escapes raw html tags instead of rendering them', () => {
    const html = renderSafeMarkdown('<img src=x onerror=alert(1)>')
    expect(html).not.toMatch(/<img/i)
    expect(html).toContain('&lt;img')
  })

  it('strips nested script payloads', () => {
    const html = renderSafeMarkdown('<<script>alert(1)<</script>>')
    expect(html).not.toMatch(/<script/i)
  })
})
