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
const URI_SAFE =
  /^(?:(?:https?|mailto):|[^a-z]|[a-z+.-]+(?:[^a-z+.\-:]|$))/i

export function renderSafeMarkdown(source: string): string {
  const raw = md.render(source || '')
  return DOMPurify.sanitize(raw, { ALLOWED_URI_REGEXP: URI_SAFE })
}
