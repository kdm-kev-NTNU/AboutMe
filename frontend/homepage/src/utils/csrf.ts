/** Reads the Spring Security CSRF cookie set for cookie-based admin sessions. */
export function readCsrfToken(): string | null {
  if (typeof document === 'undefined') return null
  const match = document.cookie.match(/(?:^|;\s*)XSRF-TOKEN=([^;]*)/)
  if (!match?.[1]) return null
  try {
    return decodeURIComponent(match[1])
  } catch {
    return match[1]
  }
}

const MUTATING = new Set(['POST', 'PUT', 'DELETE', 'PATCH'])

export function applyCsrfHeader(headers: Headers, method?: string): void {
  const m = (method ?? 'GET').toUpperCase()
  if (!MUTATING.has(m)) return
  const token = readCsrfToken()
  if (token) {
    headers.set('X-XSRF-TOKEN', token)
  }
}
