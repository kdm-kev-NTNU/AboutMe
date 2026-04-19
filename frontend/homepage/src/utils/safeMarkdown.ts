import MarkdownIt from 'markdown-it'
import DOMPurify from 'dompurify'

/** Single shared instance: html disabled, typographer on; output sanitized before any v-html. */
const md = new MarkdownIt({
  html: false,
  linkify: true,
  typographer: true,
})

/**
 * Renders markdown to HTML and passes it through DOMPurify (defense in depth vs raw HTML / bad URLs).
 */
export function renderSafeMarkdown(source: string): string {
  const raw = md.render(source || '')
  return DOMPurify.sanitize(raw)
}
